package org.zeroturnaround.javarebel.maven;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * Generate liverebel.xml.
 * 
 * @goal generate-liverebel-xml
 * @phase process-resources
 * @threadSafe true
 * 
 * @author Rein Raudjärv
 */
public class GenerateLiveRebelMojo extends AbstractMojo {

  private static final String FILENAME = "liverebel.xml";
  private static final String LINE_SEPARATOR = System.getProperty("line.separator");
  /**
   * If set rebel plugin will use provided value instead of project version as 
   * application version.
   * 
   * @parameter default-value="${project.version}"
   */
  private String version;
  /**
   * If set rebel plugin will use provided value instead of project parameters as 
   * application name.
   * 
   * @parameter default-value="${project.groupId}:${project.artifactId}"
   */
  private String name;
  /**
   * The maven project.
   * 
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  private MavenProject project;
  /**
   * If set to true rebel plugin will generate liverebel.xml on each build, 
   * otherwise the timestamps of liverebel.xml and pom.xml are compared.
   * 
   * @parameter default-value="false"
   */
  private boolean alwaysGenerate;
  /**
   * @parameter expression="${project.build.directory}/${project.build.finalName}"
   * @required
   * @readonly
   */
  private File workDirectory;

  public void execute() throws MojoExecutionException, MojoFailureException {
    Log log = getLog();

    File file;
    if ("ear".equals(project.getPackaging()))
      file = new File(workDirectory, FILENAME);
    else
      file = new File(project.getBuild().getOutputDirectory(), FILENAME);

    File pomXml = project.getFile();
    if (!alwaysGenerate && file.exists() && pomXml.exists() && file.lastModified() > pomXml.lastModified()) {
      log.debug("Skipping generating " + FILENAME);
      return;
    }

    log.info("Generating " + FILENAME);

    String xmlName = name != null ? name : project.getGroupId() + ":" + project.getArtifactId();
    String xmlVersion = version != null ? version : project.getVersion();

    String contents = getLiveRebelXml(xmlName, xmlVersion);

    if (log.isDebugEnabled())
      log.debug("Contents of generated " + FILENAME + ":" + LINE_SEPARATOR + contents);

    try {
      FileUtils.writeStringToFile(file, contents);
    }
    catch (IOException e) {
      throw new MojoExecutionException("Could not write '" + file + "'", e);
    }
  }

  private static String getLiveRebelXml(String app, String version) {
    if (app == null)
      throw new IllegalArgumentException("Application name is required");
    if (version == null)
      throw new IllegalArgumentException("Application version is required");

    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + LINE_SEPARATOR
        + "<application xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.zeroturnaround.com\" xsi:schemaLocation=\"http://www.zeroturnaround.com/alderaan/rebel-2_0.xsd\">" + LINE_SEPARATOR
        + "  <name>" + app + "</name>" + LINE_SEPARATOR
        + "  <version>" + version + "</version>" + LINE_SEPARATOR
        + "</application>";
  }
}
