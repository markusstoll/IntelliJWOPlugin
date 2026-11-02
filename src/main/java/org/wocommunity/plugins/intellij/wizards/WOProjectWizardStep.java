package org.wocommunity.plugins.intellij.wizards;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.options.ConfigurationException;

import javax.swing.*;

public class WOProjectWizardStep extends ModuleWizardStep {
    private final WOModuleBuilder builder;
    private final WizardContext wizardContext;
    private final WOProjectConfigPanel panel;

    public WOProjectWizardStep(WOModuleBuilder builder, WizardContext wizardContext) {
        this.builder = builder;
        this.wizardContext = wizardContext;
        String defaultDir = wizardContext.getProjectFileDirectory();
        this.panel = new WOProjectConfigPanel(defaultDir);
        this.panel.syncModelToUI(builder.getModel());
    }

    @Override
    public JComponent getComponent() {
        return panel;
    }

    @Override
    public void updateDataModel() {
        panel.syncUIToModel(builder.getModel());
        String name = builder.getModel().getName();
        String loc = builder.getModel().getEffectiveLocation();
        wizardContext.setProjectName(name);
        wizardContext.setProjectFileDirectory(loc);
        builder.setName(name);
        builder.setContentEntryPath(loc);
    }

    @Override
    public boolean validate() throws ConfigurationException {
        String error = panel.validateInput();
        if (error != null) {
            throw new ConfigurationException(error);
        }
        return true;
    }
}

