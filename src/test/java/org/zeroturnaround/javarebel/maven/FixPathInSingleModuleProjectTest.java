package org.zeroturnaround.javarebel.maven;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

public class FixPathInSingleModuleProjectTest {
  private GenerateRebelMojo m;

  @Before
  public void setUp() {
    m = new GenerateRebelMojo();

    // relative path to root of current project
    m.setRelativePath(".");

    // root path of maven projects
    m.setRootPath("C:/projects/topic_17576");

    // the maven project
    m.setProject(new MavenProject());
    m.getProject().setFile(new File("C:/projects/topic_17576/pom.xml"));
  }

  @Test
  public void testFixAbsoluteFilePath() throws MojoExecutionException {
    // the path, which can be provided as parameter to JRebel plugin
    String actualObj = m.fixFilePath("C:/projects/topic_17576/target/myclasses");

    // test
    assertEquals("C:/projects/topic_17576/target/myclasses", actualObj);
  }

  @Test
  public void testFixRelativeFilePath() throws MojoExecutionException {
    // the path, which can be provided as parameter to JRebel plugin
    String actualObj = m.fixFilePath("target/myclasses");

    // test
    assertEquals("C:/projects/topic_17576/target/myclasses", actualObj);
  }
}
