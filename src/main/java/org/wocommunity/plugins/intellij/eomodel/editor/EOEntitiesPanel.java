package org.wocommunity.plugins.intellij.eomodel.editor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.eomodel.core.model.EOEntity;
import org.wocommunity.plugins.intellij.eomodel.core.model.EOModel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class EOEntitiesPanel extends JBPanel<EOEntitiesPanel> {

	private final EOModel eoModel;
	private final Consumer<EOEntity> onEntitySelected;
	private final Runnable onModified;

	private final DefaultListModel<EOEntity> listModel = new DefaultListModel<>();
	private final JBList<EOEntity> entitiesList = new JBList<>(listModel);
	private final SearchTextField searchField = new SearchTextField();
	private String currentFilter = "";

	public EOEntitiesPanel(EOModel eoModel, Consumer<EOEntity> onEntitySelected, Runnable onModified) {
		super(new BorderLayout());
		this.eoModel = eoModel;
		this.onEntitySelected = onEntitySelected;
		this.onModified = onModified;
		setBorder(JBUI.Borders.customLine(JBColor.border(), 0, 0, 0, 1));

		initUi();
		refreshList();
	}

	private void initUi() {
		// Toolbar
		DefaultActionGroup actionGroup = new DefaultActionGroup();
		actionGroup.add(new AnAction("Add Entity", "Create new entity", AllIcons.General.Add) {
			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				addEntity();
			}

			@Override
			public @NotNull ActionUpdateThread getActionUpdateThread() {
				return ActionUpdateThread.BGT;
			}
		});

		actionGroup.add(new AnAction("Remove Entity", "Remove selected entity", AllIcons.General.Remove) {
			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				removeSelectedEntity();
			}

			@Override
			public @NotNull ActionUpdateThread getActionUpdateThread() {
				return ActionUpdateThread.BGT;
			}

			@Override
			public void update(@NotNull AnActionEvent e) {
				e.getPresentation().setEnabled(entitiesList.getSelectedValue() != null);
			}
		});

		ActionToolbarImpl toolbar = new ActionToolbarImpl("EOEntitiesToolbar", actionGroup, true);
		toolbar.setTargetComponent(entitiesList);

		JBPanel<?> topPanel = new JBPanel<>(new BorderLayout());
		topPanel.add(toolbar.getComponent(), BorderLayout.NORTH);

		// Search Field
		searchField.addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				applyFilter();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				applyFilter();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				applyFilter();
			}
		});
		topPanel.add(searchField, BorderLayout.SOUTH);
		add(topPanel, BorderLayout.NORTH);

		// List
		entitiesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		entitiesList.setCellRenderer(new ColoredListCellRenderer<>() {
			@Override
			protected void customizeCellRenderer(@NotNull JList<? extends EOEntity> list, EOEntity value, int index, boolean selected, boolean hasFocus) {
				if (value != null) {
					setIcon(AllIcons.Nodes.DataTables);
					append(value.getName() != null ? value.getName() : "(unnamed)", SimpleTextAttributes.REGULAR_ATTRIBUTES);
					if (value.getExternalName() != null && !value.getExternalName().isEmpty()) {
						append("  [" + value.getExternalName() + "]", SimpleTextAttributes.GRAYED_ATTRIBUTES);
					}
				}
			}
		});

		entitiesList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				EOEntity selected = entitiesList.getSelectedValue();
				if (onEntitySelected != null) {
					onEntitySelected.accept(selected);
				}
			}
		});

		add(new JBScrollPane(entitiesList), BorderLayout.CENTER);
	}

	public void refreshList() {
		EOEntity previouslySelected = entitiesList.getSelectedValue();
		listModel.clear();
		for (EOEntity ent : eoModel.getEntities()) {
			if (matchesFilter(ent)) {
				listModel.addElement(ent);
			}
		}

		if (previouslySelected != null && listModel.contains(previouslySelected)) {
			entitiesList.setSelectedValue(previouslySelected, true);
		} else if (!listModel.isEmpty()) {
			entitiesList.setSelectedIndex(0);
		} else if (onEntitySelected != null) {
			onEntitySelected.accept(null);
		}
	}

	private void applyFilter() {
		currentFilter = searchField.getText().trim().toLowerCase(Locale.ROOT);
		refreshList();
	}

	private boolean matchesFilter(EOEntity entity) {
		if (currentFilter.isEmpty()) return true;
		if (entity.getName() != null && entity.getName().toLowerCase(Locale.ROOT).contains(currentFilter)) {
			return true;
		}
		if (entity.getExternalName() != null && entity.getExternalName().toLowerCase(Locale.ROOT).contains(currentFilter)) {
			return true;
		}
		return false;
	}

	public void selectEntity(EOEntity entity) {
		if (entity != null && listModel.contains(entity)) {
			entitiesList.setSelectedValue(entity, true);
		}
	}

	public EOEntity getSelectedEntity() {
		return entitiesList.getSelectedValue();
	}

	private void addEntity() {
		String baseName = "NewEntity";
		String entName = baseName;
		int count = 1;
		while (eoModel.getEntity(entName) != null) {
			entName = baseName + count++;
		}

		EOEntity entity = eoModel.createEntity(entName);
		refreshList();
		entitiesList.setSelectedValue(entity, true);
		fireModified();
	}

	private void removeSelectedEntity() {
		EOEntity selected = entitiesList.getSelectedValue();
		if (selected != null) {
			eoModel.removeEntity(selected);
			refreshList();
			fireModified();
		}
	}

	private void fireModified() {
		eoModel.setDirty(true);
		if (onModified != null) {
			onModified.run();
		}
	}
}
