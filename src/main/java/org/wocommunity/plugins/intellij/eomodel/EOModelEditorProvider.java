package org.wocommunity.plugins.intellij.eomodel;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.eomodel.editor.EOModelEditor;

public class EOModelEditorProvider implements FileEditorProvider, DumbAware {

	@Override
	public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
		return EOModelFileUtil.fileIsModel(file) || EOModelFileUtil.fileIsInModel(file);
	}

	@Override
	public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
		VirtualFile model = EOModelFileUtil.getModel(file);
		if (model != null) {
			return new EOModelEditor(project, model, file);
		}
		return TextEditorProvider.getInstance().createEditor(project, file);
	}

	@Override
	public @NotNull String getEditorTypeId() {
		return "EOModelEditor";
	}

	@Override
	public @NotNull FileEditorPolicy getPolicy() {
		return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
	}
}

