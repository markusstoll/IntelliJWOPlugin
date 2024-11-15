package org.wocommunity.plugins.intellij.runconfig;

import com.intellij.execution.*;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.tasks.MavenBeforeRunTask;
import org.jetbrains.idea.maven.tasks.MavenBeforeRunTasksProvider;

import java.util.ArrayList;

public class WOApplicationConfiguration extends ApplicationConfiguration {
    public WOApplicationConfiguration(String name, ConfigurationFactory factory, Project project) {
        super(name, project, factory);
    }

    @Override
    protected @NotNull WORunConfigurationOptions getOptions() {
        return (WORunConfigurationOptions) super.getOptions();
    }

    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new WOApplicationSettingsEditor(this);
    }

    @Override
    public void checkSettingsBeforeRun() throws RuntimeConfigurationException {
        super.checkSettingsBeforeRun();

        addOnBeforeTask();
    }

    public void addOnBeforeTask() {
        String modulePath = WOProjectUtil.getModulePath(getConfigurationModule().getModule());
        String mavenPomPath = modulePath + "/pom.xml";

        ArrayList<BeforeRunTask<?>> taskArrayList = new ArrayList<>(getBeforeRunTasks());

        if(taskArrayList
                .stream()
                .filter(brt -> brt instanceof MavenBeforeRunTask)
                .map(brt -> (MavenBeforeRunTask)brt)
                .filter(mbrt -> "process-resources".equals(mbrt.getGoal()))
                .filter(mbrt -> mavenPomPath.equals(mbrt.getProjectPath()))
                .findAny()
                .isEmpty())
        {
            Project project = getProject();
            RunManager runManager = RunManager.getInstance(project);

            MavenBeforeRunTasksProvider mavenBeforeRunTasksProvider = new MavenBeforeRunTasksProvider(project);
            MavenBeforeRunTask mavenTask = mavenBeforeRunTasksProvider.createTask(this);

            mavenTask.setGoal("process-resources");
            mavenTask.setProjectPath(mavenPomPath);
            mavenTask.setEnabled(true);

            taskArrayList.add(mavenTask);
            setBeforeRunTasks(taskArrayList);
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
