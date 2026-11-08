package org.wocommunity.plugins.intellij.eogen;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.tools.WOIcons;

import javax.swing.*;

public final class EOGenFileType extends LanguageFileType {
	public static final EOGenFileType INSTANCE = new EOGenFileType();

	private EOGenFileType() {
		super(PlainTextLanguage.INSTANCE);
	}

	@Override
	public @NotNull String getName() {
		return "EOGenerator";
	}

	@Override
	public @NotNull String getDescription() {
		return "EOGenerator configuration file";
	}

	@Override
	public @NotNull String getDefaultExtension() {
		return "eogen";
	}

	@Override
	public @Nullable Icon getIcon() {
		return WOIcons.EOGEN_ICON;
	}
}
