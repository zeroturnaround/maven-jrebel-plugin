package org.zeroturnaround.javarebel.maven.model;

import java.util.List;

public interface RebelResource {

  List<String> getIncludes();

  void setIncludes(List<String> includes);

  List<String> getExcludes();

  void setExcludes(List<String> excludes);

  void setDirectory(String duirectory);

  String getDirectory();

  boolean doesDirExistsOrNotAbsolute();

}
