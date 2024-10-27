package org.wocommunity.plugins.intellij;

import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import org.jetbrains.annotations.NotNull;

public class WORunConfigType extends ConfigurationTypeBase {
    static final String ID = "PageObjectEvaluator";

    protected WORunConfigType() {
        super(ID, "WebObjects", "Enable Running of WebObjects Java applications", WOIcons.PLUGIN_ICON);
        addFactory(new RunConfigurationFactory(this));
    }

    @NotNull
    public static WORunConfigType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(WORunConfigType.class);
    }
}
