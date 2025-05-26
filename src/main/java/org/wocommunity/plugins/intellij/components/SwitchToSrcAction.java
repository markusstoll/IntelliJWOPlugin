package org.wocommunity.plugins.intellij.components;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SwitchToSrcAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Get the project
        Project project = e.getProject();
        if (project == null) return;

        // Get the currently selected file in the editor
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        VirtualFile currentFile = fileEditorManager.getSelectedFiles().length > 0 ? fileEditorManager.getSelectedFiles()[0] : null;

        if (currentFile != null) {
            // Find the module of the current file
            @Nullable Module module = ModuleUtil.findModuleForFile(currentFile, project);

            // Search for the class by simple name in the module's scope
            // enable for .wo folders as for .html files
            GlobalSearchScope moduleScope = GlobalSearchScope.moduleScope(module);
            String simpleClassName = currentFile.getName().replace(".wo", "");
            simpleClassName = simpleClassName.replace(".html", "");

            PsiShortNamesCache cache = PsiShortNamesCache.getInstance(project);
            PsiClass[] classes = cache.getClassesByName(simpleClassName, GlobalSearchScope.moduleScope(module));

            // Handle the search results
            if (classes.length > 0) {
                FileEditorManager.getInstance(project).openFile(classes[0].getContainingFile().getVirtualFile(), true);
            }
        }
    }
}
