package org.wocommunity.plugins.intellij.eogen.actions;

import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.eogen.core.EOGeneratorModel;
import org.wocommunity.plugins.intellij.eogen.core.EOGeneratorRunner;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class EOGenerateAction extends AnAction {

	private final Runnable preGenerateSave;
	private final EOGeneratorModel generatorModel;
	private Consumer<String> logConsumer;
	private Consumer<EOGeneratorRunner.GenerationResult> resultConsumer;

	public EOGenerateAction() {
		this(null, null, null, null);
	}

	public EOGenerateAction(Runnable preGenerateSave, EOGeneratorModel generatorModel) {
		this(preGenerateSave, generatorModel, null, null);
	}

	public EOGenerateAction(Runnable preGenerateSave, EOGeneratorModel generatorModel,
							Consumer<String> logConsumer,
							Consumer<EOGeneratorRunner.GenerationResult> resultConsumer) {
		super("EOGenerate...", "Run EOGenerator to generate Java source files", AllIcons.Actions.Execute);
		this.preGenerateSave = preGenerateSave;
		this.generatorModel = generatorModel;
		this.logConsumer = logConsumer;
		this.resultConsumer = resultConsumer;
	}

	public void setLogConsumer(Consumer<String> logConsumer) {
		this.logConsumer = logConsumer;
	}

	public void setResultConsumer(Consumer<EOGeneratorRunner.GenerationResult> resultConsumer) {
		this.resultConsumer = resultConsumer;
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

		executeGeneration(project, file);
	}

	public void executeGeneration(@NotNull Project project, @NotNull VirtualFile file) {
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

					EOGeneratorRunner.GenerationResult result = EOGeneratorRunner.generate(project, ioFile, modelToRun, indicator, logConsumer);

					ApplicationManager.getApplication().invokeLater(() -> {
						if (resultConsumer != null) {
							resultConsumer.accept(result);
						}

						if (result.isSuccess() && !result.getGeneratedFiles().isEmpty()) {
							Notification notification = new Notification(
									"EOGenerator", "EOGenerate Succeeded",
									"EOGenerate completed: " + result.getGeneratedFiles().size() + " files generated.",
									NotificationType.INFORMATION
							);
							notification.addAction(new AnAction("View Log") {
								@Override
								public void actionPerformed(@NotNull AnActionEvent e) {
									showLogDialog(project, "EOGenerate Execution Log", result.getMessages());
								}
							});
							Notifications.Bus.notify(notification);
						} else {
							String title = result.getGeneratedFiles().isEmpty()
									? "EOGenerate: 0 Files Generated"
									: "EOGenerate Finished with Errors";
							String content = result.getGeneratedFiles().isEmpty()
									? "No files were generated. Click 'View Log' to see model resolution & entity diagnostics."
									: "EOGenerate encountered issues. Click 'View Log' for details.";

							Notification notification = new Notification(
									"EOGenerator", title, content, NotificationType.WARNING
							);
							notification.addAction(new AnAction("View Log") {
								@Override
								public void actionPerformed(@NotNull AnActionEvent e) {
									showLogDialog(project, title, result.getMessages());
								}
							});
							Notifications.Bus.notify(notification);
						}
					});
				} catch (Exception ex) {
					ApplicationManager.getApplication().invokeLater(() -> {
						Notification notification = new Notification(
								"EOGenerator", "EOGenerate Error",
								"Error running EOGenerator: " + ex.getMessage(),
								NotificationType.ERROR
						);
						Notifications.Bus.notify(notification);
					});
				}
			}
		});
	}

	public static void showLogDialog(Project project, String title, List<String> messages) {
		StringBuilder sb = new StringBuilder();
		for (String msg : messages) {
			sb.append(msg).append("\n");
		}

		JBTextArea textArea = new JBTextArea(sb.toString());
		textArea.setEditable(false);
		textArea.setFont(EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.CONSOLE_PLAIN));
		textArea.setRows(22);
		textArea.setColumns(85);
		textArea.setCaretPosition(0);

		JBScrollPane scrollPane = new JBScrollPane(textArea);
		scrollPane.setPreferredSize(JBUI.size(750, 450));

		DialogBuilder builder = new DialogBuilder(project);
		builder.setTitle(title);
		builder.setCenterPanel(scrollPane);
		builder.addCloseButton();
		builder.show();
	}
}
