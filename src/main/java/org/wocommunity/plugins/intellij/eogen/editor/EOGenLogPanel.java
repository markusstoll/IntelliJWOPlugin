package org.wocommunity.plugins.intellij.eogen.editor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class EOGenLogPanel extends JBPanel<EOGenLogPanel> {

	private final JBTextArea textArea;
	private final Runnable rerunAction;

	public EOGenLogPanel(Runnable rerunAction) {
		super(new BorderLayout());
		this.rerunAction = rerunAction;

		// Toolbar
		JBPanel<?> topBar = new JBPanel<>(new BorderLayout());
		topBar.setBorder(JBUI.Borders.compound(
				JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0),
				JBUI.Borders.empty(4, 8)
		));

		JBLabel label = new JBLabel("Execution Log & Output", AllIcons.Debugger.Console, SwingConstants.LEFT);
		label.setFont(label.getFont().deriveFont(Font.BOLD));
		topBar.add(label, BorderLayout.WEST);

		DefaultActionGroup group = new DefaultActionGroup();
		group.add(new AnAction("Clear Log", "Clear log text", AllIcons.Actions.GC) {
			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				clear();
			}
		});

		if (rerunAction != null) {
			group.add(new AnAction("Run EOGenerate", "Run EOGenerator again", AllIcons.Actions.Execute) {
				@Override
				public void actionPerformed(@NotNull AnActionEvent e) {
					rerunAction.run();
				}
			});
		}

		ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("EOGenLogToolbar", group, true);
		toolbar.setTargetComponent(this);
		topBar.add(toolbar.getComponent(), BorderLayout.EAST);

		add(topBar, BorderLayout.NORTH);

		// Console area
		textArea = new JBTextArea();
		textArea.setEditable(false);
		textArea.setFont(EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.CONSOLE_PLAIN));
		textArea.setMargin(JBUI.insets(8));

		JBScrollPane scrollPane = new JBScrollPane(textArea);
		scrollPane.setBorder(JBUI.Borders.empty());
		add(scrollPane, BorderLayout.CENTER);
	}

	public void appendLine(String line) {
		ApplicationManager.getApplication().invokeLater(() -> {
			textArea.append(line + "\n");
			textArea.setCaretPosition(textArea.getDocument().getLength());
		});
	}

	public void setText(String text) {
		ApplicationManager.getApplication().invokeLater(() -> {
			textArea.setText(text);
			textArea.setCaretPosition(0);
		});
	}

	public void clear() {
		ApplicationManager.getApplication().invokeLater(() -> textArea.setText(""));
	}

	public String getText() {
		return textArea.getText();
	}
}
