package com.intellij.plugins.wo;

import com.intellij.execution.Executor;
import com.intellij.execution.JavaRunConfigurationBase;
import com.intellij.execution.JavaRunConfigurationExtensionManager;
import com.intellij.execution.ShortenCommandLine;
import com.intellij.execution.application.JavaSettingsEditorBase;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.target.LanguageRuntimeType;
import com.intellij.execution.target.TargetEnvironmentAwareRunProfile;
import com.intellij.execution.target.TargetEnvironmentConfiguration;
import com.intellij.execution.ui.CommonParameterFragments;
import com.intellij.execution.ui.ModuleClasspathCombo;
import com.intellij.execution.ui.SettingsEditorFragment;
import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class WORunConfig extends JavaRunConfigurationBase implements TargetEnvironmentAwareRunProfile {
    private final RunConfigData runConfigData; // TODO use RunConfigurationOptions ?

    public WORunConfig(String name, ConfigurationFactory factory, Project project) {
        super(name, new JavaRunConfigurationModule(project, false), factory);
        runConfigData = new RunConfigData();
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        if (getOptions().getModule() == null) {
            throw new RuntimeConfigurationError("Module must be selected");
        }
    }

    @Override
    public Collection<Module> getValidModules() {
        return Arrays.asList(ModuleManager.getInstance(getProject()).getModules());
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
//        return new PageObjectConfigurable(this, getProject());
        return new WOApplicationSettingsEditor(this);
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor,
                                    @NotNull ExecutionEnvironment environment) {
        return new WORunState(environment, this);
    }

    @Override
    public void setVMParameters(String value) {
        runConfigData.setVMParameters(value);
    }

    @Override
    public String getVMParameters() {
        return runConfigData.getVMParameters();
    }

    @Override
    public boolean isAlternativeJrePathEnabled() {
        return runConfigData.isAlternativeJrePathEnabled();
    }

    @Override
    public void setAlternativeJrePathEnabled(boolean enabled) {
        runConfigData.setAlternativeJrePathEnabled(enabled);
    }

    @Override
    public String getAlternativeJrePath() {
        return runConfigData.getAlternativeJrePath();
    }

    @Override
    public void setAlternativeJrePath(String path) {
        runConfigData.setAlternativeJrePath(path);
    }

    @Override
    public @Nullable String getRunClass() {
        return "";
    }

    public @Nullable String getMainClassName() {
        return runConfigData.mainClassName;
    }

    public void setMainClassName(@Nullable String value) {
        runConfigData.setMainClassName(value);
    }

    @Nullable
    @Override
    public String getPackage() {
        return runConfigData.getPackage();
    }

    @Override
    public void setProgramParameters(@Nullable String value) {
        runConfigData.setProgramParameters(value);
    }

    @Nullable
    @Override
    public String getProgramParameters() {
        return runConfigData.getProgramParameters();
    }

    @Override
    public void setWorkingDirectory(@Nullable String value) {
        runConfigData.setWorkingDirectory(value);
    }

    @Nullable
    @Override
    public String getWorkingDirectory() {
        return runConfigData.getWorkingDirectory();
    }

    @Override
    public void setEnvs(@NotNull Map<String, String> envs) {
        runConfigData.setEnvs(envs);
    }

    @NotNull
    @Override
    public Map<String, String> getEnvs() {
        return runConfigData.getEnvs();
    }

    @Override
    public void setPassParentEnvs(boolean passParentEnvs) {
        runConfigData.setPassParentEnvs(passParentEnvs);
    }

    @Override
    public boolean isPassParentEnvs() {
        return runConfigData.isPassParentEnvs();
    }

    @Override
    public void writeExternal(@NotNull Element element) throws WriteExternalException {
        super.writeExternal(element);
        JavaRunConfigurationExtensionManager.getInstance().writeExternal(this, element);

        PathMacroManager.getInstance(getProject()).collapsePathsRecursively(element);
    }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        PathMacroManager.getInstance(getProject()).expandPaths(element);
        super.readExternal(element);
        JavaRunConfigurationExtensionManager.getInstance().readExternal(this, element);
        readModule(element);
    }

    @Override
    public @Nullable ShortenCommandLine getShortenCommandLine() {
        return null;
    }

    @Override
    public void setShortenCommandLine(@Nullable ShortenCommandLine mode) {

    }

    @Override
    public boolean canRunOn(@NotNull TargetEnvironmentConfiguration target) {
        return false;
    }

    @Override
    public @Nullable LanguageRuntimeType<?> getDefaultLanguageRuntimeType() {
        return null;
    }

    @Override
    public @Nullable String getDefaultTargetName() {
        return "";
    }

    @Override
    public void setDefaultTargetName(@Nullable String targetName) {

    }

    public static class RunConfigData {

        private boolean passParentEnvs;
        private Map<String, String> envs = new HashMap<>();
        private String workingDirectory;
        private String programParameters;
        private String VMParameters;
        private boolean alternativeJrePathEnabled;
        private String alternativeJrePath;
        private String pageObjectClass;
        private String aPackage;
        private String mainClassName;

        public void setEnvs(Map<String, String> envs) {
            this.envs = envs;
        }

        public Map<String, String> getEnvs() {
            return envs;
        }

        public void setWorkingDirectory(String workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        public String getWorkingDirectory() {
            return workingDirectory;
        }

        public void setProgramParameters(String programParameters) {
            this.programParameters = programParameters;
        }

        public String getProgramParameters() {
            return programParameters;
        }

        public void setVMParameters(String VMParameters) {
            this.VMParameters = VMParameters;
        }

        public String getVMParameters() {
            return VMParameters;
        }

        public void setAlternativeJrePathEnabled(boolean alternativeJrePathEnabled) {
            this.alternativeJrePathEnabled = alternativeJrePathEnabled;
        }

        public boolean isAlternativeJrePathEnabled() {
            return alternativeJrePathEnabled;
        }

        public void setAlternativeJrePath(String alternativeJrePath) {
            this.alternativeJrePath = alternativeJrePath;
        }

        public String getAlternativeJrePath() {
            return alternativeJrePath;
        }

        public boolean isPassParentEnvs() {
            return passParentEnvs;
        }

        public void setPassParentEnvs(boolean passParentEnvs) {
            this.passParentEnvs = passParentEnvs;
        }

        public String getPageObjectClass() {
            return pageObjectClass;
        }

        public void setPageObjectClass(String pageObjectClass) {
            this.pageObjectClass = pageObjectClass;
        }

        public String getPackage() {
            return aPackage;
        }

        public String getaPackage() {
            return aPackage;
        }

        public void setaPackage(String aPackage) {
            this.aPackage = aPackage;
        }

        public String getMainClassName() {
            return mainClassName;
        }

        public void setMainClassName(String mainClassName) {
            this.mainClassName = mainClassName;
        }
    }
}
