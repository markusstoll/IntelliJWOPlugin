package org.wocommunity.plugins.intellij.eomodel.editor;

import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.*;
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
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.eomodel.EOModelFileUtil;
import org.wocommunity.plugins.intellij.eomodel.core.model.EOEntity;
import org.wocommunity.plugins.intellij.eomodel.core.model.EOModel;
import org.wocommunity.plugins.intellij.tools.WOIcons;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EOModelEditor implements FileEditor, DumbAware, UiDataProvider {

	private final Project project;
	private final VirtualFile modelFolderVf;
	private final VirtualFile editorFile;
	private final EOModel eoModel = new EOModel();
	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	private final Map<Key<?>, Object> userData = new HashMap<>();

	private JComponent rootComponent;
	private EOEntitiesPanel entitiesPanel;
	private EOEntityDetailPanel entityDetailPanel;
	private EOConnectionPanel connectionPanel;
	private EOModelRawSourcePanel rawSourcePanel;
	private JBTabbedPane mainTabs;
	private JBLabel statusLabel;

	private boolean isDisposed = false;

	public EOModelEditor(@NotNull Project project, @NotNull VirtualFile modelFolderVf, @NotNull VirtualFile editorFile) {
		this.project = project;
		this.modelFolderVf = modelFolderVf;
		this.editorFile = editorFile;

		loadModel();
		initUi();
	}

	private void loadModel() {
		try {
			File ioFolder = new File(modelFolderVf.getPath());
			eoModel.loadFromFolder(ioFolder);
		} catch (Exception e) {
			Notifications.Bus.notify(new Notification(
					"EOModel", "Failed to Load EOModel",
					"Error loading " + modelFolderVf.getName() + ": " + e.getMessage(),
					NotificationType.ERROR
			));
		}
	}

	private void initUi() {
		rootComponent = new JBPanel<>(new BorderLayout());

		// Top Toolbar
		JBPanel<?> topBar = new JBPanel<>(new BorderLayout());
		topBar.setBorder(JBUI.Borders.compound(
				JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0),
				JBUI.Borders.empty(4, 10)
		));

		JBPanel<?> infoPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 10, 2));
		JLabel iconLabel = new JLabel(WOIcons.EOMODEL_ICON);
		infoPanel.add(iconLabel);
		JLabel nameLabel = new JBLabel("EOModel: " + eoModel.getName());
		nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
		infoPanel.add(nameLabel);

		statusLabel = new JBLabel(getStatusText());
		statusLabel.setForeground(JBUI.CurrentTheme.Label.disabledForeground());
		infoPanel.add(statusLabel);

		topBar.add(infoPanel, BorderLayout.WEST);

		// Actions
		DefaultActionGroup actionGroup = new DefaultActionGroup();
		actionGroup.add(new AnAction("Save Model", "Save changes to disk", AllIcons.Actions.MenuSaveall) {
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

		ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("EOModelMainToolbar", actionGroup, true);
		toolbar.setTargetComponent(rootComponent);
		topBar.add(toolbar.getComponent(), BorderLayout.EAST);

		rootComponent.add(topBar, BorderLayout.NORTH);

		// Main Tabs
		mainTabs = new JBTabbedPane();

		// Tab 1: Entities Structure
		entityDetailPanel = new EOEntityDetailPanel(project, this::onModelModified, () -> {
			entitiesPanel.refreshList();
		});

		entitiesPanel = new EOEntitiesPanel(eoModel, entity -> {
			entityDetailPanel.setEntity(entity);
		}, this::onModelModified);

		OnePixelSplitter splitter = new OnePixelSplitter(false, 0.22f);
		splitter.setFirstComponent(entitiesPanel);
		splitter.setSecondComponent(entityDetailPanel);

		mainTabs.addTab("Entities", AllIcons.Nodes.DataTables, splitter);

		// Tab 2: Connection Settings
		connectionPanel = new EOConnectionPanel(eoModel, this::onModelModified);
		mainTabs.addTab("Connection", AllIcons.General.Settings, connectionPanel);

		// Tab 3: Raw Source
		rawSourcePanel = new EOModelRawSourcePanel(eoModel);
		mainTabs.addTab("Source (Plist)", AllIcons.FileTypes.Text, rawSourcePanel);

		mainTabs.addChangeListener(e -> {
			if (mainTabs.getSelectedComponent() == rawSourcePanel) {
				rawSourcePanel.refreshFileList();
			}
		});

		rootComponent.add(mainTabs, BorderLayout.CENTER);

		if (!eoModel.getEntities().isEmpty()) {
			entityDetailPanel.setEntity(eoModel.getEntities().get(0));
		}
	}

	private String getStatusText() {
		return "(" + eoModel.getEntities().size() + " entities, Adaptor: " + eoModel.getAdaptorName() + ")";
	}

	private void onModelModified() {
		statusLabel.setText(getStatusText() + (eoModel.isDirty() ? " *" : ""));
		propertyChangeSupport.firePropertyChange("modified", null, isModified());
	}

	public void save() {
		if (!eoModel.isDirty()) {
			return;
		}

		ApplicationManager.getApplication().invokeLater(() -> {
			WriteCommandAction.runWriteCommandAction(project, "Save EOModel", null, () -> {
				try {
					File ioFolder = new File(modelFolderVf.getPath());
					eoModel.saveToFolder(ioFolder);
					modelFolderVf.refresh(true, true);
					onModelModified();
					Notifications.Bus.notify(new Notification(
							"EOModel", "EOModel Saved",
							"Successfully saved " + eoModel.getName() + " to disk.",
							NotificationType.INFORMATION
					));
				} catch (Exception e) {
					Notifications.Bus.notify(new Notification(
							"EOModel", "Save Failed",
							"Could not save EOModel: " + e.getMessage(),
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
		return entitiesPanel;
	}

	@Override
	public @NlsContexts.TabTitle @NotNull String getName() {
		return eoModel.getName() != null ? eoModel.getName() + " EOModel" : "EOModel";
	}

	@Override
	public void setState(@NotNull FileEditorState state) {
	}

	@Override
	public boolean isModified() {
		return eoModel.isDirty();
	}

	@Override
	public boolean isValid() {
		return !isDisposed && modelFolderVf.isValid();
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
		return editorFile;
	}

	public EOModel getEOModel() {
		return eoModel;
	}

	@Override
	public void uiDataSnapshot(@NotNull DataSink sink) {
		sink.set(CommonDataKeys.PROJECT, project);
		sink.set(CommonDataKeys.VIRTUAL_FILE, editorFile);
	}

	@Override
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
