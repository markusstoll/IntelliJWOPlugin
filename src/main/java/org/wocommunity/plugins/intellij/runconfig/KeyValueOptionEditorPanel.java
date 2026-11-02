package org.wocommunity.plugins.intellij.runconfig;

import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import org.wocommunity.plugins.intellij.runconfig.data.KeyValueOptionTableModel;

import javax.swing.*;
import java.awt.*;

public class KeyValueOptionEditorPanel extends JPanel {
    public KeyValueOptionEditorPanel(KeyValueOptionTableModel tableModel) {
        setLayout(new BorderLayout());

        JBTable table = new JBTable(tableModel);

        table.getColumnModel().getColumn(0).setPreferredWidth(20);  // Enable Checkbox column
        table.getColumnModel().getColumn(1).setPreferredWidth(250); // Key column
        table.getColumnModel().getColumn(2).setPreferredWidth(150); // Value column

        table.setVisibleRowCount(10);

        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(table)
                .setAddAction(button -> {
                    if (table.isEditing()) {
                        table.getCellEditor().stopCellEditing();
                    }
                    tableModel.addRow();
                    int newRow = tableModel.getRowCount() - 1;
                    table.setRowSelectionInterval(newRow, newRow);
                    table.editCellAt(newRow, 1);
                })
                .setRemoveAction(button -> {
                    if (table.isEditing()) {
                        table.getCellEditor().stopCellEditing();
                    }
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow >= 0 && selectedRow < table.getRowCount()) {
                        int modelRow = table.convertRowIndexToModel(selectedRow);
                        tableModel.remove(modelRow);
                        int count = table.getRowCount();
                        if (count > 0) {
                            int newSel = Math.min(selectedRow, count - 1);
                            table.setRowSelectionInterval(newSel, newSel);
                        }
                    }
                })
                .setRemoveActionUpdater(e -> table.getSelectedRow() >= 0);

        add(decorator.createPanel(), BorderLayout.CENTER);
        setMinimumSize(new Dimension(300, 80));
    }
}
