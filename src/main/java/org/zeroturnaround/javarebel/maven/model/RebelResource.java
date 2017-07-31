package org.zeroturnaround.javarebel.maven.model;

import java.util.List;

public interface RebelResource {

  List getIncludes();

  void setIncludes(List includes);

  List getExcludes();

  void setExcludes(List excludes);

  void setDirectory(String duirectory);

  String getDirectory();

  boolean doesDirExistsOrNotAbsolute();

}
