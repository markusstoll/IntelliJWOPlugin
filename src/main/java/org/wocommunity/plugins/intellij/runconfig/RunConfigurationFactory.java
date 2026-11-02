package org.wocommunity.plugins.intellij.runconfig;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.components.BaseState;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.tools.WOProjectUtil;

import java.io.File;
import java.nio.file.Files;

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
        Module defaultModule = findDefaultModule(project);
        String name = defaultModule != null ? defaultModule.getName() : project.getName();
        WOApplicationConfiguration config = new WOApplicationConfiguration(name, this, project);
        if (defaultModule != null) {
            config.setModule(defaultModule);
        }
        config.setGeneratedName();
        return config;
    }

    @NotNull
    @Override
    public RunConfiguration createConfiguration(@Nullable String name, @NotNull RunConfiguration template) {
        RunConfiguration configuration = super.createConfiguration(name, template);
        if (configuration instanceof WOApplicationConfiguration woConfig) {
            Project project = woConfig.getProject();
            if (woConfig.getConfigurationModule().getModule() == null) {
                Module defaultModule = findDefaultModule(project);
                if (defaultModule != null) {
                    woConfig.setModule(defaultModule);
                }
            }
            if (name == null || name.isBlank() || "Unnamed".equalsIgnoreCase(name) || "WebObjects".equals(name)) {
                String suggested = woConfig.suggestedName();
                woConfig.setName(suggested != null && !suggested.isBlank() ? suggested : project.getName());
                woConfig.setGeneratedName();
            }
        }
        return configuration;
    }

    @Nullable
    public static Module findDefaultModule(@NotNull Project project) {
        Module[] modules = ModuleManager.getInstance(project).getModules();
        if (modules.length == 0) {
            return null;
        }
        if (modules.length == 1) {
            return modules[0];
        }
        // If multiple modules, prefer one that looks like a WO application (e.g. packaging woapplication / woproject)
        for (Module m : modules) {
            String path = WOProjectUtil.getModulePath(m);
            if (path != null) {
                File pomFile = new File(path, "pom.xml");
                if (pomFile.exists()) {
                    try {
                        String content = Files.readString(pomFile.toPath());
                        if (content.contains("woapplication") || content.contains("woproject")) {
                            return m;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return modules[0];
    }

    @Override
    public Class<? extends BaseState> getOptionsClass() {
        return WORunConfigurationOptions.class;
    }
}


