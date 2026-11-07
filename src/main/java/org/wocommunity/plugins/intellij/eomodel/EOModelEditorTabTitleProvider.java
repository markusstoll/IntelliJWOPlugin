package org.wocommunity.plugins.intellij.eomodel;

import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EOModelEditorTabTitleProvider implements EditorTabTitleProvider {
	@Override
	public @Nullable String getEditorTabTitle(@NotNull Project project, @NotNull VirtualFile file) {
		if (EOModelFileUtil.fileIsModel(file)) {
			return file.getNameWithoutExtension();
		}
		VirtualFile model = EOModelFileUtil.getModel(file);
		if (model != null) {
			return model.getNameWithoutExtension();
		}
		return null;
	}
}

