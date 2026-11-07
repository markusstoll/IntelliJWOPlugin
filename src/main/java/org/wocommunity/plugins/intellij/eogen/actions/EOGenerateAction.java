package org.wocommunity.plugins.intellij.eogen.actions;

import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.eogen.core.EOGeneratorModel;
import org.wocommunity.plugins.intellij.eogen.core.EOGeneratorRunner;

import java.io.File;

public class EOGenerateAction extends AnAction {

	private final Runnable preGenerateSave;
	private final EOGeneratorModel generatorModel;

	public EOGenerateAction() {
		this(null, null);
	}

	public EOGenerateAction(Runnable preGenerateSave, EOGeneratorModel generatorModel) {
		super("EOGenerate...", "Run EOGenerator to generate Java source files", AllIcons.Actions.Execute);
		this.preGenerateSave = preGenerateSave;
		this.generatorModel = generatorModel;
	}

	@Override
	public @NotNull ActionUpdateThread getActionUpdateThread() {
		return ActionUpdateThread.BGT;
	}

	@Override
	public void update(@NotNull AnActionEvent e) {
		if (generatorModel != null) {
			e.getPresentation().setEnabledAndVisible(true);
			return;
		}
		VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
		boolean visible = file != null && !file.isDirectory() && "eogen".equalsIgnoreCase(file.getExtension());
		e.getPresentation().setEnabledAndVisible(visible);
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		Project project = e.getProject();
		if (project == null) return;

		VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
		if (file == null) return;

		if (preGenerateSave != null) {
			preGenerateSave.run();
		}

		File ioFile = new File(file.getPath());

		ProgressManager.getInstance().run(new Task.Backgroundable(project, "Running EOGenerator...", true) {
			@Override
			public void run(@NotNull ProgressIndicator indicator) {
				try {
					EOGeneratorModel modelToRun = generatorModel;
					if (modelToRun == null) {
						modelToRun = new EOGeneratorModel();
						modelToRun.readFromFile(ioFile);
					}

					EOGeneratorRunner.GenerationResult result = EOGeneratorRunner.generate(project, ioFile, modelToRun, indicator);

					if (result.isSuccess()) {
						String summary = "EOGenerate completed: " + result.getGeneratedFiles().size() + " files generated.";
						Notifications.Bus.notify(new Notification(
								"EOGenerator", "EOGenerate Succeeded",
								summary,
								NotificationType.INFORMATION
						));
					} else {
						StringBuilder sb = new StringBuilder("EOGenerate finished with errors:\n");
						for (String msg : result.getMessages()) {
							sb.append(msg).append("\n");
						}
						Notifications.Bus.notify(new Notification(
								"EOGenerator", "EOGenerate Failed",
								sb.toString(),
								NotificationType.WARNING
						));
					}
				} catch (Exception ex) {
					Notifications.Bus.notify(new Notification(
							"EOGenerator", "EOGenerate Error",
							"Error running EOGenerator: " + ex.getMessage(),
							NotificationType.ERROR
					));
				}
			}
		});
	}
}

