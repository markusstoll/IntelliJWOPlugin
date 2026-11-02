package org.wocommunity.plugins.intellij.runconfig.data;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class VMParametersTableModel extends AbstractTableModel {
    private final List<String> entries = new ArrayList<>();

    // Column names
    private final String[] columnNames = {"VM Parameter"};

    // Column types
    private final Class<?>[] columnClasses = {String.class};

    public VMParametersTableModel(List<String> options) {
        if (options != null) {
            entries.addAll(options);
        }
    }

    @Override
    public int getRowCount() {
        return entries.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return entries.get(rowIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (rowIndex >= 0 && rowIndex < entries.size()) {
            entries.set(rowIndex, (String) aValue);
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;  // All cells are editable
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnClasses[columnIndex];
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnNames[columnIndex];
    }

    // Method to add a new row
    public void addRow() {
        entries.add("");
        int index = entries.size() - 1;
        fireTableRowsInserted(index, index);
    }

    public void remove(int selectedRow) {
        if (selectedRow >= 0 && selectedRow < entries.size()) {
            entries.remove(selectedRow);
            fireTableRowsDeleted(selectedRow, selectedRow);
        }
    }

    public List<String> getEntries() {
        return new ArrayList<>(entries);
    }

    public void replaceEntries(List<String> parameters) {
        this.entries.clear();
        if (parameters != null) {
            this.entries.addAll(parameters);
        }
        fireTableDataChanged();
    }
}
