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
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.tools.WOProjectUtil;
import org.wocommunity.plugins.intellij.runconfig.data.KeyValueOption;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.model.MavenModel;
import org.jetbrains.idea.maven.model.MavenId;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class WOApplicationCommandLineState<T extends WOApplicationConfiguration> extends ApplicationConfiguration.JavaApplicationCommandLineState<T> {
    public WOApplicationCommandLineState(T configuration, @NotNull ExecutionEnvironment environment) {
        super(configuration, environment);
    }

    @Override
    protected JavaParameters createJavaParameters() throws ExecutionException {
        JavaParameters javaParameters = super.createJavaParameters();

        com.intellij.openapi.module.Module module = myConfiguration.getConfigurationModule().getModule();
        String modulePath = WOProjectUtil.getModulePath(module);

        Project project = myConfiguration.getProject();

        if(StringUtils.isEmpty(javaParameters.getWorkingDirectory())
            || javaParameters.getWorkingDirectory().equals(project.getBasePath()))
        {
            javaParameters.setWorkingDirectory(modulePath + "/target/" + getProjectFinalName(project, module) + ".woa");
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
     * This method determines the finalName of the current maven project.
     * Support custom variables (which maven does, but IntelliJ does not take care of)
     * @param project
     * @param module
     * @return
     */
    public String getProjectFinalName(Project project, com.intellij.openapi.module.Module module) {
        MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(project);
        MavenProject mavenProject = mavenProjectsManager.findProject(module);

        String finalName = mavenProject.getFinalName();

        org.jetbrains.idea.maven.model.MavenId mavenId = mavenProject.getMavenId();
        String artifactId = mavenId.getArtifactId();
        String version = mavenId.getVersion();
        String groupId = mavenId.getGroupId();

        if (finalName != null) {
            finalName = finalName.replace("${project.artifactId}", artifactId)
                    .replace("${project.version}", version)
                    .replace("${project.name}", artifactId)
                    .replace("${project.groupId}", groupId);
            return finalName;
        }

        return null;
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
     * 
     * Examples:
     * - classpath.txt: "wonder/core/ERPDFGeneration/7.3.0-BOSCH-20250603.151922-265/ERPDFGeneration-7.3.0-BOSCH-20250603.151922-265.jar"
     * - actual path:   "wonder/core/ERPDFGeneration/7.3.0-BOSCH-SNAPSHOT/ERPDFGeneration-7.3.0-BOSCH-20250603.133829-264.jar"
     * 
     * Should match because they represent the same artifact with different SNAPSHOT timestamps
     */
    private boolean matchesWithSnapshotAwareness(String mavenPath, String desiredPath) {
        // Simple exact match first (most common case)
        if (mavenPath.endsWith(desiredPath)) {
            return true;
        }
        
        // Extract normalized paths for SNAPSHOT comparison
        String normalizedMavenPath = normalizeSnapshotPath(extractRelativeRepositoryPath(mavenPath));
        String normalizedDesiredPath = normalizeSnapshotPath(desiredPath);
        
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
}
