package org.wocommunity.plugins.intellij.runconfig;

import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import org.wocommunity.plugins.intellij.runconfig.data.VMParametersTableModel;

import javax.swing.*;
import java.awt.*;

public class VMOptionEditorPanel extends JPanel {
    public VMOptionEditorPanel(VMParametersTableModel vmParametersTableModel) {
        setLayout(new BorderLayout());

        JBTable table = new JBTable(vmParametersTableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(500);
        table.setVisibleRowCount(4);

        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(table)
                .setAddAction(button -> {
                    if (table.isEditing()) {
                        table.getCellEditor().stopCellEditing();
                    }
                    vmParametersTableModel.addRow();
                    int newRow = vmParametersTableModel.getRowCount() - 1;
                    table.setRowSelectionInterval(newRow, newRow);
                    table.editCellAt(newRow, 0);
                })
                .setRemoveAction(button -> {
                    if (table.isEditing()) {
                        table.getCellEditor().stopCellEditing();
                    }
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow >= 0 && selectedRow < table.getRowCount()) {
                        int modelRow = table.convertRowIndexToModel(selectedRow);
                        vmParametersTableModel.remove(modelRow);
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
