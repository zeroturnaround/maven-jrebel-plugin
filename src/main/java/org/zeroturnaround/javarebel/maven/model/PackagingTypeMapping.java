package org.zeroturnaround.javarebel.maven.model;

/**
 * Packaging type mapping configuration.
 */
public class PackagingTypeMapping {
  /**
   * Custom packaging type to map.
   *
   * @parameter
   * @required
   */
  private String type;

  /**
   * Standard packaging type to be mapped to.
   *
   * @parameter
   * @required
   */
  private String mapping;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getMapping() {
    return mapping;
  }

  public void setMapping(String mapping) {
    this.mapping = mapping;
  }
}
