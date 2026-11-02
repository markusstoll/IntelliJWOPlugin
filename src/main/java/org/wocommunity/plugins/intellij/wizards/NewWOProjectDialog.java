package org.wocommunity.plugins.intellij.wizards;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;

public class NewWOProjectDialog extends DialogWrapper {
    private final WOProjectConfigPanel panel;
    private final WOProjectModel model = new WOProjectModel();

    public NewWOProjectDialog(@Nullable Project project) {
        super(project, true);
        setTitle("New WebObjects / Wonder Project");

        String defaultLocation = null;
        if (project != null && project.getBasePath() != null) {
            defaultLocation = new File(project.getBasePath()).getParent();
        } else {
            defaultLocation = System.getProperty("user.home");
        }

        panel = new WOProjectConfigPanel(defaultLocation);
        panel.syncModelToUI(model);

        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return panel;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        String err = panel.validateInput();
        if (err != null) {
            return new ValidationInfo(err);
        }
        return null;
    }

    @Override
    protected void doOKAction() {
        panel.syncUIToModel(model);
        super.doOKAction();
    }

    public WOProjectModel getModel() {
        return model;
    }
}

