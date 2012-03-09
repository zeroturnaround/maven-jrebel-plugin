JRebel plugin for Maven
-----------------------

This plugin is used in order to generate rebel.xml configuration file for JRebel or liverebel.xml configuration for LiveRebel.

Usage
-----

Add the following snippet to pom.xml of your maven project to generate rebel.xml configuration

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

Add the following snippet to pom.xml of your maven project to generate liverebel.xml configuration

```xml
<plugin>
  <groupId>org.zeroturnaround</groupId>
  <artifactId>jrebel-maven-plugin</artifactId>
  <!-- Optional configuration -->
  <configuration>
    <name>${project.artifactId}-development</name>
    <version>${project.version}-${maven.build.timestamp}</version>
  </configuration>
  <executions>
    <execution>
      <id>generate-liverebel-xml</id>
      <phase>process-resources</phase>
      <goals>
        <goal>generate-liverebel-xml</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

To manually execute the goal, run 'mvn jrebel:generate-liverebel-xml' and rebel.xml will be generated to the target directory. 

