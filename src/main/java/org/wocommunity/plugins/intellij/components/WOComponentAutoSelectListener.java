package org.wocommunity.plugins.intellij.components;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

public class WOComponentAutoSelectListener implements FileEditorManagerListener {

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        Project project = event.getManager().getProject();
        FileEditor editor = event.getNewEditor();
        if (editor == null) return;

        VirtualFile vf;
        if (editor instanceof WOComponentEditor) {
            vf = ((WOComponentEditor) editor).getFile();
        } else {
            vf = event.getNewFile();
        }

        if (vf == null) return;

        VirtualFile dir = vf.isDirectory() ? vf : vf.getParent();
        if (dir == null || !dir.getName().endsWith(".wo")) return;

        PsiDirectory psiDir = PsiManager.getInstance(project).findDirectory(dir);
        if (psiDir != null) {
            ProjectView.getInstance(project).selectPsiElement(psiDir, false);
        }
    }
}


