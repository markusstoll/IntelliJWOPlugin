package org.wocommunity.plugins.intellij;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import java.awt.*;
        import java.util.List;

public class KeyValueOptionEditorPanel extends JPanel {
    KeyValueOptionTableModel tableModel;

    public KeyValueOptionEditorPanel(List<KeyValueOption> options) {
        // Initialize with BorderLayout
        setLayout(new BorderLayout());

        // Initialize the table with custom model
        tableModel = new KeyValueOptionTableModel(options);
        JBTable table = new JBTable(tableModel);

        // Customize column widths and renderers
        table.getColumnModel().getColumn(0).setPreferredWidth(20);  // Enable Checkbox column
        table.getColumnModel().getColumn(1).setPreferredWidth(250); // Key column
        table.getColumnModel().getColumn(2).setPreferredWidth(150); // Value column

        // Add table to a scroll pane and then to the panel
        JBScrollPane scrollPane = new JBScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Add list to a toolbar decorator for add/remove buttons
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(table)
                .setAddAction(e -> tableModel.addRow())
                .setRemoveAction(e -> tableModel.remove(table.getSelectedRow()));

        add(decorator.createPanel(), BorderLayout.CENTER);
        setMinimumSize(new Dimension(300, 80));
    }

    public List<KeyValueOption> getKeyValueOptions() {
        return tableModel.getEntries();
    }

    public void setKeyValueOptions(List<KeyValueOption> keyValueOptions) {
        tableModel.replaceEntries(keyValueOptions);
    }
}

