package com.intellij.plugins.wo;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.components.BaseState;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class WOConfigurationFactory extends ConfigurationFactory {
    public WOConfigurationFactory(WORunConfigType type) {
        super(type);
    }

    @Override
    public @NotNull String getId() {
        return WORunConfigType.ID;
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new WORunConfig("WebObjects", this, project);
    }

    @Override
    public Class<? extends BaseState> getOptionsClass() {
        return WORunConfigurationOptions.class;
    }

}

