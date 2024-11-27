package org.wocommunity.plugins.intellij.components;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

public class SwitchToJavaAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Get the project
        Project project = e.getProject();
        if (project == null) return;

        // Get the currently selected file in the editor
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        VirtualFile currentFile = fileEditorManager.getSelectedFiles().length > 0 ? fileEditorManager.getSelectedFiles()[0] : null;

        if (currentFile != null) {
            VirtualFile baseDir = project.getBaseDir();
            if (baseDir == null) return;

            // Find the folder by name
            VirtualFile targetFolder = findFileByName(baseDir, currentFile.getName().replace(".wo", ".java"));

            if (targetFolder != null) {
                // Open the folder in the Project View (or handle it as needed)
                FileEditorManager.getInstance(project).openFile(targetFolder, true);
            }
        }
    }

    private VirtualFile findFileByName(VirtualFile root, String filename) {
        for (VirtualFile child : root.getChildren()) {
            if (child.isDirectory()) {
                VirtualFile found = findFileByName(child, filename);
                if (found != null) return found;
            } else if (child.getName().equals(filename))
            {
                return child;
            }
        }
        return null;
    }

}
