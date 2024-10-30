package org.wocommunity.plugins.intellij;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.RunManager;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.tasks.MavenBeforeRunTask;
import org.jetbrains.idea.maven.tasks.MavenBeforeRunTasksProvider;

import javax.sound.midi.SysexMessage;
import java.util.ArrayList;

public class WOApplicationConfiguration extends ApplicationConfiguration {

    public WOApplicationConfiguration(String name, ConfigurationFactory factory, Project project) {
        super(name, project, factory);
    }

//    public void addOnBeforeTask() {
////        if(!getBeforeRunTasks().isEmpty())
////            return;
//
//        MavenBeforeRunTasksProvider mavenBeforeRunTasksProvider = new MavenBeforeRunTasksProvider(getProject());
//
//        MavenBeforeRunTask mavenTask = mavenBeforeRunTasksProvider.createTask(this);
//
//        mavenTask.setGoal("process-resources");
//        mavenTask.setProjectPath(getProject().getBasePath());
//        mavenTask.setEnabled(true);
//
//        ArrayList<BeforeRunTask<?>> taskArrayList = new ArrayList<>(getBeforeRunTasks());
//        taskArrayList.add(mavenTask);
//        setBeforeRunTasks(taskArrayList);
//    }

    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
        final JavaCommandLineState state = new WOApplicationCommandLineState<>(this, env);
        JavaRunConfigurationModule module = getConfigurationModule();
        state.setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(getProject(), module.getSearchScope()));
        return state;
    }
}
