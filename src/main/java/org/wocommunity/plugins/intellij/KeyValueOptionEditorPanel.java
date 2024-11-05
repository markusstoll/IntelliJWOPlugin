package org.wocommunity.plugins.intellij;

import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.ToolbarDecorator;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
        import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class KeyValueOptionEditorPanel extends JPanel {
    private final DefaultListModel<KeyValueOption> listModel = new DefaultListModel<>();
    private final JBList<KeyValueOption> list;

    public KeyValueOptionEditorPanel(List<KeyValueOption> options) {
        setLayout(new BorderLayout());

        // Fill model with current options
        options.forEach(listModel::addElement);

        // Configure JBList
        list = new JBList<>(listModel);
        list.setCellRenderer((list, option, index, isSelected, cellHasFocus) -> {
            JPanel panel = new JPanel(new BorderLayout());
            JBCheckBox checkBox = new JBCheckBox("", option.isActive());
            JBTextField keyField = new JBTextField(option.getKey());
            keyField.setEnabled(true);
            JBTextField valueField = new JBTextField(option.getValue());

            checkBox.addActionListener(e -> option.setActive(checkBox.isSelected()));
            keyField.getDocument().addDocumentListener(new SimpleDocumentListener() {
                @Override
                public void update(DocumentEvent e) {
                    option.setKey(keyField.getText());
                }
            });
            valueField.getDocument().addDocumentListener(new SimpleDocumentListener() {
                @Override
                public void update(DocumentEvent e) {
                    option.setValue(valueField.getText());
                }
            });

            panel.add(checkBox, BorderLayout.WEST);
            panel.add(keyField, BorderLayout.CENTER);
            panel.add(valueField, BorderLayout.EAST);
            return panel;
        });

        // Add list to a toolbar decorator for add/remove buttons
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(list)
                .setAddAction(e -> listModel.addElement(new KeyValueOption(true, "", "")))
                .setRemoveAction(e -> listModel.remove(list.getSelectedIndex()));

        add(decorator.createPanel(), BorderLayout.CENTER);
    }

    public List<KeyValueOption> getEditedOptions() {
        return IntStream.range(0,listModel.size()).mapToObj(listModel::get).collect(Collectors.toList());
    }
}

