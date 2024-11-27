package org.wocommunity.plugins.intellij.runconfig;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import org.wocommunity.plugins.intellij.runconfig.data.KeyValueOption;
import org.wocommunity.plugins.intellij.runconfig.data.KeyValueOptionTableModel;

import javax.swing.*;
import java.awt.*;
        import java.util.List;

public class KeyValueOptionEditorPanel extends JPanel {
    public KeyValueOptionEditorPanel(KeyValueOptionTableModel tableModel) {
        // Initialize with BorderLayout
        setLayout(new BorderLayout());

        // Initialize the table with custom model
        JBTable table = new JBTable(tableModel);

        // Customize column widths and renderers
        table.getColumnModel().getColumn(0).setPreferredWidth(20);  // Enable Checkbox column
        table.getColumnModel().getColumn(1).setPreferredWidth(250); // Key column
        table.getColumnModel().getColumn(2).setPreferredWidth(150); // Value column

        table.setVisibleRowCount(10);

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
}
