package org.wocommunity.plugins.intellij.components;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public final class WOEditorUtils {
    private static final Logger LOG = Logger.getInstance(WOEditorUtils.class);

    private static final Method REQUEST_OPEN_FILE_METHOD;
    private static final Method OPEN_FILE_METHOD;

    static {
        Method requestMethod = null;
        Method openMethod = null;
        try {
            // requestOpenFile(VirtualFile) exists in 2026.2 (void) and 2026.3+ (CompletableFuture)
            requestMethod = FileEditorManager.class.getMethod("requestOpenFile", VirtualFile.class);
        } catch (NoSuchMethodException e) {
            LOG.debug("requestOpenFile(VirtualFile) not available", e);
        }

        try {
            // Fallback for older IntelliJ platforms: openFile(VirtualFile, boolean)
            openMethod = FileEditorManager.class.getMethod("openFile", VirtualFile.class, boolean.class);
        } catch (NoSuchMethodException e) {
            LOG.debug("openFile(VirtualFile, boolean) not available", e);
        }

        REQUEST_OPEN_FILE_METHOD = requestMethod;
        OPEN_FILE_METHOD = openMethod;
    }

    private WOEditorUtils() {
    }

    /**
     * Opens a WebObjects component in the editor.
     * Always resolves .wo directories to their primary template file (e.g. .html)
     * so IntelliJ's FileEditorManager and EditorsSplitters never attempt to read bytes
     * from a directory, avoiding "Cannot get content of directory" exceptions.
     */
    public static void openComponent(@NotNull Project project, @NotNull VirtualFile file) {
        if (project.isDisposed() || !file.isValid()) {
            return;
        }

        VirtualFile fileToOpen = file;
        if (file.isDirectory() && file.getName().endsWith(".wo")) {
            String componentName = file.getName().replace(".wo", "");
            VirtualFile htmlFile = file.findChild(componentName + ".html");
            if (htmlFile != null && !htmlFile.isDirectory()) {
                fileToOpen = htmlFile;
            } else {
                VirtualFile wodFile = file.findChild(componentName + ".wod");
                if (wodFile != null && !wodFile.isDirectory()) {
                    fileToOpen = wodFile;
                } else {
                    // Empty component folder - ensure componentName.html exists so FileEditorManager opens a real file
                    final VirtualFile[] created = new VirtualFile[1];
                    WriteCommandAction.runWriteCommandAction(project, "Create Component HTML", null, () -> {
                        try {
                            created[0] = file.createChildData(WOEditorUtils.class, componentName + ".html");
                        } catch (Exception ignored) {}
                    });
                    if (created[0] != null) {
                        fileToOpen = created[0];
                    }
                }
            }
        }

        if (fileToOpen.isDirectory()) {
            LOG.warn("Cannot open directory in editor: " + fileToOpen.getPath());
            return;
        }

        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);

        if (REQUEST_OPEN_FILE_METHOD != null) {
            try {
                REQUEST_OPEN_FILE_METHOD.invoke(fileEditorManager, fileToOpen);
                return;
            } catch (Exception e) {
                LOG.warn("Failed to open file via requestOpenFile, falling back to openFile", e);
            }
        }

        if (OPEN_FILE_METHOD != null) {
            try {
                OPEN_FILE_METHOD.invoke(fileEditorManager, fileToOpen, true);
                return;
            } catch (Exception e) {
                LOG.error("Failed to open file via openFile", e);
            }
        }
    }
}
