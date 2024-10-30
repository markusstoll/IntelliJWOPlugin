package org.wocommunity.plugins.intellij;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.components.BaseState;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class RunConfigurationFactory extends ConfigurationFactory {
    public RunConfigurationFactory(WORunConfigType type) {
        super(type);
    }

    @Override
    public @NotNull String getId() {
        return WORunConfigType.ID;
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        WOApplicationConfiguration woApplicationConfiguration = new WOApplicationConfiguration("WebObjects", this, project);

        return woApplicationConfiguration;
    }

    @Override
    public Class<? extends BaseState> getOptionsClass() {
        return WORunConfigurationOptions.class;
    }

}

