package org.wocommunity.plugins.intellij.runconfig;

import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.wocommunity.plugins.intellij.runconfig.data.VMParametersTableModel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class VMOptionEditorPanel extends JPanel {
    public VMOptionEditorPanel(VMParametersTableModel vmParametersTableModel) {
        {
            // Initialize with BorderLayout
            setLayout(new BorderLayout());

            // Initialize the table with custom model
            JBTable table = new JBTable(vmParametersTableModel);

            // Customize column widths and renderers
            table.getColumnModel().getColumn(0).setPreferredWidth(500);  // Enable Checkbox column

            table.setVisibleRowCount(4);

            // Add table to a scroll pane and then to the panel
            JBScrollPane scrollPane = new JBScrollPane(table);
            add(scrollPane, BorderLayout.CENTER);

            // Add list to a toolbar decorator for add/remove buttons
            ToolbarDecorator decorator = ToolbarDecorator.createDecorator(table)
                    .setAddAction(e -> vmParametersTableModel.addRow())
                    .setRemoveAction(e -> vmParametersTableModel.remove(table.getSelectedRow()));

            add(decorator.createPanel(), BorderLayout.CENTER);
            setMinimumSize(new Dimension(300, 80));
        }
    }
}
