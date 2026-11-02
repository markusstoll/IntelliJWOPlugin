package org.wocommunity.plugins.intellij.wizards;

import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.tools.WOIcons;

import java.io.File;

public class NewWOProjectAction extends AnAction {
    public NewWOProjectAction() {
        super("WebObjects / Wonder Project...", "Create a new WebObjects or Project Wonder Maven project from archetype", WOIcons.PLUGIN_ICON);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project currentProject = e.getProject();
        NewWOProjectDialog dialog = new NewWOProjectDialog(currentProject);
        if (!dialog.showAndGet()) {
            return;
        }

        WOProjectModel model = dialog.getModel();
        File targetDir = new File(model.getEffectiveLocation());

        final boolean[] success = {false};
        final Exception[] error = {null};

        ProgressManager.getInstance().run(new Task.Modal(currentProject, "Creating WebObjects Project...", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    WOArchetypeGenerator.generate(model, targetDir, indicator);
                    success[0] = true;
                } catch (Exception ex) {
                    error[0] = ex;
                }
            }
        });

        if (!success[0]) {
            String msg = (error[0] != null) ? error[0].getMessage() : "Unknown error";
            Messages.showErrorDialog(currentProject, "Failed to create WebObjects project: " + msg, "Error Creating Project");
            return;
        }

        ProjectUtil.openOrImport(targetDir.toPath(), currentProject, false);
    }
}


