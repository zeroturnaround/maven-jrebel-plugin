# JRebel Maven plugin

[![Build Status](https://travis-ci.org/zeroturnaround/maven-jrebel-plugin.svg?branch=master)](https://travis-ci.org/zeroturnaround/maven-jrebel-plugin) [![Quality Gate](https://sonarcloud.io/api/badges/gate?key=org.zeroturnaround%3Ajrebel-maven-plugin)](https://sonarcloud.io/dashboard/index/org.zeroturnaround%3Ajrebel-maven-plugin) [![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html) [![Maven metadata](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/zeroturnaround/jrebel-maven-plugin/maven-metadata.xml.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.zeroturnaround%22%20AND%20a%3A%22jrebel-maven-plugin%22)

This plugin is used to generate the **rebel.xml** configuration file for JRebel.
Requires Maven 3.0 or newer (use JRebel Maven plugin version 1.1.5 for Maven 2.x projects).

For more information, please refer to JRebel documentation at: **https://manuals.zeroturnaround.com/jrebel/standalone/maven.html** 

Usage
-----

Add the following snippet to the **pom.xml** of your Maven project to generate the **rebel.xml** configuration:

```xml
<plugin>
  <groupId>org.zeroturnaround</groupId>
  <artifactId>jrebel-maven-plugin</artifactId>
  <version>1.1.10</version>
  <executions>
    <execution>
      <id>generate-rebel-xml</id>
      <phase>process-resources</phase>
      <goals>
        <goal>generate</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

To manually execute the goal, run 'mvn jrebel:generate' and rebel.xml will be generated in the target directory.
