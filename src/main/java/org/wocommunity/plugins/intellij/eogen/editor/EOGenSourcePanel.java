package org.wocommunity.plugins.intellij.eogen.editor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.eogen.core.EOGeneratorModel;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;

public class EOGenSourcePanel extends JBPanel<EOGenSourcePanel> {

	private final EOGeneratorModel model;
	private final File workingDir;
	private final Runnable onModified;
	private final JBTextArea textArea = new JBTextArea();
	private boolean updating = false;

	public EOGenSourcePanel(EOGeneratorModel model, File workingDir, Runnable onModified) {
		super(new BorderLayout());
		this.model = model;
		this.workingDir = workingDir;
		this.onModified = onModified;

		initUi();
	}

	private void initUi() {
		DefaultActionGroup actionGroup = new DefaultActionGroup();
		actionGroup.add(new AnAction("Refresh from Form", "Update raw text from form values", AllIcons.Actions.Refresh) {
			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				updateFromModel();
			}

			@Override
			public @NotNull ActionUpdateThread getActionUpdateThread() {
				return ActionUpdateThread.BGT;
			}
		});

		ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("EOGenSourceToolbar", actionGroup, true);
		toolbar.setTargetComponent(this);
		add(toolbar.getComponent(), BorderLayout.NORTH);

		textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		textArea.setMargin(JBUI.insets(8));
		textArea.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				onTextModified();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				onTextModified();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				onTextModified();
			}
		});

		add(new JBScrollPane(textArea), BorderLayout.CENTER);
	}

	public void updateFromModel() {
		updating = true;
		try {
			String text = model.writeToString(workingDir);
			textArea.setText(text);
			textArea.setCaretPosition(0);
		} finally {
			updating = false;
		}
	}

	private void onTextModified() {
		if (!updating) {
			try {
				model.readFromString(textArea.getText(), workingDir);
				if (onModified != null) {
					onModified.run();
				}
			} catch (Exception ignored) {
			}
		}
	}
}

