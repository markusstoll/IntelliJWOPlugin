package org.wocommunity.plugins.intellij.components;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class SwitchToWOComponentAction extends AnAction {
    static List<String> srcSuffixes = Arrays.asList(new  String[] {"java", "kt", "groovy"});
    static String woTemplateSuffix = "wo";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Get the project
        Project project = e.getProject();
        if (project == null) return;

        // Get the currently selected file in the editor
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        VirtualFile currentFile = fileEditorManager.getSelectedFiles().length > 0 ? fileEditorManager.getSelectedFiles()[0] : null;

        if (currentFile != null) {
            @Nullable Module module = ModuleUtil.findModuleForFile(currentFile, project);

            String componentFilename = currentFile.getName();
            String suffix = FilenameUtils.getExtension(componentFilename);

            if((srcSuffixes).contains(suffix))
            {
                String woTemplate = StringUtils.left(componentFilename, componentFilename.length() - suffix.length()) + woTemplateSuffix;

                // Find the folder by name
                VirtualFile targetFolder = findResourceFolderByName(module, woTemplate);

                if (targetFolder != null) {
                    // Open the folder in the Project View (or handle it as needed)
                    FileEditorManager.getInstance(project).openFile(targetFolder, true);
                }
            }
        }
    }

    private VirtualFile findResourceFolderByName(Module module, String folderName) {
        // Get content roots of the module
        VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();

        // Iterate through the content roots to find folders by name
        for (VirtualFile root : contentRoots) {
            VirtualFile folder;

            try {
                folder = findFolderByName(root.findFileByRelativePath("src/main/components"), folderName);
                if(folder != null)
                    return folder;
            } catch (Exception exception)
            {}

            try {
                folder = findFolderByName(root.findFileByRelativePath("Components"), folderName);
                if(folder != null)
                    return folder;
            } catch (Exception exception)
            {}
        }

        return null;
    }

    private VirtualFile findFolderByName(VirtualFile root, String folderName) {
        for(VirtualFile file : root.getChildren()) {
            if (file.isDirectory())
            {
                if(file.getName().equals(folderName)) {
                    return file;
                } else {
                    VirtualFile result = findFolderByName(file, folderName);
                    if(result != null)
                        return result;
                }
            }
        };

        // Return the first match or null
        return null;
    }
}
