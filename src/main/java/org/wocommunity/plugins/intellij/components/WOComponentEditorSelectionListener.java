
package org.wocommunity.plugins.intellij.components;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vfs.VirtualFile;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Selektiert die WOComponentNode, wenn ein WOComponentEditor geöffnet wird.
 */
public class WOComponentEditorSelectionListener implements ProjectActivity {


    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        project.getMessageBus().connect().subscribe(
                FileEditorManagerListener.FILE_EDITOR_MANAGER,
                new FileEditorManagerListener() {
                    @Override
                    public void selectionChanged(@NotNull com.intellij.openapi.fileEditor.FileEditorManagerEvent event) {
                        VirtualFile file = event.getNewFile();
                        if (file != null && file.isDirectory() && file.getName().endsWith(".wo")) {
                            // Trigger selection in project view
                            selectInProjectView(project, file);
                        }
                    }
                }
        );

        return null;
    }

    private void selectInProjectView(Project project, VirtualFile file) {
        ApplicationManager.getApplication().invokeLater(() -> {
            // Zuerst PsiDirectory aus VirtualFile holen
            com.intellij.psi.PsiManager psiManager = com.intellij.psi.PsiManager.getInstance(project);
            com.intellij.psi.PsiDirectory directory = psiManager.findDirectory(file);

            if (directory != null) {
                com.intellij.ide.projectView.ProjectView projectView =
                        com.intellij.ide.projectView.ProjectView.getInstance(project);
                if (projectView != null) {
                    // PsiElement verwenden statt VirtualFile
                    projectView.selectPsiElement(directory, true);
                }
            }
        });
    }
}
