package org.zeroturnaround.javarebel.maven;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.zeroturnaround.javarebel.maven.util.SystemUtils;

public class SystemUtilsTest {

  @Test
  public void testEnsurePathAndURLSafeName() {
    assertEquals("Usual__project", SystemUtils.ensurePathAndURLSafeName("Usual project"));

    assertEquals("_NUL_", SystemUtils.ensurePathAndURLSafeName("NUL"));
    assertEquals("_nul_", SystemUtils.ensurePathAndURLSafeName("nul"));
    assertEquals("_CLOCK_24_", SystemUtils.ensurePathAndURLSafeName("CLOCK$"));
    assertEquals("_24", SystemUtils.ensurePathAndURLSafeName("$"));

    assertEquals("_22Hello_22", SystemUtils.ensurePathAndURLSafeName("\"Hello\""));
    assertEquals("_5CHello_2F", SystemUtils.ensurePathAndURLSafeName("\\Hello/"));
    assertEquals("._2F.._2FHello__World_2C__and__all_26_7C_2B_2A", SystemUtils.ensurePathAndURLSafeName("./../Hello World, and all&|+*"));
    assertEquals("_", SystemUtils.ensurePathAndURLSafeName(null));
    assertEquals("_", SystemUtils.ensurePathAndURLSafeName(""));
    assertEquals("ASW_3A__JAX-WS___5BClient_5D", SystemUtils.ensurePathAndURLSafeName("ASW: JAX-WS [Client]"));
  }
}
