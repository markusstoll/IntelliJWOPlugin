package org.wocommunity.plugins.intellij.eomodel.editor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute;
import org.wocommunity.plugins.intellij.eomodel.core.model.EOEntity;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EOAttributesTablePanel extends JBPanel<EOAttributesTablePanel> {

	private static final String[] COLUMNS = {
			"Name", "Column", "External Type", "Java Class", "PK", "Class Prop", "Locking", "Allows Null", "Width"
	};

	private final Runnable onModified;
	private EOEntity currentEntity;
	private final AttributesTableModel tableModel = new AttributesTableModel();
	private final JBTable table = new JBTable(tableModel);

	public EOAttributesTablePanel(Runnable onModified) {
		super(new BorderLayout());
		this.onModified = onModified;
		setBorder(JBUI.Borders.empty(4));

		initUi();
	}

	private void initUi() {
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(24);
		table.getTableHeader().setReorderingAllowed(false);

		// Column widths
		table.getColumnModel().getColumn(0).setPreferredWidth(140);
		table.getColumnModel().getColumn(1).setPreferredWidth(140);
		table.getColumnModel().getColumn(2).setPreferredWidth(100);
		table.getColumnModel().getColumn(3).setPreferredWidth(140);
		table.getColumnModel().getColumn(4).setPreferredWidth(45);
		table.getColumnModel().getColumn(5).setPreferredWidth(75);
		table.getColumnModel().getColumn(6).setPreferredWidth(60);
		table.getColumnModel().getColumn(7).setPreferredWidth(75);
		table.getColumnModel().getColumn(8).setPreferredWidth(60);

		// Align checkboxes to center
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

		DefaultActionGroup actionGroup = new DefaultActionGroup();
		actionGroup.add(new AnAction("Add Attribute", "Add new attribute", AllIcons.General.Add) {
			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				addAttribute();
			}

			@Override
			public @NotNull ActionUpdateThread getActionUpdateThread() {
				return ActionUpdateThread.BGT;
			}

			@Override
			public void update(@NotNull AnActionEvent e) {
				e.getPresentation().setEnabled(currentEntity != null);
			}
		});

		actionGroup.add(new AnAction("Remove Attribute", "Remove selected attribute", AllIcons.General.Remove) {
			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				removeAttribute();
			}

			@Override
			public @NotNull ActionUpdateThread getActionUpdateThread() {
				return ActionUpdateThread.BGT;
			}

			@Override
			public void update(@NotNull AnActionEvent e) {
				e.getPresentation().setEnabled(currentEntity != null && table.getSelectedRow() >= 0);
			}
		});

		ActionToolbarImpl toolbar = new ActionToolbarImpl("EOAttributesToolbar", actionGroup, true);
		toolbar.setTargetComponent(table);

		add(toolbar.getComponent(), BorderLayout.NORTH);
		add(new JBScrollPane(table), BorderLayout.CENTER);
	}

	public void setEntity(EOEntity entity) {
		this.currentEntity = entity;
		tableModel.fireTableDataChanged();
	}

	private void addAttribute() {
		if (currentEntity == null) return;
		String baseName = "newAttribute";
		String attrName = baseName;
		int count = 1;
		while (currentEntity.getAttribute(attrName) != null) {
			attrName = baseName + count++;
		}

		EOAttribute attr = new EOAttribute(attrName);
		attr.setColumnName(attrName.toUpperCase(Locale.ROOT));
		attr.setExternalType("varchar");
		attr.setValueClassName("NSString");
		attr.setWidth(255);
		attr.setClassProperty(true);
		attr.setUsedForLocking(true);
		attr.setAllowsNull(true);

		currentEntity.addAttribute(attr);
		tableModel.fireTableDataChanged();
		int newRow = currentEntity.getAttributes().size() - 1;
		table.setRowSelectionInterval(newRow, newRow);
		fireModified();
	}

	private void removeAttribute() {
		int row = table.getSelectedRow();
		if (row >= 0 && currentEntity != null && row < currentEntity.getAttributes().size()) {
			EOAttribute attr = currentEntity.getAttributes().get(row);
			currentEntity.removeAttribute(attr);
			tableModel.fireTableDataChanged();
			if (!currentEntity.getAttributes().isEmpty()) {
				int next = Math.min(row, currentEntity.getAttributes().size() - 1);
				table.setRowSelectionInterval(next, next);
			}
			fireModified();
		}
	}

	private void fireModified() {
		if (currentEntity != null && currentEntity.getModel() != null) {
			currentEntity.getModel().setDirty(true);
		}
		if (onModified != null) {
			onModified.run();
		}
	}

	private class AttributesTableModel extends AbstractTableModel {
		@Override
		public int getRowCount() {
			return currentEntity != null ? currentEntity.getAttributes().size() : 0;
		}

		@Override
		public int getColumnCount() {
			return COLUMNS.length;
		}

		@Override
		public String getColumnName(int column) {
			return COLUMNS[column];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
				case 4:
				case 5:
				case 6:
				case 7:
					return Boolean.class;
				case 8:
					return Integer.class;
				default:
					return String.class;
			}
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return true;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (currentEntity == null || rowIndex >= currentEntity.getAttributes().size()) return null;
			EOAttribute attr = currentEntity.getAttributes().get(rowIndex);
			switch (columnIndex) {
				case 0:
					return attr.getName();
				case 1:
					return attr.getColumnName();
				case 2:
					return attr.getExternalType();
				case 3:
					return attr.getValueClassName();
				case 4:
					return attr.isPrimaryKey();
				case 5:
					return attr.isClassProperty();
				case 6:
					return attr.isUsedForLocking();
				case 7:
					return attr.isAllowsNull();
				case 8:
					return attr.getWidth();
				default:
					return null;
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (currentEntity == null || rowIndex >= currentEntity.getAttributes().size()) return;
			EOAttribute attr = currentEntity.getAttributes().get(rowIndex);
			switch (columnIndex) {
				case 0:
					if (aValue != null && !aValue.toString().trim().isEmpty()) {
						attr.setName(aValue.toString().trim());
					}
					break;
				case 1:
					attr.setColumnName(aValue != null ? aValue.toString().trim() : null);
					break;
				case 2:
					attr.setExternalType(aValue != null ? aValue.toString().trim() : null);
					break;
				case 3:
					attr.setValueClassName(aValue != null ? aValue.toString().trim() : null);
					break;
				case 4:
					attr.setPrimaryKey(Boolean.TRUE.equals(aValue));
					break;
				case 5:
					attr.setClassProperty(Boolean.TRUE.equals(aValue));
					break;
				case 6:
					attr.setUsedForLocking(Boolean.TRUE.equals(aValue));
					break;
				case 7:
					attr.setAllowsNull(Boolean.TRUE.equals(aValue));
					break;
				case 8:
					if (aValue instanceof Integer) {
						attr.setWidth((Integer) aValue);
					} else if (aValue != null && !aValue.toString().trim().isEmpty()) {
						try {
							attr.setWidth(Integer.parseInt(aValue.toString().trim()));
						} catch (NumberFormatException ignored) {
						}
					} else {
						attr.setWidth(null);
					}
					break;
			}
			fireTableCellUpdated(rowIndex, columnIndex);
			fireModified();
		}
	}
}
