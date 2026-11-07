package org.wocommunity.plugins.intellij.eomodel;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.eomodel.core.model.EOModel;
import org.wocommunity.plugins.intellij.eomodel.core.plist.PropertyListMap;
import org.wocommunity.plugins.intellij.eomodel.core.plist.WOLPropertyListSerialization;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class NewEOModelDialog extends DialogWrapper {

	private final Project project;
	private final VirtualFile contextDirectory;

	private JBTextField modelNameField;
	private ComboBox<String> adaptorComboBox;
	private TextFieldWithBrowseButton folderSelector;

	public NewEOModelDialog(@NotNull Project project, @Nullable VirtualFile contextDirectory) {
		super(project);
		this.project = project;
		this.contextDirectory = contextDirectory;
		setTitle("Create New EOModel");
		init();
	}

	@Override
	protected @Nullable JComponent createCenterPanel() {
		JBPanel<?> panel = new JBPanel<>(new GridBagLayout());
		panel.setPreferredSize(new Dimension(450, 160));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = JBUI.insets(6);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// Model Name
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.0;
		panel.add(new JBLabel("Model Name:"), gbc);

		gbc.gridx = 1;
		gbc.weightx = 1.0;
		modelNameField = new JBTextField();
		modelNameField.setText("Main");
		panel.add(modelNameField, gbc);

		// Adaptor
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 0.0;
		panel.add(new JBLabel("Database Adaptor:"), gbc);

		gbc.gridx = 1;
		gbc.weightx = 1.0;
		adaptorComboBox = new ComboBox<>(new String[]{"JDBC", "None"});
		panel.add(adaptorComboBox, gbc);

		// Folder
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 0.0;
		panel.add(new JBLabel("Target Folder:"), gbc);

		gbc.gridx = 1;
		gbc.weightx = 1.0;
		folderSelector = new TextFieldWithBrowseButton();
		if (contextDirectory != null) {
			folderSelector.setText(contextDirectory.getPath());
		} else if (project.getBasePath() != null) {
			folderSelector.setText(project.getBasePath());
		}
		folderSelector.addBrowseFolderListener(
				project,
				new FileChooserDescriptor(false, true, false, false, false, false)
						.withTitle("Select Destination Directory")
		);
		panel.add(folderSelector, gbc);

		return panel;
	}

	@Override
	public @Nullable JComponent getPreferredFocusedComponent() {
		return modelNameField;
	}

	@Override
	protected void doOKAction() {
		String modelName = modelNameField.getText().trim();
		if (modelName.isEmpty()) {
			Messages.showErrorDialog(project, "Model name cannot be empty.", "Invalid Name");
			return;
		}

		String folderPath = folderSelector.getText().trim();
		if (folderPath.isEmpty()) {
			Messages.showErrorDialog(project, "Please specify a destination directory.", "Invalid Folder");
			return;
		}

		File parentDir = new File(folderPath);
		if (!parentDir.exists() || !parentDir.isDirectory()) {
			Messages.showErrorDialog(project, "Selected folder does not exist: " + folderPath, "Folder Not Found");
			return;
		}

		String dirName = modelName.endsWith(".eomodeld") ? modelName : modelName + ".eomodeld";
		File modelDir = new File(parentDir, dirName);
		if (modelDir.exists()) {
			Messages.showErrorDialog(project, "Model directory already exists: " + modelDir.getPath(), "Model Exists");
			return;
		}

		String chosenAdaptor = (String) adaptorComboBox.getSelectedItem();

		WriteCommandAction.runWriteCommandAction(project, "Create EOModel", null, () -> {
			try {
				if (!modelDir.mkdirs()) {
					throw new IOException("Failed to create directory: " + modelDir);
				}

				File indexFile = new File(modelDir, "index.eomodeld");
				PropertyListMap<String, Object> indexMap = new PropertyListMap<>();
				indexMap.put("EOModelVersion", EOModel.DEFAULT_VERSION);
				indexMap.put("adaptorName", chosenAdaptor != null ? chosenAdaptor : "None");
				indexMap.put("connectionDictionary", new PropertyListMap<>());
				indexMap.put("entities", new ArrayList<>());

				WOLPropertyListSerialization.propertyListToFile("Entity Modeler", indexFile, indexMap);

				// Refresh VFS
				VirtualFile parentVf = com.intellij.openapi.vfs.LocalFileSystem.getInstance().refreshAndFindFileByIoFile(parentDir);
				if (parentVf != null) {
					parentVf.refresh(false, true);
					VirtualFile modelVf = parentVf.findChild(dirName);
					if (modelVf != null) {
						EOModelEditorUtils.openModel(project, modelVf);
					}
				}
			} catch (Exception ex) {
				Messages.showErrorDialog(project, "Failed to create EOModel: " + ex.getMessage(), "Error");
			}
		});

		super.doOKAction();
	}
}

