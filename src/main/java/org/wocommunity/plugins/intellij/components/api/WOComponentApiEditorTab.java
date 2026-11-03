package org.wocommunity.plugins.intellij.components.api;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBTabbedPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Tab container for the Component API, offering:
 * - "API" (visual UI editor matching Eclipse WOLips)
 * - "Source" (raw XML text editor)
 * Synchronized automatically via the underlying VirtualFile / Document.
 */
public class WOComponentApiEditorTab extends JPanel {

    private final Project project;
    private final VirtualFile apiFile;
    private final WOComponentApiPanel visualPanel;
    private final FileEditor textEditor;
    private final JBTabbedPane subTabbedPane;

    public WOComponentApiEditorTab(@NotNull Project project,
                                   @NotNull VirtualFile apiFile,
                                   @NotNull Map<JComponent, FileEditor> embeddedEditorsByComponent) {
        super(new BorderLayout());
        this.project = project;
        this.apiFile = apiFile;

        this.subTabbedPane = new JBTabbedPane();
        this.subTabbedPane.setTabPlacement(JBTabbedPane.BOTTOM);

        this.visualPanel = new WOComponentApiPanel(project, apiFile);
        this.textEditor = TextEditorProvider.getInstance().createEditor(project, apiFile);
        embeddedEditorsByComponent.put(textEditor.getComponent(), textEditor);

        subTabbedPane.addTab("API", visualPanel);
        subTabbedPane.addTab("Source", textEditor.getComponent());
        if (subTabbedPane.getTabCount() > 1) {
            subTabbedPane.setSelectedIndex(1);
        }
        subTabbedPane.setSelectedIndex(0);

        subTabbedPane.addChangeListener(e -> {
            int selected = subTabbedPane.getSelectedIndex();
            if (selected == 0) {
                // Switched to visual API tab -> refresh from document in case XML was edited in Source tab
                visualPanel.reloadFromDocument();
            }
        });

        add(subTabbedPane, BorderLayout.CENTER);
    }

    public FileEditor getTextEditor() {
        return textEditor;
    }

    public WOComponentApiPanel getVisualPanel() {
        return visualPanel;
    }

    public void selectApiTab() {
        SwingUtilities.invokeLater(() -> {
            if (subTabbedPane.getTabCount() > 1) {
                subTabbedPane.setSelectedIndex(1);
            }
            if (subTabbedPane.getTabCount() > 0) {
                subTabbedPane.setSelectedIndex(0);
            }
            if (visualPanel != null) {
                visualPanel.ensureSelection();
                visualPanel.revalidate();
                visualPanel.repaint();
            }
            subTabbedPane.revalidate();
            subTabbedPane.repaint();
        });
    }

    public void dispose() {
        if (textEditor != null) {
            textEditor.dispose();
        }
    }
}

