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
        return entries.get(rowIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        entries.remove(rowIndex);
        entries.add(rowIndex, (String)aValue);
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
    }

    public void remove(int selectedRow) {
        entries.remove(selectedRow);
    }

    public List<String> getEntries() {
        return new ArrayList<>(entries);
    }

    public void replaceEntries(List<String> parameters) {
        this.entries.clear();
        this.entries.addAll(parameters);
    }
}
