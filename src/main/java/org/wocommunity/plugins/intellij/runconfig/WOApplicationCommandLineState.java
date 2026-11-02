package org.wocommunity.plugins.intellij.runconfig;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.util.PathsList;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.tools.WOProjectUtil;
import org.wocommunity.plugins.intellij.runconfig.data.KeyValueOption;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilderFactory;

public class WOApplicationCommandLineState<T extends WOApplicationConfiguration> extends ApplicationConfiguration.JavaApplicationCommandLineState<T> {
    public WOApplicationCommandLineState(T configuration, @NotNull ExecutionEnvironment environment) {
        super(configuration, environment);
    }

    @Override
    protected JavaParameters createJavaParameters() throws ExecutionException {
        JavaParameters javaParameters = super.createJavaParameters();

        com.intellij.openapi.module.Module module = myConfiguration.getConfigurationModule().getModule();
        Project project = myConfiguration.getProject();

        String resolvedMain = WOApplicationClassUtil.resolveExecutableMainClassName(project, module, javaParameters.getMainClass());
        if (resolvedMain != null && !resolvedMain.equals(javaParameters.getMainClass())) {
            javaParameters.setMainClass(resolvedMain);
        }

        String modulePath = WOProjectUtil.getModulePath(module);

        if(StringUtils.isEmpty(javaParameters.getWorkingDirectory())
            || javaParameters.getWorkingDirectory().equals(project.getBasePath()))
        {
            if (modulePath == null || modulePath.isBlank()) {
                return javaParameters;
            }
            String woaPath = resolveWoaDirectory(modulePath, module);
            javaParameters.setWorkingDirectory(woaPath);
        }

        ParametersList vmParametersList = javaParameters.getVMParametersList();

        Sdk jdk = javaParameters.getJdk();
        SdkTypeId sdkType = jdk.getSdkType();
        JavaSdk javaSdk = (JavaSdk) sdkType;
        if(javaSdk.isOfVersionOrHigher(jdk, JavaSdkVersion.JDK_11))
        {
            for(String vmParameter : myConfiguration.getOptions().getHigherJdkVMParameters()) {
                vmParametersList.add(vmParameter);
            }
        }

        ParametersList programParametersList = javaParameters.getProgramParametersList();

        for(KeyValueOption kvo : myConfiguration.getOptions().getWoOptions()) {
            if(!kvo.getActive())
                continue;

            if (kvo.key.startsWith("-D"))
            {
                vmParametersList.add(kvo.key + "=" + kvo.value);
            } else
            {
                programParametersList.add(kvo.key, kvo.value);
            }
        }

        new WOProjectUtil().createOrUpdateProjectDescriptionFile(new File(modulePath));

        // reorder classpath based on target/classpath.txt
        modifyClasspath(javaParameters);

        return javaParameters;
    }

    /**
     * Resolve the absolute path to the .woa directory in target.
     *
     * Prefers existing .woa directories in target/ if available, otherwise computes
     * the expected target/<finalName>.woa path.
     */
    public @NotNull String resolveWoaDirectory(@NotNull String modulePath, @NotNull com.intellij.openapi.module.Module module) {
        String finalName = getProjectFinalName(modulePath, module);
        Path targetDir = Paths.get(modulePath, "target");

        Path expectedWoa = targetDir.resolve(finalName + ".woa");
        if (Files.isDirectory(expectedWoa)) {
            return expectedWoa.toString();
        }

        if (Files.isDirectory(targetDir)) {
            try (var stream = Files.list(targetDir)) {
                List<Path> woaDirs = stream
                        .filter(Files::isDirectory)
                        .filter(p -> p.getFileName().toString().endsWith(".woa"))
                        .toList();

                if (woaDirs.size() == 1) {
                    return woaDirs.get(0).toString();
                } else if (woaDirs.size() > 1) {
                    Path best = findBestMatchingWoa(woaDirs, finalName);
                    if (best != null) {
                        return best.toString();
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return expectedWoa.toString();
    }

    /**
     * Determine the finalName of the current project without depending on the Maven plugin.
     *
     * Uses {@code pom.xml} if present:
     * - {@code <build><finalName>} if set (supports ${project.*}, ${pom.*}, and custom property substitutions)
     * - otherwise Maven default: {@code ${project.artifactId}-${project.version}} (or {@code <artifactId>} if version omitted)
     *
     * Also checks existing {@code target/*.woa} bundles if present.
     * Falls back to {@code module.getName()}.
     */
    public @NotNull String getProjectFinalName(@NotNull String modulePath, @NotNull com.intellij.openapi.module.Module module) {
        Path targetDir = Paths.get(modulePath, "target");
        String pomPath = Paths.get(modulePath, "pom.xml").toString();
        PomInfo info = null;
        try {
            Path pom = Paths.get(pomPath);
            if (Files.exists(pom)) {
                info = readPomInfo(pom);
            }
        } catch (Exception ignored) {
        }

        String computedName = null;
        if (info != null) {
            String base;
            if (info.finalName != null && !info.finalName.isBlank()) {
                base = info.finalName;
            } else if (info.version != null && !info.version.isBlank()) {
                base = info.artifactId + "-" + info.version;
            } else {
                base = info.artifactId;
            }
            if (base != null && !base.isBlank()) {
                computedName = substitutePomVars(base, info);
            }
        }

        if (computedName == null || computedName.isBlank()) {
            computedName = module.getName();
        }

        // If target/<computedName>.woa exists on disk, we have an exact match
        if (Files.isDirectory(targetDir.resolve(computedName + ".woa"))) {
            return computedName;
        }

        // If target/ contains existing .woa bundle(s) on disk:
        if (Files.isDirectory(targetDir)) {
            try (var stream = Files.list(targetDir)) {
                List<Path> woaDirs = stream
                        .filter(Files::isDirectory)
                        .filter(p -> p.getFileName().toString().endsWith(".woa"))
                        .toList();
                if (woaDirs.size() == 1) {
                    String folderName = woaDirs.get(0).getFileName().toString();
                    return folderName.substring(0, folderName.length() - ".woa".length());
                } else if (woaDirs.size() > 1) {
                    Path best = findBestMatchingWoa(woaDirs, computedName);
                    if (best != null) {
                        String folderName = best.getFileName().toString();
                        return folderName.substring(0, folderName.length() - ".woa".length());
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return computedName;
    }

    private static Path findBestMatchingWoa(List<Path> woaDirs, String expectedName) {
        List<Path> candidates = woaDirs.stream()
                .filter(p -> p.getFileName().toString().startsWith(expectedName))
                .toList();

        List<Path> toSort = candidates.isEmpty() ? woaDirs : candidates;
        return toSort.stream()
                .max(Comparator.comparingLong(p -> {
                    try {
                        return Files.getLastModifiedTime(p).toMillis();
                    } catch (IOException e) {
                        return 0L;
                    }
                }))
                .orElse(null);
    }

    static final class PomInfo {
        final String groupId;
        final String artifactId;
        final String version;
        final String name;
        final String finalName;
        final Map<String, String> properties;

        PomInfo(String groupId, String artifactId, String version, String name, String finalName, Map<String, String> properties) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.name = name;
            this.finalName = finalName;
            this.properties = properties != null ? properties : Collections.emptyMap();
        }
    }

    static PomInfo readPomInfo(@NotNull Path pom) {
        try (var in = Files.newInputStream(pom)) {
            var dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            try {
                dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
                dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            } catch (Exception ignored) {
            }

            var doc = dbf.newDocumentBuilder().parse(in);
            var root = doc.getDocumentElement();
            if (root == null) {
                return null;
            }

            // IMPORTANT: do not use getElementsByTagName() here, as it searches recursively and would
            // often return values from <parent> instead of the project's own coordinates.
            String groupId = directChildText(root, "groupId");
            String artifactId = directChildText(root, "artifactId");
            String version = directChildText(root, "version");
            String name = directChildText(root, "name");

            // If coordinates are inherited from <parent>, try parent nodes.
            org.w3c.dom.Element parentEl = directChildElement(root, "parent");
            if ((groupId == null || groupId.isBlank()) && parentEl != null) {
                groupId = directChildText(parentEl, "groupId");
            }
            if ((version == null || version.isBlank()) && parentEl != null) {
                version = directChildText(parentEl, "version");
            }
            if ((name == null || name.isBlank()) && parentEl != null) {
                name = directChildText(parentEl, "name");
            }

            String finalName = null;
            org.w3c.dom.Element buildEl = directChildElement(root, "build");
            if (buildEl != null) {
                finalName = directChildText(buildEl, "finalName");
            }

            Map<String, String> properties = new HashMap<>();
            org.w3c.dom.Element propsEl = directChildElement(root, "properties");
            if (propsEl != null) {
                var child = propsEl.getFirstChild();
                while (child != null) {
                    if (child instanceof org.w3c.dom.Element propChild) {
                        String pVal = propChild.getTextContent();
                        if (pVal != null) {
                            properties.put(propChild.getTagName(), pVal.trim());
                        }
                    }
                    child = child.getNextSibling();
                }
            }

            // If parent pom exists on disk and version/name/properties are still missing, try reading parent pom
            if (parentEl != null && (version == null || version.isBlank() || name == null || name.isBlank())) {
                try {
                    String relativePath = directChildText(parentEl, "relativePath");
                    Path parentPomPath = pom.getParent().resolve(relativePath != null && !relativePath.isBlank() ? relativePath : "../pom.xml").normalize();
                    if (Files.exists(parentPomPath) && !Files.isSameFile(pom, parentPomPath)) {
                        PomInfo parentInfo = readPomInfo(parentPomPath);
                        if (parentInfo != null) {
                            if (version == null || version.isBlank()) {
                                version = parentInfo.version;
                            }
                            if (name == null || name.isBlank()) {
                                name = parentInfo.name;
                            }
                            for (Map.Entry<String, String> entry : parentInfo.properties.entrySet()) {
                                properties.putIfAbsent(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            if (artifactId == null || artifactId.isBlank()) {
                return null;
            }
            return new PomInfo(groupId, artifactId, version, name, finalName, properties);
        } catch (Exception e) {
            return null;
        }
    }

    private static org.w3c.dom.Element directChildElement(@NotNull org.w3c.dom.Element parent, @NotNull String tagName) {
        var n = parent.getFirstChild();
        while (n != null) {
            if (n instanceof org.w3c.dom.Element e && tagName.equals(e.getTagName())) {
                return e;
            }
            n = n.getNextSibling();
        }
        return null;
    }

    private static String directChildText(@NotNull org.w3c.dom.Element parent, @NotNull String tagName) {
        org.w3c.dom.Element el = directChildElement(parent, tagName);
        if (el == null) {
            return null;
        }
        String t = el.getTextContent();
        return t != null ? t.trim() : null;
    }

    static @NotNull String substitutePomVars(@NotNull String s, @NotNull PomInfo info) {
        String out = s;
        for (int i = 0; i < 2; i++) {
            if (info.artifactId != null) {
                out = out.replace("${project.artifactId}", info.artifactId)
                         .replace("${artifactId}", info.artifactId)
                         .replace("${pom.artifactId}", info.artifactId);
            }
            String projName = (info.name != null && !info.name.isBlank()) ? info.name : info.artifactId;
            if (projName != null) {
                out = out.replace("${project.name}", projName)
                         .replace("${name}", projName)
                         .replace("${pom.name}", projName);
            }
            if (info.groupId != null) {
                out = out.replace("${project.groupId}", info.groupId)
                         .replace("${groupId}", info.groupId)
                         .replace("${pom.groupId}", info.groupId);
            }
            if (info.version != null) {
                out = out.replace("${project.version}", info.version)
                         .replace("${version}", info.version)
                         .replace("${pom.version}", info.version);
            }
            for (Map.Entry<String, String> prop : info.properties.entrySet()) {
                out = out.replace("${" + prop.getKey() + "}", prop.getValue())
                         .replace("${project.properties." + prop.getKey() + "}", prop.getValue());
            }
        }
        return out;
    }

    private void modifyClasspath(JavaParameters params) {
        try {
            // Path to classpath.txt file
            com.intellij.openapi.module.Module module = myConfiguration.getConfigurationModule().getModule();
            String modulePath = WOProjectUtil.getModulePath(module);
            if (modulePath == null) {
                return;
            }

            Path classpathFile = Paths.get(modulePath, "target", "classpath.txt");
            if (!Files.exists(classpathFile)) {
                return;
            }

            // Read desired order from classpath.txt
            List<String> desiredOrder = Files.readAllLines(classpathFile)
                    .stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toList());

            // Get current classpath
            PathsList currentClasspath = params.getClassPath();
            List<String> currentPaths = currentClasspath.getPathList();

            // Categorize paths - detect Maven repository paths
            List<String> nonMavenPaths = new ArrayList<>();
            List<String> mavenPaths = new ArrayList<>();

            for (String path : currentPaths) {
                if (isMavenRepositoryPath(path)) {
                    mavenPaths.add(path);
                } else {
                    nonMavenPaths.add(path);
                }
            }

            // Sort Maven paths based on classpath.txt
            List<String> sortedMavenPaths = new ArrayList<>();
            Set<String> processedPaths = new HashSet<>();

            // First add paths in desired order
            for (String desiredPath : desiredOrder) {
                for (String mavenPath : mavenPaths) {
                    if (matchesWithSnapshotAwareness(mavenPath, desiredPath)) {
                        if (!processedPaths.contains(mavenPath)) {
                            sortedMavenPaths.add(mavenPath);
                            processedPaths.add(mavenPath);
                        }
                    }
                }
            }

            // Add remaining Maven paths at the end
            for (String mavenPath : mavenPaths) {
                if (!processedPaths.contains(mavenPath)) {
                    sortedMavenPaths.add(mavenPath);
                }
            }

            // Assemble new classpath
            currentClasspath.clear();

            // 1. Non-Maven paths at the beginning
            for (String path : nonMavenPaths) {
                currentClasspath.add(path);
            }

            // 2. Sorted Maven paths
            for (String path : sortedMavenPaths) {
                currentClasspath.add(path);
            }

        } catch (IOException e) {
            // Ignore errors when reading classpath.txt, keep original classpath
        }
    }

    /**
     * Detects Maven repository paths based on typical patterns
     */
    private boolean isMavenRepositoryPath(String path) {
        // Maven Repository Pattern: /path/repository/groupId/artifactId/version/artifactId-version.jar
        // At least 3 directory levels after /repository/ for groupId/artifactId/version
        if (path.matches(".*[/\\\\]repository[/\\\\](?:[^/\\\\]+[/\\\\]){3,}[^/\\\\]+\\.jar$")) {
            return true;
        }

        // Fallback: Standard Maven Repository path
        String userHome = System.getProperty("user.home");
        String defaultM2RepoPath = Paths.get(userHome, ".m2", "repository").toString();
        return path.startsWith(defaultM2RepoPath);
    }

    /**
     * Matches Maven paths with SNAPSHOT-aware logic to handle timestamp variations
     * and ignores filename differences (e.g., "json-lib-2.4jdk15.jar" vs "json-lib-2.4-jdk15.jar")
     * 
     * The method compares only the directory structure, not the actual filename.
     * 
     * Examples:
     * - classpath.txt: "net/sf/json-lib/json-lib/2.4/json-lib-2.4jdk15.jar"
     * - actual path:   "/Users/user/.m2/repository/net/sf/json-lib/json-lib/2.4/json-lib-2.4-jdk15.jar"
     * 
     * Should match because they have the same directory structure: "net/sf/json-lib/json-lib/2.4/"
     */
    private boolean matchesWithSnapshotAwareness(String mavenPath, String desiredPath) {
        // Extract directory paths (without filename) for comparison
        String mavenDirectoryPath = getDirectoryPath(extractRelativeRepositoryPath(mavenPath));
        String desiredDirectoryPath = getDirectoryPath(desiredPath);
        
        if (mavenDirectoryPath == null || desiredDirectoryPath == null) {
            return false;
        }
        
        // Normalize SNAPSHOT paths for comparison
        String normalizedMavenPath = normalizeSnapshotPath(mavenDirectoryPath);
        String normalizedDesiredPath = normalizeSnapshotPath(desiredDirectoryPath);
        
        return normalizedMavenPath.equals(normalizedDesiredPath);
    }
    
    /**
     * Normalizes SNAPSHOT paths by replacing timestamp patterns with generic SNAPSHOT
     * 
     * Examples:
     * - "wonder/core/ERPDFGeneration/7.3.0-BOSCH-20250603.151922-265/ERPDFGeneration-7.3.0-BOSCH-20250603.151922-265.jar"
     *   becomes: "wonder/core/ERPDFGeneration/7.3.0-BOSCH-SNAPSHOT/ERPDFGeneration-7.3.0-BOSCH-SNAPSHOT.jar"
     */
    private String normalizeSnapshotPath(String path) {
        if (path == null) {
            return null;
        }
        
        // Pattern for Maven SNAPSHOT timestamps: YYYYMMDD.HHMMSS-buildNumber
        // e.g., "20250603.151922-265"
        String timestampPattern = "\\d{8}\\.\\d{6}-\\d+";
        
        // Replace timestamp patterns with "SNAPSHOT"
        // Handle both in directory names and file names
        return path.replaceAll(timestampPattern, "SNAPSHOT");
    }
    
    /**
     * Extracts the relative repository path from a full Maven path
     * 
     * Example:
     * - Input:  "/Users/user/.m2/repository/wonder/core/ERPDFGeneration/7.3.0-BOSCH-SNAPSHOT/ERPDFGeneration-7.3.0-BOSCH-20250603.133829-264.jar"
     * - Output: "wonder/core/ERPDFGeneration/7.3.0-BOSCH-SNAPSHOT/ERPDFGeneration-7.3.0-BOSCH-20250603.133829-264.jar"
     */
    private String extractRelativeRepositoryPath(String fullPath) {
        if (fullPath == null) {
            return null;
        }
        
        // Find the "repository" directory and extract everything after it
        String[] pathParts = fullPath.split("[/\\\\]");
        boolean foundRepository = false;
        StringBuilder relativePath = new StringBuilder();
        
        for (String part : pathParts) {
            if (foundRepository && !part.isEmpty()) {
                if (relativePath.length() > 0) {
                    relativePath.append("/");
                }
                relativePath.append(part);
            } else if ("repository".equals(part)) {
                foundRepository = true;
            }
        }
        
        return relativePath.length() > 0 ? relativePath.toString() : fullPath;
    }

    /**
     * Extracts the directory path from a full path by removing the filename
     * 
     * Example:
     * - Input:  "net/sf/json-lib/json-lib/2.4/json-lib-2.4-jdk15.jar"
     * - Output: "net/sf/json-lib/json-lib/2.4/"
     */
    private String getDirectoryPath(String fullPath) {
        if (fullPath == null || fullPath.isEmpty()) {
            return null;
        }
        
        // Find the last slash to separate directory from filename
        int lastSlashIndex = Math.max(fullPath.lastIndexOf('/'), fullPath.lastIndexOf('\\'));
        
        if (lastSlashIndex == -1) {
            // No directory separator found
            return "";
        }
        
        // Return directory path including trailing slash
        return fullPath.substring(0, lastSlashIndex + 1);
    }
}
