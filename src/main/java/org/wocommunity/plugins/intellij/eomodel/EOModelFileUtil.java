package org.wocommunity.plugins.intellij.eomodel;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class EOModelFileUtil {
	public static final String EOMODELD_EXTENSION = "eomodeld";
	public static final String INDEX_FILE_NAME = "index.eomodeld";
	public static final String PLIST_EXTENSION = "plist";
	public static final String FSPEC_EXTENSION = "fspec";

	private EOModelFileUtil() {
	}

	public static boolean fileIsModel(@Nullable VirtualFile file) {
		return file != null && file.isDirectory() && EOMODELD_EXTENSION.equalsIgnoreCase(file.getExtension());
	}

	public static boolean fileIsInModel(@Nullable VirtualFile file) {
		if (file == null || file.isDirectory()) {
			return false;
		}
		VirtualFile parent = file.getParent();
		return fileIsModel(parent);
	}

	public static @Nullable VirtualFile getModel(@Nullable VirtualFile file) {
		if (file == null) {
			return null;
		}
		if (fileIsModel(file)) {
			return file;
		}
		if (fileIsInModel(file)) {
			return file.getParent();
		}
		return null;
	}

	public static @Nullable String getModelName(@Nullable VirtualFile file) {
		VirtualFile model = getModel(file);
		if (model != null) {
			return model.getNameWithoutExtension();
		}
		return null;
	}
}

