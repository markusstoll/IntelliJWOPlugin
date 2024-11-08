package org.wocommunity.plugins.intellij;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public final class WOIcons {
    private WOIcons() {
    }

    public static final Icon PLUGIN_ICON = IconLoader.getIcon("META-INF/pluginIcon.svg", WOIcons.class);
    public static final Icon PLUGIN_ICON_DARK = IconLoader.getIcon("META-INF/pluginIcon_dark.svg", WOIcons.class);
}
