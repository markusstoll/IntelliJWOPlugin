
package org.wocommunity.plugins.intellij.components;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * Selektiert die WOComponentNode, wenn ein WOComponentEditor geöffnet wird.
 */
public class WOComponentSelectionListener implements FileEditorManagerListener {

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile openedFile) {
        Project project = source.getProject();
        if (project == null) return;

        // Nur reagieren, wenn der geöffnete Editor unser WOComponentEditor ist
        if (source.getSelectedEditor(openedFile) instanceof WOComponentEditor) {
            if (openedFile == null) return;

            VirtualFile parent = openedFile.getParent();
            if (parent != null) {
                // Im Projekt‑View die übergeordnete Directory‑Node auswählen
                ProjectView.getInstance(project).select(parent, null, true);
            }
        }
    }
}
