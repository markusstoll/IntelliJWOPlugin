package org.wocommunity.plugins.intellij.components.api;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;

/**
 * Visual Form Editor for WebObjects Component API (.api) files,
 * matching the layout and functionality of the Eclipse WOLips Component API editor.
 */
public class WOComponentApiPanel extends JPanel implements Disposable {

    private final Project project;
    private final VirtualFile apiFile;
    private final Document document;

    private WOComponentApiModel model;
    private boolean isUpdatingUi = false;

    // Header components
    private JBCheckBox componentContentCheckBox;

    // Bindings list & actions
    private DefaultListModel<String> listModel;
    private JBList<String> bindingsList;
    private JButton addButton;
    private JButton removeButton;

    // Binding Details
    private JBTextField nameField;
    private ComboBox<String> valueSetCombo;
    private JBCheckBox requiredCheckBox;
    private JBCheckBox willSetCheckBox;
    private JPanel detailsPanel;

    private static final String[] DEFAULT_VALUE_SETS = new String[]{
            "", "Boolean", "Date", "Number", "String", "Actions", "Resources"
    };

    public WOComponentApiPanel(@NotNull Project project, @NotNull VirtualFile apiFile) {
        this.project = project;
        this.apiFile = apiFile;
        this.document = ApplicationManager.getApplication().runReadAction((Computable<Document>) () ->
                FileDocumentManager.getInstance().getDocument(apiFile)
        );

        initModel();
        buildUI();
        bindEvents();
        if (!listModel.isEmpty()) {
            bindingsList.setSelectedIndex(0);
        }
        updateDetailsState();

        if (document != null) {
            document.addDocumentListener(new DocumentListener() {
                @Override
                public void documentChanged(@NotNull DocumentEvent event) {
                    if (!isUpdatingUi) {
                        SwingUtilities.invokeLater(() -> reloadFromDocument());
                    }
                }
            }, this);
        }
    }

    private void initModel() {
        String content = ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            if (document != null) {
                return document.getText();
            } else {
                try {
                    return new String(apiFile.contentsToByteArray(), StandardCharsets.UTF_8);
                } catch (Exception ignored) {
                    return "";
                }
            }
        });

        this.model = WOComponentApiXmlParser.parse(content);
        if (this.model.getComponentClassName() == null || this.model.getComponentClassName().isBlank()) {
            String name = apiFile.getNameWithoutExtension();
            this.model.setComponentClassName(name);
        }
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(12));

        // --- TOP AREA: Component API ---
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        TitledSeparator apiSeparator = new TitledSeparator("Component API");
        topPanel.add(apiSeparator);
        topPanel.add(Box.createVerticalStrut(4));

        componentContentCheckBox = new JBCheckBox("Component Content", model.isComponentContent());
        topPanel.add(componentContentCheckBox);
        topPanel.add(Box.createVerticalStrut(12));

        add(topPanel, BorderLayout.NORTH);

        // --- CENTER AREA: Split Two-Column (Bindings on Left, Details on Right) ---
        JPanel mainContentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Left Panel: Bindings
        JPanel bindingsPanel = new JPanel(new BorderLayout(0, 4));
        bindingsPanel.add(new TitledSeparator("Bindings"), BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        for (WOComponentApiBinding b : model.getBindings()) {
            listModel.addElement(b.getName());
        }

        bindingsList = new JBList<>(listModel);
        bindingsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JBScrollPane listScroll = new JBScrollPane(bindingsList);
        listScroll.setPreferredSize(new Dimension(280, 300));
        listScroll.setMinimumSize(new Dimension(200, 200));

        JPanel listWithButtonsPanel = new JPanel(new BorderLayout(6, 0));
        listWithButtonsPanel.add(listScroll, BorderLayout.CENTER);

        // Add/Remove buttons beside list (like in Eclipse)
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        addButton = new JButton("Add");
        removeButton = new JButton("Remove");
        addButton.setMaximumSize(new Dimension(100, 30));
        removeButton.setMaximumSize(new Dimension(100, 30));
        buttonsPanel.add(addButton);
        buttonsPanel.add(Box.createVerticalStrut(4));
        buttonsPanel.add(removeButton);
        listWithButtonsPanel.add(buttonsPanel, BorderLayout.EAST);

        bindingsPanel.add(listWithButtonsPanel, BorderLayout.CENTER);

        // Right Panel: Binding Details
        detailsPanel = new JPanel(new BorderLayout(0, 4));
        detailsPanel.add(new TitledSeparator("Binding Details"), BorderLayout.NORTH);

        JPanel detailsForm = new JPanel(new GridBagLayout());
        detailsForm.setBorder(JBUI.Borders.empty(8, 12));

        GridBagConstraints fGbc = new GridBagConstraints();
        fGbc.anchor = GridBagConstraints.WEST;
        fGbc.insets = JBUI.insets(4, 4, 4, 8);

        // Name
        fGbc.gridx = 0;
        fGbc.gridy = 0;
        fGbc.weightx = 0;
        fGbc.fill = GridBagConstraints.NONE;
        detailsForm.add(new JBLabel("Name:"), fGbc);

        nameField = new JBTextField(20);
        fGbc.gridx = 1;
        fGbc.weightx = 1.0;
        fGbc.fill = GridBagConstraints.HORIZONTAL;
        detailsForm.add(nameField, fGbc);

        // Value Set
        fGbc.gridx = 0;
        fGbc.gridy = 1;
        fGbc.weightx = 0;
        fGbc.fill = GridBagConstraints.NONE;
        detailsForm.add(new JBLabel("Value Set:"), fGbc);

        valueSetCombo = new ComboBox<>(DEFAULT_VALUE_SETS);
        valueSetCombo.setEditable(true);
        fGbc.gridx = 1;
        fGbc.weightx = 1.0;
        fGbc.fill = GridBagConstraints.HORIZONTAL;
        detailsForm.add(valueSetCombo, fGbc);

        // Checkboxes: Required & Will Set
        fGbc.gridx = 1;
        fGbc.gridy = 2;
        fGbc.weightx = 1.0;
        fGbc.fill = GridBagConstraints.NONE;
        fGbc.insets = JBUI.insets(12, 4, 4, 8);
        requiredCheckBox = new JBCheckBox("Required");
        detailsForm.add(requiredCheckBox, fGbc);

        fGbc.gridy = 3;
        fGbc.insets = JBUI.insets(4, 4, 4, 8);
        willSetCheckBox = new JBCheckBox("Will Set");
        detailsForm.add(willSetCheckBox, fGbc);

        // Vertical spacer to keep form pushed to top
        fGbc.gridy = 4;
        fGbc.weighty = 1.0;
        detailsForm.add(Box.createVerticalGlue(), fGbc);

        detailsPanel.add(detailsForm, BorderLayout.CENTER);

        // Assemble Left and Right in GridBag
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.45;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = JBUI.insetsRight(16);
        mainContentPanel.add(bindingsPanel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.55;
        gbc.insets = JBUI.emptyInsets();
        mainContentPanel.add(detailsPanel, gbc);

        add(mainContentPanel, BorderLayout.CENTER);
    }

    private void bindEvents() {
        // Component Content checkbox
        componentContentCheckBox.addActionListener(e -> {
            if (isUpdatingUi) return;
            model.setComponentContent(componentContentCheckBox.isSelected());
            saveToDocument();
        });

        // List selection change
        bindingsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateDetailsState();
            }
        });

        // Add binding
        addButton.addActionListener(e -> {
            String baseName = "newBinding";
            String candidate = baseName;
            int counter = 1;
            while (hasBindingName(candidate)) {
                candidate = baseName + counter++;
            }

            WOComponentApiBinding newBinding = new WOComponentApiBinding(candidate);
            model.addBinding(newBinding);
            listModel.addElement(candidate);
            bindingsList.setSelectedValue(candidate, true);
            saveToDocument();
            nameField.requestFocusInWindow();
            nameField.selectAll();
        });

        // Remove binding
        removeButton.addActionListener(e -> {
            int selectedIdx = bindingsList.getSelectedIndex();
            if (selectedIdx >= 0 && selectedIdx < model.getBindings().size()) {
                model.getBindings().remove(selectedIdx);
                listModel.remove(selectedIdx);
                int nextSel = Math.min(selectedIdx, listModel.size() - 1);
                if (nextSel >= 0) {
                    bindingsList.setSelectedIndex(nextSel);
                } else {
                    updateDetailsState();
                }
                saveToDocument();
            }
        });

        // Name field editing
        nameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { onNameChanged(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { onNameChanged(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { onNameChanged(); }
        });

        // Value Set combo
        valueSetCombo.addActionListener(e -> {
            if (isUpdatingUi) return;
            WOComponentApiBinding b = getSelectedBinding();
            if (b != null) {
                Object item = valueSetCombo.getSelectedItem();
                String val = item != null ? item.toString().trim() : null;
                b.setDefaults((val != null && !val.isBlank()) ? val : null);
                saveToDocument();
            }
        });

        // Required checkbox
        requiredCheckBox.addActionListener(e -> {
            if (isUpdatingUi) return;
            WOComponentApiBinding b = getSelectedBinding();
            if (b != null) {
                b.setRequired(requiredCheckBox.isSelected());
                saveToDocument();
            }
        });

        // Will Set checkbox
        willSetCheckBox.addActionListener(e -> {
            if (isUpdatingUi) return;
            WOComponentApiBinding b = getSelectedBinding();
            if (b != null) {
                b.setWillSet(willSetCheckBox.isSelected());
                saveToDocument();
            }
        });
    }

    private void onNameChanged() {
        if (isUpdatingUi) return;
        int idx = bindingsList.getSelectedIndex();
        if (idx >= 0 && idx < model.getBindings().size()) {
            WOComponentApiBinding b = model.getBindings().get(idx);
            String newName = nameField.getText().trim();
            b.setName(newName);
            listModel.set(idx, newName);
            saveToDocument();
        }
    }

    private boolean hasBindingName(String name) {
        for (WOComponentApiBinding b : model.getBindings()) {
            if (b.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private WOComponentApiBinding getSelectedBinding() {
        int idx = bindingsList.getSelectedIndex();
        if (idx >= 0 && idx < model.getBindings().size()) {
            return model.getBindings().get(idx);
        }
        return null;
    }

    private void updateDetailsState() {
        isUpdatingUi = true;
        try {
            WOComponentApiBinding b = getSelectedBinding();
            boolean hasSelection = (b != null);
            removeButton.setEnabled(hasSelection);
            nameField.setEnabled(hasSelection);
            valueSetCombo.setEnabled(hasSelection);
            requiredCheckBox.setEnabled(hasSelection);
            willSetCheckBox.setEnabled(hasSelection);

            if (hasSelection) {
                nameField.setText(b.getName());
                valueSetCombo.setSelectedItem(b.getDefaults() != null ? b.getDefaults() : "");
                requiredCheckBox.setSelected(b.isRequired());
                willSetCheckBox.setSelected(b.isWillSet());
            } else {
                nameField.setText("");
                valueSetCombo.setSelectedItem("");
                requiredCheckBox.setSelected(false);
                willSetCheckBox.setSelected(false);
            }
        } finally {
            isUpdatingUi = false;
        }
    }

    public void reloadFromDocument() {
        if (document == null) return;
        isUpdatingUi = true;
        try {
            int selectedIdx = bindingsList.getSelectedIndex();
            initModel();

            componentContentCheckBox.setSelected(model.isComponentContent());

            listModel.clear();
            for (WOComponentApiBinding b : model.getBindings()) {
                listModel.addElement(b.getName());
            }

            if (selectedIdx >= 0 && selectedIdx < listModel.size()) {
                bindingsList.setSelectedIndex(selectedIdx);
            } else if (!listModel.isEmpty()) {
                bindingsList.setSelectedIndex(0);
            }
            updateDetailsState();
        } finally {
            isUpdatingUi = false;
        }
    }

    private void saveToDocument() {
        if (document == null || project.isDisposed()) return;
        isUpdatingUi = true;
        try {
            String xml = WOComponentApiXmlParser.toXml(model);
            String currentText = ApplicationManager.getApplication().runReadAction((Computable<String>) document::getText);
            if (!xml.equals(currentText)) {
                WriteCommandAction.runWriteCommandAction(project, "Edit Component API", null, () -> {
                    document.setText(xml);
                });
            }
        } finally {
            isUpdatingUi = false;
        }
    }

    public void ensureSelection() {
        if (!listModel.isEmpty() && bindingsList.getSelectedIndex() < 0) {
            bindingsList.setSelectedIndex(0);
        }
        updateDetailsState();
    }

    @Override
    public void dispose() {
    }
}
