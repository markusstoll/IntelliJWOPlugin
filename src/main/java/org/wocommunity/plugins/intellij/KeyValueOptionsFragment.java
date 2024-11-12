package org.wocommunity.plugins.intellij;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.ui.ClasspathModifier;
import com.intellij.execution.ui.SettingsEditorFragment;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class KeyValueOptionsFragment extends SettingsEditorFragment<WOApplicationConfiguration, LabeledComponent<KeyValueOptionEditorPanel>> {
    private final KeyValueOptionTableModel tableModel;

    public KeyValueOptionsFragment(KeyValueOptionTableModel tableModel)
    {
        super("woOptions", "WebObjects Options", "Options",
                LabeledComponent.create(new KeyValueOptionEditorPanel(tableModel), "WebObjects Options:"),
                30,
                (configuration, component) -> {},
                (configuration, component) -> {},
                configuration -> {
                    return true;
                });

        this.tableModel = tableModel;
    }

    @Override
    protected void resetEditorFrom(@NotNull WOApplicationConfiguration c) {
        super.resetEditorFrom(c);

        tableModel.replaceEntries(c.getOptions().getWoOptions());
    }

    @Override
    protected void applyEditorTo(@NotNull WOApplicationConfiguration c) {
        super.applyEditorTo(c);

        c.getOptions().setWoOptions(tableModel.getEntries());
    }
}

