package org.wocommunity.plugins.intellij.components;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import org.wocommunity.plugins.intellij.wotemplate.WOComponentClassResolver;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class SwitchToWOComponentAction extends AnAction {
    private static final List<String> SRC_SUFFIXES = Arrays.asList("java", "kt", "groovy");
    private static final String WO_TEMPLATE_SUFFIX = "wo";

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        boolean enabled = project != null && file != null && isSourceFile(file);
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    private static boolean isSourceFile(@NotNull VirtualFile file) {
        String ext = file.getExtension();
        return ext != null && SRC_SUFFIXES.contains(ext.toLowerCase());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        VirtualFile currentFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (currentFile == null) {
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
            currentFile = fileEditorManager.getSelectedFiles().length > 0 ? fileEditorManager.getSelectedFiles()[0] : null;
        }

        if (currentFile == null) return;

        String componentFilename = currentFile.getName();
        String suffix = FilenameUtils.getExtension(componentFilename);

        if (SRC_SUFFIXES.contains(suffix.toLowerCase())) {
            String componentName = FilenameUtils.getBaseName(componentFilename);
            String woTemplate = componentName + "." + WO_TEMPLATE_SUFFIX;

            Module module = ModuleUtilCore.findModuleForFile(currentFile, project);
            VirtualFile targetFolder = findResourceFolderByName(project, module, currentFile, woTemplate);

            if (targetFolder != null) {
                VirtualFile htmlFile = targetFolder.findChild(componentName + ".html");
                if (htmlFile != null && !htmlFile.isDirectory()) {
                    WOEditorUtils.openComponent(project, htmlFile);
                } else {
                    VirtualFile wodFile = targetFolder.findChild(componentName + ".wod");
                    if (wodFile != null && !wodFile.isDirectory()) {
                        WOEditorUtils.openComponent(project, wodFile);
                    } else {
                        WOEditorUtils.openComponent(project, targetFolder);
                    }
                }
            }
        }
    }

    public static VirtualFile findResourceFolderByName(@NotNull Project project,
                                                 @Nullable Module module,
                                                 @NotNull VirtualFile sourceFile,
                                                 @NotNull String folderName) {
        if (module != null) {
            // Strictly within the same module - never search in other modules or build artifacts!
            GlobalSearchScope moduleScope = GlobalSearchScope.moduleScope(module);
            Collection<VirtualFile> matches = FilenameIndex.getVirtualFilesByName(folderName, moduleScope);
            for (VirtualFile match : matches) {
                if (WOComponentClassResolver.isValidComponentFolder(project, match)) {
                    return match;
                }
            }

            // Fallback: search source/content roots of the module directly (in case index is building)
            VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
            for (VirtualFile root : contentRoots) {
                if (WOComponentClassResolver.isBuildArtifact(root)) continue;

                VirtualFile folder = WOComponentClassResolver.findFolderByNameRecursive(root.findFileByRelativePath("src/main/components"), folderName);
                if (folder != null && WOComponentClassResolver.isValidComponentFolder(project, folder)) return folder;

                folder = WOComponentClassResolver.findFolderByNameRecursive(root.findFileByRelativePath("src/main/resources"), folderName);
                if (folder != null && WOComponentClassResolver.isValidComponentFolder(project, folder)) return folder;

                folder = WOComponentClassResolver.findFolderByNameRecursive(root.findFileByRelativePath("Components"), folderName);
                if (folder != null && WOComponentClassResolver.isValidComponentFolder(project, folder)) return folder;
            }

            // Strictly same module: do NOT search outside this module!
            return null;
        }

        // Module could not be determined: search strictly within content root of sourceFile
        VirtualFile contentRoot = null;
        try {
            contentRoot = ProjectRootManager.getInstance(project).getFileIndex().getContentRootForFile(sourceFile);
        } catch (Exception ignored) {
        }
        if (contentRoot != null && !WOComponentClassResolver.isBuildArtifact(contentRoot)) {
            VirtualFile folder = WOComponentClassResolver.findFolderByNameRecursive(contentRoot, folderName);
            if (folder != null && WOComponentClassResolver.isValidComponentFolder(project, folder)) {
                return folder;
            }
        }

        return null;
    }
}
