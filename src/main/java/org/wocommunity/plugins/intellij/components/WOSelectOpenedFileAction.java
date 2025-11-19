package org.wocommunity.plugins.intellij.components;

import com.intellij.ide.FileSelectInContext;
import com.intellij.ide.SelectInContext;
import com.intellij.ide.actions.SelectInContextImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class WOSelectOpenedFileAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file == null) return;

        // Build a context that looks like the one used by Alt‑F1
        SelectInContext context = new FileSelectInContext(project, file);

        WOComponentSelectInTarget target = new WOComponentSelectInTarget();
        if (target.canSelect(context)) {
            target.selectIn(context, true);
        }
    }
}