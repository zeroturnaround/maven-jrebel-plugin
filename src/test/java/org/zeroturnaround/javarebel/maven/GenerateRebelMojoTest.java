package org.zeroturnaround.javarebel.maven;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import org.apache.commons.lang.SystemUtils;
import org.junit.Assume;

public class GenerateRebelMojoTest {
  private GenerateRebelMojo grm = new GenerateRebelMojo();

  @Test
  public void testCalculatePathToRoot() throws Exception {
    Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

    assertEquals(".", grm.calculatePathToRoot(new File("/home/project/"), new File("/home/project/")));
    assertEquals(".", grm.calculatePathToRoot(new File("/home/project/"), new File("/home/project")));
    assertEquals("../", grm.calculatePathToRoot(new File("/home/project/"), new File("/home/project/module1.1/")));
    assertEquals("../../", grm.calculatePathToRoot(new File("/home/project/"), new File("/home/project/module1.1/module2.1")));
    assertEquals("../../../", grm.calculatePathToRoot(new File("/home/project/"), new File("/home/project/module1.1/module2.1/module2.2")));
    assertEquals("../../../", grm.calculatePathToRoot(new File("/home/project"), new File("/home/project/module1.1/module2.1/module2.2/")));
    assertEquals("../../../", grm.calculatePathToRoot(new File("/home/project"), new File("/home/project/module1.1/module2.1/module2.2")));
    assertEquals("../../../", grm.calculatePathToRoot(new File("/home/project/"), new File("/home/project/module1.1/module2.1/module2.2/")));
  }

  @Test
  public void getRelativePathFromRootRelativePath() throws Exception {
    Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

    assertEquals(".", grm.calculateRelativePath(".", "."));
    assertEquals("../", grm.calculateRelativePath(".", "../"));
    assertEquals("../", grm.calculateRelativePath("../", "."));
    assertEquals("../../", grm.calculateRelativePath("../", "../"));
    assertEquals("../../../", grm.calculateRelativePath("../../", "../"));
    assertEquals("../../../", grm.calculateRelativePath("../", "../../"));
  }
}
