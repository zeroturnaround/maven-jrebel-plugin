package org.zeroturnaround.javarebel.maven;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

import java.io.StringWriter;
import org.junit.Test;

public class RebelXmlBuilderTest {
  @Test
  public void writeXmlShouldAddGeneratedByMaven() throws Exception {
    RebelXmlBuilder builder = new RebelXmlBuilder(null, null, null);
    StringWriter result = new StringWriter();
    builder.writeXml(result);
    assertThat(result.toString(), containsString("generated-by=\"maven\""));
  }

  @Test
  public void writeXmlShouldAddBuildVersion() throws Exception {
    RebelXmlBuilder builder = new RebelXmlBuilder("3.0.0", "1.0.8", null);
    StringWriter result = new StringWriter();
    builder.writeXml(result);
    assertThat(result.toString(), containsString("build-tool-version=\"3.0.0\""));
  }

  @Test
  public void writeXmlShouldAddPluginVersion() throws Exception {
    RebelXmlBuilder builder = new RebelXmlBuilder("3.0.0", "1.0.8", null);
    StringWriter result = new StringWriter();
    builder.writeXml(result);
    assertThat(result.toString(), containsString("plugin-version=\"1.0.8\""));
  }
}