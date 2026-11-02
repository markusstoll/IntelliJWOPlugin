package org.wocommunity.plugins.intellij.wizards;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class WOArchetypeGenerator {
    private static final Logger LOG = Logger.getInstance(WOArchetypeGenerator.class);

    public static void generate(@NotNull WOProjectModel model, @NotNull File targetDir, @Nullable ProgressIndicator indicator) throws Exception {
        if (indicator != null) {
            indicator.setText("Preparing WebObjects project generation...");
        }

        File parentDir = targetDir.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        File mvnExe = findMavenExecutable();
        boolean generated = false;

        if (mvnExe != null && mvnExe.exists()) {
            try {
                if (indicator != null) {
                    indicator.setText("Generating project using Maven archetype...");
                }
                generateWithMaven(mvnExe, model, targetDir, indicator);
                generated = true;
            } catch (Exception e) {
                LOG.warn("Maven CLI archetype generation failed, falling back to direct extraction: " + e.getMessage(), e);
            }
        }

        if (!generated) {
            if (indicator != null) {
                indicator.setText("Extracting WebObjects archetype directly...");
            }
            generateDirectly(model, targetDir, indicator);
        }

        // Post-process pom.xml if needed
        File pomFile = new File(targetDir, "pom.xml");
        if (pomFile.exists() && model.isAddRepositoryToPom()) {
            addWOCommunityRepositories(pomFile);
        }

        // Refresh VFS
        VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetDir);
        if (vFile != null) {
            VfsUtil.markDirtyAndRefresh(false, true, true, vFile);
        }
    }

    public static void generateWithMaven(@NotNull File mvnExe,
                                         @NotNull WOProjectModel model,
                                         @NotNull File targetDir,
                                         @Nullable ProgressIndicator indicator) throws Exception {
        com.intellij.openapi.application.Application app = ApplicationManager.getApplication();
        if (app != null && (app.isDispatchThread() || app.holdsReadLock())) {
            LOG.warn("generateWithMaven called on EDT or under read lock, falling back to direct extraction");
            generateDirectly(model, targetDir, indicator);
            return;
        }

        targetDir.mkdirs();

        Path tempGenDir = Files.createTempDirectory("wo-gen-");
        try {
            GeneralCommandLine cmd = new GeneralCommandLine();
            cmd.setExePath(mvnExe.getAbsolutePath());
            cmd.setWorkDirectory(tempGenDir.toFile());

            cmd.addParameters("archetype:generate", "-B");
            cmd.addParameter("-DarchetypeGroupId=" + WOArchetype.GROUP_ID);
            cmd.addParameter("-DarchetypeArtifactId=" + model.getArchetype().getArtifactId());
            cmd.addParameter("-DarchetypeVersion=" + model.getArchetypeVersion());
            cmd.addParameter("-DarchetypeRepository=" + WOArchetype.REPOSITORY_URL);
            cmd.addParameter("-DgroupId=" + model.getGroupId());
            cmd.addParameter("-DartifactId=" + model.getArtifactId());
            cmd.addParameter("-Dversion=" + model.getVersion());
            cmd.addParameter("-Dpackage=" + model.getPackageName());
            cmd.addParameter("-DJavaVersion=" + model.getJavaVersion());

            if (model.getArchetype().isWonder()) {
                cmd.addParameter("-DWonderVersion=" + model.getWonderVersion());
            }
            if (model.getArchetype() == WOArchetype.WO_FRAMEWORK) {
                cmd.addParameter("-DWonderSupport=" + (model.isWonderSupport() ? "yes" : "no"));
                cmd.addParameter("-DWonderVersion=" + model.getWonderVersion());
            }
            cmd.addParameter("-DinteractiveMode=false");

            CapturingProcessHandler handler = new CapturingProcessHandler(cmd);
            ProcessOutput output = (indicator != null)
                    ? handler.runProcessWithProgressIndicator(indicator, 120_000)
                    : handler.runProcess(120_000);

            if (output.getExitCode() != 0) {
                String err = output.getStderr();
                if (err == null || err.isBlank()) {
                    err = output.getStdout();
                }
                throw new IOException("Maven archetype generation failed (exit code " + output.getExitCode() + "): " + err);
            }

            File generatedProject = new File(tempGenDir.toFile(), model.getArtifactId());
            if (!generatedProject.exists()) {
                File[] subDirs = tempGenDir.toFile().listFiles(File::isDirectory);
                if (subDirs != null && subDirs.length == 1) {
                    generatedProject = subDirs[0];
                }
            }

            if (generatedProject != null && generatedProject.exists()) {
                moveDirectoryContents(generatedProject, targetDir);
            } else {
                throw new IOException("Generated project directory not found in temporary directory: " + tempGenDir);
            }
        } finally {
            deleteRecursively(tempGenDir.toFile());
        }
    }

    public static void generateDirectly(@NotNull WOProjectModel model,
                                        @NotNull File targetDir,
                                        @Nullable ProgressIndicator indicator) throws Exception {
        targetDir.mkdirs();
        File jarFile = resolveArchetypeJar(model.getArchetype(), model.getArchetypeVersion(), indicator);

        try (JarFile jar = new JarFile(jarFile)) {
            String prefix = "archetype-resources/";
            String packagePath = model.getPackageName().replace('.', '/');

            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(prefix) || name.equals(prefix)) {
                    continue;
                }

                String relativePath = name.substring(prefix.length());
                if (relativePath.startsWith("/")) {
                    relativePath = relativePath.substring(1);
                }

                File destinationFile;
                if (relativePath.startsWith("src/main/java/")) {
                    String sub = relativePath.substring("src/main/java/".length());
                    destinationFile = new File(targetDir, "src/main/java/" + packagePath + "/" + sub);
                } else if (relativePath.startsWith("src/test/java/")) {
                    String sub = relativePath.substring("src/test/java/".length());
                    destinationFile = new File(targetDir, "src/test/java/" + packagePath + "/" + sub);
                } else {
                    destinationFile = new File(targetDir, relativePath);
                }

                if (entry.isDirectory()) {
                    destinationFile.mkdirs();
                    continue;
                }

                File parent = destinationFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }

                if (isTextFile(relativePath)) {
                    try (InputStream is = jar.getInputStream(entry)) {
                        String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        String transformed = transformTemplateContent(content, model);
                        Files.writeString(destinationFile.toPath(), transformed, StandardCharsets.UTF_8);
                    }
                } else {
                    try (InputStream is = jar.getInputStream(entry)) {
                        Files.copy(is, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    public static @NotNull String transformTemplateContent(@NotNull String content, @NotNull WOProjectModel model) {
        String result = content;
        // Remove Velocity directive #set($dollar = '$')
        result = result.replaceAll("(?m)^#set\\(\\$dollar\\s*=\\s*'\\$'\\)\\r?\\n?", "");
        result = result.replace("${dollar}{", "${");
        result = result.replace("${dollar}", "$");

        result = result.replace("${groupId}", model.getGroupId());
        result = result.replace("${artifactId}", model.getArtifactId());
        result = result.replace("${version}", model.getVersion());
        result = result.replace("${package}", model.getPackageName());
        result = result.replace("${JavaVersion}", model.getJavaVersion());
        result = result.replace("${WonderVersion}", model.getWonderVersion());
        result = result.replace("${WonderSupport}", model.isWonderSupport() ? "yes" : "no");
        return result;
    }

    private static boolean isTextFile(@NotNull String path) {
        String lower = path.toLowerCase();
        return lower.endsWith(".java")
                || lower.endsWith(".xml")
                || lower.endsWith(".properties")
                || lower.endsWith(".plist")
                || lower.endsWith(".html")
                || lower.endsWith(".wod")
                || lower.endsWith(".woo")
                || lower.endsWith(".api")
                || lower.endsWith(".txt")
                || lower.endsWith(".project")
                || lower.endsWith(".classpath");
    }

    private static File resolveArchetypeJar(@NotNull WOArchetype archetype,
                                           @NotNull String archetypeVersion,
                                           @Nullable ProgressIndicator indicator) throws IOException {
        // 1. Check local Maven repository
        String home = System.getProperty("user.home");
        Path localPath = Paths.get(home, ".m2", "repository", "org", "wocommunity",
                archetype.getArtifactId(), archetypeVersion,
                archetype.getArtifactId() + "-" + archetypeVersion + ".jar");
        if (Files.exists(localPath)) {
            return localPath.toFile();
        }

        // 2. Check cached download in system temp
        Path cacheDir = Paths.get(System.getProperty("java.io.tmpdir"), "wocommunity-archetypes");
        Files.createDirectories(cacheDir);
        Path cachedJar = cacheDir.resolve(archetype.getArtifactId() + "-" + archetypeVersion + ".jar");
        if (Files.exists(cachedJar) && Files.size(cachedJar) > 1000) {
            return cachedJar.toFile();
        }

        // 3. Download from remote repository
        String urlString = WOArchetype.REPOSITORY_URL + "org/wocommunity/"
                + archetype.getArtifactId() + "/" + archetypeVersion + "/"
                + archetype.getArtifactId() + "-" + archetypeVersion + ".jar";

        if (indicator != null) {
            indicator.setText("Downloading archetype from " + urlString + "...");
        }

        downloadFile(urlString, cachedJar);
        return cachedJar.toFile();
    }

    private static void downloadFile(@NotNull String urlString, @NotNull Path targetFile) throws IOException {
        URL url = URI.create(urlString).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);

        int code = conn.getResponseCode();
        if (code >= 300 && code < 400) {
            String redirectUrl = conn.getHeaderField("Location");
            if (redirectUrl != null) {
                downloadFile(redirectUrl, targetFile);
                return;
            }
        }

        if (code != 200) {
            throw new IOException("Failed to download archetype jar from " + urlString + " (HTTP " + code + ")");
        }

        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void addWOCommunityRepositories(@NotNull File pomFile) {
        try {
            String content = Files.readString(pomFile.toPath(), StandardCharsets.UTF_8);
            if (content.contains("maven.wocommunity.org")) {
                return;
            }

            int endProject = content.lastIndexOf("</project>");
            if (endProject < 0) {
                return;
            }

            String repoSnippet = """
\t<repositories>
\t\t<repository>
\t\t\t<id>wocommunity</id>
\t\t\t<name>WOCommunity Maven Repository</name>
\t\t\t<url>https://maven.wocommunity.org/content/groups/public/</url>
\t\t\t<releases>
\t\t\t\t<enabled>true</enabled>
\t\t\t</releases>
\t\t\t<snapshots>
\t\t\t\t<enabled>true</enabled>
\t\t\t</snapshots>
\t\t</repository>
\t</repositories>
\t<pluginRepositories>
\t\t<pluginRepository>
\t\t\t<id>wocommunity</id>
\t\t\t<name>WOCommunity Maven Plugin Repository</name>
\t\t\t<url>https://maven.wocommunity.org/content/groups/public/</url>
\t\t\t<releases>
\t\t\t\t<enabled>true</enabled>
\t\t\t</releases>
\t\t\t<snapshots>
\t\t\t\t<enabled>true</enabled>
\t\t\t</snapshots>
\t\t</pluginRepository>
\t</pluginRepositories>
""";

            String updated = content.substring(0, endProject) + repoSnippet + content.substring(endProject);
            Files.writeString(pomFile.toPath(), updated, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.warn("Failed to inject WOCommunity repository to pom.xml: " + e.getMessage(), e);
        }
    }

    public static void importMavenProjectIfPossible(@NotNull Project project, @NotNull File targetDir) {
        try {
            File pomFile = new File(targetDir, "pom.xml");
            if (!pomFile.exists()) {
                return;
            }
            VirtualFile vPom = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(pomFile);
            if (vPom == null) {
                return;
            }
            VfsUtil.markDirtyAndRefresh(false, true, true, vPom);

            org.jetbrains.idea.maven.project.MavenProjectsManager mpm =
                    org.jetbrains.idea.maven.project.MavenProjectsManager.getInstance(project);
            if (mpm != null) {
                mpm.addManagedFilesOrUnignore(Collections.singletonList(vPom));
            }
        } catch (Throwable t) {
            LOG.warn("Failed to import Maven project: " + t.getMessage(), t);
        }
    }

    public static void deleteRecursively(@NotNull File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteRecursively(f);
                }
            }
        }
        file.delete();
    }

    public static @Nullable File findMavenExecutable() {
        String envMaven = System.getenv("MAVEN_HOME");
        if (envMaven == null || envMaven.isBlank()) {
            envMaven = System.getenv("M2_HOME");
        }
        if (envMaven != null && !envMaven.isBlank()) {
            File f = new File(envMaven, "bin/mvn" + (SystemInfo.isWindows ? ".cmd" : ""));
            if (f.exists() && f.canExecute()) {
                return f;
            }
        }

        // Check common Unix paths
        String[] candidates = {
                "/opt/homebrew/bin/mvn",
                "/usr/local/bin/mvn",
                "/usr/bin/mvn"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists() && f.canExecute()) {
                return f;
            }
        }

        // Check IntelliJ's bundled maven
        try {
            String home = PathManager.getHomePath();
            File bundled = new File(home, "plugins/maven/lib/maven3/bin/mvn" + (SystemInfo.isWindows ? ".cmd" : ""));
            if (bundled.exists()) {
                return bundled;
            }
        } catch (Throwable ignored) {
        }

        // Check PATH
        String path = System.getenv("PATH");
        if (path != null) {
            String exeName = SystemInfo.isWindows ? "mvn.cmd" : "mvn";
            for (String p : path.split(File.pathSeparator)) {
                File f = new File(p, exeName);
                if (f.exists() && f.canExecute()) {
                    return f;
                }
            }
        }

        return null;
    }

    private static void moveDirectoryContents(@NotNull File src, @NotNull File dst) throws IOException {
        dst.mkdirs();
        File[] files = src.listFiles();
        if (files == null) return;
        for (File f : files) {
            File target = new File(dst, f.getName());
            if (f.isDirectory()) {
                moveDirectoryContents(f, target);
                f.delete();
            } else {
                Files.move(f.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        src.delete();
    }
}

