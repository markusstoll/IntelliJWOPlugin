package org.wocommunity.plugins.intellij.wizards;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class WOArchetypeGeneratorTest {

    @Test
    public void testAllArchetypesDefined() {
        assertEquals(7, WOArchetype.values().length);
        assertEquals("erxapplication-archetype", WOArchetype.ERX_APPLICATION.getArtifactId());
        assertEquals("erxapplication-d2w-archetype", WOArchetype.ERX_APPLICATION_D2W.getArtifactId());
        assertEquals("erxapplication-servlet-archetype", WOArchetype.ERX_APPLICATION_SERVLET.getArtifactId());
        assertEquals("woapplication-archetype", WOArchetype.WO_APPLICATION.getArtifactId());
        assertEquals("woframework-archetype", WOArchetype.WO_FRAMEWORK.getArtifactId());
        assertEquals("woapplication-servlet-archetype", WOArchetype.WO_APPLICATION_SERVLET.getArtifactId());
        assertEquals("partial-woapplication-servlet-archetype", WOArchetype.PARTIAL_WO_SERVLET.getArtifactId());

        for (WOArchetype a : WOArchetype.values()) {
            assertEquals("org.wocommunity", WOArchetype.GROUP_ID);
            assertEquals("https://maven.wocommunity.org/content/groups/public/", WOArchetype.REPOSITORY_URL);
            assertEquals("3.0", WOArchetype.DEFAULT_ARCHETYPE_VERSION);
            assertNotNull(a.getDisplayName());
            assertNotNull(a.getDescription());
        }
    }

    @Test
    public void testTemplateContentTransformation() {
        WOProjectModel model = new WOProjectModel();
        model.setGroupId("org.example.test");
        model.setArtifactId("MyCoolApp");
        model.setVersion("2.1.0");
        model.setPackageName("org.example.test.app");
        model.setJavaVersion("17");
        model.setWonderVersion("7.4");

        String rawTemplate = """
            #set($dollar = '$')
            <project>
                <groupId>${groupId}</groupId>
                <artifactId>${artifactId}</artifactId>
                <version>${version}</version>
                <properties>
                    <wonder.version>${WonderVersion}</wonder.version>
                </properties>
                <build>
                    <finalName>${dollar}{project.artifactId}</finalName>
                    <plugins>
                        <plugin>
                            <configuration>
                                <source>${JavaVersion}</source>
                            </configuration>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;

        String result = WOArchetypeGenerator.transformTemplateContent(rawTemplate, model);

        assertFalse(result.contains("#set($dollar = '$')"));
        assertTrue(result.contains("<groupId>org.example.test</groupId>"));
        assertTrue(result.contains("<artifactId>MyCoolApp</artifactId>"));
        assertTrue(result.contains("<version>2.1.0</version>"));
        assertTrue(result.contains("<wonder.version>7.4</wonder.version>"));
        assertTrue(result.contains("<source>17</source>"));
        assertTrue(result.contains("<finalName>${project.artifactId}</finalName>"));
    }

    @Test
    public void testAddWOCommunityRepositories(@TempDir Path tempDir) throws IOException {
        Path pomPath = tempDir.resolve("pom.xml");
        Files.writeString(pomPath, """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test</artifactId>
                <version>1.0</version>
            </project>
            """);

        File pomFile = pomPath.toFile();
        WOArchetypeGenerator.addWOCommunityRepositories(pomFile);

        String updated = Files.readString(pomPath);
        assertTrue(updated.contains("<id>wocommunity</id>"));
        assertTrue(updated.contains("https://maven.wocommunity.org/content/groups/public/"));
        assertTrue(updated.contains("<repositories>"));
        assertTrue(updated.contains("<pluginRepositories>"));

        // Running a second time should not duplicate
        WOArchetypeGenerator.addWOCommunityRepositories(pomFile);
        String secondUpdate = Files.readString(pomPath);
        assertEquals(updated, secondUpdate);
    }

    @Test
    public void testEffectiveLocationCalculation() {
        WOProjectModel model = new WOProjectModel();
        model.setArtifactId("TestApp");
        model.setLocation("/Users/test/Projects");
        assertEquals(new File("/Users/test/Projects/TestApp").getAbsolutePath(), model.getEffectiveLocation());

        model.setLocation("/Users/test/Projects/TestApp");
        assertEquals(new File("/Users/test/Projects/TestApp").getAbsolutePath(), model.getEffectiveLocation());
    }

    @Test
    public void testDirectArchetypeExtraction(@TempDir Path tempDir) throws Exception {
        WOProjectModel model = new WOProjectModel();
        model.setName("GenTestApp");
        model.setArtifactId("GenTestApp");
        model.setGroupId("com.test.gen");
        model.setVersion("1.0.0");
        model.setPackageName("com.test.gen.app");
        model.setArchetype(WOArchetype.ERX_APPLICATION);
        model.setArchetypeVersion("3.0");
        model.setJavaVersion("17");
        model.setWonderVersion("7.2");
        model.setAddRepositoryToPom(true);

        File targetDir = tempDir.resolve("GenTestApp").toFile();

        // This will test direct archetype downloading/caching and expansion
        WOArchetypeGenerator.generateDirectly(model, targetDir, null);

        assertTrue(targetDir.exists());
        File pom = new File(targetDir, "pom.xml");
        assertTrue(pom.exists(), "pom.xml must be generated");

        String pomContent = Files.readString(pom.toPath());
        assertTrue(pomContent.contains("<artifactId>GenTestApp</artifactId>"));
        assertTrue(pomContent.contains("<groupId>com.test.gen</groupId>"));

        File appClass = new File(targetDir, "src/main/java/com/test/gen/app/app/Application.java");
        assertTrue(appClass.exists(), "Application.java must be generated in correct package path");

        String appContent = Files.readString(appClass.toPath());
        assertTrue(appContent.contains("package com.test.gen.app.app;"));
        assertTrue(appContent.contains("class Application extends ERXApplication"));

        File mainComponent = new File(targetDir, "src/main/components/Main.wo/Main.html");
        assertTrue(mainComponent.exists(), "Main.html must be generated");
    }

    @Test
    public void testDirectArchetypeExtractionIntoPreexistingDirectory(@TempDir Path tempDir) throws Exception {
        WOProjectModel model = new WOProjectModel();
        model.setName("PreexistingApp");
        model.setArtifactId("PreexistingApp");
        model.setGroupId("org.preexisting");
        model.setVersion("1.0.0");
        model.setPackageName("org.preexisting.app");
        model.setArchetype(WOArchetype.WO_APPLICATION);
        model.setArchetypeVersion("3.0");
        model.setJavaVersion("17");

        File targetDir = tempDir.resolve("PreexistingApp").toFile();
        targetDir.mkdirs();

        // Simulate pre-existing file (such as .iml or .gitignore created by IDE)
        File placeholder = new File(targetDir, "placeholder.txt");
        Files.writeString(placeholder.toPath(), "test");

        WOArchetypeGenerator.generateDirectly(model, targetDir, null);

        assertTrue(new File(targetDir, "pom.xml").exists());
        assertTrue(new File(targetDir, "src/main/java/org/preexisting/app/app/Application.java").exists());
        assertTrue(new File(targetDir, "src/main/components/Main.wo/Main.html").exists());
        assertTrue(new File(targetDir, "build.properties").exists());
        assertTrue(new File(targetDir, "woproject").isDirectory());
        assertTrue(placeholder.exists());
    }

    @Test
    public void testMavenGenerationIfAvailable(@TempDir Path tempDir) throws Exception {
        Assumptions.assumeFalse("true".equalsIgnoreCase(System.getenv("CI")),
                "Skipping live Maven CLI archetype:generate test in CI");
        File mvnExe = WOArchetypeGenerator.findMavenExecutable();
        Assumptions.assumeTrue(mvnExe != null && mvnExe.exists(),
                "Skipping Maven CLI test because mvn executable was not found");

        WOProjectModel model = new WOProjectModel();
        model.setName("MavenTestApp");
        model.setArtifactId("MavenTestApp");
        model.setGroupId("com.maven.test");
        model.setVersion("1.0.0-SNAPSHOT");
        model.setPackageName("com.maven.test.app");
        model.setArchetype(WOArchetype.ERX_APPLICATION);
        model.setArchetypeVersion("3.0");
        model.setJavaVersion("17");
        model.setWonderVersion("7.2");

        // Target directory has a different name than artifactId to verify proper directory handling
        File targetDir = tempDir.resolve("CustomFolderForApp").toFile();
        targetDir.mkdirs();

        WOArchetypeGenerator.generateWithMaven(mvnExe, model, targetDir, null);

        File pom = new File(targetDir, "pom.xml");
        assertTrue(pom.exists(), "pom.xml must exist in targetDir");
        String pomContent = Files.readString(pom.toPath());
        assertTrue(pomContent.contains("<artifactId>MavenTestApp</artifactId>"));

        File appClass = new File(targetDir, "src/main/java/com/maven/test/app/app/Application.java");
        assertTrue(appClass.exists(), "Application.java must exist under src/main/java");

        File mainComponent = new File(targetDir, "src/main/components/Main.wo/Main.html");
        assertTrue(mainComponent.exists(), "Main.html must exist under src/main/components/Main.wo");

        assertTrue(new File(targetDir, "build.properties").exists(), "build.properties must exist");
        assertTrue(new File(targetDir, "woproject").isDirectory(), "woproject must exist");
    }

    @Test
    public void testModuleBuilderIgnoresProjectSettingsStep() {
        WOModuleBuilder builder = new WOModuleBuilder();
        java.util.List<Class<? extends com.intellij.ide.util.projectWizard.ModuleWizardStep>> ignored = builder.getIgnoredSteps();
        assertNotNull(ignored);
        assertFalse(ignored.isEmpty(), "Should ignore ProjectSettingsStep to prevent duplicate name/location step");
        assertEquals("com.intellij.ide.projectWizard.ProjectSettingsStep", ignored.get(0).getName());
    }
}

