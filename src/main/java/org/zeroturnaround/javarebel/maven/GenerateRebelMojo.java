package org.zeroturnaround.javarebel.maven;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.interpolation.ObjectBasedValueSource;
import org.codehaus.plexus.util.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.zeroturnaround.javarebel.maven.model.RebelClasspath;
import org.zeroturnaround.javarebel.maven.model.RebelClasspathResource;
import org.zeroturnaround.javarebel.maven.model.RebelResource;
import org.zeroturnaround.javarebel.maven.model.RebelWar;
import org.zeroturnaround.javarebel.maven.model.RebelWeb;
import org.zeroturnaround.javarebel.maven.model.RebelWebResource;
import org.zeroturnaround.javarebel.maven.util.SystemUtils;

/**
 * Generate rebel.xml
 *
 * @goal generate
 * @phase process-resources
 * @threadSafe true
 */
@SuppressWarnings({"JavaDoc", "unused"})
public class GenerateRebelMojo extends AbstractMojo {

  private static final String[] DEFAULT_INCLUDES = {"**/**"};

  private static final Set<String> JAR_PACKAGING = new HashSet<String>();
  private static final Set<String> WAR_PACKAGING = new HashSet<String>();
  private static final String POM_PACKAGING = "pom";
  public static final String CHARTSET_UTF8 = "UTF-8";

  static {
    JAR_PACKAGING.addAll(Arrays.asList("jar", "ejb", "ejb3", "nbm", "hk2-jar", "bundle", "eclipse-plugin", "atlassian-plugin"));
    WAR_PACKAGING.addAll(Arrays.asList("war", "grails-app"));
  }

  /**
   * The maven project.
   *
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  private MavenProject project;

  /**
   * Packaging of project.
   *
   * @parameter expression="${project.packaging}"
   * @required
   */
  private String packaging;

  /**
   * The directory containing generated classes.
   *
   * @parameter expression="${project.build.outputDirectory}"
   * @required
   */
  private File classesDirectory;

  /**
   * Root directory for all html/jsp etc files.
   *
   * @parameter expression="${basedir}/src/main/webapp"
   * @required
   */
  private File warSourceDirectory;

  /**
   * The directory where the webapp is built.
   *
   * @parameter expression="${project.build.directory}/${project.build.finalName}"
   * @required
   */
  private File webappDirectory;

  /**
   * Rebel classpath configuration.
   *
   * @parameter
   */
  private RebelClasspath classpath;

  /**
   * Rebel war configuration.
   *
   * @parameter
   */
  private RebelWar war;

  /**
   * Rebel web configuration.
   *
   * @parameter
   */
  private RebelWeb web;

  /**
   * Root path of maven projects.
   *
   * @parameter
   */
  private String rootPath;

  /**
   * Relative path to root of current project.
   *
   * @parameter
   */
  private String relativePath;

  /**
   * Root relative path.
   *
   * @parameter
   */
  private String rootRelativePath;

  /**
   * Target directory for generated rebel.xml and rebel-remote.xml files.
   *
   * @parameter expression="${rebel.xml.dir}" default-value="${project.build.outputDirectory}"
   * @required
   */
  private File rebelXmlDirectory;

  /**
   * If set to true rebel plugin will write generated xml at info level.
   *
   * @parameter expression="${rebel.generate.show}" default-value="false"
   */
  private boolean showGenerated;

  /**
   * If set to true rebel plugin will add resources directories to rebel.xml classpath.
   *
   * @parameter default-value="false"
   */
  private boolean addResourcesDirToRebelXml;

  /**
   * If set to true rebel plugin will generate rebel.xml and rebel-remote.xml (if 'generateRebelRemote' is set) on each build.
   * Otherwise the timestamps of rebel.xml and pom.xml are compared. The rebel-remote.xml would then be generated nevertheless.
   *
   * @parameter default-value="false"
   */
  private boolean alwaysGenerate;

  /**
   * Indicates whether the default web element will be generated or not. This parameter has effect only when {@link #generateDefaultElements} is <code>true</code>.
   *
   * @parameter default-value="true"
   */
  private boolean generateDefaultWeb;

  /**
   * Indicates whether the default classpath element will be generated or not. This parameter has effect only when {@link #generateDefaultElements} is <code>true</code>.
   *
   * @parameter default-value="true"
   */
  private boolean generateDefaultClasspath;

  /**
   * If set to false rebel plugin will not generate default elements in rebel.xml.
   *
   * @parameter default-value="true"
   */
  private boolean generateDefaultElements;

  /**
   * Indicates whether the rebel-remote.xml file will be generated or not.
   *
   * @parameter default-value="false"
   */
  private boolean generateRebelRemote;

  /**
   * If set to true rebel plugin execution will be skipped.
   *
   * @parameter default-value="false"
   */
  private boolean skip;

  /** @component */
  private BuildContext buildContext;

  /** @parameter default-value="${mojoExecution}" */
  private MojoExecution execution;

  /** @parameter default-value="${session}" */
  private MavenSession session;

  /** @parameter default-value="${project.build.directory}" */
  private String projectBuildDir;


  public void execute() throws MojoExecutionException {
    if (this.rootPath == null) {
      // relative paths generation is OFF
      this.rootPath = project.getBasedir().getAbsolutePath();
      this.relativePath = ".";

    }
    else {
      if (this.rootRelativePath == null) {
        // manual config mode
        if (this.relativePath == null) {
          // have <relativePath> point up to maven root directory
          this.relativePath = ".";
        }
      }
      else {
        // use auto-detection & ignore all <relativePath> variables
        try {
          this.relativePath = calculateRelativePath(calculatePathToRoot(findBaseDirOfMainProject(), this.project.getBasedir()), rootRelativePath);
          getLog().info("auto-detected relative path to main project : " + this.relativePath);
        }
        catch (IOException ex) {
          getLog().debug("Error during relative path calculation", ex);
          getLog().error("ERROR! Path defined in <rootRelativePath> is not a valid relative path with regard to root module's path. Falling back to absolute paths.");
        }
      }
    }

    // do not generate JRebel configuration files if skip parameter or 'performRelease' system property is set to true
    try {
      if (this.skip || Boolean.getBoolean("performRelease")) {
        getLog().info("Skipped generating JRebel configuration files.");
        return;
      }
    }
    catch (SecurityException ignore) {
      // ignore exception which potentially can be thrown by Boolean.getBoolean for security options
    }

    // if generateDefaultElements is set to false, then disable default classpath and web elements no matter what are their initial values.
    if (!this.generateDefaultElements) {
      this.generateDefaultClasspath = false;
      this.generateDefaultWeb = false;
    }

    File rebelXmlFile = new File(rebelXmlDirectory, "rebel.xml").getAbsoluteFile();
    File rebelRemoteXmlFile = new File(rebelXmlDirectory, "rebel-remote.xml").getAbsoluteFile();
    File pomXmlFile = getProject().getFile();

    if (generateRebelRemote && (alwaysGenerate || !rebelRemoteXmlFile.exists())) {
      generateRebelRemoteXmlFile(rebelRemoteXmlFile);
    }

    if (!alwaysGenerate && rebelXmlFile.exists() && pomXmlFile.exists() && rebelXmlFile.lastModified() > pomXmlFile.lastModified()) {
      return;
    }

    getLog().info("Processing " + getProject().getGroupId() + ":" + getProject().getArtifactId() + " with packaging " + packaging);

    RebelXmlBuilder builder = null;
    if (WAR_PACKAGING.contains(packaging)) {
      builder = buildWar();
    }
    else if (JAR_PACKAGING.contains(packaging)) {
      builder = buildJar();
    }
    else if (POM_PACKAGING.equals(packaging)) {
      getLog().info("Skipped generating rebel.xml for parent.");
    }
    else {
      getLog().warn("Unsupported packaging type: " + packaging);
    }

    if (builder != null) {
      Writer w = null;
      if (this.showGenerated) {
        try {
          w = new StringWriter();
          builder.writeXml(w);
          getLog().info(w.toString());
        }
        catch (IOException e) {
          getLog().debug("Detected exception during 'showGenerated' : ", e);
        }
      }

      try {
        rebelXmlDirectory.mkdirs();
        w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(rebelXmlFile), CHARTSET_UTF8));
        builder.writeXml(w);
      }
      catch (IOException e) {
        throw new MojoExecutionException("Failed writing rebel.xml", e);
      }
      finally {
        IOUtils.closeQuietly(w);
        if (this.buildContext != null) {
          // safeguard for null buildContext. Can it be null, actually? E.g when field is not injected.
          this.buildContext.refresh(rebelXmlFile);
        }
      }
    }
  }

  /**
   * Generates rebel-remote.xml.
   * @throws MojoExecutionException
   */
  private void generateRebelRemoteXmlFile(File rebelRemoteXmlFile) throws MojoExecutionException {
    getLog().info("Generating rebel-remote.xml on : " + rebelRemoteXmlFile.getAbsolutePath());

    Writer w = null;
    if (this.showGenerated) {
      try {
        w = new StringWriter();
        generateRebelRemoteXml(w);
        getLog().info(w.toString());
      } catch (IOException e) {
        getLog().debug("Detected exception during 'showGenerated' : ", e);
      }
    }

    try {
      rebelXmlDirectory.mkdirs();
      w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(rebelRemoteXmlFile), CHARTSET_UTF8));
      generateRebelRemoteXml(w);
    } catch (IOException e) {
      throw new MojoExecutionException("Failed writing rebel-remote.xml", e);
    } finally {
      IOUtils.closeQuietly(w);
      if (this.buildContext != null) {
        this.buildContext.refresh(rebelRemoteXmlFile);
      }
    }
  }

  /**
   * Generates rebel-remote.xml content and write it into the provided Writer.
   */
  void generateRebelRemoteXml(Writer w) throws IOException {
    w.write(String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<rebel-remote xmlns=\"http://www.zeroturnaround.com/rebel/remote\">\n" +
        "    <id>%s</id>\n" +
        "</rebel-remote>",
        SystemUtils.ensurePathAndURLSafeName(String.format("%s.%s", getProject().getGroupId(), getProject().getArtifactId())))
    );
  }

  /**
   * Build war configuration.
   *
   * @return
   * @throws MojoExecutionException
   */
  private RebelXmlBuilder buildWar() throws MojoExecutionException {
    RebelXmlBuilder builder = createXmlBuilder();

    buildWeb(builder);
    buildClasspath(builder);

    if (war != null) {
      war.setPath(fixFilePath(war.getPath()));
      builder.setWar(war);
    }

    return builder;
  }

  /**
   * Build jar configuration.
   *
   * @return
   * @throws MojoExecutionException
   */
  private RebelXmlBuilder buildJar() throws MojoExecutionException {
    RebelXmlBuilder builder = createXmlBuilder();
    buildClasspath(builder);

    // if user has specified any web elements, then let's generate these in the result file.
    if (web != null && web.getResources() != null && web.getResources().length != 0) {
      generateDefaultWeb = false; // but don't generate default web element because this folder is most likely missing.
      buildWeb(builder);
    }

    return builder;
  }

  /**
   * Create a new instance of RebelXmlBuilder with the version of maven and JRebel plugin used during build
   *
   * @return new instance of RebelXmlBuilder
   */
  private RebelXmlBuilder createXmlBuilder() {
    return new RebelXmlBuilder(
        session.getSystemProperties().getProperty("maven.version"),
        execution.getVersion(),
        projectBuildDir);
  }

  private void buildClasspath(RebelXmlBuilder builder) throws MojoExecutionException {
    boolean addDefaultAsFirst = true;
    RebelClasspathResource defaultClasspath = null;
    // check if there is a element with no dir/jar/dirset/jarset set. if there
    // is then don't put default classpath as
    // first but put it where this element was.
    if (classpath != null) {
      RebelClasspathResource[] resources = classpath.getResources();
      if (resources != null && resources.length > 0) {
        for (RebelClasspathResource r : resources) {
          if (!r.isTargetSet()) {
            addDefaultAsFirst = false;
            defaultClasspath = r;
            break;
          }
        }
      }
    }

    if (addDefaultAsFirst) {
      buildDefaultClasspath(builder, defaultClasspath);
    }

    if (classpath != null) {
      builder.setFallbackClasspath(classpath.getFallback());
      RebelClasspathResource[] resources = classpath.getResources();
      if (resources != null && resources.length > 0) {
        for (RebelClasspathResource r : resources) {
          if (r.isTargetSet()) {
            if (r.getDirectory() != null) {
              r.setDirectory(fixFilePath(r.getDirectory()));
              builder.addClasspathDir(r);
            }
            if (r.getJar() != null) {
              r.setJar(fixFilePath(r.getJar()));
              builder.addClasspathJar(r);
            }
            if (r.getJarset() != null) {
              r.setJarset(fixFilePath(r.getJarset()));
              builder.addClasspathJarset(r);
            }
            if (r.getDirset() != null) {
              r.setDirset(fixFilePath(r.getDirset()));
              builder.addClasspathDirset(r);
            }
          }
          else {
            buildDefaultClasspath(builder, r);
          }
        }
      }
    }
  }

  private void buildDefaultClasspath(RebelXmlBuilder builder, RebelClasspathResource defaultClasspath) throws MojoExecutionException {
    if (!generateDefaultClasspath) {
      return;
    }
    if (addResourcesDirToRebelXml) {
      buildDefaultClasspathResources(builder);
    }

    // project output directory
    RebelClasspathResource r = new RebelClasspathResource();
    r.setDirectory(fixFilePath(classesDirectory));
    if (defaultClasspath != null) {
      r.setIncludes(defaultClasspath.getIncludes());
      r.setExcludes(defaultClasspath.getExcludes());
    }
    builder.addClasspathDir(r);
  }

  private void buildDefaultClasspathResources(RebelXmlBuilder builder) throws MojoExecutionException {
    boolean overwrite = Boolean.valueOf(getPluginSetting(getProject(), "org.apache.maven.plugins:maven-resources-plugin", "overwrite", "false"));

    RebelClasspathResource rebelClassPathResource;

    List<Resource> resources = getProject().getResources();
    //if resources plugin is set to overwrite then reverse the order of resources
    if (overwrite) {
      Collections.reverse(resources);
    }
    for (Resource resource : resources) {
      File dir = new File(resource.getDirectory());
      if (!dir.isAbsolute()) {
        dir = new File(getProject().getBasedir(), resource.getDirectory());
      }
      //skip directories that don't exist
      if (!dir.exists() || !dir.isDirectory()) {
        continue;
      }

      rebelClassPathResource = new RebelClasspathResource();

      if (resource.isFiltering() || resource.getTargetPath() != null) {
        if (!handleResourceAsInclude(rebelClassPathResource, resource)) {
          continue;
        }
        //point filtered resources to target directory
        rebelClassPathResource.setDirectory(fixFilePath(classesDirectory));
        //add target path as prefix to includes
        if (resource.getTargetPath() != null) {
          setIncludePrefix(rebelClassPathResource.getIncludes(), resource.getTargetPath());
        }
      }
      else {
        rebelClassPathResource.setDirectory(fixFilePath(resource.getDirectory()));
        rebelClassPathResource.setExcludes(resource.getExcludes());
        rebelClassPathResource.setIncludes(resource.getIncludes());
      }

      builder.addClasspathDir(rebelClassPathResource);
    }
  }

  private void setIncludePrefix(List<String> includes, String prefix) {
    if (!prefix.endsWith("/")) {
      prefix = prefix + "/";
    }
    for (int i = 0; i < includes.size(); i++) {
      includes.set(i, prefix + includes.get(i));
    }
  }

  /**
   * Set includes & excludes for filtered resources.
   */
  private boolean handleResourceAsInclude(RebelResource rebelResource, Resource resource) {
    File dir = new File(resource.getDirectory());
    if (!dir.isAbsolute()) {
      dir = new File(getProject().getBasedir(), resource.getDirectory());
    }
    //if directory does not exist then exclude all
    if (!dir.exists() || !dir.isDirectory()) {
      return false;
    }

    resource.setDirectory(dir.getAbsolutePath());

    String[] files = getFilesToCopy(resource);
    if (files.length > 0) {
      //only include files that come from this directory
      List<String> includedFiles = new ArrayList<String>();
      for (String file : files) {
        includedFiles.add(StringUtils.replace(file, '\\', '/'));
      }
      rebelResource.setIncludes(includedFiles);
    }
    else {
      //there weren't any matching files
      return false;
    }

    return true;
  }

  /**
   * Taken from war plugin.
   *
   * @param resource
   * @return array of file names that would be copied from specified resource
   */
  private String[] getFilesToCopy(Resource resource) {
    DirectoryScanner scanner = new DirectoryScanner();
    scanner.setBasedir(resource.getDirectory());
    if (resource.getIncludes() != null && !resource.getIncludes().isEmpty()) {
      scanner.setIncludes(resource.getIncludes().toArray(new String[0]));
    }
    else {
      scanner.setIncludes(DEFAULT_INCLUDES);
    }
    if (resource.getExcludes() != null && !resource.getExcludes().isEmpty()) {
      scanner.setExcludes(resource.getExcludes().toArray(new String[0]));
    }

    scanner.addDefaultExcludes();

    scanner.scan();

    return scanner.getIncludedFiles();
  }

  private void buildWeb(RebelXmlBuilder builder) throws MojoExecutionException {
    boolean addDefaultAsFirst = true;
    RebelWebResource defaultWeb = null;
    if (web != null) {
      RebelWebResource[] resources = web.getResources();
      if (resources != null && resources.length > 0) {
        for (RebelWebResource r : resources) {
          if (r.getDirectory() == null && r.getTarget() == null) {
            defaultWeb = r;
            addDefaultAsFirst = false;
            break;
          }
        }
      }
    }

    if (addDefaultAsFirst) {
      buildDefaultWeb(builder, defaultWeb);
    }

    if (web != null) {
      RebelWebResource[] resources = web.getResources();
      if (resources != null && resources.length > 0) {
        for (RebelWebResource r : resources) {
          if (r.getDirectory() == null && r.getTarget() == null) {
            buildDefaultWeb(builder, r);
            continue;
          }
          r.setDirectory(fixFilePath(r.getDirectory()));
          builder.addWebresource(r);
        }
      }
    }
  }

  private void buildDefaultWeb(RebelXmlBuilder builder, RebelWebResource defaultWeb) throws MojoExecutionException {
    if (!generateDefaultWeb) {
      return;
    }
    Xpp3Dom warPluginConf = getPluginConfigurationDom(getProject(), "org.apache.maven.plugins:maven-war-plugin");
    if (warPluginConf != null) {
      //override defaults with configuration from war plugin
      Xpp3Dom warSourceNode = warPluginConf.getChild("warSourceDirectory");
      if (warSourceNode != null && warSourceNode.getValue() != null) {
        warSourceDirectory = new File(getValue(getProject(), warSourceNode));
      }

      Xpp3Dom webappDirNode = warPluginConf.getChild("webappDirectory");
      if (webappDirNode != null && webappDirNode.getValue() != null) {
        webappDirectory = new File(getValue(getProject(), webappDirNode));
      }

      //handle web resources configured for war plugin
      Xpp3Dom wr = warPluginConf.getChild("webResources");
      if (wr != null) {
        List<Resource> resources = parseWarResources(wr);
        //web resources overwrite each other
        Collections.reverse(resources);

        for (Resource resource : resources) {
          File dir = new File(resource.getDirectory());
          if (!dir.isAbsolute()) {
            dir = new File(getProject().getBasedir(), resource.getDirectory());
          }
          //skip directories that don't exist
          if (!dir.exists() || !dir.isDirectory()) {
            continue;
          }

          if (resource.getTargetPath() == null) {
            resource.setTargetPath("/");
          }
          if (!resource.getTargetPath().endsWith("/")) {
            resource.setTargetPath(resource.getTargetPath() + "/");
          }

          //web resources that go under WEB-INF/classes should be placed to classpath
          if (resource.getTargetPath().startsWith("WEB-INF/classes/")) {
            if (addResourcesDirToRebelXml) {
              String target = resource.getTargetPath().substring("WEB-INF/classes/".length());
              RebelClasspathResource rc = new RebelClasspathResource();
              if (resource.isFiltering() || StringUtils.isNotEmpty(target)) {
                if (!handleResourceAsInclude(rc, resource)) {
                  continue;
                }
                rc.setDirectory(fixFilePath(new File(webappDirectory, "WEB-INF/classes")));
                if (StringUtils.isNotEmpty(target)) {
                  setIncludePrefix(rc.getIncludes(), target);
                }
              }
              else {
                rc.setDirectory(fixFilePath(resource.getDirectory()));
                rc.setExcludes(resource.getExcludes());
                rc.setIncludes(resource.getIncludes());
              }

              builder.addClasspathDir(rc);
            }
          }
          else {
            RebelWebResource r = new RebelWebResource();
            r.setTarget(resource.getTargetPath());

            if (resource.isFiltering()) {
              r.setDirectory(fixFilePath(new File(webappDirectory, resource.getTargetPath())));
              if (!handleResourceAsInclude(r, resource)) {
                continue;
              }
            }
            else {
              r.setDirectory(fixFilePath(resource.getDirectory()));
              r.setExcludes(resource.getExcludes());
              r.setIncludes(resource.getIncludes());
            }
            builder.addWebresource(r);
          }
        }
      }
    }

    RebelWebResource r = new RebelWebResource();
    r.setTarget("/");
    r.setDirectory(fixFilePath(warSourceDirectory));
    if (defaultWeb != null) {
      r.setIncludes(defaultWeb.getIncludes());
      r.setExcludes(defaultWeb.getExcludes());
    }
    builder.addWebresource(r);
  }

  /**
   * Parse resources node content.
   *
   * @param warResourcesNode
   * @return
   */
  private List<Resource> parseWarResources(Xpp3Dom warResourcesNode) {
    List<Resource> resources = new ArrayList<Resource>();
    Xpp3Dom[] resourceNodes = warResourcesNode.getChildren("resource");
    for (Xpp3Dom resourceNode : resourceNodes) {
      if (resourceNode == null || resourceNode.getChild("directory") == null) {
        continue;
      }
      resources.add(parseResourceNode(resourceNode));
    }

    return resources;
  }

  /**
   * Parse resouce node content.
   *
   * @param rn resource node content
   * @return resource parsed
   */
  private Resource parseResourceNode(Xpp3Dom rn) {
    Resource r = new Resource();
    if (rn.getChild("directory") != null) {
      r.setDirectory(getValue(getProject(), rn.getChild("directory")));
    }
    if (rn.getChild("filtering") != null) {
      r.setFiltering((Boolean.valueOf(getValue(getProject(), rn.getChild("filtering")))));
    }
    if (rn.getChild("targetPath") != null) {
      r.setTargetPath(rn.getChild("targetPath").getValue());
    }

    if (rn.getChild("excludes") != null) {
      List<String> excludes = new ArrayList<String>();
      Xpp3Dom[] excludeNodes = rn.getChild("excludes").getChildren("exclude");
      for (Xpp3Dom excludeNode : excludeNodes) {
        if (excludeNode != null && excludeNode.getValue() != null) {
          excludes.add(getValue(getProject(), excludeNode));
        }
      }
      r.setExcludes(excludes);
    }
    if (rn.getChild("includes") != null) {
      List<String> includes = new ArrayList<String>();
      Xpp3Dom[] includeNodes = rn.getChild("includes").getChildren("include");
      for (Xpp3Dom includeNode : includeNodes) {
        if (includeNode != null && includeNode.getValue() != null) {
          includes.add(getValue(getProject(), includeNode));
        }
      }
      r.setIncludes(includes);
    }

    return r;
  }

  /**
   * Taken from eclipse plugin. Search for the configuration Xpp3 dom of an other plugin.
   *
   * @param project  the current maven project to get the configuration from.
   * @param pluginId the group id and artifact id of the plugin to search for
   * @return the value of the plugin configuration
   */
  private static Xpp3Dom getPluginConfigurationDom(MavenProject project, String pluginId) {
    Plugin plugin = project.getBuild().getPluginsAsMap().get(pluginId);
    if (plugin != null) {
      return (Xpp3Dom) plugin.getConfiguration();
    }

    return null;
  }

  /**
   * Search for a configuration setting of an other plugin.
   *
   * @param project      the current maven project to get the configuration from.
   * @param pluginId     the group id and artifact id of the plugin to search for
   * @param optionName   the option to get from the configuration
   * @param defaultValue the default value if the configuration was not found
   * @return the value of the option configured in the plugin configuration
   */
  private String getPluginSetting(MavenProject project, String pluginId, String optionName, String defaultValue) {
    Xpp3Dom dom = getPluginConfigurationDom(project, pluginId);
    if (dom != null && dom.getChild(optionName) != null) {
      return getValue(project, dom.getChild(optionName));
    }
    return defaultValue;
  }

  private String getValue(MavenProject project, Xpp3Dom dom) {
    String value = dom.getValue();

    return getValue(project, value);
  }

  private String getValue(MavenProject project, String value) {
    if (value != null && value.contains("$")) {
      return getInterpolatorValue(project, value);
    }

    return value;
  }

  /**
   * Maven versions prior to 2.0.9 don't interpolate all ${project.*} values, so we'll need to do it ourself.
   *
   * @param project
   * @param value
   * @return
   */
  private String getInterpolatorValue(MavenProject project, String value) {
    RegexBasedInterpolator interpolator = new RegexBasedInterpolator();
    interpolator.addValueSource(new ObjectBasedValueSource(project));

    try {
      return interpolator.interpolate(value, "project");
    }
    catch (Exception e) {
      getLog().debug("Detected exception during 'getInterpolatorValue' : ", e);
      e.printStackTrace();
    }
    return value;
  }

  protected String fixFilePath(String path) throws MojoExecutionException {
    return fixFilePath(new File(path));
  }

  /**
   * Returns path expressed through rootPath and relativePath.
   *
   * @param file to be fixed
   * @return fixed path
   * @throws MojoExecutionException if something goes wrong
   */
  protected String fixFilePath(File file) throws MojoExecutionException {
    File baseDir = getProject().getFile().getParentFile();

    if (file.isAbsolute() && !isRelativeToPath(new File(baseDir, getRelativePath()), file)) {
      return StringUtils.replace(getCanonicalPath(file), '\\', '/');
    }

    if (!file.isAbsolute()) {
      file = new File(baseDir, file.getPath());
    }

    String relative = getRelativePath(new File(baseDir, getRelativePath()), file);
    if (!(new File(relative)).isAbsolute()) {
      return StringUtils.replace(getRootPath(), '\\', '/') + "/" + relative;
    }
    //relative path was outside baseDir

    //if root path is absolute then try to get a path relative to root
    if ((new File(getRootPath())).isAbsolute()) {
      String s = getRelativePath(new File(getRootPath()), file);

      if (!(new File(s)).isAbsolute()) {
        return StringUtils.replace(getRootPath(), '\\', '/') + "/" + s;
      }
      else {
        // root path and the calculated path are absolute, so 
        // just return calculated path
        return s;
      }
    }

    //return absolute path to file
    return StringUtils.replace(file.getAbsolutePath(), '\\', '/');
  }

  private static String getRelativePath(File baseDir, File file) throws MojoExecutionException {

    // Avoid the common prefix problem (see case 17005)
    //  if:
    //    baseDir = /myProject/web-module/.
    //    file    = /myProject/web-module-shared/something/something/something
    //  then basedirpath cannot be a prefix of the absolutePath, or the relative path will be calculated incorrectly! 
    //  This problem is avoided by adding a trailing slash to basedirpath. 
    String basedirpath = getCanonicalPath(baseDir) + File.separator;
    String absolutePath = getCanonicalPath(file);

    String relative;

    if (absolutePath.equals(basedirpath)) {
      relative = ".";
    }
    else if (absolutePath.startsWith(basedirpath)) {
      relative = absolutePath.substring(basedirpath.length());
    }
    else {
      relative = absolutePath;
    }

    relative = StringUtils.replace(relative, '\\', '/');

    return relative;
  }

  private static boolean isRelativeToPath(File baseDir, File file) throws MojoExecutionException {
    String basedirpath = getCanonicalPath(baseDir);
    String absolutePath = getCanonicalPath(file);

    return absolutePath.startsWith(basedirpath);
  }

  private static String getCanonicalPath(File file) throws MojoExecutionException {
    try {
      return file.getCanonicalPath();
    }
    catch (IOException e) {
      throw new MojoExecutionException("Failed to get canonical path of " + file.getAbsolutePath(), e);
    }
  }

  public void setProject(MavenProject project) {
    this.project = project;
  }

  public MavenProject getProject() {
    return project;
  }

  public void setRelativePath(String relativePath) {
    this.relativePath = relativePath;
  }

  public String getRelativePath() {
    return relativePath;
  }

  public void setRootPath(String rootPath) {
    this.rootPath = rootPath;
  }

  public String getRootPath() {
    return rootPath;
  }

  String calculatePathToRoot(File root, File folder) throws IOException {
    String result = ".";

    if (root != null && !folder.equals(root)) {
      String normalizedBase = FilenameUtils.normalizeNoEndSeparator(folder.getCanonicalPath());
      String normalizedMain = FilenameUtils.normalizeNoEndSeparator(root.getCanonicalPath());

      if (normalizedMain.length() > normalizedBase.length()) {
        throw new IOException("Can't find main project folder, module folder = " + normalizedBase + ", calculated main folder = " + normalizedMain);
      }

      String diff = normalizedBase.substring(normalizedMain.length());
      if (diff.length() != 0) {
        StringBuilder buffer = new StringBuilder();
        for (char c : diff.toCharArray()) {
          if (c == '/' || c == '\\') {
            buffer.append("..").append(File.separatorChar);
          }
        }

        result = buffer.toString();
      }
    }
    getLog().debug("root:" + root + " folder:" + folder + " result:" + result);
    return result;
  }

  String calculateRelativePath(String relativePathToRoot, String rootRelativePath) {
    getLog().debug("relativePathToRoot:" + relativePathToRoot + " rootRelativePath:" + rootRelativePath);
    if (".".equals(relativePathToRoot)) {
      if (".".equals(rootRelativePath)) {
        return ".";
      }
      else {
        return rootRelativePath;
      }
    }
    else if (".".equals(rootRelativePath)) {
      return relativePathToRoot;
    }

    return relativePathToRoot + rootRelativePath;
  }
  
  private File findBaseDirOfMainProject() {
    MavenProject current = this.project;
    while (current.hasParent() && current.getParent().getBasedir() != null) {
      current = current.getParent();
    }
    getLog().debug("project:" + this.project + " baseDir:" + current.getBasedir());
    return current.getBasedir();
  }
}
