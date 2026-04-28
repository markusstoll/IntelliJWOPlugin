package org.wocommunity.plugins.intellij.runconfig;

import com.intellij.execution.*;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.tools.WOProjectUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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
        ensureMavenProcessResourcesBeforeRunTask();
    }

    /**
     * Ensures a "Maven process-resources" before-run task exists for the module's pom.xml.
     *
     * Implemented via reflection to avoid a hard dependency on the Maven plugin at compile-time.
     * If the Maven plugin is not present in the IDE, this becomes a no-op.
     */
    private void ensureMavenProcessResourcesBeforeRunTask() {
        String modulePath = WOProjectUtil.getModulePath(getConfigurationModule().getModule());
        if (modulePath == null || modulePath.isBlank()) {
            return;
        }
        String mavenPomPath = modulePath + "/pom.xml";

        Project project = getProject();

        // 1) If the task is already present, do nothing.
        List<BeforeRunTask<?>> tasks = new ArrayList<>(getBeforeRunTasks());
        for (BeforeRunTask<?> t : tasks) {
            if (isMavenBeforeRunTaskWithGoalAndPom(t, "process-resources", mavenPomPath)) {
                return;
            }
        }

        // 2) Try to create Maven before-run task (requires Maven plugin).
        BeforeRunTask<?> created = createMavenBeforeRunTaskReflective(project, this);
        if (created == null) {
            return; // Maven plugin not available
        }

        // 3) Configure the created task.
        trySet(created, "setGoal", new Class[]{String.class}, new Object[]{"process-resources"});
        trySet(created, "setProjectPath", new Class[]{String.class}, new Object[]{mavenPomPath});
        trySet(created, "setEnabled", new Class[]{boolean.class}, new Object[]{true});

        tasks.add(created);
        setBeforeRunTasks(tasks);
    }

    private static boolean isMavenBeforeRunTaskWithGoalAndPom(BeforeRunTask<?> task, String goal, String pomPath) {
        if (task == null) {
            return false;
        }
        // We can't reference MavenBeforeRunTask class directly; identify by class name.
        String cn = task.getClass().getName();
        if (!cn.endsWith(".MavenBeforeRunTask")) {
            return false;
        }
        Object existingGoal = tryCall(task, "getGoal");
        Object existingPath = tryCall(task, "getProjectPath");
        return goal.equals(existingGoal) && pomPath.equals(existingPath);
    }

    private static BeforeRunTask<?> createMavenBeforeRunTaskReflective(Project project, RunConfiguration configuration) {
        try {
            Class<?> providerClass = Class.forName("org.jetbrains.idea.maven.tasks.MavenBeforeRunTasksProvider");
            Constructor<?> ctor = providerClass.getConstructor(Project.class);
            Object provider = ctor.newInstance(project);

            Method createTask = providerClass.getMethod("createTask", RunConfiguration.class);
            Object task = createTask.invoke(provider, configuration);
            return (BeforeRunTask<?>) task;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object tryCall(Object target, String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            return m.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void trySet(Object target, String methodName, Class<?>[] sig, Object[] args) {
        try {
            Method m = target.getClass().getMethod(methodName, sig);
            m.invoke(target, args);
        } catch (Throwable ignored) {
            // ignore if method not present in this IDE version
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
