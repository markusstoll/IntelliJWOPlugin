package com.intellij.plugins.wo;

import com.intellij.execution.JavaRunConfigurationBase;
import com.intellij.execution.JavaRunConfigurationExtensionManager;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.project.Project;

public class WORunConfig extends JavaRunConfigurationBase {
    public WORunConfig(String name, ConfigurationFactory factory, Project project) {
        super(name, new JavaRunConfigurationModule(project, false), factory);
        runConfigData = new RunConfigData();
    }
}
