package org.wocommunity.plugins.intellij.components;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.wotemplate.WOComponentClassResolver;

import java.util.Collection;
import java.util.List;

public class SwitchToSrcAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile file = getComponentFile(e);
        boolean enabled = project != null && file != null && isComponentFile(file);
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    public static @Nullable VirtualFile getComponentFile(@NotNull AnActionEvent e) {
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        FileEditor editor = e.getData(PlatformDataKeys.FILE_EDITOR);
        FileEditor lastActive = e.getData(PlatformDataKeys.LAST_ACTIVE_FILE_EDITOR);

        VirtualFile target = resolveComponentFile(file, files, editor, lastActive);
        if (target != null) {
            return target;
        }

        Project project = e.getProject();
        if (project != null && !project.isDisposed()) {
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
            FileEditor selectedEditor = fileEditorManager.getSelectedEditor();
            if (selectedEditor instanceof WOComponentEditor wce) {
                return wce.getFolder();
            }
            if (selectedEditor != null && selectedEditor.getFile() != null && isComponentFile(selectedEditor.getFile())) {
                return selectedEditor.getFile();
            }
            for (VirtualFile sf : fileEditorManager.getSelectedFiles()) {
                if (isComponentFile(sf)) {
                    return sf;
                }
            }
        }

        return null;
    }

    public static @Nullable VirtualFile resolveComponentFile(@Nullable VirtualFile file,
                                                             VirtualFile @Nullable [] files,
                                                             @Nullable FileEditor editor,
                                                             @Nullable FileEditor lastActiveEditor) {
        if (file != null && isComponentFile(file)) {
            return file;
        }
        if (files != null && files.length > 0 && isComponentFile(files[0])) {
            return files[0];
        }
        if (editor instanceof WOComponentEditor wce) {
            return wce.getFolder();
        }
        if (editor != null && editor.getFile() != null && isComponentFile(editor.getFile())) {
            return editor.getFile();
        }
        if (lastActiveEditor instanceof WOComponentEditor wce) {
            return wce.getFolder();
        }
        if (lastActiveEditor != null && lastActiveEditor.getFile() != null && isComponentFile(lastActiveEditor.getFile())) {
            return lastActiveEditor.getFile();
        }
        return null;
    }

    private static boolean isComponentFile(@NotNull VirtualFile file) {
        String name = file.getName();
        if (name.endsWith(".wo") || name.endsWith(".html") || name.endsWith(".wod") || name.endsWith(".woo")) {
            return true;
        }
        VirtualFile parent = file.getParent();
        return parent != null && parent.getName().endsWith(".wo");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        VirtualFile currentFile = getComponentFile(e);
        if (currentFile == null) return;

        // 1. Try resolving the PsiClass (strictly within the same module, non-build artifact)
        PsiClass componentClass = WOComponentClassResolver.findComponentClass(project, currentFile);
        if (componentClass != null && componentClass.canNavigate() && WOComponentClassResolver.isValidSourceClass(componentClass)) {
            componentClass.navigate(true);
            return;
        }

        // 2. Direct file lookup fallback (.kt, .java, .groovy) strictly in the same module
        String componentName = WOComponentClassResolver.getComponentName(currentFile);
        if (componentName == null || componentName.isBlank()) return;

        Module module = ModuleUtilCore.findModuleForFile(currentFile, project);
        if (module != null) {
            GlobalSearchScope moduleScope = GlobalSearchScope.moduleScope(module);
            for (String ext : List.of(".kt", ".java", ".groovy")) {
                String fileName = componentName + ext;
                Collection<VirtualFile> files = FilenameIndex.getVirtualFilesByName(fileName, moduleScope);
                for (VirtualFile targetFile : files) {
                    if (WOComponentClassResolver.isValidSourceFile(project, targetFile)) {
                        new OpenFileDescriptor(project, targetFile).navigate(true);
                        return;
                    }
                }
            }

            // Direct search in source roots of this module
            VirtualFile[] sourceRoots = ModuleRootManager.getInstance(module).getSourceRoots();
            for (VirtualFile sRoot : sourceRoots) {
                if (WOComponentClassResolver.isBuildArtifact(sRoot)) continue;
                for (String ext : List.of(".kt", ".java", ".groovy")) {
                    VirtualFile targetFile = WOComponentClassResolver.findFileByNameRecursive(sRoot, componentName + ext);
                    if (targetFile != null && WOComponentClassResolver.isValidSourceFile(project, targetFile)) {
                        new OpenFileDescriptor(project, targetFile).navigate(true);
                        return;
                    }
                }
            }

            // Strictly same module: do NOT search outside this module!
            return;
        }

        // Module could not be determined: search within content root containing currentFile
        VirtualFile contentRoot = null;
        try {
            contentRoot = ProjectRootManager.getInstance(project).getFileIndex().getContentRootForFile(currentFile);
        } catch (Exception ignored) {
        }
        if (contentRoot != null && !WOComponentClassResolver.isBuildArtifact(contentRoot)) {
            for (String ext : List.of(".kt", ".java", ".groovy")) {
                VirtualFile targetFile = WOComponentClassResolver.findFileByNameRecursive(contentRoot, componentName + ext);
                if (targetFile != null && WOComponentClassResolver.isValidSourceFile(project, targetFile)) {
                    new OpenFileDescriptor(project, targetFile).navigate(true);
                    return;
                }
            }
        }
    }
}
