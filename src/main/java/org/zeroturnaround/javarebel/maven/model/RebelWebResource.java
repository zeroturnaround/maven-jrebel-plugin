package org.zeroturnaround.javarebel.maven.model;

import java.io.File;
import java.util.List;

/**
 * Web reource configuration.
 */
public class RebelWebResource implements RebelResource {

  private String target;
  private String directory;

  private List<String> excludes;
  private List<String> includes;

  public String getTarget() {
    return target;
  }

  public boolean doesDirExistsOrNotAbsolute() {
    boolean result = true;

    File file = new File(directory);
    if (file.isAbsolute()) {
      result = file.isDirectory();
    }

    return result;
  }


  public void setTarget(String target) {
    this.target = target;
  }

  public String getDirectory() {
    return directory;
  }

  public void setDirectory(String directory) {
    this.directory = directory;
  }

  public List<String> getExcludes() {
    return excludes;
  }

  public void setExcludes(List<String> excludes) {
    this.excludes = excludes;
  }

  public List<String> getIncludes() {
    return includes;
  }

  public void setIncludes(List<String> includes) {
    this.includes = includes;
  }

}
