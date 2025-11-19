package org.wocommunity.plugins.intellij.components;

import com.intellij.ide.SelectInContext;
import com.intellij.ide.SelectInTarget;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.ProjectViewPane;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WOComponentSelectInTarget implements SelectInTarget {

    @Override
    public boolean canSelect(@NotNull SelectInContext context) {
        VirtualFile file = context.getVirtualFile();
        // Unterstützt .wo Verzeichnisse
        return file != null && file.isDirectory() && file.getName().endsWith(".wo");
    }

    @Override
    public void selectIn(@NotNull SelectInContext context, boolean requestFocus) {
        Project project = context.getProject();
        VirtualFile file = context.getVirtualFile();

        if (file == null || !file.isDirectory() || !file.getName().endsWith(".wo")) {
            return;
        }

        PsiManager psiManager = PsiManager.getInstance(project);
        PsiDirectory directory = psiManager.findDirectory(file);

        if (directory != null) {
            ProjectView projectView = ProjectView.getInstance(project);
            if (projectView != null) {
                projectView.selectPsiElement(directory, requestFocus);
            }
        }
    }

    @Override
    public @NotNull String getToolWindowId() {
        return ToolWindowId.PROJECT_VIEW;
    }

    @Override
    public @Nullable String getMinorViewId() {
        return ProjectViewPane.ID; // ID des Standard-Project-Pane
    }

    @Override
    public float getWeight() {
        return 5f;
    }

    @NotNull
    @Override
    public String toString() {
        return "WO Component in Project View";
    }
}