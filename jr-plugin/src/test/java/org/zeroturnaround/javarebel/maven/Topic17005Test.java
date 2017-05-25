package org.zeroturnaround.javarebel.maven;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

public class Topic17005Test {

	private GenerateRebelMojo m;
	private String projectParentBasedir;

	@Before
	public void setUp() {
		m = new GenerateRebelMojo();

		// relative path to root of current project
		m.setRelativePath(".");

		projectParentBasedir = "/projects/topic_17226/org.zeroturnaround.demoApps/org.zeroturnaround.demoApps.web.shared";

		// root path of maven projects
		m.setRootPath(projectParentBasedir);

		// the maven project
		m.setProject(new MavenProject());
		m.getProject().setFile(new File("/projects/topic_17226/org.zeroturnaround.demoApps/org.zeroturnaround.demoApps.web.shared/pom.xml"));
	}

	@Test
	public void testTopic17005() throws MojoExecutionException {
		// the path, which can be provided as parameter to JRebel plugin
		String actualObj = m.fixFilePath("/projects/topic_17226/org.zeroturnaround.demoApps/org.zeroturnaround.demoApps.web.shared/src/main/webapp");

		// test
		assertEquals("/projects/topic_17226/org.zeroturnaround.demoApps/org.zeroturnaround.demoApps.web.shared/src/main/webapp", actualObj);
	}
}
