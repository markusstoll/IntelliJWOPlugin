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
        return getComponentDirectory(file) != null;
    }

    @Override
    public void selectIn(@NotNull SelectInContext context, boolean requestFocus) {
        Project project = context.getProject();
        VirtualFile file = context.getVirtualFile();
        VirtualFile woDir = getComponentDirectory(file);
        if (woDir == null) {
            return;
        }

        PsiManager psiManager = PsiManager.getInstance(project);
        PsiDirectory directory = psiManager.findDirectory(woDir);

        if (directory != null) {
            ProjectView projectView = ProjectView.getInstance(project);
            if (projectView != null) {
                projectView.selectPsiElement(directory, requestFocus);
            }
        }
    }

    @Nullable
    private VirtualFile getComponentDirectory(@Nullable VirtualFile file) {
        if (file == null) {
            return null;
        }
        if (file.isDirectory() && file.getName().endsWith(".wo")) {
            return file;
        }
        VirtualFile parent = file.getParent();
        while (parent != null) {
            if (parent.isDirectory() && parent.getName().endsWith(".wo")) {
                return parent;
            }
            parent = parent.getParent();
        }
        if (file.getName().endsWith(".api")) {
            VirtualFile parentDir = file.getParent();
            if (parentDir != null) {
                String componentName = file.getName().substring(0, file.getName().length() - ".api".length());
                VirtualFile woDir = parentDir.findChild(componentName + ".wo");
                if (woDir != null && woDir.isDirectory()) {
                    return woDir;
                }
            }
        }
        return null;
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