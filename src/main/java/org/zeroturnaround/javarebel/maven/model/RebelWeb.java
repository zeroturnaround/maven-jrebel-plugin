package org.zeroturnaround.javarebel.maven.model;

/**
 * Web configuration.
 */
public class RebelWeb {

  private RebelWebResource[] resources;

  public RebelWebResource[] getResources() {
    return resources;
  }

  public void setResources(RebelWebResource[] resources) {
    this.resources = resources;
  }

}
