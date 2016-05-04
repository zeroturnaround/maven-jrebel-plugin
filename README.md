JRebel Maven plugin
-------------------

This plugin is used to generate the **rebel.xml** configuration file for JRebel. Requires Maven 3.0 or newer. Please use JRebel Maven plugin 1.1.5 for Maven 2.x.

For more information, please refer to JRebel documentation at: **https://manuals.zeroturnaround.com/jrebel/standalone/maven.html** 

Usage
-----

Add the following snippet to the **pom.xml** of your Maven project to generate the **rebel.xml** configuration:

```xml
<plugin>
  <groupId>org.zeroturnaround</groupId>
  <artifactId>jrebel-maven-plugin</artifactId>
  <version>1.1.6</version>
  <executions>
    <execution>
      <id>generate-rebel-xml</id>
      <goals>
        <goal>generate</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

To manually execute the goal, run 'mvn jrebel:generate' and rebel.xml will be generated in the resources directory. 
