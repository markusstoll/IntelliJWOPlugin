package org.wocommunity.plugins.intellij.components;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class SwitchToWOComponentAction extends AnAction {
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
            VirtualFile targetFolder = findFolderByName(baseDir, currentFile.getName().replace(".java", ".wo"));

            if (targetFolder != null) {
                // Open the folder in the Project View (or handle it as needed)
                FileEditorManager.getInstance(project).openFile(targetFolder, true);
            }
        }
    }

    private VirtualFile findFolderByName(VirtualFile root, String folderName) {
        if (root.isDirectory() && root.getName().equals(folderName)) {
            return root;
        }
        for (VirtualFile child : root.getChildren()) {
            if (child.isDirectory()) {
                VirtualFile found = findFolderByName(child, folderName);
                if (found != null) return found;
            }
        }
        return null;
    }
}
