package org.wocommunity.plugins.intellij.wizards;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;

public class WOProjectConfigPanel extends JPanel {
    private final ComboBox<WOArchetype> archetypeCombo;
    private final JTextArea descriptionArea;
    private final JBTextField nameField;
    private final TextFieldWithBrowseButton locationField;
    private final JBTextField groupIdField;
    private final JBTextField artifactIdField;
    private final JBTextField versionField;
    private final JBTextField packageField;
    private final ComboBox<String> javaVersionCombo;
    private final ComboBox<String> wonderVersionCombo;
    private final JBCheckBox wonderSupportCheckBox;
    private final JBCheckBox addRepositoryCheckBox;

    private boolean locationManuallyModified = false;
    private boolean packageManuallyModified = false;
    private boolean artifactIdManuallyModified = false;
    private boolean updatingInternally = false;

    public WOProjectConfigPanel(@Nullable String defaultLocation) {
        setLayout(new GridBagLayout());
        setBorder(JBUI.Borders.empty(10));

        archetypeCombo = new ComboBox<>(WOArchetype.values());
        descriptionArea = new JTextArea(3, 40);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setLineWrap(true);
        descriptionArea.setEditable(false);
        descriptionArea.setOpaque(false);
        descriptionArea.setFont(UIManager.getFont("Label.font"));

        nameField = new JBTextField("MyWonderApp", 25);
        artifactIdField = new JBTextField("MyWonderApp", 25);
        groupIdField = new JBTextField("your.domain", 25);
        versionField = new JBTextField("1.0.0-SNAPSHOT", 25);
        packageField = new JBTextField("your.domain.app", 25);

        locationField = new TextFieldWithBrowseButton();
        locationField.addBrowseFolderListener(null,
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle("Select Project Location")
                        .withDescription("Choose directory for the new WebObjects project"));
        if (defaultLocation != null && !defaultLocation.isBlank()) {
            locationField.setText(new File(defaultLocation, "MyWonderApp").getAbsolutePath());
        }

        javaVersionCombo = new ComboBox<>(new String[]{"17", "21", "11", "1.8"});
        javaVersionCombo.setEditable(true);

        wonderVersionCombo = new ComboBox<>(new String[]{"7.2", "7.4", "7.5", "6.1.4"});
        wonderVersionCombo.setEditable(true);

        wonderSupportCheckBox = new JBCheckBox("Enable Project Wonder support", true);
        addRepositoryCheckBox = new JBCheckBox("Add WOCommunity repository to pom.xml", true);
        addRepositoryCheckBox.setToolTipText("Ensures wonder dependencies like ERExtensions resolve without requiring ~/.m2/settings.xml configuration");

        setupListeners();
        layoutComponents();
        updateArchetypeDetails();
    }

    private void setupListeners() {
        archetypeCombo.addActionListener(e -> updateArchetypeDetails());

        nameField.getDocument().addDocumentListener(new SimpleDocListener(() -> {
            if (updatingInternally) return;
            updatingInternally = true;
            String name = nameField.getText().trim();
            if (!artifactIdManuallyModified) {
                artifactIdField.setText(name);
            }
            if (!locationManuallyModified) {
                updateLocationForName(name);
            }
            if (!packageManuallyModified) {
                updatePackage(groupIdField.getText().trim(), name);
            }
            updatingInternally = false;
        }));

        artifactIdField.getDocument().addDocumentListener(new SimpleDocListener(() -> {
            if (updatingInternally) return;
            artifactIdManuallyModified = true;
            String artifactId = artifactIdField.getText().trim();
            if (!locationManuallyModified) {
                updateLocationForName(artifactId);
            }
            if (!packageManuallyModified) {
                updatePackage(groupIdField.getText().trim(), artifactId);
            }
        }));

        groupIdField.getDocument().addDocumentListener(new SimpleDocListener(() -> {
            if (updatingInternally) return;
            if (!packageManuallyModified) {
                updatePackage(groupIdField.getText().trim(), artifactIdField.getText().trim());
            }
        }));

        locationField.getTextField().getDocument().addDocumentListener(new SimpleDocListener(() -> {
            if (!updatingInternally) {
                locationManuallyModified = true;
            }
        }));

        packageField.getDocument().addDocumentListener(new SimpleDocListener(() -> {
            if (!updatingInternally) {
                packageManuallyModified = true;
            }
        }));
    }

    private void updateLocationForName(String name) {
        String current = locationField.getText().trim();
        if (current.isEmpty()) return;
        File f = new File(current);
        File parent = f.getParentFile();
        if (parent != null) {
            locationField.setText(new File(parent, name).getAbsolutePath());
        }
    }

    private void updatePackage(String groupId, String artifactId) {
        String sanitizedGroup = groupId.replaceAll("[^a-zA-Z0-9_.]", "").toLowerCase();
        if (sanitizedGroup.endsWith(".")) {
            sanitizedGroup = sanitizedGroup.substring(0, sanitizedGroup.length() - 1);
        }
        String sanitizedApp = artifactId.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
        if (sanitizedGroup.isEmpty()) {
            packageField.setText(sanitizedApp.isEmpty() ? "app" : sanitizedApp);
        } else {
            packageField.setText(sanitizedGroup + (sanitizedApp.isEmpty() ? ".app" : "." + sanitizedApp));
        }
    }

    private void updateArchetypeDetails() {
        WOArchetype selected = (WOArchetype) archetypeCombo.getSelectedItem();
        if (selected == null) return;
        descriptionArea.setText(selected.getDescription());

        boolean isWonder = selected.isWonder();
        wonderVersionCombo.setEnabled(isWonder);
        wonderSupportCheckBox.setVisible(selected == WOArchetype.WO_FRAMEWORK);
    }

    private void layoutComponents() {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = JBUI.insets(4);
        int y = 0;

        // Archetype Section
        c.gridx = 0; c.gridy = y++; c.gridwidth = 2;
        add(new TitledSeparator("Project Template (Archetype)"), c);

        c.gridwidth = 1;
        c.gridx = 0; c.gridy = y; c.weightx = 0.0;
        add(new JBLabel("Archetype:"), c);
        c.gridx = 1; c.gridy = y++; c.weightx = 1.0;
        add(archetypeCombo, c);

        c.gridx = 0; c.gridy = y; c.weightx = 0.0;
        add(new JBLabel("Description:"), c);
        c.gridx = 1; c.gridy = y++; c.weightx = 1.0;
        add(descriptionArea, c);

        // Project Coordinates Section
        c.gridx = 0; c.gridy = y++; c.gridwidth = 2;
        add(new TitledSeparator("Project Coordinates"), c);

        c.gridwidth = 1;
        c.gridx = 0; c.gridy = y; c.weightx = 0.0;
        add(new JBLabel("Name:"), c);
        c.gridx = 1; c.gridy = y++; c.weightx = 1.0;
        add(nameField, c);

        c.gridx = 0; c.gridy = y; c.weightx = 0.0;
        add(new JBLabel("Location:"), c);
        c.gridx = 1; c.gridy = y++; c.weightx = 1.0;
        add(locationField, c);

        c.gridx = 0; c.gridy = y; c.weightx = 0.0;
        add(new JBLabel("Group ID:"), c);
        c.gridx = 1; c.gridy = y++; c.weightx = 1.0;
        add(groupIdField, c);

        c.gridx = 0; c.gridy = y; c.weightx = 0.0;
        add(new JBLabel("Artifact ID:"), c);
        c.gridx = 1; c.gridy = y++; c.weightx = 1.0;
        add(artifactIdField, c);

        c.gridx = 0; c.gridy = y; c.weightx = 0.0;
        add(new JBLabel("Version:"), c);
        c.gridx = 1; c.gridy = y++; c.weightx = 1.0;
        add(versionField, c);

        c.gridx = 0; c.gridy = y; c.weightx = 0.0;
        add(new JBLabel("Package:"), c);
        c.gridx = 1; c.gridy = y++; c.weightx = 1.0;
        add(packageField, c);

        // Settings Section
        c.gridx = 0; c.gridy = y++; c.gridwidth = 2;
        add(new TitledSeparator("Build & Runtime Versions"), c);

        c.gridwidth = 1;
        c.gridx = 0; c.gridy = y; c.weightx = 0.0;
        add(new JBLabel("Java Version:"), c);
        c.gridx = 1; c.gridy = y++; c.weightx = 1.0;
        add(javaVersionCombo, c);

        c.gridx = 0; c.gridy = y; c.weightx = 0.0;
        add(new JBLabel("Wonder Version:"), c);
        c.gridx = 1; c.gridy = y++; c.weightx = 1.0;
        add(wonderVersionCombo, c);

        c.gridx = 1; c.gridy = y++; c.weightx = 1.0;
        add(wonderSupportCheckBox, c);

        c.gridx = 1; c.gridy = y++; c.weightx = 1.0;
        add(addRepositoryCheckBox, c);

        // Push everything up
        c.gridx = 0; c.gridy = y; c.gridwidth = 2; c.weighty = 1.0;
        add(Box.createVerticalGlue(), c);
    }

    public void syncUIToModel(WOProjectModel model) {
        model.setName(nameField.getText().trim());
        model.setLocation(locationField.getText().trim());
        model.setGroupId(groupIdField.getText().trim());
        model.setArtifactId(artifactIdField.getText().trim());
        model.setVersion(versionField.getText().trim());
        model.setPackageName(packageField.getText().trim());
        model.setArchetype((WOArchetype) archetypeCombo.getSelectedItem());

        Object javaVer = javaVersionCombo.getSelectedItem();
        if (javaVer != null) {
            model.setJavaVersion(javaVer.toString().trim());
        }

        Object wonderVer = wonderVersionCombo.getSelectedItem();
        if (wonderVer != null) {
            model.setWonderVersion(wonderVer.toString().trim());
        }

        model.setWonderSupport(wonderSupportCheckBox.isSelected());
        model.setAddRepositoryToPom(addRepositoryCheckBox.isSelected());
    }

    public void syncModelToUI(WOProjectModel model) {
        updatingInternally = true;
        nameField.setText(model.getName());
        locationField.setText(model.getLocation());
        groupIdField.setText(model.getGroupId());
        artifactIdField.setText(model.getArtifactId());
        versionField.setText(model.getVersion());
        packageField.setText(model.getPackageName());
        archetypeCombo.setSelectedItem(model.getArchetype());
        javaVersionCombo.setSelectedItem(model.getJavaVersion());
        wonderVersionCombo.setSelectedItem(model.getWonderVersion());
        wonderSupportCheckBox.setSelected(model.isWonderSupport());
        addRepositoryCheckBox.setSelected(model.isAddRepositoryToPom());
        updateArchetypeDetails();
        updatingInternally = false;
    }

    public @Nullable String validateInput() {
        if (nameField.getText().trim().isEmpty()) {
            return "Project name cannot be empty.";
        }
        if (locationField.getText().trim().isEmpty()) {
            return "Project location cannot be empty.";
        }
        if (groupIdField.getText().trim().isEmpty()) {
            return "Group ID cannot be empty.";
        }
        if (artifactIdField.getText().trim().isEmpty()) {
            return "Artifact ID cannot be empty.";
        }
        if (versionField.getText().trim().isEmpty()) {
            return "Version cannot be empty.";
        }
        if (packageField.getText().trim().isEmpty()) {
            return "Package name cannot be empty.";
        }

        File target = new File(locationField.getText().trim());
        if (target.exists() && target.isDirectory()) {
            File[] files = target.listFiles();
            if (files != null && files.length > 0) {
                // If contains pom.xml or is not empty, warn
                boolean hasPom = new File(target, "pom.xml").exists();
                if (hasPom) {
                    return "A project with a pom.xml already exists at " + target.getAbsolutePath();
                }
            }
        }
        return null;
    }

    private static class SimpleDocListener implements DocumentListener {
        private final Runnable runnable;

        SimpleDocListener(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override public void insertUpdate(DocumentEvent e) { runnable.run(); }
        @Override public void removeUpdate(DocumentEvent e) { runnable.run(); }
        @Override public void changedUpdate(DocumentEvent e) { runnable.run(); }
    }
}
