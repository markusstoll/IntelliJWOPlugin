package org.wocommunity.plugins.intellij.eomodel.editor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.eomodel.core.model.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class EORelationshipsTablePanel extends JBPanel<EORelationshipsTablePanel> {

	private static final String[] REL_COLUMNS = {
			"Name", "Destination Entity", "To Many", "Delete Rule", "Joins"
	};

	private static final String[] JOIN_COLUMNS = {
			"Source Attribute", "Destination Attribute"
	};

	private final Runnable onModified;
	private EOEntity currentEntity;
	private EORelationship selectedRelationship;

	private final RelationshipsTableModel relTableModel = new RelationshipsTableModel();
	private final JBTable relTable = new JBTable(relTableModel);

	private final JoinsTableModel joinsTableModel = new JoinsTableModel();
	private final JBTable joinsTable = new JBTable(joinsTableModel);

	public EORelationshipsTablePanel(Runnable onModified) {
		super(new BorderLayout());
		this.onModified = onModified;
		setBorder(JBUI.Borders.empty(4));

		initUi();
	}

	private void initUi() {
		relTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		relTable.setRowHeight(24);
		relTable.getTableHeader().setReorderingAllowed(false);

		relTable.getColumnModel().getColumn(0).setPreferredWidth(140);
		relTable.getColumnModel().getColumn(1).setPreferredWidth(140);
		relTable.getColumnModel().getColumn(2).setPreferredWidth(60);
		relTable.getColumnModel().getColumn(3).setPreferredWidth(100);
		relTable.getColumnModel().getColumn(4).setPreferredWidth(180);

		// Delete rule combo editor
		JComboBox<EODeleteRule> deleteRuleCombo = new JComboBox<>(EODeleteRule.values());
		relTable.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(deleteRuleCombo));

		// Joins table
		joinsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		joinsTable.setRowHeight(24);
		joinsTable.getTableHeader().setReorderingAllowed(false);

		relTable.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				int row = relTable.getSelectedRow();
				if (row >= 0 && currentEntity != null && row < currentEntity.getRelationships().size()) {
					selectedRelationship = currentEntity.getRelationships().get(row);
				} else {
					selectedRelationship = null;
				}
				updateJoinsEditors();
				joinsTableModel.fireTableDataChanged();
			}
		});

		// Top toolbar for relationships
		DefaultActionGroup relActionGroup = new DefaultActionGroup();
		relActionGroup.add(new AnAction("Add Relationship", "Add new relationship", AllIcons.General.Add) {
			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				addRelationship();
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

		relActionGroup.add(new AnAction("Remove Relationship", "Remove selected relationship", AllIcons.General.Remove) {
			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				removeRelationship();
			}

			@Override
			public @NotNull ActionUpdateThread getActionUpdateThread() {
				return ActionUpdateThread.BGT;
			}

			@Override
			public void update(@NotNull AnActionEvent e) {
				e.getPresentation().setEnabled(currentEntity != null && relTable.getSelectedRow() >= 0);
			}
		});

		ActionToolbarImpl relToolbar = new ActionToolbarImpl("EORelationshipsToolbar", relActionGroup, true);
		relToolbar.setTargetComponent(relTable);

		JBPanel<?> topPanel = new JBPanel<>(new BorderLayout());
		topPanel.add(relToolbar.getComponent(), BorderLayout.NORTH);
		topPanel.add(new JBScrollPane(relTable), BorderLayout.CENTER);

		// Bottom toolbar for joins
		DefaultActionGroup joinActionGroup = new DefaultActionGroup();
		joinActionGroup.add(new AnAction("Add Join", "Add new join to selected relationship", AllIcons.General.Add) {
			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				addJoin();
			}

			@Override
			public @NotNull ActionUpdateThread getActionUpdateThread() {
				return ActionUpdateThread.BGT;
			}

			@Override
			public void update(@NotNull AnActionEvent e) {
				e.getPresentation().setEnabled(selectedRelationship != null);
			}
		});

		joinActionGroup.add(new AnAction("Remove Join", "Remove selected join", AllIcons.General.Remove) {
			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				removeJoin();
			}

			@Override
			public @NotNull ActionUpdateThread getActionUpdateThread() {
				return ActionUpdateThread.BGT;
			}

			@Override
			public void update(@NotNull AnActionEvent e) {
				e.getPresentation().setEnabled(selectedRelationship != null && joinsTable.getSelectedRow() >= 0);
			}
		});

		ActionToolbarImpl joinToolbar = new ActionToolbarImpl("EOJoinsToolbar", joinActionGroup, true);
		joinToolbar.setTargetComponent(joinsTable);

		JBPanel<?> bottomPanel = new JBPanel<>(new BorderLayout());
		JBPanel<?> bottomHeader = new JBPanel<>(new BorderLayout());
		bottomHeader.add(new JBLabel(" Joins for Selected Relationship:"), BorderLayout.WEST);
		bottomHeader.add(joinToolbar.getComponent(), BorderLayout.EAST);
		bottomPanel.add(bottomHeader, BorderLayout.NORTH);
		bottomPanel.add(new JBScrollPane(joinsTable), BorderLayout.CENTER);

		OnePixelSplitter splitter = new OnePixelSplitter(true, 0.65f);
		splitter.setFirstComponent(topPanel);
		splitter.setSecondComponent(bottomPanel);

		add(splitter, BorderLayout.CENTER);
	}

	public void setEntity(EOEntity entity) {
		this.currentEntity = entity;
		this.selectedRelationship = null;
		updateDestinationEntityEditor();
		updateJoinsEditors();
		relTableModel.fireTableDataChanged();
		joinsTableModel.fireTableDataChanged();
	}

	private void updateDestinationEntityEditor() {
		if (currentEntity != null && currentEntity.getModel() != null) {
			JComboBox<String> entityCombo = new JComboBox<>();
			for (EOEntity ent : currentEntity.getModel().getEntities()) {
				entityCombo.addItem(ent.getName());
			}
			relTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(entityCombo));
		}
	}

	private void updateJoinsEditors() {
		JComboBox<String> srcCombo = new JComboBox<>();
		if (currentEntity != null) {
			for (EOAttribute attr : currentEntity.getAttributes()) {
				srcCombo.addItem(attr.getName());
			}
		}
		joinsTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(srcCombo));

		JComboBox<String> dstCombo = new JComboBox<>();
		if (selectedRelationship != null && selectedRelationship.getDestination() != null) {
			for (EOAttribute attr : selectedRelationship.getDestination().getAttributes()) {
				dstCombo.addItem(attr.getName());
			}
		}
		joinsTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(dstCombo));
	}

	private void addRelationship() {
		if (currentEntity == null) return;
		String baseName = "toNewRelationship";
		String relName = baseName;
		int count = 1;
		while (currentEntity.getRelationship(relName) != null) {
			relName = baseName + count++;
		}

		EORelationship rel = new EORelationship(relName);
		if (currentEntity.getModel() != null && !currentEntity.getModel().getEntities().isEmpty()) {
			rel.setDestinationEntityName(currentEntity.getModel().getEntities().get(0).getName());
			rel.setDestination(currentEntity.getModel().getEntities().get(0));
		}
		rel.setClassProperty(true);
		rel.setDeleteRule(EODeleteRule.NULLIFY);
		rel.setJoinSemantic(EOJoinSemantic.INNER);

		currentEntity.addRelationship(rel);
		relTableModel.fireTableDataChanged();
		int newRow = currentEntity.getRelationships().size() - 1;
		relTable.setRowSelectionInterval(newRow, newRow);
		fireModified();
	}

	private void removeRelationship() {
		int row = relTable.getSelectedRow();
		if (row >= 0 && currentEntity != null && row < currentEntity.getRelationships().size()) {
			EORelationship rel = currentEntity.getRelationships().get(row);
			currentEntity.removeRelationship(rel);
			relTableModel.fireTableDataChanged();
			if (!currentEntity.getRelationships().isEmpty()) {
				int next = Math.min(row, currentEntity.getRelationships().size() - 1);
				relTable.setRowSelectionInterval(next, next);
			}
			fireModified();
		}
	}

	private void addJoin() {
		if (selectedRelationship == null) return;
		EOJoin join = new EOJoin();
		if (currentEntity != null && !currentEntity.getAttributes().isEmpty()) {
			join.setSourceAttribute(currentEntity.getAttributes().get(0));
		}
		if (selectedRelationship.getDestination() != null && !selectedRelationship.getDestination().getAttributes().isEmpty()) {
			join.setDestinationAttribute(selectedRelationship.getDestination().getAttributes().get(0));
		}
		selectedRelationship.addJoin(join);
		joinsTableModel.fireTableDataChanged();
		relTableModel.fireTableDataChanged();
		fireModified();
	}

	private void removeJoin() {
		int row = joinsTable.getSelectedRow();
		if (row >= 0 && selectedRelationship != null && row < selectedRelationship.getJoins().size()) {
			EOJoin join = selectedRelationship.getJoins().get(row);
			selectedRelationship.removeJoin(join);
			joinsTableModel.fireTableDataChanged();
			relTableModel.fireTableDataChanged();
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

	private class RelationshipsTableModel extends AbstractTableModel {
		@Override
		public int getRowCount() {
			return currentEntity != null ? currentEntity.getRelationships().size() : 0;
		}

		@Override
		public int getColumnCount() {
			return REL_COLUMNS.length;
		}

		@Override
		public String getColumnName(int column) {
			return REL_COLUMNS[column];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex == 2) {
				return Boolean.class;
			}
			if (columnIndex == 3) {
				return EODeleteRule.class;
			}
			return String.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex != 4; // Joins is edited via joins table
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (currentEntity == null || rowIndex >= currentEntity.getRelationships().size()) return null;
			EORelationship rel = currentEntity.getRelationships().get(rowIndex);
			switch (columnIndex) {
				case 0:
					return rel.getName();
				case 1:
					return rel.getDestinationEntityName();
				case 2:
					return rel.isToMany();
				case 3:
					return rel.getDeleteRule();
				case 4:
					if (rel.getJoins().isEmpty()) return "(no joins)";
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < rel.getJoins().size(); i++) {
						if (i > 0) sb.append(", ");
						sb.append(rel.getJoins().get(i).toString());
					}
					return sb.toString();
				default:
					return null;
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (currentEntity == null || rowIndex >= currentEntity.getRelationships().size()) return;
			EORelationship rel = currentEntity.getRelationships().get(rowIndex);
			switch (columnIndex) {
				case 0:
					if (aValue != null && !aValue.toString().trim().isEmpty()) {
						rel.setName(aValue.toString().trim());
					}
					break;
				case 1:
					if (aValue != null) {
						rel.setDestinationEntityName(aValue.toString());
						if (currentEntity.getModel() != null) {
							rel.setDestination(currentEntity.getModel().getEntity(aValue.toString()));
						}
						updateJoinsEditors();
					}
					break;
				case 2:
					rel.setToMany(Boolean.TRUE.equals(aValue));
					break;
				case 3:
					if (aValue instanceof EODeleteRule) {
						rel.setDeleteRule((EODeleteRule) aValue);
					}
					break;
			}
			fireTableCellUpdated(rowIndex, columnIndex);
			fireModified();
		}
	}

	private class JoinsTableModel extends AbstractTableModel {
		@Override
		public int getRowCount() {
			return selectedRelationship != null ? selectedRelationship.getJoins().size() : 0;
		}

		@Override
		public int getColumnCount() {
			return JOIN_COLUMNS.length;
		}

		@Override
		public String getColumnName(int column) {
			return JOIN_COLUMNS[column];
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return true;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (selectedRelationship == null || rowIndex >= selectedRelationship.getJoins().size()) return null;
			EOJoin join = selectedRelationship.getJoins().get(rowIndex);
			return columnIndex == 0 ? join.getSourceAttributeName() : join.getDestinationAttributeName();
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (selectedRelationship == null || rowIndex >= selectedRelationship.getJoins().size()) return;
			EOJoin join = selectedRelationship.getJoins().get(rowIndex);
			String strVal = aValue != null ? aValue.toString() : null;
			if (columnIndex == 0) {
				join.setSourceAttributeName(strVal);
				if (currentEntity != null) {
					join.setSourceAttribute(currentEntity.getAttribute(strVal));
				}
			} else {
				join.setDestinationAttributeName(strVal);
				if (selectedRelationship.getDestination() != null) {
					join.setDestinationAttribute(selectedRelationship.getDestination().getAttribute(strVal));
				}
			}
			fireTableCellUpdated(rowIndex, columnIndex);
			relTableModel.fireTableDataChanged();
			fireModified();
		}
	}
}

