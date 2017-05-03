package org.zeroturnaround.javarebel.maven;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;
import org.apache.commons.lang.SystemUtils;
import org.junit.Assume;

public class GenerateRebelMojoTest {
  
  @Test
  public void testMakePathPrefixToMainFolder() throws Exception {
    Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);
    assertEquals(".",GenerateRebelMojo.makePathPrefixToMainFolder(new File("/home/project/"), new File("/home/project/")));
    assertEquals(".",GenerateRebelMojo.makePathPrefixToMainFolder(new File("/home/project/"), new File("/home/project")));
    assertEquals("../",GenerateRebelMojo.makePathPrefixToMainFolder(new File("/home/project/"), new File("/home/project/module1.1/")));
    assertEquals("../../",GenerateRebelMojo.makePathPrefixToMainFolder(new File("/home/project/"), new File("/home/project/module1.1/module2.1")));
    assertEquals("../../../",GenerateRebelMojo.makePathPrefixToMainFolder(new File("/home/project/"), new File("/home/project/module1.1/module2.1/module2.2")));
    assertEquals("../../../",GenerateRebelMojo.makePathPrefixToMainFolder(new File("/home/project"), new File("/home/project/module1.1/module2.1/module2.2/")));
    assertEquals("../../../",GenerateRebelMojo.makePathPrefixToMainFolder(new File("/home/project"), new File("/home/project/module1.1/module2.1/module2.2")));
    assertEquals("../../../",GenerateRebelMojo.makePathPrefixToMainFolder(new File("/home/project/"), new File("/home/project/module1.1/module2.1/module2.2/")));
  }
  
}
