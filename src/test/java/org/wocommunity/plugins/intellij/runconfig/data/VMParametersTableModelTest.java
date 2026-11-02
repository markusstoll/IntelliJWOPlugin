package org.wocommunity.plugins.intellij.runconfig.data;

import org.junit.jupiter.api.Test;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class VMParametersTableModelTest {

    @Test
    public void testRemoveFiresTableRowsDeleted() {
        List<String> initial = List.of("--add-opens=java.base/java.lang=ALL-UNNAMED", "--add-opens=java.base/java.util=ALL-UNNAMED");
        VMParametersTableModel model = new VMParametersTableModel(initial);
        assertEquals(2, model.getRowCount());

        AtomicReference<TableModelEvent> eventRef = new AtomicReference<>();
        model.addTableModelListener(eventRef::set);

        model.remove(0);

        assertEquals(1, model.getRowCount());
        assertEquals("--add-opens=java.base/java.util=ALL-UNNAMED", model.getValueAt(0, 0));
        assertNotNull(eventRef.get(), "TableModelEvent must be fired on remove");
        assertEquals(TableModelEvent.DELETE, eventRef.get().getType());
        assertEquals(0, eventRef.get().getFirstRow());
        assertEquals(0, eventRef.get().getLastRow());
    }

    @Test
    public void testAddRowFiresTableRowsInserted() {
        VMParametersTableModel model = new VMParametersTableModel(new ArrayList<>());
        assertEquals(0, model.getRowCount());

        AtomicReference<TableModelEvent> eventRef = new AtomicReference<>();
        model.addTableModelListener(eventRef::set);

        model.addRow();

        assertEquals(1, model.getRowCount());
        assertEquals("", model.getValueAt(0, 0));
        assertNotNull(eventRef.get(), "TableModelEvent must be fired on add");
        assertEquals(TableModelEvent.INSERT, eventRef.get().getType());
        assertEquals(0, eventRef.get().getFirstRow());
        assertEquals(0, eventRef.get().getLastRow());
    }

    @Test
    public void testSetValueAtFiresTableCellUpdated() {
        VMParametersTableModel model = new VMParametersTableModel(List.of("oldVal"));
        AtomicReference<TableModelEvent> eventRef = new AtomicReference<>();
        model.addTableModelListener(eventRef::set);

        model.setValueAt("newVal", 0, 0);

        assertEquals("newVal", model.getValueAt(0, 0));
        assertNotNull(eventRef.get(), "TableModelEvent must be fired on setValueAt");
        assertEquals(TableModelEvent.UPDATE, eventRef.get().getType());
        assertEquals(0, eventRef.get().getFirstRow());
        assertEquals(0, eventRef.get().getColumn());
    }

    @Test
    public void testReplaceEntriesFiresTableDataChanged() {
        VMParametersTableModel model = new VMParametersTableModel(List.of("oldVal"));
        AtomicReference<TableModelEvent> eventRef = new AtomicReference<>();
        model.addTableModelListener(eventRef::set);

        model.replaceEntries(List.of("val1", "val2"));

        assertEquals(2, model.getRowCount());
        assertNotNull(eventRef.get());
        assertEquals(0, eventRef.get().getFirstRow());
        assertEquals(Integer.MAX_VALUE, eventRef.get().getLastRow());
    }

    @Test
    public void testRemoveOutOfBoundsDoesNotThrow() {
        VMParametersTableModel model = new VMParametersTableModel(List.of("only"));
        assertDoesNotThrow(() -> model.remove(-1));
        assertDoesNotThrow(() -> model.remove(5));
        assertEquals(1, model.getRowCount());
    }
}

