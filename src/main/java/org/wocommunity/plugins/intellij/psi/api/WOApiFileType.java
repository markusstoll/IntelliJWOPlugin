package org.wocommunity.plugins.intellij.psi.api;

import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.tools.WOIcons;

import javax.swing.*;

public final class WOApiFileType extends LanguageFileType {
    public static final WOApiFileType INSTANCE = new WOApiFileType();

    private WOApiFileType() {
        super(XMLLanguage.INSTANCE);
    }

    @Override
    public @NotNull String getName() {
        return "WebObjects API";
    }

    @Override
    public @NotNull String getDescription() {
        return "WebObjects component API definition";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "api";
    }

    @Override
    public Icon getIcon() {
        return WOIcons.WOCOMPONENT_ICON;
    }
}

