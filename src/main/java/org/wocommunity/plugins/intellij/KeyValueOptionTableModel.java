package org.wocommunity.plugins.intellij;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class KeyValueOptionTableModel extends AbstractTableModel {
    private final List<KeyValueOption> entries = new ArrayList<>();

    // Column names
    private final String[] columnNames = {"Enabled", "Key", "Value"};

    // Column types
    private final Class<?>[] columnClasses = {Boolean.class, String.class, String.class};

    public KeyValueOptionTableModel(List<KeyValueOption> options) {
        // Add a sample row for demo purposes
        entries.addAll(options);
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
        KeyValueOption entry = entries.get(rowIndex);
        switch (columnIndex) {
            case 0: return entry.getActive();
            case 1: return entry.getKey();
            case 2: return entry.getValue();
            default: throw new IndexOutOfBoundsException("Column index out of range");
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        KeyValueOption newEntry = entries.get(rowIndex).clone();
        switch (columnIndex) {
            case 0: newEntry.setActive((Boolean) aValue); break;
            case 1: newEntry.setKey((String) aValue); break;
            case 2: newEntry.setValue((String) aValue); break;
        }

        entries.remove(rowIndex);
        entries.add(rowIndex, newEntry);
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
        entries.add(new KeyValueOption(false, "", ""));
        int newRow = entries.size() - 1;
    }

    public void remove(int selectedRow) {
        entries.remove(selectedRow);
    }

    public List<KeyValueOption> getEntries() {
        return entries;
    }

    public void replaceEntries(List<KeyValueOption> keyValueOptions) {
        this.entries.clear();
        this.entries.addAll(keyValueOptions);
    }
}
