package org.wocommunity.plugins.intellij.eomodel.editor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;
import org.wocommunity.plugins.intellij.eomodel.core.model.EOEntity;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class EOEntityDetailPanel extends JBPanel<EOEntityDetailPanel> {

	private final Project project;
	private final Runnable onModified;
	private final Runnable onEntityRenamed;

	private EOEntity currentEntity;
	private boolean updatingUi = false;

	private JBTextField nameField;
	private JBTextField tableField;
	private JBTextField classField;
	private JButton openClassButton;
	private JBCheckBox abstractCheckBox;
	private JBCheckBox readOnlyCheckBox;

	private EOAttributesTablePanel attributesPanel;
	private EORelationshipsTablePanel relationshipsPanel;
	private JBTabbedPane tabbedPane;

	public EOEntityDetailPanel(Project project, Runnable onModified, Runnable onEntityRenamed) {
		super(new BorderLayout());
		this.project = project;
		this.onModified = onModified;
		this.onEntityRenamed = onEntityRenamed;

		initUi();
	}

	private void initUi() {
		JBPanel<?> headerPanel = new JBPanel<>(new GridBagLayout());
		headerPanel.setBorder(JBUI.Borders.compound(
				JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0),
				JBUI.Borders.empty(10, 12)
		));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = JBUI.insets(4, 6);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// Row 0: Entity Name & Table Name
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.0;
		headerPanel.add(new JBLabel("Entity Name:"), gbc);

		gbc.gridx = 1;
		gbc.weightx = 0.5;
		nameField = new JBTextField();
		addDocListener(nameField, () -> {
			if (currentEntity != null) {
				currentEntity.setName(nameField.getText().trim());
				fireModified();
				if (onEntityRenamed != null) onEntityRenamed.run();
			}
		});
		headerPanel.add(nameField, gbc);

		gbc.gridx = 2;
		gbc.weightx = 0.0;
		headerPanel.add(new JBLabel("Table Name:"), gbc);

		gbc.gridx = 3;
		gbc.weightx = 0.5;
		tableField = new JBTextField();
		addDocListener(tableField, () -> {
			if (currentEntity != null) {
				currentEntity.setExternalName(tableField.getText().trim());
				fireModified();
			}
		});
		headerPanel.add(tableField, gbc);

		// Row 1: Java Class & Jump Button
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 0.0;
		headerPanel.add(new JBLabel("Java Class:"), gbc);

		gbc.gridx = 1;
		gbc.weightx = 0.5;
		classField = new JBTextField();
		addDocListener(classField, () -> {
			if (currentEntity != null) {
				currentEntity.setClassName(classField.getText().trim());
				fireModified();
			}
		});
		headerPanel.add(classField, gbc);

		gbc.gridx = 2;
		gbc.gridwidth = 2;
		gbc.weightx = 0.5;
		JBPanel<?> classActionPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 4, 0));
		openClassButton = new JButton("Open Java Class", AllIcons.Nodes.Class);
		openClassButton.addActionListener(e -> navigateToJavaClass());
		classActionPanel.add(openClassButton);

		abstractCheckBox = new JBCheckBox("Abstract Entity");
		abstractCheckBox.addActionListener(e -> {
			if (!updatingUi && currentEntity != null) {
				currentEntity.setAbstractEntity(abstractCheckBox.isSelected());
				fireModified();
			}
		});
		classActionPanel.add(abstractCheckBox);

		readOnlyCheckBox = new JBCheckBox("Read Only");
		readOnlyCheckBox.addActionListener(e -> {
			if (!updatingUi && currentEntity != null) {
				currentEntity.setReadOnly(readOnlyCheckBox.isSelected());
				fireModified();
			}
		});
		classActionPanel.add(readOnlyCheckBox);

		headerPanel.add(classActionPanel, gbc);

		add(headerPanel, BorderLayout.NORTH);

		// Detail Tabs
		tabbedPane = new JBTabbedPane();
		attributesPanel = new EOAttributesTablePanel(onModified);
		relationshipsPanel = new EORelationshipsTablePanel(onModified);

		tabbedPane.addTab("Attributes", AllIcons.Nodes.Field, attributesPanel);
		tabbedPane.addTab("Relationships", AllIcons.Vcs.Branch, relationshipsPanel);

		add(tabbedPane, BorderLayout.CENTER);
	}

	public void setEntity(EOEntity entity) {
		this.currentEntity = entity;
		updatingUi = true;
		try {
			if (entity != null) {
				nameField.setText(entity.getName() != null ? entity.getName() : "");
				tableField.setText(entity.getExternalName() != null ? entity.getExternalName() : "");
				classField.setText(entity.getClassName() != null ? entity.getClassName() : "");
				abstractCheckBox.setSelected(entity.isAbstractEntity());
				readOnlyCheckBox.setSelected(entity.isReadOnly());
				nameField.setEnabled(true);
				tableField.setEnabled(true);
				classField.setEnabled(true);
				openClassButton.setEnabled(true);
				abstractCheckBox.setEnabled(true);
				readOnlyCheckBox.setEnabled(true);
			} else {
				nameField.setText("");
				tableField.setText("");
				classField.setText("");
				abstractCheckBox.setSelected(false);
				readOnlyCheckBox.setSelected(false);
				nameField.setEnabled(false);
				tableField.setEnabled(false);
				classField.setEnabled(false);
				openClassButton.setEnabled(false);
				abstractCheckBox.setEnabled(false);
				readOnlyCheckBox.setEnabled(false);
			}
		} finally {
			updatingUi = false;
		}

		attributesPanel.setEntity(entity);
		relationshipsPanel.setEntity(entity);
	}

	private void navigateToJavaClass() {
		if (currentEntity == null || project == null) return;
		String className = currentEntity.getClassName();
		if (className == null || className.trim().isEmpty()) {
			Messages.showInfoMessage(project, "No Java class specified for this entity.", "Java Class");
			return;
		}

		PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project));
		if (psiClass != null && psiClass.canNavigate()) {
			psiClass.navigate(true);
		} else {
			Messages.showWarningDialog(project, "Could not find class: " + className, "Class Not Found");
		}
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
		if (currentEntity != null && currentEntity.getModel() != null) {
			currentEntity.getModel().setDirty(true);
		}
		if (onModified != null) {
			onModified.run();
		}
	}
}
