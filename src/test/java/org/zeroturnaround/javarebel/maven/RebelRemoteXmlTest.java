package org.zeroturnaround.javarebel.maven;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

public class RebelRemoteXmlTest {

  private GenerateRebelMojo grm;

  @Before
  public void setUp() {
    grm = new GenerateRebelMojo();
    grm.setProject(new MavenProject());
    grm.getProject().setArtifactId("test-artifact-id");
  }

  @Test
  public void name() throws IOException {
    StringWriter result = new StringWriter();
    grm.generateRebelRemoteXml(result);
    assertThat(result.toString(), containsString("<id>test-artifact-id</id>"));
  }
}
