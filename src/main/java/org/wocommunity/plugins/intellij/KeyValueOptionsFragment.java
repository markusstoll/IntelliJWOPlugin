package org.wocommunity.plugins.intellij;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.ui.SettingsEditorFragment;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class KeyValueOptionsFragment extends SettingsEditorFragment<WOApplicationConfiguration, KeyValueOptionEditorPanel> {
    public KeyValueOptionsFragment(WOApplicationConfiguration myConfig) {
        super("woOptions", "WebObjects Options", "Options",
                new KeyValueOptionEditorPanel(myConfig.getWOOptions()),
                30,
                (configuration, component) -> {
                },
                (configuration, component) -> {
                    configuration.setWOOptions(component.getEditedOptions());
                },
                configuration -> {
                    return true;
                });
    }
}

