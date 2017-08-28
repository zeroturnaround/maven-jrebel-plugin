package org.zeroturnaround.javarebel.maven;

import static org.junit.Assert.*;

import java.io.StringWriter;
import org.junit.Test;

public class RebelXmlBuilderTest {
  @Test
  public void writeXmlShouldAddGeneratedByMaven() throws Exception {
    RebelXmlBuilder builder = new RebelXmlBuilder(null, null);
    StringWriter result = new StringWriter();
    builder.writeXml(result);
    assertTrue(result.toString().contains("generated-by=\"maven\""));
  }

  @Test
  public void writeXmlShouldAddBuildVersion() throws Exception {
    RebelXmlBuilder builder = new RebelXmlBuilder("3.0.0", "1.0.8");
    StringWriter result = new StringWriter();
    builder.writeXml(result);
    assertTrue(result.toString().contains("build-tool-version=\"3.0.0\""));
  }

  @Test
  public void writeXmlShouldAddPluginVersion() throws Exception {
    RebelXmlBuilder builder = new RebelXmlBuilder("3.0.0", "1.0.8");
    StringWriter result = new StringWriter();
    builder.writeXml(result);
    assertTrue(result.toString().contains("plugin-version=\"1.0.8\""));
  }
}