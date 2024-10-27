package org.wocommunity.plugins.intellij;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import com.intellij.util.PathUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class WOApplicationConfiguration extends ApplicationConfiguration {

    public WOApplicationConfiguration(String name, ConfigurationFactory factory, Project project) {
        super(name, project, factory);
    }

    @Override
    public void readExternal(@NotNull Element element) {
        super.readExternal(element);

        if(WORKING_DIRECTORY != null
            && WORKING_DIRECTORY.equals(PathUtil.toSystemDependentName(getProject().getBasePath())))
        {
            WORKING_DIRECTORY = WORKING_DIRECTORY + "/build/" + getProject().getName() + ".woa";
        }
    }

    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
        final JavaCommandLineState state = new WOApplicationCommandLineState<>(this, env);
        JavaRunConfigurationModule module = getConfigurationModule();
        state.setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(getProject(), module.getSearchScope()));
        return state;
    }
}
