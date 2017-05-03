JRebel Maven plugin
-------------------

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
  <version>1.1.7</version>
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
