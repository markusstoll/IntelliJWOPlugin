package org.wocommunity.plugins.intellij.eomodel;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class NewEOModelAction extends AnAction {

	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		Project project = e.getProject();
		if (project == null) {
			return;
		}

		VirtualFile vf = e.getData(CommonDataKeys.VIRTUAL_FILE);
		VirtualFile targetDir = null;
		if (vf != null) {
			targetDir = vf.isDirectory() ? vf : vf.getParent();
		}

		NewEOModelDialog dialog = new NewEOModelDialog(project, targetDir);
		dialog.show();
	}
}

