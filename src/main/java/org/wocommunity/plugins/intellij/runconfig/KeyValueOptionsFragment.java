package org.wocommunity.plugins.intellij.runconfig;

import com.intellij.execution.ui.SettingsEditorFragment;
import com.intellij.openapi.ui.LabeledComponent;
import org.wocommunity.plugins.intellij.runconfig.data.KeyValueOptionTableModel;

public class KeyValueOptionsFragment extends SettingsEditorFragment<WOApplicationConfiguration, LabeledComponent<KeyValueOptionEditorPanel>> {
    private final KeyValueOptionTableModel tableModel;

    public KeyValueOptionsFragment(KeyValueOptionTableModel tableModel)
    {
        super("woOptions", "WebObjects Options", "Options",
                LabeledComponent.create(new KeyValueOptionEditorPanel(tableModel), "WebObjects Options:"),
                30,
                (configuration, component) -> {
                    tableModel.replaceEntries(configuration.getOptions().getWoOptions());
                },
                (configuration, component) -> {
                    configuration.getOptions().setWoOptions(tableModel.getEntries());
                },
                configuration -> {
                    return true;
                });

        this.tableModel = tableModel;
    }
}

