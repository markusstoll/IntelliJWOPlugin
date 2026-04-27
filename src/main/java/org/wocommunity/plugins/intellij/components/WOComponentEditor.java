package org.wocommunity.plugins.intellij.components;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.UiDataProvider;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class WOComponentEditor implements FileEditor {

    private final VirtualFile folder;
    private final JComponent component;
    private final Map<String, String> changes; // Centralized change tracker
    private final Project project;
    private final Map<JComponent, FileEditor> embeddedEditorsByComponent;

    private final Map<Key<?>, Object> userData = new HashMap<>();

    public WOComponentEditor(@NotNull Project project, @NotNull VirtualFile folder) throws IOException {
        this.project = project;
        this.folder = folder;
        this.changes = new HashMap<>();
        this.embeddedEditorsByComponent = new HashMap<>();

        JBTabbedPane tabbedPane = new JBTabbedPane();

        String componentName = folder.getName().replace(".wo", "");

                // Tab 1: HTML Editor
        VirtualFile htmlFile = folder.findChild(componentName + ".html");
        VirtualFile wodFile = folder.findChild(componentName + ".wod");

        if (htmlFile != null && !htmlFile.isDirectory()) {
            FileEditor htmlEditor = createIntellijFileEditor(htmlFile);
            FileEditor wodFileEditor = (wodFile != null && !wodFile.isDirectory())
                    ? createIntellijFileEditor(wodFile)
                    : null;

            // Create a splitter with 80:20 ratio
            OnePixelSplitter splitter = new OnePixelSplitter(true, 0.8f);
            splitter.setFirstComponent(htmlEditor.getComponent());
            splitter.setSecondComponent(wodFileEditor != null ? wodFileEditor.getComponent() : JBUI.Panels.simplePanel());

            tabbedPane.addTab("Component", splitter);
        }

        VirtualFile apiFile = folder.getParent().findChild(componentName + ".api");
        if(apiFile == null)
        {
            //TODO
//            apiFile = folder.getParent().createChildData(null, componentName + ".api");
        } else if (!apiFile.isDirectory()) {
            FileEditor apiFileEditor = createIntellijFileEditor(apiFile);
            tabbedPane.addTab("API", apiFileEditor.getComponent());
        }

        // Tab 2: Plain Text Editor
        VirtualFile wooFile = folder.findChild(componentName + ".woo");
        if(wooFile == null)
        {
           // TODO wooFile = folder.getParent().createChildData(null, componentName + ".woo");
        } else if (!wooFile.isDirectory()) {
            FileEditor textEditor = createIntellijFileEditor(wooFile);
            tabbedPane.addTab("DisplayGroup", textEditor.getComponent());
        }

        tabbedPane.setTabPlacement(JBTabbedPane.BOTTOM);
        tabbedPane.setSelectedIndex(0);

        this.component = new WOComponentPanel(project, folder, tabbedPane, embeddedEditorsByComponent);
    }

    private FileEditor createIntellijFileEditor(@NotNull VirtualFile file) {
        FileEditor editor = TextEditorProvider.getInstance().createEditor(project, file);
        embeddedEditorsByComponent.put(editor.getComponent(), editor);
        return editor;
    }

    private @Nullable FileEditor getFocusedEmbeddedEditor() {
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

    @Override
    public @NotNull JComponent getComponent() {
        return component;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        FileEditor focused = getFocusedEmbeddedEditor();
        if (focused != null && focused.getPreferredFocusedComponent() != null) {
            return focused.getPreferredFocusedComponent();
        }
        for (FileEditor ed : embeddedEditorsByComponent.values()) {
            JComponent preferred = ed.getPreferredFocusedComponent();
            if (preferred != null) {
                return preferred;
            }
        }
        return component;
    }

    @Override
    public @NotNull String getName() {
        return "WO Folder Editor";
    }

    @Override
    public void dispose() {
        for (FileEditor ed : embeddedEditorsByComponent.values()) {
            ed.dispose();
        }
        embeddedEditorsByComponent.clear();
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
        // Handle editor state changes if needed
    }

    @Override
    public boolean isModified() {
        return !changes.isEmpty(); // Return true if there are unsaved changes
    }

    @Override
    public boolean isValid() {
        return folder.isValid();
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
        // Add property change listener for external tools
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
        // Remove property change listener
    }

    @Override
    public @Nullable FileEditorLocation getCurrentLocation() {
        return null;
    }

    public void saveChanges() {
        changes.forEach((fileName, content) -> {
            VirtualFile file = folder.findChild(fileName);
            if (file != null && file.isWritable()) {
                try {
                    file.setBinaryContent(content.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    // Log error if unable to save
                    e.printStackTrace();
                }
            }
        });
        changes.clear(); // Clear the changes after saving
    }

    @Override
    public @Nullable <T> T getUserData(@NotNull Key<T> key) {
        // Retrieve data by key
        return (T) userData.get(key);
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
        if (value == null) {
            userData.remove(key); // Remove the key if value is null
        } else {
            userData.put(key, value); // Store the value
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
            sink.set(CommonDataKeys.VIRTUAL_FILE, folder);
            sink.lazy(CommonDataKeys.PSI_ELEMENT, () -> PsiManager.getInstance(project).findDirectory(folder));
            sink.lazy(CommonDataKeys.NAVIGATABLE, () -> PsiManager.getInstance(project).findDirectory(folder));
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
