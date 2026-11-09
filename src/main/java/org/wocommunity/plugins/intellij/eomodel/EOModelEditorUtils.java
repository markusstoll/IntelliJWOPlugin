package org.wocommunity.plugins.intellij.eomodel;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public final class EOModelEditorUtils {
	private static final Logger LOG = Logger.getInstance(EOModelEditorUtils.class);

	private static final Method REQUEST_OPEN_FILE_METHOD;
	private static final Method OPEN_FILE_METHOD;

	static {
		Method requestMethod = null;
		Method openMethod = null;
		try {
			requestMethod = FileEditorManager.class.getMethod("requestOpenFile", VirtualFile.class);
		} catch (NoSuchMethodException e) {
			LOG.debug("requestOpenFile(VirtualFile) not available", e);
		}

		try {
			openMethod = FileEditorManager.class.getMethod("openFile", VirtualFile.class, boolean.class);
		} catch (NoSuchMethodException e) {
			LOG.debug("openFile(VirtualFile, boolean) not available", e);
		}

		REQUEST_OPEN_FILE_METHOD = requestMethod;
		OPEN_FILE_METHOD = openMethod;
	}

	private EOModelEditorUtils() {
	}

	public static void openModel(@NotNull Project project, @NotNull VirtualFile file) {
		if (project.isDisposed() || !file.isValid()) {
			return;
		}

		VirtualFile fileToOpen = file;
		if (EOModelFileUtil.fileIsModel(file)) {
			VirtualFile indexFile = file.findChild(EOModelFileUtil.INDEX_FILE_NAME);
			if (indexFile != null && !indexFile.isDirectory()) {
				fileToOpen = indexFile;
			} else {
				final VirtualFile[] created = new VirtualFile[1];
				WriteCommandAction.runWriteCommandAction(project, "Create index.eomodeld", null, () -> {
					try {
						created[0] = file.createChildData(EOModelEditorUtils.class, EOModelFileUtil.INDEX_FILE_NAME);
					} catch (Exception ignored) {
					}
				});
				if (created[0] != null) {
					fileToOpen = created[0];
				}
			}
		}

		FileEditorManager manager = FileEditorManager.getInstance(project);
		if (REQUEST_OPEN_FILE_METHOD != null) {
			try {
				REQUEST_OPEN_FILE_METHOD.invoke(manager, fileToOpen);
				return;
			} catch (Exception e) {
				LOG.warn("requestOpenFile failed, falling back to openFile", e);
			}
		}

		if (OPEN_FILE_METHOD != null) {
			try {
				OPEN_FILE_METHOD.invoke(manager, fileToOpen, true);
				return;
			} catch (Exception e) {
				LOG.warn("openFile failed", e);
			}
		}

		new OpenFileDescriptor(project, fileToOpen).navigate(true);
	}
}

