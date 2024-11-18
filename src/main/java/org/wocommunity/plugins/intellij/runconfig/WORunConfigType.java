package org.wocommunity.plugins.intellij.runconfig;

import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import org.jetbrains.annotations.NotNull;
import com.intellij.util.ui.UIUtil;
import org.wocommunity.plugins.intellij.tools.WOIcons;

import javax.swing.*;

public class WORunConfigType extends ConfigurationTypeBase {
    static final String ID = "WORunConfig";

    protected WORunConfigType() {
        super(ID, "WebObjects", "Enable Running of WebObjects Java applications", getDynamicIcon());
        addFactory(new RunConfigurationFactory(this));
    }

    private static Icon getDynamicIcon() {
        return UIUtil.isUnderDarcula() ? WOIcons.PLUGIN_ICON_DARK : WOIcons.PLUGIN_ICON;
    }

    @NotNull
    public static WORunConfigType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(WORunConfigType.class);
    }
}
