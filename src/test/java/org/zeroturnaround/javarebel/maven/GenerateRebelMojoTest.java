package org.zeroturnaround.javarebel.maven;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;
import java.io.FileReader;
import org.apache.commons.lang.SystemUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.junit.Assume;

public class GenerateRebelMojoTest {

  @Test
  public void testMakePathPrefixToMainFolder() throws Exception {
    Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

    GenerateRebelMojo grm = new GenerateRebelMojo();
    assertEquals(".", grm.makePathPrefixToMainFolder(new File("/home/project/"), new File("/home/project/")));
    assertEquals(".", grm.makePathPrefixToMainFolder(new File("/home/project/"), new File("/home/project")));
    assertEquals("../", grm.makePathPrefixToMainFolder(new File("/home/project/"), new File("/home/project/module1.1/")));
    assertEquals("../../", grm.makePathPrefixToMainFolder(new File("/home/project/"), new File("/home/project/module1.1/module2.1")));
    assertEquals("../../../", grm.makePathPrefixToMainFolder(new File("/home/project/"), new File("/home/project/module1.1/module2.1/module2.2")));
    assertEquals("../../../", grm.makePathPrefixToMainFolder(new File("/home/project"), new File("/home/project/module1.1/module2.1/module2.2/")));
    assertEquals("../../../", grm.makePathPrefixToMainFolder(new File("/home/project"), new File("/home/project/module1.1/module2.1/module2.2")));
    assertEquals("../../../", grm.makePathPrefixToMainFolder(new File("/home/project/"), new File("/home/project/module1.1/module2.1/module2.2/")));
  }

  //@Test TODO read pom from resources and evaluate calculated relative path
  public void testNotSupportedSetups() throws Exception {
    GenerateRebelMojo grm = new GenerateRebelMojo();
    MavenProject project = getMavenProject(new File("src/test/resources/remote-with-child/pom.xml"));
    grm.setProject(project);
    grm.execute();
    assertNotNull(grm.getRelativePath());
  }

  private MavenProject getMavenProject(File pomfile) throws Exception {
    FileReader reader = new FileReader(pomfile);
    MavenXpp3Reader mavenreader = new MavenXpp3Reader();
    Model model = mavenreader.read(reader);
    model.setPomFile(pomfile);
    return new MavenProject(model);
  }
}
