package org.zeroturnaround.javarebel.maven.model;

import java.io.IOException;
import java.util.List;

public interface RebelResource {

  List<String> getIncludes();

  void setIncludes(List<String> includes);

  List<String> getExcludes();

  void setExcludes(List<String> excludes);

  void setDirectory(String directory);

  String getDirectory();

  boolean validRebelTargetDir(String outputDirectory) throws IOException;

}
