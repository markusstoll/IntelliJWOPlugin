package org.wocommunity.plugins.intellij.actions.wocomponent;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;

public class NewWOComponentAction extends AnAction {

    @Override
    public void update(AnActionEvent e) {
        // Control visibility based on the context, e.g., only show for certain file types.
//        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
//        boolean isContextSuitable = file != null && file.getName().endsWith(".java");  // Example condition
//        e.getPresentation().setEnabledAndVisible(isContextSuitable);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        @Nullable PsiElement psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);
        @Nullable VirtualFile vf = e.getData(CommonDataKeys.VIRTUAL_FILE);
        @Nullable PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        NewWOComponentDialog dialog = new NewWOComponentDialog(project, psiElement, psiFile);
        dialog.show();
    }
}
