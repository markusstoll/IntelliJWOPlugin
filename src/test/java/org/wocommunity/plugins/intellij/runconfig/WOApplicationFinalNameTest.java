package org.wocommunity.plugins.intellij.runconfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class WOApplicationFinalNameTest {

    @Test
    public void testFinalNameWithProjectNameAndExplicitName(@TempDir Path tempDir) throws IOException {
        Path pom = tempDir.resolve("pom.xml");
        Files.writeString(pom, """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>my-artifact</artifactId>
                <version>1.0.0</version>
                <name>MyCoolApplication</name>
                <build>
                    <finalName>${project.name}</finalName>
                </build>
            </project>
            """);

        WOApplicationCommandLineState.PomInfo info = WOApplicationCommandLineState.readPomInfo(pom);
        assertNotNull(info);
        assertEquals("MyCoolApplication", info.name);
        assertEquals("${project.name}", info.finalName);

        String result = WOApplicationCommandLineState.substitutePomVars(info.finalName, info);
        assertEquals("MyCoolApplication", result);
    }

    @Test
    public void testFinalNameWithProjectNameFallbackToArtifactId(@TempDir Path tempDir) throws IOException {
        Path pom = tempDir.resolve("pom.xml");
        Files.writeString(pom, """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>MyArtifactOnly</artifactId>
                <version>1.0.0</version>
                <build>
                    <finalName>${project.name}</finalName>
                </build>
            </project>
            """);

        WOApplicationCommandLineState.PomInfo info = WOApplicationCommandLineState.readPomInfo(pom);
        assertNotNull(info);
        assertNull(info.name);
        assertEquals("${project.name}", info.finalName);

        String result = WOApplicationCommandLineState.substitutePomVars(info.finalName, info);
        assertEquals("MyArtifactOnly", result);
    }

    @Test
    public void testDefaultFinalNameWithVersion(@TempDir Path tempDir) throws IOException {
        Path pom = tempDir.resolve("pom.xml");
        Files.writeString(pom, """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>DefaultArtifact</artifactId>
                <version>2.5.1-SNAPSHOT</version>
            </project>
            """);

        WOApplicationCommandLineState.PomInfo info = WOApplicationCommandLineState.readPomInfo(pom);
        assertNotNull(info);
        assertNull(info.finalName);

        String base = (info.version != null && !info.version.isBlank())
                ? info.artifactId + "-" + info.version
                : info.artifactId;
        assertEquals("DefaultArtifact-2.5.1-SNAPSHOT", base);
    }

    @Test
    public void testDefaultFinalNameWithoutVersion(@TempDir Path tempDir) throws IOException {
        Path pom = tempDir.resolve("pom.xml");
        Files.writeString(pom, """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>DefaultArtifactNoVer</artifactId>
            </project>
            """);

        WOApplicationCommandLineState.PomInfo info = WOApplicationCommandLineState.readPomInfo(pom);
        assertNotNull(info);
        assertNull(info.finalName);

        String base = (info.version != null && !info.version.isBlank())
                ? info.artifactId + "-" + info.version
                : info.artifactId;
        assertEquals("DefaultArtifactNoVer", base);
    }

    @Test
    public void testFinalNameWithCustomProperties(@TempDir Path tempDir) throws IOException {
        Path pom = tempDir.resolve("pom.xml");
        Files.writeString(pom, """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>prop-artifact</artifactId>
                <version>3.0.0</version>
                <properties>
                    <app.bundle.name>MyCustomBundle</app.bundle.name>
                </properties>
                <build>
                    <finalName>${app.bundle.name}</finalName>
                </build>
            </project>
            """);

        WOApplicationCommandLineState.PomInfo info = WOApplicationCommandLineState.readPomInfo(pom);
        assertNotNull(info);
        assertEquals("MyCustomBundle", info.properties.get("app.bundle.name"));

        String result = WOApplicationCommandLineState.substitutePomVars(info.finalName, info);
        assertEquals("MyCustomBundle", result);
    }

    @Test
    public void testInheritedVersionFromParentPom(@TempDir Path tempDir) throws IOException {
        Path parentPom = tempDir.resolve("parent-pom.xml");
        Files.writeString(parentPom, """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>parent-proj</artifactId>
                <version>4.1.0-RC1</version>
                <name>ParentProject</name>
            </project>
            """);

        Path childDir = tempDir.resolve("child");
        Files.createDirectories(childDir);
        Path childPom = childDir.resolve("pom.xml");
        Files.writeString(childPom, """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <parent>
                    <groupId>com.example</groupId>
                    <artifactId>parent-proj</artifactId>
                    <version>4.1.0-RC1</version>
                    <relativePath>../parent-pom.xml</relativePath>
                </parent>
                <artifactId>child-artifact</artifactId>
            </project>
            """);

        WOApplicationCommandLineState.PomInfo info = WOApplicationCommandLineState.readPomInfo(childPom);
        assertNotNull(info);
        assertEquals("4.1.0-RC1", info.version);
        assertEquals("child-artifact", info.artifactId);

        String base = (info.version != null && !info.version.isBlank())
                ? info.artifactId + "-" + info.version
                : info.artifactId;
        assertEquals("child-artifact-4.1.0-RC1", base);
    }

    @Test
    public void testExistingWoaDirectoryInTargetFound(@TempDir Path tempDir) throws IOException {
        // Create target directory with an existing .woa folder
        Path targetDir = tempDir.resolve("target");
        Files.createDirectories(targetDir);
        Path woaFolder = targetDir.resolve("RealApp-1.0.0.woa");
        Files.createDirectories(woaFolder);

        Path pom = tempDir.resolve("pom.xml");
        Files.writeString(pom, """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>RealApp</artifactId>
                <version>1.0.0</version>
            </project>
            """);

        // Single .woa in target matches
        try (var stream = Files.list(targetDir)) {
            var woaDirs = stream
                    .filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().endsWith(".woa"))
                    .toList();
            assertEquals(1, woaDirs.size());
            assertEquals("RealApp-1.0.0.woa", woaDirs.get(0).getFileName().toString());
        }
    }
}

