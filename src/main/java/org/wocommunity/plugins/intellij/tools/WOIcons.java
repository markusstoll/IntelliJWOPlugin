package org.wocommunity.plugins.intellij.tools;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public final class WOIcons {
    private WOIcons() {
    }

    public static final Icon PLUGIN_ICON = IconLoader.getIcon("icons/WebObjectsIcon.svg", WOIcons.class);
    public static final Icon PLUGIN_ICON_DARK = IconLoader.getIcon("icons/WebObjectsIcon_dark.svg", WOIcons.class);
    public static final Icon WOCOMPONENT_ICON = IconLoader.getIcon("icons/WOComponentIcon.svg", WOIcons.class);
}
