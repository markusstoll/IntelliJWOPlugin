package org.wocommunity.plugins.intellij.eomodel.editor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.eomodel.core.model.EOEntity;
import org.wocommunity.plugins.intellij.eomodel.core.model.EOModel;
import org.wocommunity.plugins.intellij.eomodel.core.plist.WOLPropertyListSerialization;

import javax.swing.*;
import java.awt.*;

public class EOModelRawSourcePanel extends JBPanel<EOModelRawSourcePanel> {

	private final EOModel eoModel;
	private ComboBox<String> fileSelector;
	private JBTextArea sourceArea;
	private boolean updating = false;

	public EOModelRawSourcePanel(EOModel eoModel) {
		super(new BorderLayout());
		this.eoModel = eoModel;

		initUi();
		refreshFileList();
	}

	private void initUi() {
		JBPanel<?> topPanel = new JBPanel<>(new BorderLayout());
		topPanel.setBorder(JBUI.Borders.empty(6));

		JBPanel<?> selectorPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 6, 0));
		selectorPanel.add(new JBLabel("Source Plist:"));
		fileSelector = new ComboBox<>();
		fileSelector.addActionListener(e -> {
			if (!updating) {
				updateSourceView();
			}
		});
		selectorPanel.add(fileSelector);
		topPanel.add(selectorPanel, BorderLayout.WEST);

		DefaultActionGroup actionGroup = new DefaultActionGroup();
		actionGroup.add(new AnAction("Refresh Source", "Refresh source from memory", AllIcons.Actions.Refresh) {
			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				updateSourceView();
			}

			@Override
			public @NotNull ActionUpdateThread getActionUpdateThread() {
				return ActionUpdateThread.BGT;
			}
		});

		ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("EOSourceToolbar", actionGroup, true);
		toolbar.setTargetComponent(this);
		topPanel.add(toolbar.getComponent(), BorderLayout.EAST);

		add(topPanel, BorderLayout.NORTH);

		sourceArea = new JBTextArea();
		sourceArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		sourceArea.setEditable(false);
		sourceArea.setMargin(JBUI.insets(8));

		add(new JBScrollPane(sourceArea), BorderLayout.CENTER);
	}

	public void refreshFileList() {
		updating = true;
		try {
			String current = (String) fileSelector.getSelectedItem();
			fileSelector.removeAllItems();
			fileSelector.addItem("index.eomodeld");
			for (EOEntity entity : eoModel.getEntities()) {
				fileSelector.addItem(entity.getName() + ".plist");
			}
			if (current != null) {
				fileSelector.setSelectedItem(current);
			}
		} finally {
			updating = false;
		}
		updateSourceView();
	}

	public void updateSourceView() {
		String selected = (String) fileSelector.getSelectedItem();
		if (selected == null) {
			sourceArea.setText("");
			return;
		}

		try {
			if ("index.eomodeld".equals(selected)) {
				String str = WOLPropertyListSerialization.stringFromPropertyList(eoModel.toIndexMap().getBackingMap());
				sourceArea.setText(str);
			} else if (selected.endsWith(".plist")) {
				String entityName = selected.substring(0, selected.length() - ".plist".length());
				EOEntity entity = eoModel.getEntity(entityName);
				if (entity != null) {
					String str = WOLPropertyListSerialization.stringFromPropertyList(entity.toMap().getBackingMap());
					sourceArea.setText(str);
				} else {
					sourceArea.setText("// Entity not found: " + entityName);
				}
			}
			sourceArea.setCaretPosition(0);
		} catch (Exception ex) {
			sourceArea.setText("// Error generating plist: " + ex.getMessage());
		}
	}
}

