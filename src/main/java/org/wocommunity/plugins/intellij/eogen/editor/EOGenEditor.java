package org.wocommunity.plugins.intellij.eogen.editor;

import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.eogen.actions.EOGenerateAction;
import org.wocommunity.plugins.intellij.eogen.core.EOGeneratorModel;
import org.wocommunity.plugins.intellij.tools.WOIcons;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class EOGenEditor implements FileEditor, DumbAware, UiDataProvider {

	private final Project project;
	private final VirtualFile file;
	private final File ioFile;
	private final EOGeneratorModel model = new EOGeneratorModel();
	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	private final Map<Key<?>, Object> userData = new HashMap<>();

	private JComponent rootComponent;
	private JBLabel titleLabel;
	private EOGenFormPanel formPanel;
	private EOGenSourcePanel sourcePanel;
	private JBTabbedPane tabbedPane;
	private boolean isDisposed = false;

	public EOGenEditor(@NotNull Project project, @NotNull VirtualFile file) {
		this.project = project;
		this.file = file;
		this.ioFile = new File(file.getPath());

		loadModel();
		initUi();
	}

	private void loadModel() {
		try {
			model.readFromFile(ioFile);
		} catch (Exception e) {
			Notifications.Bus.notify(new Notification(
					"EOGenerator", "Failed to load .eogen file",
					"Error parsing " + file.getName() + ": " + e.getMessage(),
					NotificationType.ERROR
			));
		}
	}

	private void initUi() {
		rootComponent = new JBPanel<>(new BorderLayout());

		// Header Bar
		JBPanel<?> topBar = new JBPanel<>(new BorderLayout());
		topBar.setBorder(JBUI.Borders.compound(
				JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0),
				JBUI.Borders.empty(6, 12)
		));

		JBPanel<?> leftHeader = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 8, 2));
		leftHeader.add(new JLabel(WOIcons.PLUGIN_ICON));
		titleLabel = new JBLabel(getTitleText());
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
		leftHeader.add(titleLabel);
		topBar.add(leftHeader, BorderLayout.WEST);

		// Right Toolbar: Save button AND EOGenerate... button
		DefaultActionGroup actionGroup = new DefaultActionGroup();

		actionGroup.add(new AnAction("Save", "Save changes to .eogen file", AllIcons.Actions.MenuSaveall) {
			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				save();
			}

			@Override
			public @NotNull ActionUpdateThread getActionUpdateThread() {
				return ActionUpdateThread.BGT;
			}

			@Override
			public void update(@NotNull AnActionEvent e) {
				e.getPresentation().setEnabled(isModified());
			}
		});

		// "EOGenerate..." Button positioned directly at the top right of the view
		EOGenerateAction generateAction = new EOGenerateAction(this::save, model);
		actionGroup.add(generateAction);

		ActionToolbarImpl toolbar = new ActionToolbarImpl("EOGenMainToolbar", actionGroup, true);
		toolbar.setTargetComponent(rootComponent);
		topBar.add(toolbar.getComponent(), BorderLayout.EAST);

		rootComponent.add(topBar, BorderLayout.NORTH);

		// Main Tabs
		tabbedPane = new JBTabbedPane();
		formPanel = new EOGenFormPanel(project, model, this::onModelModified);
		sourcePanel = new EOGenSourcePanel(model, ioFile.getParentFile(), this::onModelModified);

		tabbedPane.addTab("Configuration Form", AllIcons.Nodes.Editorconfig, formPanel);
		tabbedPane.addTab("Source", AllIcons.FileTypes.Text, sourcePanel);

		tabbedPane.addChangeListener(e -> {
			if (tabbedPane.getSelectedComponent() == sourcePanel) {
				sourcePanel.updateFromModel();
			} else if (tabbedPane.getSelectedComponent() == formPanel) {
				formPanel.loadFromModel();
			}
		});

		rootComponent.add(tabbedPane, BorderLayout.CENTER);
	}

	private String getTitleText() {
		return "EOGenerator: " + file.getName() + (model.isDirty() ? " *" : "");
	}

	private void onModelModified() {
		model.setDirty(true);
		titleLabel.setText(getTitleText());
		propertyChangeSupport.firePropertyChange("modified", null, isModified());
	}

	public void save() {
		ApplicationManager.getApplication().invokeLater(() -> {
			WriteCommandAction.runWriteCommandAction(project, "Save .eogen", null, () -> {
				try {
					model.writeToFile(ioFile);
					file.refresh(true, false);
					model.setDirty(false);
					titleLabel.setText(getTitleText());
					propertyChangeSupport.firePropertyChange("modified", null, isModified());
					Notifications.Bus.notify(new Notification(
							"EOGenerator", "File Saved",
							"Saved " + file.getName(),
							NotificationType.INFORMATION
					));
				} catch (Exception e) {
					Notifications.Bus.notify(new Notification(
							"EOGenerator", "Save Failed",
							"Could not save " + file.getName() + ": " + e.getMessage(),
							NotificationType.ERROR
					));
				}
			});
		});
	}

	@Override
	public @NotNull JComponent getComponent() {
		return rootComponent;
	}

	@Override
	public @Nullable JComponent getPreferredFocusedComponent() {
		return formPanel;
	}

	@Override
	public @NlsContexts.TabTitle @NotNull String getName() {
		return file.getName();
	}

	@Override
	public void setState(@NotNull FileEditorState state) {
	}

	@Override
	public boolean isModified() {
		return model.isDirty();
	}

	@Override
	public boolean isValid() {
		return !isDisposed && file.isValid();
	}

	@Override
	public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}

	@Override
	public @Nullable FileEditorLocation getCurrentLocation() {
		return null;
	}

	@Override
	public @Nullable VirtualFile getFile() {
		return file;
	}

	@Override
	public void uiDataSnapshot(@NotNull DataSink sink) {
		sink.set(CommonDataKeys.PROJECT, project);
		sink.set(CommonDataKeys.VIRTUAL_FILE, file);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> @Nullable T getUserData(@NotNull Key<T> key) {
		return (T) userData.get(key);
	}

	@Override
	public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
		userData.put(key, value);
	}

	@Override
	public void dispose() {
		isDisposed = true;
	}
}
