package org.zeroturnaround.javarebel.maven.model;

/**
 * Classpath configuration.
 */
public class RebelClasspath {

  private RebelClasspathResource[] resources;

  private String fallback;

  public RebelClasspathResource[] getResources() {
    return resources;
  }

  public void setResources(RebelClasspathResource[] resources) {
    this.resources = resources;
  }

  public String getFallback() {
    return fallback;
  }

  public void setFallback(String fallback) {
    this.fallback = fallback;
  }

}
