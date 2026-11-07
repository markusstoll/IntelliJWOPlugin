package org.wocommunity.plugins.intellij.eogen.editor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.eogen.core.EODefine;
import org.wocommunity.plugins.intellij.eogen.core.EOGeneratorModel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.File;

public class EOGenFormPanel extends JBPanel<EOGenFormPanel> {

	private final Project project;
	private final EOGeneratorModel model;
	private final Runnable onModified;
	private boolean updatingUi = false;

	// Models
	private final DefaultListModel<String> modelsListModel = new DefaultListModel<>();
	private final JBList<String> modelsList = new JBList<>(modelsListModel);

	// Ref Models
	private final DefaultListModel<String> refModelsListModel = new DefaultListModel<>();
	private final JBList<String> refModelsList = new JBList<>(refModelsListModel);
	private JBCheckBox loadModelGroupCheckBox;

	// File Names
	private JBTextField filenameTemplateField;
	private JBTextField prefixField;
	private JBTextField extensionField;

	// Destination Paths
	private JBTextField superclassPackageField;
	private TextFieldWithBrowseButton destinationField;
	private TextFieldWithBrowseButton subclassDestinationField;

	// Templates
	private TextFieldWithBrowseButton templatesFolderField;
	private JBTextField templateField;
	private JBTextField subclassTemplateField;

	// Options
	private JBCheckBox packageDirsCheckBox;
	private JBCheckBox javaCheckBox;
	private JBCheckBox javaClientCheckBox;
	private JBCheckBox javaClientCommonCheckBox;
	private JBCheckBox verboseCheckBox;

	// Defines
	private final DefinesTableModel definesTableModel = new DefinesTableModel();
	private final JBTable definesTable = new JBTable(definesTableModel);

	public EOGenFormPanel(Project project, EOGeneratorModel model, Runnable onModified) {
		super(new BorderLayout());
		this.project = project;
		this.model = model;
		this.onModified = onModified;

		initUi();
		loadFromModel();
	}

	private void initUi() {
		JBPanel<?> content = new JBPanel<>();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBorder(JBUI.Borders.empty(12));

		// 1. Models Section
		content.add(createModelsSection());
		content.add(Box.createVerticalStrut(12));

		// 2. Ref Models Section
		content.add(createRefModelsSection());
		content.add(Box.createVerticalStrut(12));

		// 3. File Names Section
		content.add(createFileNamesSection());
		content.add(Box.createVerticalStrut(12));

		// 4. Destination Paths Section
		content.add(createPathsSection());
		content.add(Box.createVerticalStrut(12));

		// 5. Templates Section
		content.add(createTemplatesSection());
		content.add(Box.createVerticalStrut(12));

		// 6. Options Section
		content.add(createOptionsSection());
		content.add(Box.createVerticalStrut(12));

		// 7. Defines Section
		content.add(createDefinesSection());

		JBScrollPane scrollPane = new JBScrollPane(content);
		scrollPane.setBorder(JBUI.Borders.empty());
		add(scrollPane, BorderLayout.CENTER);
	}

	private JComponent createModelsSection() {
		JBPanel<?> panel = new JBPanel<>(new BorderLayout());
		panel.setBorder(IdeBorderFactory.createTitledBorder("Models", false));

		JBLabel desc = new JBLabel("These models will have Java files generated for all of their entities.");
		desc.setForeground(JBUI.CurrentTheme.Label.disabledForeground());
		panel.add(desc, BorderLayout.NORTH);

		modelsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JBScrollPane listScroll = new JBScrollPane(modelsList);
		listScroll.setPreferredSize(new Dimension(300, 80));
		panel.add(listScroll, BorderLayout.CENTER);

		JBPanel<?> buttons = new JBPanel<>(new GridLayout(2, 1, 4, 4));
		buttons.setBorder(JBUI.Borders.emptyLeft(6));
		JButton addBtn = new JButton("Add...", AllIcons.General.Add);
		addBtn.addActionListener(e -> addModelPath(false));
		JButton removeBtn = new JButton("Remove", AllIcons.General.Remove);
		removeBtn.addActionListener(e -> {
			int idx = modelsList.getSelectedIndex();
			if (idx >= 0) {
				String item = modelsListModel.get(idx);
				model.removeModel(item);
				modelsListModel.remove(idx);
				fireModified();
			}
		});
		buttons.add(addBtn);
		buttons.add(removeBtn);
		panel.add(buttons, BorderLayout.EAST);

		return panel;
	}

	private JComponent createRefModelsSection() {
		JBPanel<?> panel = new JBPanel<>(new BorderLayout());
		panel.setBorder(IdeBorderFactory.createTitledBorder("Referenced Models", false));

		JBPanel<?> top = new JBPanel<>(new BorderLayout());
		JBLabel desc = new JBLabel("These models are used to resolve type references. No Java files will be generated for these models.");
		desc.setForeground(JBUI.CurrentTheme.Label.disabledForeground());
		top.add(desc, BorderLayout.NORTH);

		loadModelGroupCheckBox = new JBCheckBox("Load Model Group");
		loadModelGroupCheckBox.addActionListener(e -> {
			if (!updatingUi) {
				model.setLoadModelGroup(loadModelGroupCheckBox.isSelected());
				fireModified();
			}
		});
		top.add(loadModelGroupCheckBox, BorderLayout.SOUTH);
		panel.add(top, BorderLayout.NORTH);

		refModelsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JBScrollPane listScroll = new JBScrollPane(refModelsList);
		listScroll.setPreferredSize(new Dimension(300, 80));
		panel.add(listScroll, BorderLayout.CENTER);

		JBPanel<?> buttons = new JBPanel<>(new GridLayout(2, 1, 4, 4));
		buttons.setBorder(JBUI.Borders.emptyLeft(6));
		JButton addBtn = new JButton("Add...", AllIcons.General.Add);
		addBtn.addActionListener(e -> addModelPath(true));
		JButton removeBtn = new JButton("Remove", AllIcons.General.Remove);
		removeBtn.addActionListener(e -> {
			int idx = refModelsList.getSelectedIndex();
			if (idx >= 0) {
				String item = refModelsListModel.get(idx);
				model.removeRefModel(item);
				refModelsListModel.remove(idx);
				fireModified();
			}
		});
		buttons.add(addBtn);
		buttons.add(removeBtn);
		panel.add(buttons, BorderLayout.EAST);

		return panel;
	}

	private JComponent createFileNamesSection() {
		JBPanel<?> panel = new JBPanel<>(new GridBagLayout());
		panel.setBorder(IdeBorderFactory.createTitledBorder("File Names", false));

		GridBagConstraints gbc = createGbc();

		// Filename Template
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
		panel.add(new JBLabel("Filename Template:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1.0;
		filenameTemplateField = new JBTextField();
		addDocListener(filenameTemplateField, () -> {
			model.setFilenameTemplate(filenameTemplateField.getText().trim());
			fireModified();
		});
		panel.add(filenameTemplateField, gbc);

		// Prefix
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
		panel.add(new JBLabel("Prefix:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1.0;
		prefixField = new JBTextField();
		addDocListener(prefixField, () -> {
			model.setPrefix(prefixField.getText().trim());
			fireModified();
		});
		panel.add(prefixField, gbc);

		// Extension
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
		panel.add(new JBLabel("Extension:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1.0;
		extensionField = new JBTextField();
		addDocListener(extensionField, () -> {
			model.setExtension(extensionField.getText().trim());
			fireModified();
		});
		panel.add(extensionField, gbc);

		return panel;
	}

	private JComponent createPathsSection() {
		JBPanel<?> panel = new JBPanel<>(new GridBagLayout());
		panel.setBorder(IdeBorderFactory.createTitledBorder("Destination Paths", false));

		GridBagConstraints gbc = createGbc();

		// Superclass Package
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
		panel.add(new JBLabel("Superclass Package (e.g. \"base\"):"), gbc);
		gbc.gridx = 1; gbc.weightx = 1.0;
		superclassPackageField = new JBTextField();
		addDocListener(superclassPackageField, () -> {
			model.setSuperclassPackage(superclassPackageField.getText().trim());
			fireModified();
		});
		panel.add(superclassPackageField, gbc);

		// Destination
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
		panel.add(new JBLabel("Destination:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1.0;
		destinationField = new TextFieldWithBrowseButton();
		destinationField.addBrowseFolderListener(project, new FileChooserDescriptor(false, true, false, false, false, false).withTitle("Select Destination Folder"));
		addDocListener(destinationField.getTextField(), () -> {
			model.setDestination(destinationField.getText().trim());
			fireModified();
		});
		panel.add(destinationField, gbc);

		// Subclass Destination
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
		panel.add(new JBLabel("Subclass Destination:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1.0;
		subclassDestinationField = new TextFieldWithBrowseButton();
		subclassDestinationField.addBrowseFolderListener(project, new FileChooserDescriptor(false, true, false, false, false, false).withTitle("Select Subclass Destination Folder"));
		addDocListener(subclassDestinationField.getTextField(), () -> {
			model.setSubclassDestination(subclassDestinationField.getText().trim());
			fireModified();
		});
		panel.add(subclassDestinationField, gbc);

		return panel;
	}

	private JComponent createTemplatesSection() {
		JBPanel<?> panel = new JBPanel<>(new GridBagLayout());
		panel.setBorder(IdeBorderFactory.createTitledBorder("Templates", false));

		GridBagConstraints gbc = createGbc();

		// Templates Folder
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
		panel.add(new JBLabel("Templates Folder:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1.0;
		templatesFolderField = new TextFieldWithBrowseButton();
		templatesFolderField.addBrowseFolderListener(project, new FileChooserDescriptor(false, true, false, false, false, false).withTitle("Select Templates Directory"));
		addDocListener(templatesFolderField.getTextField(), () -> {
			model.setTemplateDir(templatesFolderField.getText().trim());
			fireModified();
		});
		panel.add(templatesFolderField, gbc);

		// Template
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
		panel.add(new JBLabel("Template:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1.0;
		templateField = new JBTextField();
		addDocListener(templateField, () -> {
			model.setJavaTemplate(templateField.getText().trim());
			fireModified();
		});
		panel.add(templateField, gbc);

		// Subclass Template
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
		panel.add(new JBLabel("Subclass Template:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1.0;
		subclassTemplateField = new JBTextField();
		addDocListener(subclassTemplateField, () -> {
			model.setSubclassJavaTemplate(subclassTemplateField.getText().trim());
			fireModified();
		});
		panel.add(subclassTemplateField, gbc);

		return panel;
	}

	private JComponent createOptionsSection() {
		JBPanel<?> panel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 16, 6));
		panel.setBorder(IdeBorderFactory.createTitledBorder("Options", false));

		packageDirsCheckBox = new JBCheckBox("Create Packages");
		packageDirsCheckBox.addActionListener(e -> {
			if (!updatingUi) {
				model.setPackageDirs(packageDirsCheckBox.isSelected());
				fireModified();
			}
		});
		panel.add(packageDirsCheckBox);

		javaCheckBox = new JBCheckBox("Java");
		javaCheckBox.addActionListener(e -> {
			if (!updatingUi) {
				model.setJava(javaCheckBox.isSelected());
				fireModified();
			}
		});
		panel.add(javaCheckBox);

		javaClientCheckBox = new JBCheckBox("Java Client");
		javaClientCheckBox.addActionListener(e -> {
			if (!updatingUi) {
				model.setJavaClient(javaClientCheckBox.isSelected());
				fireModified();
			}
		});
		panel.add(javaClientCheckBox);

		javaClientCommonCheckBox = new JBCheckBox("Java Client Common");
		javaClientCommonCheckBox.addActionListener(e -> {
			if (!updatingUi) {
				model.setJavaClientCommon(javaClientCommonCheckBox.isSelected());
				fireModified();
			}
		});
		panel.add(javaClientCommonCheckBox);

		verboseCheckBox = new JBCheckBox("Verbose");
		verboseCheckBox.addActionListener(e -> {
			if (!updatingUi) {
				model.setVerbose(verboseCheckBox.isSelected());
				fireModified();
			}
		});
		panel.add(verboseCheckBox);

		return panel;
	}

	private JComponent createDefinesSection() {
		JBPanel<?> panel = new JBPanel<>(new BorderLayout());
		panel.setBorder(IdeBorderFactory.createTitledBorder("Defines", false));

		JBLabel desc = new JBLabel("Variables that turn into EOGenerator -define-Xxx parameters (e.g. EOGenericRecord)");
		desc.setForeground(JBUI.CurrentTheme.Label.disabledForeground());
		panel.add(desc, BorderLayout.NORTH);

		definesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		definesTable.setRowHeight(24);
		JBScrollPane tableScroll = new JBScrollPane(definesTable);
		tableScroll.setPreferredSize(new Dimension(400, 100));
		panel.add(tableScroll, BorderLayout.CENTER);

		JBPanel<?> buttons = new JBPanel<>(new GridLayout(2, 1, 4, 4));
		buttons.setBorder(JBUI.Borders.emptyLeft(6));
		JButton addBtn = new JButton("Add...", AllIcons.General.Add);
		addBtn.addActionListener(e -> {
			String name = Messages.showInputDialog(project, "Define variable name (without -define-):", "New Define", Messages.getQuestionIcon());
			if (name != null && !name.trim().isEmpty()) {
				String value = Messages.showInputDialog(project, "Define value for " + name + ":", "Define Value", Messages.getQuestionIcon());
				model.addDefine(name.trim(), value != null ? value.trim() : "");
				definesTableModel.fireTableDataChanged();
				fireModified();
			}
		});
		JButton removeBtn = new JButton("Remove", AllIcons.General.Remove);
		removeBtn.addActionListener(e -> {
			int row = definesTable.getSelectedRow();
			if (row >= 0 && row < model.getDefines().size()) {
				model.removeDefine(model.getDefines().get(row));
				definesTableModel.fireTableDataChanged();
				fireModified();
			}
		});
		buttons.add(addBtn);
		buttons.add(removeBtn);
		panel.add(buttons, BorderLayout.EAST);

		return panel;
	}

	private void addModelPath(boolean isRefModel) {
		FileChooserDescriptor desc = new FileChooserDescriptor(false, true, false, false, false, false)
				.withTitle(isRefModel ? "Select Referenced EOModel Folder" : "Select EOModel Folder");
		com.intellij.openapi.fileChooser.FileChooser.chooseFile(desc, project, null, chosenFile -> {
			if (chosenFile != null) {
				String path = chosenFile.getPath();
				if (project.getBasePath() != null && path.startsWith(project.getBasePath())) {
					path = path.substring(project.getBasePath().length());
					if (path.startsWith(File.separator) || path.startsWith("/")) {
						path = path.substring(1);
					}
				}
				if (isRefModel) {
					model.addRefModel(path);
					refModelsListModel.addElement(path);
				} else {
					model.addModel(path);
					modelsListModel.addElement(path);
				}
				fireModified();
			}
		});
	}

	public void loadFromModel() {
		updatingUi = true;
		try {
			modelsListModel.clear();
			for (String m : model.getModels()) {
				modelsListModel.addElement(m);
			}

			refModelsListModel.clear();
			for (String rm : model.getRefModels()) {
				refModelsListModel.addElement(rm);
			}
			loadModelGroupCheckBox.setSelected(Boolean.TRUE.equals(model.isLoadModelGroup()));

			filenameTemplateField.setText(model.getFilenameTemplate() != null ? model.getFilenameTemplate() : "");
			prefixField.setText(model.getPrefix());
			extensionField.setText(model.getExtension());

			superclassPackageField.setText(model.getSuperclassPackage() != null ? model.getSuperclassPackage() : "");
			destinationField.setText(model.getDestination() != null ? model.getDestination() : "");
			subclassDestinationField.setText(model.getSubclassDestination() != null ? model.getSubclassDestination() : "");

			templatesFolderField.setText(model.getTemplateDir() != null ? model.getTemplateDir() : "");
			templateField.setText(model.getJavaTemplate() != null ? model.getJavaTemplate() : "");
			subclassTemplateField.setText(model.getSubclassJavaTemplate() != null ? model.getSubclassJavaTemplate() : "");

			packageDirsCheckBox.setSelected(Boolean.TRUE.equals(model.isPackageDirs()));
			javaCheckBox.setSelected(Boolean.TRUE.equals(model.isJava()));
			javaClientCheckBox.setSelected(Boolean.TRUE.equals(model.isJavaClient()));
			javaClientCommonCheckBox.setSelected(Boolean.TRUE.equals(model.isJavaClientCommon()));
			verboseCheckBox.setSelected(Boolean.TRUE.equals(model.isVerbose()));

			definesTableModel.fireTableDataChanged();
		} finally {
			updatingUi = false;
		}
	}

	private GridBagConstraints createGbc() {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = JBUI.insets(4, 6);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		return gbc;
	}

	private void addDocListener(JTextField field, Runnable onAction) {
		field.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				if (!updatingUi) onAction.run();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				if (!updatingUi) onAction.run();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				if (!updatingUi) onAction.run();
			}
		});
	}

	private void fireModified() {
		if (onModified != null) {
			onModified.run();
		}
	}

	private class DefinesTableModel extends AbstractTableModel {
		private final String[] columns = {"Name", "Value"};

		@Override
		public int getRowCount() {
			return model.getDefines().size();
		}

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public String getColumnName(int column) {
			return columns[column];
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return true;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (rowIndex >= model.getDefines().size()) return null;
			EODefine def = model.getDefines().get(rowIndex);
			return columnIndex == 0 ? def.getName() : def.getValue();
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (rowIndex >= model.getDefines().size()) return;
			EODefine def = model.getDefines().get(rowIndex);
			String str = aValue != null ? aValue.toString().trim() : "";
			if (columnIndex == 0) {
				def.setName(str);
			} else {
				def.setValue(str);
			}
			fireTableCellUpdated(rowIndex, columnIndex);
			fireModified();
		}
	}
}

