package org.wocommunity.plugins.intellij.components;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.FileEditor;
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
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile file = getSourceFile(e);
        boolean enabled = project != null && file != null && isSourceFile(file);
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    public static @Nullable VirtualFile getSourceFile(@NotNull AnActionEvent e) {
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        FileEditor editor = e.getData(PlatformDataKeys.FILE_EDITOR);
        FileEditor lastActive = e.getData(PlatformDataKeys.LAST_ACTIVE_FILE_EDITOR);

        VirtualFile target = resolveSourceFile(file, files, editor, lastActive);
        if (target != null) {
            return target;
        }

        Project project = e.getProject();
        if (project != null && !project.isDisposed()) {
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
            FileEditor selectedEditor = fileEditorManager.getSelectedEditor();
            if (selectedEditor != null && selectedEditor.getFile() != null && isSourceFile(selectedEditor.getFile())) {
                return selectedEditor.getFile();
            }
            for (VirtualFile sf : fileEditorManager.getSelectedFiles()) {
                if (isSourceFile(sf)) {
                    return sf;
                }
            }
        }

        return null;
    }

    public static @Nullable VirtualFile resolveSourceFile(@Nullable VirtualFile file,
                                                          VirtualFile @Nullable [] files,
                                                          @Nullable FileEditor editor,
                                                          @Nullable FileEditor lastActiveEditor) {
        if (file != null && isSourceFile(file)) {
            return file;
        }
        if (files != null && files.length > 0 && isSourceFile(files[0])) {
            return files[0];
        }
        if (editor != null && editor.getFile() != null && isSourceFile(editor.getFile())) {
            return editor.getFile();
        }
        if (lastActiveEditor != null && lastActiveEditor.getFile() != null && isSourceFile(lastActiveEditor.getFile())) {
            return lastActiveEditor.getFile();
        }
        return null;
    }

    private static boolean isSourceFile(@NotNull VirtualFile file) {
        String ext = file.getExtension();
        return ext != null && SRC_SUFFIXES.contains(ext.toLowerCase());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        VirtualFile currentFile = getSourceFile(e);
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
