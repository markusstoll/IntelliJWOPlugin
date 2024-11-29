package org.wocommunity.plugins.intellij.components;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class WOComponentEditorProvider implements FileEditorProvider, DumbAware {

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        // Only accept .wo folders
        return file.isDirectory() && file.getName().endsWith(".wo");
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        try {
            return new WOComponentEditor(project, file);
        } catch (IOException e) {
            Notifications.Bus.notify(new Notification(
                    "WOComponent", "File Not Found",
                    file.getName() + " cannot be openend: " + e.getMessage(),
                    NotificationType.WARNING
            ));

            return null;
        }
    }

    @Override
    public @NotNull String getEditorTypeId() {
        return "WOFolderEditor";
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}