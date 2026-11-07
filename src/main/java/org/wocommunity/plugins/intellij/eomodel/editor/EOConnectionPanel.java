package org.wocommunity.plugins.intellij.eomodel.editor;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.wocommunity.plugins.intellij.eomodel.core.model.EOConnectionDictionary;
import org.wocommunity.plugins.intellij.eomodel.core.model.EOModel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class EOConnectionPanel extends JBPanel<EOConnectionPanel> {

	private final EOModel eoModel;
	private final Runnable onModified;

	private ComboBox<String> adaptorComboBox;
	private JBTextField urlField;
	private JBTextField driverField;
	private JBTextField usernameField;
	private JBPasswordField passwordField;
	private JBTextField pluginField;
	private boolean updatingUi = false;

	public EOConnectionPanel(EOModel eoModel, Runnable onModified) {
		super(new BorderLayout());
		this.eoModel = eoModel;
		this.onModified = onModified;
		setBorder(JBUI.Borders.empty(16));

		initUi();
		loadFromModel();
	}

	private void initUi() {
		JBPanel<?> form = new JBPanel<>(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = JBUI.insets(6);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// Adaptor
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.0;
		form.add(new JBLabel("Database Adaptor:"), gbc);

		gbc.gridx = 1;
		gbc.weightx = 1.0;
		adaptorComboBox = new ComboBox<>(new String[]{"JDBC", "None"});
		adaptorComboBox.addActionListener(e -> {
			if (!updatingUi) {
				eoModel.setAdaptorName((String) adaptorComboBox.getSelectedItem());
				fireModified();
			}
		});
		form.add(adaptorComboBox, gbc);

		// URL
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 0.0;
		form.add(new JBLabel("JDBC URL:"), gbc);

		gbc.gridx = 1;
		gbc.weightx = 1.0;
		urlField = new JBTextField();
		addDocListener(urlField, () -> {
			eoModel.getConnectionDictionary().setUrl(urlField.getText().trim());
			fireModified();
		});
		form.add(urlField, gbc);

		// Driver
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 0.0;
		form.add(new JBLabel("Driver Class:"), gbc);

		gbc.gridx = 1;
		gbc.weightx = 1.0;
		driverField = new JBTextField();
		addDocListener(driverField, () -> {
			eoModel.getConnectionDictionary().setDriver(driverField.getText().trim());
			fireModified();
		});
		form.add(driverField, gbc);

		// Username
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.weightx = 0.0;
		form.add(new JBLabel("Username:"), gbc);

		gbc.gridx = 1;
		gbc.weightx = 1.0;
		usernameField = new JBTextField();
		addDocListener(usernameField, () -> {
			eoModel.getConnectionDictionary().setUsername(usernameField.getText().trim());
			fireModified();
		});
		form.add(usernameField, gbc);

		// Password
		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.weightx = 0.0;
		form.add(new JBLabel("Password:"), gbc);

		gbc.gridx = 1;
		gbc.weightx = 1.0;
		passwordField = new JBPasswordField();
		addDocListener(passwordField, () -> {
			eoModel.getConnectionDictionary().setPassword(new String(passwordField.getPassword()));
			fireModified();
		});
		form.add(passwordField, gbc);

		// Plugin
		gbc.gridx = 0;
		gbc.gridy = 5;
		gbc.weightx = 0.0;
		form.add(new JBLabel("Plugin Class:"), gbc);

		gbc.gridx = 1;
		gbc.weightx = 1.0;
		pluginField = new JBTextField();
		addDocListener(pluginField, () -> {
			eoModel.getConnectionDictionary().setPlugin(pluginField.getText().trim());
			fireModified();
		});
		form.add(pluginField, gbc);

		// Filler
		gbc.gridx = 0;
		gbc.gridy = 6;
		gbc.gridwidth = 2;
		gbc.weighty = 1.0;
		form.add(new JBPanel<>(), gbc);

		add(form, BorderLayout.NORTH);
	}

	public void loadFromModel() {
		updatingUi = true;
		try {
			adaptorComboBox.setSelectedItem(eoModel.getAdaptorName());
			EOConnectionDictionary conn = eoModel.getConnectionDictionary();
			urlField.setText(conn.getUrl() != null ? conn.getUrl() : "");
			driverField.setText(conn.getDriver() != null ? conn.getDriver() : "");
			usernameField.setText(conn.getUsername() != null ? conn.getUsername() : "");
			passwordField.setText(conn.getPassword() != null ? conn.getPassword() : "");
			pluginField.setText(conn.getPlugin() != null ? conn.getPlugin() : "");
		} finally {
			updatingUi = false;
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
		eoModel.setDirty(true);
		if (onModified != null) {
			onModified.run();
		}
	}
}

