package org.wocommunity.plugins.intellij.wod;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.components.WOComponentEditor;

public final class WOHtmlToWodNavigationUtil {
    private WOHtmlToWodNavigationUtil() {}

    public static void navigateToWod(@NotNull Project project, @NotNull VirtualFile wodFile, int offset) {
        navigateToWodAndSelect(project, wodFile, offset, offset);
    }

    public static void navigateToWodAndSelect(@NotNull Project project, @NotNull VirtualFile wodFile, int startOffset, int endOffset) {
        FileEditorManager fem = FileEditorManager.getInstance(project);

        // 1. Check currently active/selected editor first
        FileEditor selected = fem.getSelectedEditor();
        if (selected instanceof WOComponentEditor ce && isMatchingWoFolder(ce, wodFile)) {
            if (ce.scrollToWodOffset(startOffset, endOffset)) {
                return;
            }
        }

        // 2. Check all open editors
        for (FileEditor editor : fem.getAllEditors()) {
            if (editor instanceof WOComponentEditor ce && isMatchingWoFolder(ce, wodFile)) {
                if (ce.scrollToWodOffset(startOffset, endOffset)) {
                    return;
                }
            }
        }

        OpenFileDescriptor descriptor = new OpenFileDescriptor(project, wodFile, startOffset);
        descriptor.navigate(true);
    }

    public static void navigateToHtml(@NotNull Project project, @NotNull VirtualFile htmlFile, int offset) {
        FileEditorManager fem = FileEditorManager.getInstance(project);

        // 1. Check currently active/selected editor first
        FileEditor selected = fem.getSelectedEditor();
        if (selected instanceof WOComponentEditor ce && isMatchingWoFolder(ce, htmlFile)) {
            if (ce.scrollToHtmlOffset(offset)) {
                return;
            }
        }

        // 2. Check all open editors
        for (FileEditor editor : fem.getAllEditors()) {
            if (editor instanceof WOComponentEditor ce && isMatchingWoFolder(ce, htmlFile)) {
                if (ce.scrollToHtmlOffset(offset)) {
                    return;
                }
            }
        }

        OpenFileDescriptor descriptor = new OpenFileDescriptor(project, htmlFile, offset);
        descriptor.navigate(true);
    }

    private static boolean isMatchingWoFolder(@NotNull WOComponentEditor ce, @NotNull VirtualFile childFile) {
        VirtualFile parent = childFile.getParent();
        VirtualFile folder = ce.getFolder();
        if (parent == null || folder == null) {
            return false;
        }
        return parent.equals(folder) || parent.getPath().equals(folder.getPath());
    }
}

