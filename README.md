JRebel plugin for Maven
-----------------------

This plugin is used in order to generate rebel.xml configuration file for JRebel.

Usage
-----

Add the following snippet to pom.xml of your maven project

```xml
<plugin>
  <groupId>org.zeroturnaround</groupId>
  <artifactId>jrebel-maven-plugin</artifactId>
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

To manually execute the goal, run 'mvn jrebel:generate' and rebel.xml will be generated to the target directory. 



