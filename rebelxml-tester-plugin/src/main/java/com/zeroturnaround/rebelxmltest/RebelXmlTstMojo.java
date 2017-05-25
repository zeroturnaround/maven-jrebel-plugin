package com.zeroturnaround.rebelxmltest;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Mojo(name = "rebelxml-test", defaultPhase = LifecyclePhase.TEST, threadSafe = true)
public class RebelXmlTstMojo extends AbstractMojo {

  /**
   * Path to the rebel.xml file.
   */
  @Parameter(alias = "rebelXml", required = true)
  private File rebelXmlPath;

  /**
   * Set of XPath expressions which must return node set.
   */
  @Parameter(alias = "xpaths", required = false)
  private String[] xpaths;

  /**
   * Set of XPath expressions which must not return node set.
   */
  @Parameter(alias = "notxpaths", required = false)
  private String[] notxpaths;

  private static final int LINE = 64;
  
  private void printOkLine(final String text) {
    final int dots = LINE - text.length() - 2;
    final StringBuilder buffer = new StringBuilder(LINE);
    buffer.append(text);
    for(int i=0;i<dots;i++) buffer.append('.');
    buffer.append("OK");
    getLog().info(buffer.toString());
  }

  private void printErrorLine(final String text) {
    final int dots = LINE - text.length() - 3;
    final StringBuilder buffer = new StringBuilder(LINE);
    buffer.append(text);
    for(int i=0;i<dots;i++) buffer.append('.');
    buffer.append("BAD");
    getLog().error(buffer.toString());
  }
  
  public void execute() throws MojoExecutionException {
    if (!this.rebelXmlPath.isFile()) {
      printErrorLine("Detecting rebel.xml");
      throw new MojoExecutionException("Can't find rebel.xml : " + this.rebelXmlPath);
    } else {
      printOkLine("Detecting rebel.xml");
    }

    try {

      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setIgnoringComments(true);
      factory.setCoalescing(true);
      factory.setValidating(false);
      
      final Document doc;
      final DocumentBuilder builder = factory.newDocumentBuilder();
      try{
        doc = builder.parse(this.rebelXmlPath);
        printOkLine("Parsing rebel.xml");
      }catch(Exception ex){
        printErrorLine("Parsing rebel.xml");
        throw ex;
      }
      
      if (this.xpaths != null && this.xpaths.length > 0) {
        final XPathFactory xPathfactory = XPathFactory.newInstance();
        final XPath xpath = xPathfactory.newXPath();

        for (final String xp : this.xpaths) {
          final XPathExpression ex = xpath.compile(xp);
          final NodeList result = (NodeList)ex.evaluate(doc,XPathConstants.NODESET);
          
          if (result == null || result.getLength() == 0) {
            printErrorLine("XPath tests");
            throw new MojoExecutionException("XPath expression can't find nodes : "+xp);
          }
        }
        
        printOkLine("XPath tests");
      }

      if (this.notxpaths != null && this.notxpaths.length > 0) {
        final XPathFactory xPathfactory = XPathFactory.newInstance();
        final XPath xpath = xPathfactory.newXPath();

        for (final String xp : this.notxpaths) {
          final XPathExpression ex = xpath.compile(xp);
          final NodeList result = (NodeList)ex.evaluate(doc,XPathConstants.NODESET);
          
          if (result != null && result.getLength() != 0) {
            printErrorLine("NotXPath tests");
            throw new MojoExecutionException("NotXPath expression found nodes : "+xp);
          }
        }
        
        printOkLine("NotXPath tests");
      }
    }
    catch (Exception ex) {
      throw new MojoExecutionException("Error during rebel.xml processing", ex);
    }
  }
}
