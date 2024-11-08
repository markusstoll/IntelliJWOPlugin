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
    public KeyValueOptionsFragment(WOApplicationConfiguration myConfig) {
        super("woOptions", "WebObjects Options", "Options",
                LabeledComponent.create(new KeyValueOptionEditorPanel(myConfig.getOptions().getWoOptions()), "WebObjects Options:"),
                30,
                (configuration, component) -> {
                    component.getComponent().setKeyValueOptions(myConfig.getOptions().getWoOptions());
                },
                (configuration, component) -> {
                    myConfig.getOptions().setWoOptions(component.getComponent().getKeyValueOptions());
                },
                configuration -> {
                    return true;
                });
    }
}

