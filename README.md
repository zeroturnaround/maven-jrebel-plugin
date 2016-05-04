JRebel plugin for Maven
-----------------------

A Maven 3.0+ plugin used to generate the **rebel.xml** configuration file for JRebel.

For more information, please refer to the documentation at: **https://manuals.zeroturnaround.com/jrebel/standalone/maven.html** 

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
