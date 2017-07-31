package org.zeroturnaround.javarebel.maven.model;

import java.io.File;
import java.util.List;

/**
 * Classpath resource configuration.
 */
public class RebelClasspathResource implements RebelResource {

  private String directory;
  private String jar;
  private String jarset;
  private String dirset;

  private List excludes;
  private List includes;

  public boolean doesDirExistsOrNotAbsolute() {
    File file = new File(directory);
    if (file.isAbsolute()) {
      return file.isDirectory();
    }
    return true;
  }

  public String getDirectory() {
    return directory;
  }

  public void setDirectory(String directory) {
    this.directory = directory;
  }

  public String getJar() {
    return jar;
  }

  public void setJar(String jar) {
    this.jar = jar;
  }

  public String getJarset() {
    return jarset;
  }

  public void setJarset(String jarset) {
    this.jarset = jarset;
  }

  public String getDirset() {
    return dirset;
  }

  public void setDirset(String dirset) {
    this.dirset = dirset;
  }

  public List getExcludes() {
    return excludes;
  }

  public void setExcludes(List excludes) {
    this.excludes = excludes;
  }

  public List getIncludes() {
    return includes;
  }

  public void setIncludes(List includes) {
    this.includes = includes;
  }

  public boolean isTargetSet() {
    return directory != null || jar != null || jarset != null || dirset != null;
  }

}
