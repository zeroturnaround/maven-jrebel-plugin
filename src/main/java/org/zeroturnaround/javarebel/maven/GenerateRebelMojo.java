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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.interpolation.ObjectBasedValueSource;
import org.codehaus.plexus.util.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.zeroturnaround.javarebel.maven.model.RebelClasspath;
import org.zeroturnaround.javarebel.maven.model.RebelClasspathResource;
import org.zeroturnaround.javarebel.maven.model.RebelResource;
import org.zeroturnaround.javarebel.maven.model.RebelWar;
import org.zeroturnaround.javarebel.maven.model.RebelWeb;
import org.zeroturnaround.javarebel.maven.model.RebelWebResource;

/**
 * Generate rebel.xml
 * 
 * @goal generate
 * @phase process-resources
 * @threadSafe true
 */
public class GenerateRebelMojo extends AbstractMojo {

  private static final String[] DEFAULT_INCLUDES = { "**/**" };

  private static final Set JAR_PACKAGING = new HashSet();
  private static final Set WAR_PACKAGING = new HashSet();
  static {
    JAR_PACKAGING.addAll(Arrays.asList(new String[] {"jar", "ejb", "ejb3", "nbm", "hk2-jar", "bundle", "eclipse-plugin", "atlassian-plugin"}));
    WAR_PACKAGING.addAll(Arrays.asList(new String[] {"war", "grails-app"}));
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
   * @parameter default-value="${basedir}"
   * @required
   */
  private String rootPath;

  /**
   * Relative path to root of current project.
   * 
   * @parameter default-value="."
   * @required
   */
  private String relativePath;

  /**
   * Target directory for generated rebel.xml
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
   * If set to true rebel plugin will generate rebel.xml on each build, otherwise the timestamps of rebel.xml and pom.xml are compared.
   * 
   * @parameter default-value="false"
   */
  private boolean alwaysGenerate;
  
  /**
   * Indicates whether the default web element will be generated or not. Initial value is the same as {@link #generateDefaultElements} value.
   */
  private boolean generateDefaultWeb;

  /**
   * Indicates whether the default classpath element will be generated or not. Initial value is the same as {@link #generateDefaultElements} value.
   */
  private boolean generateDefaultClasspath;

  /**
   * If set to false rebel plugin will not generate default elements in rebel.xml.
   * @parameter default-value="true"
   */
  private boolean generateDefaultElements;

  public void execute() throws MojoExecutionException, MojoFailureException {
    // do not generate rebel.xml file if 'performRelease' system property is set to true
    try {
      if (Boolean.getBoolean("performRelease")) {
        getLog().info("Skipped generating rebel.xml.");
        return;
      }
    }
    catch (SecurityException ignore) {}
    
    generateDefaultWeb = generateDefaultClasspath = generateDefaultElements;
    File rebelXmlFile = new File(rebelXmlDirectory, "rebel.xml").getAbsoluteFile();
    File pomXmlFile = getProject().getFile();
    if (!alwaysGenerate && rebelXmlFile.exists() && pomXmlFile.exists() && rebelXmlFile.lastModified() > pomXmlFile.lastModified()) {
      return;
    }

    getLog().info("Processing " + getProject().getGroupId() + ":" + getProject().getArtifactId() + " with packaging " + packaging);

    RebelXmlBuilder builder = null;
    if (WAR_PACKAGING.contains(packaging)) {
      builder = buildWar();
    } else if (JAR_PACKAGING.contains(packaging)) {
      builder = buildJar();
    } else {
      getLog().warn("Unsupported packaging type: " + packaging);
    }

    if (builder != null) {
      Writer w = null;
      if (showGenerated) {
        try {
          w = new StringWriter();
          builder.writeXml(w);
          getLog().info(w.toString());
        }
        catch (IOException e) {
          // ignore exception
        }
      }

      try {
        rebelXmlDirectory.mkdirs();
        w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(rebelXmlFile), "UTF-8"));
        builder.writeXml(w);
      }
      catch (IOException e) {
        throw new MojoExecutionException("Failed writing rebel.xml", e);
      }
      finally {
        if (w != null) {
          try {
            w.close();
          }
          catch (IOException ie) {
            // ignore exception
          }
        }
      }
    }
  }

  /**
   * Build war configuration.
   * 
   * @return
   * @throws MojoExecutionException
   */
  private RebelXmlBuilder buildWar() throws MojoExecutionException {
    RebelXmlBuilder builder = new RebelXmlBuilder();

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
    RebelXmlBuilder builder = new RebelXmlBuilder();
    buildClasspath(builder);
    
    // if user has specified any web elements, then let's generate these in the result file.
    if (web != null && web.getResources() != null && web.getResources().length != 0) {
      generateDefaultWeb = false; // but don't generate default web element because this folder is most likely missing.
      buildWeb(builder);
    }

    return builder;
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
        for (int i = 0; i < resources.length; i++) {
          RebelClasspathResource r = resources[i];
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
        for (int i = 0; i < resources.length; i++) {
          RebelClasspathResource r = resources[i];
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
          } else {
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
    boolean overwrite = Boolean.valueOf(getPluginSetting(getProject(), "org.apache.maven.plugins:maven-resources-plugin", "overwrite", "false")).booleanValue();

    RebelClasspathResource r = null;

    List resources = getProject().getResources();
    //if resources plugin is set to overwrite then reverse the order of resources
    if (overwrite) {
      Collections.reverse(resources);
    }
    for (Iterator i = resources.iterator(); i.hasNext();) {
      Resource resource = (Resource) i.next();

      File dir = new File(resource.getDirectory());
      if (!dir.isAbsolute()) {
        dir = new File(getProject().getBasedir(), resource.getDirectory());
      }
      //skip directories that don't exist
      if (!dir.exists() || !dir.isDirectory()) {
        continue;
      }

      r = new RebelClasspathResource();

      if (resource.isFiltering() || resource.getTargetPath() != null) {
        if (!handleResourceAsInclude(r, resource)) {
          continue;
        }
        //point filtered resources to target directory
        r.setDirectory(fixFilePath(classesDirectory));
        //add target path as prefix to includes
        if (resource.getTargetPath() != null) {
          setIncludePrefix(r.getIncludes(), resource.getTargetPath());
        }
      }
      else {
        r.setDirectory(fixFilePath(resource.getDirectory()));
        r.setExcludes(resource.getExcludes());
        r.setIncludes(resource.getIncludes());
      }

      builder.addClasspathDir(r);
    }
  }

  private void setIncludePrefix(List includes, String prefix) {
    if (!prefix.endsWith("/")) {
      prefix = prefix + "/";
    }
    for (int i = 0; i < includes.size(); i++) {
      includes.set(i, prefix + (String)includes.get(i));
    }
  }

  /**
   * Set includes & excludes for filtered resources.
   */
  private boolean handleResourceAsInclude(RebelResource rebelResouce, Resource resource) throws MojoExecutionException {
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
      List includedFiles = new ArrayList();
      for (int i = 0; i < files.length; i++) {
        includedFiles.add(StringUtils.replace(files[i], '\\', '/'));
      }
      rebelResouce.setIncludes(includedFiles);
    } else {
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
      scanner.setIncludes((String[]) resource.getIncludes().toArray(new String[resource.getIncludes().size()]));
    }
    else {
      scanner.setIncludes(DEFAULT_INCLUDES);
    }
    if (resource.getExcludes() != null && !resource.getExcludes().isEmpty()) {
      scanner.setExcludes((String[]) resource.getExcludes().toArray(new String[resource.getExcludes().size()]));
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
        for (int i = 0; i < resources.length; i++) {
          RebelWebResource r = resources[i];
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
        for (int i = 0; i < resources.length; i++) {
          RebelWebResource r = resources[i];
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
        List resources = parseWarResources(wr);
        //web resources overwrite each other
        Collections.reverse(resources);

        for (Iterator i = resources.iterator(); i.hasNext();) {
          Resource resource = (Resource) i.next();

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
  private List parseWarResources(Xpp3Dom warResourcesNode) {
    List resources = new ArrayList();
    Xpp3Dom[] resourceNodes = warResourcesNode.getChildren("resource");
    for (int i = 0; i < resourceNodes.length; i++) {
      if (resourceNodes[i] == null || resourceNodes[i].getChild("directory") == null) {
        continue;
      }
      Resource resource = parseResourceNode(resourceNodes[i]);
      resources.add(resource);
    }

    return resources;
  }

  /**
   * Parse resouce node content.
   * 
   * @param rn
   * @return
   */
  private Resource parseResourceNode(Xpp3Dom rn) {
    Resource r = new Resource();
    if (rn.getChild("directory") != null) {
      r.setDirectory(getValue(getProject(), rn.getChild("directory")));
    }
    if (rn.getChild("filtering") != null) {
      r.setFiltering((new Boolean(getValue(getProject(), rn.getChild("filtering")))).booleanValue());
    }
    if (rn.getChild("targetPath") != null) {
      r.setTargetPath(rn.getChild("targetPath").getValue());
    }

    if (rn.getChild("excludes") != null) {
      List excludes = new ArrayList();
      Xpp3Dom[] excludeNodes = rn.getChild("excludes").getChildren("exclude");
      for (int i = 0; i < excludeNodes.length; i++) {
        if (excludeNodes[i] != null && excludeNodes[i].getValue() != null) {
          excludes.add(getValue(getProject(), excludeNodes[i]));
        }
      }
      r.setExcludes(excludes);
    }
    if (rn.getChild("includes") != null) {
      List includes = new ArrayList();
      Xpp3Dom[] includeNodes = rn.getChild("includes").getChildren("include");
      for (int i = 0; i < includeNodes.length; i++) {
        if (includeNodes[i] != null && includeNodes[i].getValue() != null) {
          includes.add(getValue(getProject(), includeNodes[i]));
        }
      }
      r.setIncludes(includes);
    }

    return r;
  }

  /**
   * Taken from eclipse plugin.
   * Search for the configuration Xpp3 dom of an other plugin.
   * 
   * @param project
   *          the current maven project to get the configuration from.
   * @param pluginId
   *          the group id and artifact id of the plugin to search for
   * @return the value of the plugin configuration
   */
  private static Xpp3Dom getPluginConfigurationDom(MavenProject project, String pluginId) {
    Plugin plugin = (Plugin) project.getBuild().getPluginsAsMap().get(pluginId);
    if (plugin != null) {
      return (Xpp3Dom) plugin.getConfiguration();
    }

    return null;
  }

  /**
   * Search for a configuration setting of an other plugin.
   * 
   * @param project
   *          the current maven project to get the configuration from.
   * @param pluginId
   *          the group id and artifact id of the plugin to search for
   * @param optionName
   *          the option to get from the configuration
   * @param defaultValue
   *          the default value if the configuration was not found
   * @return the value of the option configured in the plugin configuration
   */
  private static String getPluginSetting(MavenProject project, String pluginId, String optionName, String defaultValue) {
    Xpp3Dom dom = getPluginConfigurationDom(project, pluginId);
    if (dom != null && dom.getChild(optionName) != null) {
      return getValue(project, dom.getChild(optionName));
    }
    return defaultValue;
  }

  private static String getValue(MavenProject project, Xpp3Dom dom) {
    String value = dom.getValue();

    return getValue(project, value);
  }

  private static String getValue(MavenProject project, String value) {
    if (value != null && value.indexOf("$") > -1) {
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
  private static String getInterpolatorValue(MavenProject project, String value) {
    RegexBasedInterpolator interpolator = new RegexBasedInterpolator();
    interpolator.addValueSource(new ObjectBasedValueSource(project));

    return interpolator.interpolate(value, "project");
  }

  protected String fixFilePath(String path) throws MojoExecutionException {
    return fixFilePath(new File(path));
  }

  /**
   * Returns path expressed through rootPath & relativePath.
   * 
   * @param path
   * @return fixed path
   * @throws MojoExecutionException
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
      } else {
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
    } else if (absolutePath.startsWith(basedirpath)) {
      relative = absolutePath.substring(basedirpath.length());
    } else {
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

}
