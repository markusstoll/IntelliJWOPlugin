package org.wocommunity.plugins.intellij.runconfig;

import com.intellij.execution.ui.SettingsEditorFragment;
import com.intellij.openapi.ui.LabeledComponent;
import org.wocommunity.plugins.intellij.runconfig.data.VMParametersTableModel;

public class VMOptionsFragment extends SettingsEditorFragment<WOApplicationConfiguration, LabeledComponent<VMOptionEditorPanel>> {
    public VMOptionsFragment(VMParametersTableModel vmParametersTableModel) {
        super("higherJdkVMParameters", "VMParameters for JDK >= 11", "Options",
                LabeledComponent.create(new VMOptionEditorPanel(vmParametersTableModel), "VMParameters for JDK >= 11:"),
                30,
                (configuration, component) -> {
                    vmParametersTableModel.replaceEntries(configuration.getOptions().getHigherJdkVMParameters());
                },
                (configuration, component) -> {
                    configuration.getOptions().setHigherJdkVMParameters(vmParametersTableModel.getEntries());
                },
                configuration -> {
                    return true;
                });
    }
}
