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
import java.util.Map;

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

        return javaParameters;
    }

    /**
     * This method determines the finalName of the current maven project.
     * This looks stupid, but unfortunately mavenProject.getFinalName() is using
     * the projects parent artifactId, so the output would be wrong in some cases
     * @param project
     * @param module
     * @return
     */
    public String getProjectFinalName(Project project, com.intellij.openapi.module.Module module) {
        MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(project);
        MavenProject mavenProject = mavenProjectsManager.findProject(module);
        @NotNull Map<String, String> map = mavenProject.getModelMap();
        if (mavenProject != null) {
            org.jetbrains.idea.maven.model.MavenId mavenId = mavenProject.getMavenId();
            String artifactId = mavenId.getArtifactId();
            String version = mavenId.getVersion();
            String groupId = mavenId.getGroupId();

            com.intellij.openapi.vfs.VirtualFile pomFile = mavenProject.getFile();
            if (pomFile != null && pomFile.exists()) {
                try {
                    String pomContent = new String(pomFile.contentsToByteArray());
                    int finalNameStart = pomContent.indexOf("<finalName>");
                    if (finalNameStart != -1) {
                        finalNameStart += "<finalName>".length();
                        int finalNameEnd = pomContent.indexOf("</finalName>", finalNameStart);
                        if (finalNameEnd != -1) {
                            String finalName = pomContent.substring(finalNameStart, finalNameEnd).trim();
                            if (!finalName.isEmpty()) {
                                finalName = finalName.replace("${project.artifactId}", artifactId)
                                        .replace("${project.version}", version)
                                        .replace("${project.name}", artifactId)
                                        .replace("${project.groupId}", groupId);
                                return finalName;
                            }
                        }
                    }
                } catch (Exception e) {
                    // Fehler beim Lesen der POM ignorieren
                }
            }
            return artifactId + "-" + version;
        }
        return null;
    }
}
