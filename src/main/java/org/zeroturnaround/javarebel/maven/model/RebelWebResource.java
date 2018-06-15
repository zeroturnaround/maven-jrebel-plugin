package org.zeroturnaround.javarebel.maven.model;

import java.io.File;
import java.io.IOException;
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

  public boolean validRebelTargetDir(String outputDirectory) throws IOException {
    File file = new File(directory);
    if (file.isAbsolute()) {
      if (file.isDirectory()) {
        return true;
      }
      else if (file.isFile()) {
        return false;
      }
      return file.getCanonicalPath().startsWith(new File(outputDirectory).getCanonicalPath());
    }
    return true;
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
