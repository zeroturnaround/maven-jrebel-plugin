package org.zeroturnaround.javarebel.maven;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

public class FixPathInMultimoduleProjectTest {
	private GenerateRebelMojo m;

	@Before
	public void setUp() {
		m = new GenerateRebelMojo();

		// relative path to root of current project
		m.setRelativePath(".");

		// root path of maven projects
		m.setRootPath("C:/projects/topic_17576/iur-service/");

		// the maven project
		m.setProject(new MavenProject());
		m.getProject().setFile(new File("C:/projects/topic_17576/iur-service/pom.xml"));
	}

	@Test
	public void testFixAbsoluteFilePath() throws MojoExecutionException {
		// the path, which can be provided as parameter to JRebel plugin
		String actualObj = m.fixFilePath("C:/projects/topic_17576/iur-service-client/target/classes");

		// test
		assertEquals("C:/projects/topic_17576/iur-service-client/target/classes", actualObj);
	}

	@Test
	public void testFixAbsoluteFilePath2() throws MojoExecutionException {
		// the path, which can be provided as parameter to JRebel plugin
		String actualObj = m.fixFilePath("C:/projects/topic_17576/../topic_17576/iur-service-client/target/classes");

		// test
		assertEquals("C:/projects/topic_17576/iur-service-client/target/classes", actualObj);
	}

	@Test
	public void testFixAbsoluteFilePath3() throws MojoExecutionException {
		// the path, which can be provided as parameter to JRebel plugin
		String actualObj = m.fixFilePath("C:/projects/topic_17576/iur-service-client/target/classes/");

		// test
		assertEquals("C:/projects/topic_17576/iur-service-client/target/classes", actualObj);
	}

	@Test
	public void testFixRelativeFilePath() throws MojoExecutionException {
		// the path, which can be provided as parameter to JRebel plugin
		String actualObj = m.fixFilePath("../iur-service-client/target/classes");

		// test
		assertEquals("C:/projects/topic_17576/iur-service-client/target/classes", actualObj);
	}

	@Test
	public void testFixRelativeFilePath2() throws MojoExecutionException {
		// the path, which can be provided as parameter to JRebel plugin
		String actualObj = m.fixFilePath("../../iur-service-client/target/classes/");

		// test
		assertEquals("C:/projects/iur-service-client/target/classes", actualObj);
	}

	@Test
	public void testFixRelativeFilePathWithDots() throws MojoExecutionException {
		// the path, which can be provided as parameter to JRebel plugin
		String actualObj = m.fixFilePath("../../com.zeroturnaround/jrebel.commons/target/classes");

		// test
		assertEquals("C:/projects/com.zeroturnaround/jrebel.commons/target/classes", actualObj);
	}
}
