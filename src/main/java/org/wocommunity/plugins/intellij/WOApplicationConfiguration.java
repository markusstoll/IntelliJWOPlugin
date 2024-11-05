package org.wocommunity.plugins.intellij;

import com.intellij.diagnostic.logging.LogConfigurationPanel;
import com.intellij.execution.*;
import com.intellij.execution.application.ApplicationConfigurable;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.application.JavaApplicationSettingsEditor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.SettingsEditorGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.tasks.MavenBeforeRunTask;
import org.jetbrains.idea.maven.tasks.MavenBeforeRunTasksProvider;

import java.util.ArrayList;
import java.util.List;

public class WOApplicationConfiguration extends ApplicationConfiguration {
    @XCollection(propertyElementName = "woOptions")
    private List<KeyValueOption> woOptions = new ArrayList<>();

    public WOApplicationConfiguration(String name, ConfigurationFactory factory, Project project) {
        super(name, project, factory);

        woOptions.add(new KeyValueOption(true, "-WOPort", "4040"));
        woOptions.add(new KeyValueOption(false, "-WOXXX", "123"));
    }

    public List<KeyValueOption> getWOOptions() { return woOptions; }
    public void setWOOptions(List<KeyValueOption> options) { this.woOptions = options; }

    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new WOApplicationSettingsEditor(this);
    }

    @Override
    public void checkSettingsBeforeRun() throws RuntimeConfigurationException {
        super.checkSettingsBeforeRun();

        addOnBeforeTask();
    }

    public void addOnBeforeTask() {
        String modulePath = ProjectUtil.getModulePath(getConfigurationModule().getModule());
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
