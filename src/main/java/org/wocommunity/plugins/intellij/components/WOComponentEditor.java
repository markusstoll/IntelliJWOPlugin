package org.wocommunity.plugins.intellij.components;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.UiDataProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.components.api.WOComponentApiEditorTab;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.pom.Navigatable;
import java.util.Map;

public class WOComponentEditor implements com.intellij.openapi.fileEditor.TextEditor, UiDataProvider {

    private final VirtualFile folder;
    private final VirtualFile requestedFile;
    private final JComponent component;
    private final Map<String, String> changes; // Centralized change tracker
    private final Project project;
    private final Map<JComponent, FileEditor> embeddedEditorsByComponent;
    private final Map<VirtualFile, FileEditor> embeddedEditorsByFile = new HashMap<>();
    private final Map<VirtualFile, Integer> tabIndexByFile = new HashMap<>();
    private FileEditor htmlEditor;
    private FileEditor wodFileEditor;
    private OnePixelSplitter componentSplitter;
    private JBTabbedPane tabbedPane;
    private boolean apiTabInitialized = false;
    private WOComponentApiEditorTab apiEditorTab;
    private JPanel apiContainer;

    private final Map<Key<?>, Object> userData = new HashMap<>();

    public WOComponentEditor(@NotNull Project project, @NotNull VirtualFile folder) throws IOException {
        this(project, folder, folder);
    }

    public WOComponentEditor(@NotNull Project project,
                             @NotNull VirtualFile folder,
                             @NotNull VirtualFile editorFile) throws IOException {
        this.project = project;
        this.folder = folder;
        this.requestedFile = editorFile;
        this.changes = new HashMap<>();
        this.embeddedEditorsByComponent = new HashMap<>();

        this.tabbedPane = new JBTabbedPane();

        String componentName = folder.getName().replace(".wo", "");

        // Tab 1: HTML & WOD Editor
        VirtualFile htmlFile = folder.findChild(componentName + ".html");
        VirtualFile wodFile = folder.findChild(componentName + ".wod");

        if (htmlFile != null && !htmlFile.isDirectory()) {
            this.htmlEditor = createIntellijFileEditor(htmlFile);
            this.wodFileEditor = (wodFile != null && !wodFile.isDirectory())
                    ? createIntellijFileEditor(wodFile)
                    : null;

            embeddedEditorsByFile.put(htmlFile, htmlEditor);
            tabIndexByFile.put(htmlFile, 0);
            if (wodFile != null && wodFileEditor != null) {
                embeddedEditorsByFile.put(wodFile, wodFileEditor);
                tabIndexByFile.put(wodFile, 0);
            }

            // Create a splitter with 80:20 ratio
            OnePixelSplitter splitter = new OnePixelSplitter(true, 0.8f);
            this.componentSplitter = splitter;
            splitter.setFirstComponent(htmlEditor.getComponent());
            splitter.setSecondComponent(wodFileEditor != null ? wodFileEditor.getComponent() : JBUI.Panels.simplePanel());

            tabbedPane.addTab("Component", splitter);
        }

        VirtualFile parentFolder = folder.getParent();
        VirtualFile apiFile = (parentFolder != null) ? parentFolder.findChild(componentName + ".api") : null;
        if (apiFile != null && !apiFile.isDirectory()) {
            this.apiEditorTab = new WOComponentApiEditorTab(project, apiFile, embeddedEditorsByComponent);
            tabbedPane.addTab("API", this.apiEditorTab);
            apiTabInitialized = true;
            tabIndexByFile.put(apiFile, tabbedPane.getTabCount() - 1);
        } else {
            this.apiContainer = new JPanel(new BorderLayout());
            tabbedPane.addTab("API", this.apiContainer);
        }

        tabbedPane.addChangeListener(e -> {
            int selected = tabbedPane.getSelectedIndex();
            if (selected >= 0 && "API".equals(tabbedPane.getTitleAt(selected))) {
                if (!apiTabInitialized && apiContainer != null) {
                    ensureApiFileAndEditor(apiContainer, componentName);
                }
                if (apiEditorTab != null) {
                    apiEditorTab.selectApiTab();
                }
            }
        });

        // Tab 2: Plain Text Editor (.woo)
        VirtualFile wooFile = folder.findChild(componentName + ".woo");
        if (wooFile != null && !wooFile.isDirectory()) {
            FileEditor textEditor = createIntellijFileEditor(wooFile);
            embeddedEditorsByFile.put(wooFile, textEditor);
            tabbedPane.addTab("DisplayGroup", textEditor.getComponent());
            tabIndexByFile.put(wooFile, tabbedPane.getTabCount() - 1);
        }

        tabbedPane.setTabPlacement(JBTabbedPane.BOTTOM);
        tabbedPane.setSelectedIndex(0);

        if (editorFile != null && !editorFile.equals(folder)) {
            Integer targetTabIndex = tabIndexByFile.get(editorFile);
            if (targetTabIndex != null && targetTabIndex < tabbedPane.getTabCount()) {
                tabbedPane.setSelectedIndex(targetTabIndex);
            }
            if (editorFile.getName().endsWith(".wod") && wodFileEditor instanceof com.intellij.openapi.fileEditor.TextEditor te) {
                IdeFocusManager.getInstance(project).requestFocus(te.getEditor().getContentComponent(), true);
            }
        }

        if (tabbedPane.getTabCount() > 0 && "API".equals(tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()))) {
            if (!apiTabInitialized && apiContainer != null) {
                ensureApiFileAndEditor(apiContainer, componentName);
            }
            if (apiEditorTab != null) {
                apiEditorTab.selectApiTab();
            }
        }

        this.component = new WOComponentPanel(project, folder, tabbedPane, embeddedEditorsByComponent);

        ApplicationManager.getApplication().invokeLater(() -> {
            if (project != null && !project.isDisposed()) {
                JComponent prefFocus = getPreferredFocusedComponent();
                if (prefFocus != null) {
                    IdeFocusManager.getInstance(project).requestFocus(prefFocus, true);
                }
            }
        });
    }

    private void ensureApiFileAndEditor(JPanel container, String componentName) {
        VirtualFile parentFolder = folder.getParent();
        if (parentFolder == null) {
            return;
        }

        VirtualFile apiFile = parentFolder.findChild(componentName + ".api");
        if (apiFile == null || !apiFile.isValid()) {
            final VirtualFile[] created = new VirtualFile[1];
            WriteCommandAction.runWriteCommandAction(project, () -> {
                try {
                    created[0] = parentFolder.createChildData(this, componentName + ".api");
                    String initialContent = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                            "<wodefinitions>\n" +
                            "  <wo class=\"" + componentName + "\">\n" +
                            "  </wo>\n" +
                            "</wodefinitions>\n";
                    created[0].setBinaryContent(initialContent.getBytes(StandardCharsets.UTF_8));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
            apiFile = created[0];
        }

        if (apiFile != null && apiFile.isValid()) {
            this.apiEditorTab = new WOComponentApiEditorTab(project, apiFile, embeddedEditorsByComponent);
            container.removeAll();
            container.add(this.apiEditorTab, BorderLayout.CENTER);
            container.revalidate();
            container.repaint();
            apiTabInitialized = true;
            tabIndexByFile.put(apiFile, 1);
        }
    }

    private FileEditor createIntellijFileEditor(VirtualFile file) {
        FileEditor editor = TextEditorProvider.getInstance().createEditor(project, file);
        JComponent comp = editor.getComponent();
        this.embeddedEditorsByComponent.put(comp, editor);
        return editor;
    }

    public FileEditor getHtmlEditor() {
        return htmlEditor;
    }

    public FileEditor getWodFileEditor() {
        return wodFileEditor;
    }

    public OnePixelSplitter getComponentSplitter() {
        return componentSplitter;
    }

    public JBTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public WOComponentApiEditorTab getApiEditorTab() {
        return apiEditorTab;
    }

    @Override
    public com.intellij.openapi.editor.@NotNull Editor getEditor() {
        if (requestedFile != null) {
            FileEditor editor = embeddedEditorsByFile.get(requestedFile);
            if (editor instanceof com.intellij.openapi.fileEditor.TextEditor textEditor) {
                return textEditor.getEditor();
            }
        }
        if (htmlEditor instanceof com.intellij.openapi.fileEditor.TextEditor te) {
            return te.getEditor();
        }
        if (wodFileEditor instanceof com.intellij.openapi.fileEditor.TextEditor te) {
            return te.getEditor();
        }
        for (FileEditor fileEditor : embeddedEditorsByFile.values()) {
            if (fileEditor instanceof com.intellij.openapi.fileEditor.TextEditor textEditor) {
                return textEditor.getEditor();
            }
        }
        return com.intellij.openapi.editor.EditorFactory.getInstance().createViewer(
                com.intellij.openapi.editor.EditorFactory.getInstance().createDocument("")
        );
    }

    public void setComponentSplitterProportion(float proportion) {
        if (componentSplitter != null) {
            componentSplitter.setProportion(proportion);
        }
    }

    public VirtualFile getFolder() {
        return folder;
    }

    public boolean scrollToHtmlOffset(int offset) {
        if (tabbedPane != null && tabbedPane.getTabCount() > 0) {
            tabbedPane.setSelectedIndex(0);
        }
        return scrollToOffsetInEditor(htmlEditor, offset, offset);
    }

    public boolean scrollToWodOffset(int offset) {
        return scrollToWodOffset(offset, offset);
    }

    public boolean scrollToWodOffset(int startOffset, int endOffset) {
        if (tabbedPane != null && tabbedPane.getTabCount() > 0) {
            tabbedPane.setSelectedIndex(0);
        }
        return scrollToOffsetInEditor(wodFileEditor, startOffset, endOffset);
    }

    private boolean scrollToOffsetInEditor(FileEditor fileEditor, int startOffset, int endOffset) {
        if (!(fileEditor instanceof com.intellij.openapi.fileEditor.TextEditor te)) {
            return false;
        }
        Runnable scrollAction = () -> {
            if (componentSplitter != null) {
                if (fileEditor == wodFileEditor && componentSplitter.getProportion() > 0.90f) {
                    componentSplitter.setProportion(0.5f);
                } else if (fileEditor == htmlEditor && componentSplitter.getProportion() < 0.10f) {
                    componentSplitter.setProportion(0.5f);
                }
            }
            if (te.getEditor() != null) {
                com.intellij.openapi.editor.Editor ed = te.getEditor();
                int docLen = ed.getDocument().getTextLength();
                int safeStart = Math.max(0, Math.min(startOffset, docLen));
                int safeEnd = Math.max(safeStart, Math.min(endOffset, docLen));

                ed.getCaretModel().moveToOffset(safeStart);
                ed.getSelectionModel().setSelection(safeStart, safeEnd);

                ed.getScrollingModel().disableAnimation();
                com.intellij.openapi.editor.LogicalPosition logPos = ed.offsetToLogicalPosition(safeStart);
                ed.getScrollingModel().scrollTo(logPos, com.intellij.openapi.editor.ScrollType.CENTER);
                ed.getScrollingModel().scrollToCaret(com.intellij.openapi.editor.ScrollType.CENTER);
                ed.getScrollingModel().enableAnimation();

                if (ed instanceof com.intellij.openapi.editor.ex.EditorEx editorEx) {
                    javax.swing.JScrollPane sp = editorEx.getScrollPane();
                    if (sp != null && sp.getViewport() != null) {
                        java.awt.Point p = ed.visualPositionToXY(ed.offsetToVisualPosition(safeStart));
                        int vpHeight = sp.getViewport().getHeight();
                        if (vpHeight <= 0) {
                            vpHeight = sp.getHeight();
                        }
                        int targetY = Math.max(0, p.y - Math.max(vpHeight, 100) / 2);
                        sp.getViewport().setViewPosition(new java.awt.Point(0, targetY));
                        sp.repaint();
                    }
                }

                IdeFocusManager.getInstance(project).requestFocus(ed.getContentComponent(), true);
            }
        };

        if (com.intellij.openapi.application.ApplicationManager.getApplication().isDispatchThread()) {
            scrollAction.run();
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(scrollAction);
        } else {
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(scrollAction);
        }
        return true;
    }

    @Override
    public boolean canNavigateTo(@NotNull Navigatable navigatable) {
        if (navigatable instanceof OpenFileDescriptor ofd) {
            VirtualFile vf = ofd.getFile();
            return folder.equals(vf) || folder.equals(vf.getParent()) || embeddedEditorsByFile.containsKey(vf)
                    || (vf.getParent() != null && folder.getPath().equals(vf.getParent().getPath()));
        }
        return false;
    }

    @Override
    public void navigateTo(@NotNull Navigatable navigatable) {
        if (!(navigatable instanceof OpenFileDescriptor descriptor)) {
            return;
        }

        VirtualFile vf = descriptor.getFile();
        FileEditor fileEditor = embeddedEditorsByFile.get(vf);
        if (fileEditor instanceof com.intellij.openapi.fileEditor.TextEditor textEditor) {
            int tabIndex = tabIndexByFile.getOrDefault(vf, 0);
            if (tabIndex < tabbedPane.getTabCount()) {
                tabbedPane.setSelectedIndex(tabIndex);
            }
            com.intellij.openapi.editor.Editor editor = textEditor.getEditor();
            editor.getCaretModel().removeSecondaryCarets();
            if (descriptor.getLine() >= 0) {
                int line = Math.min(descriptor.getLine(), Math.max(0, editor.getDocument().getLineCount() - 1));
                int column = Math.max(0, descriptor.getColumn());
                editor.getCaretModel().moveToLogicalPosition(new com.intellij.openapi.editor.LogicalPosition(line, column));
            } else {
                int offset = Math.max(0, Math.min(descriptor.getOffset(), editor.getDocument().getTextLength()));
                editor.getCaretModel().moveToOffset(offset);
            }
            editor.getScrollingModel().scrollToCaret(com.intellij.openapi.editor.ScrollType.CENTER);
            IdeFocusManager.getInstance(project).requestFocus(editor.getContentComponent(), true);
            return;
        }

        int offset = descriptor.getOffset();
        if (vf.getName().endsWith(".wod")) {
            scrollToWodOffset(offset);
        } else if (vf.getName().endsWith(".html")) {
            scrollToHtmlOffset(offset);
        }
    }

    public void saveChanges() {
        changes.forEach((fileName, content) -> {
            VirtualFile file = folder.findChild(fileName);
            if (file != null && file.isWritable()) {
                try {
                    file.setBinaryContent(content.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        changes.clear();
    }

    @Override
    public @NotNull JComponent getComponent() {
        return component;
    }

    @Override
    public void selectNotify() {
        JComponent focusedComp = getPreferredFocusedComponent();
        if (focusedComp != null && project != null && !project.isDisposed()) {
            IdeFocusManager.getInstance(project).requestFocus(focusedComp, true);
        }
        FileEditor activeEditor = getActiveEmbeddedEditor();
        if (activeEditor != null) {
            activeEditor.selectNotify();
        }
    }

    @Override
    public void deselectNotify() {
        FileEditor activeEditor = getActiveEmbeddedEditor();
        if (activeEditor != null) {
            activeEditor.deselectNotify();
        }
    }

    public @Nullable FileEditor getActiveEmbeddedEditor() {
        int selected = tabbedPane != null ? tabbedPane.getSelectedIndex() : -1;
        if (selected == 0) {
            Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (wodFileEditor != null && focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, wodFileEditor.getComponent())) {
                return wodFileEditor;
            }
            return htmlEditor != null ? htmlEditor : wodFileEditor;
        } else if (selected >= 0 && selected < tabbedPane.getTabCount()) {
            Component tabComp = tabbedPane.getComponentAt(selected);
            return embeddedEditorsByComponent.get(tabComp);
        }
        return htmlEditor != null ? htmlEditor : wodFileEditor;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        if (requestedFile != null) {
            FileEditor ed = embeddedEditorsByFile.get(requestedFile);
            if (ed != null && ed.getPreferredFocusedComponent() != null) {
                return ed.getPreferredFocusedComponent();
            }
        }
        FileEditor activeEditor = getActiveEmbeddedEditor();
        if (activeEditor != null && activeEditor.getPreferredFocusedComponent() != null) {
            return activeEditor.getPreferredFocusedComponent();
        }
        if (htmlEditor != null && htmlEditor.getPreferredFocusedComponent() != null) {
            return htmlEditor.getPreferredFocusedComponent();
        }
        if (wodFileEditor != null && wodFileEditor.getPreferredFocusedComponent() != null) {
            return wodFileEditor.getPreferredFocusedComponent();
        }
        return component;
    }

    @Override
    public void uiDataSnapshot(@NotNull DataSink sink) {
        String componentName = folder.getName().replace(".wo", "");
        VirtualFile html = folder.findChild(componentName + ".html");
        VirtualFile targetFile = (html != null && !html.isDirectory()) ? html : folder.findChild(componentName + ".wod");
        sink.set(CommonDataKeys.VIRTUAL_FILE, targetFile != null ? targetFile : folder);
        sink.lazy(CommonDataKeys.PSI_ELEMENT, () -> {
            if (targetFile != null && !targetFile.isDirectory()) {
                return PsiManager.getInstance(project).findFile(targetFile);
            }
            return PsiManager.getInstance(project).findDirectory(folder);
        });
        sink.lazy(CommonDataKeys.NAVIGATABLE, () -> {
            if (targetFile != null && !targetFile.isDirectory()) {
                return PsiManager.getInstance(project).findFile(targetFile);
            }
            return PsiManager.getInstance(project).findDirectory(folder);
        });
        sink.lazy(CommonDataKeys.EDITOR, () -> getEditor());
        sink.set(PlatformDataKeys.FILE_EDITOR, this);
    }

    @Override
    public @NotNull String getName() {
        return "WOComponent";
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
    }

    @Override
    public boolean isModified() {
        return !changes.isEmpty();
    }

    @Override
    public boolean isValid() {
        return folder.isValid();
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public @Nullable FileEditorLocation getCurrentLocation() {
        return null;
    }

    @Override
    public void dispose() {
        for (FileEditor editor : embeddedEditorsByComponent.values()) {
            editor.dispose();
        }
        embeddedEditorsByComponent.clear();
        embeddedEditorsByFile.clear();
        tabIndexByFile.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T> T getUserData(@NotNull Key<T> key) {
        return (T) userData.get(key);
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
        if (value == null) {
            userData.remove(key);
        } else {
            userData.put(key, value);
        }
    }

    @Override
    public VirtualFile getFile() {
        return folder;
    }

    private static class WOComponentPanel extends JPanel implements UiDataProvider {
        private final Project project;
        private final VirtualFile folder;
        private final Map<JComponent, FileEditor> embeddedEditorsByComponent;

        WOComponentPanel(@NotNull Project project,
                         @NotNull VirtualFile folder,
                         @NotNull JComponent content,
                         @NotNull Map<JComponent, FileEditor> embeddedEditorsByComponent) {
            super(new BorderLayout());
            this.project = project;
            this.folder = folder;
            this.embeddedEditorsByComponent = embeddedEditorsByComponent;
            add(content, java.awt.BorderLayout.CENTER);
        }

        @Override
        public void uiDataSnapshot(@NotNull DataSink sink) {
            String componentName = folder.getName().replace(".wo", "");
            VirtualFile html = folder.findChild(componentName + ".html");
            VirtualFile targetFile = (html != null && !html.isDirectory()) ? html : folder.findChild(componentName + ".wod");
            sink.set(CommonDataKeys.VIRTUAL_FILE, targetFile != null ? targetFile : folder);
            sink.lazy(CommonDataKeys.PSI_ELEMENT, () -> {
                if (targetFile != null && !targetFile.isDirectory()) {
                    return PsiManager.getInstance(project).findFile(targetFile);
                }
                return PsiManager.getInstance(project).findDirectory(folder);
            });
            sink.lazy(CommonDataKeys.NAVIGATABLE, () -> {
                if (targetFile != null && !targetFile.isDirectory()) {
                    return PsiManager.getInstance(project).findFile(targetFile);
                }
                return PsiManager.getInstance(project).findDirectory(folder);
            });
            sink.lazy(CommonDataKeys.EDITOR, () -> {
                FileEditor focused = getFocusedEmbeddedEditor(embeddedEditorsByComponent);
                if (focused instanceof com.intellij.openapi.fileEditor.TextEditor te) {
                    return te.getEditor();
                }
                return null;
            });
            sink.lazy(PlatformDataKeys.FILE_EDITOR, () -> getFocusedEmbeddedEditor(embeddedEditorsByComponent));
        }

        private static @Nullable FileEditor getFocusedEmbeddedEditor(@NotNull Map<JComponent, FileEditor> embeddedEditorsByComponent) {
            Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (focusOwner == null) {
                return null;
            }
            Component c = focusOwner;
            while (c != null) {
                if (c instanceof JComponent jc) {
                    FileEditor ed = embeddedEditorsByComponent.get(jc);
                    if (ed != null) {
                        return ed;
                    }
                }
                c = c.getParent();
            }
            return null;
        }
    }
}
