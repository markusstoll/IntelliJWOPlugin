package org.wocommunity.plugins.intellij.components;

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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class WOFolderEditor implements FileEditor {

    private final VirtualFile folder;
    private final JComponent component;
    private final Map<String, String> changes; // Centralized change tracker
    private final Project project;

    private final Map<Key<?>, Object> userData = new HashMap<>();

    public WOFolderEditor(@NotNull Project project, @NotNull VirtualFile folder) throws IOException {
        this.project = project;
        this.folder = folder;
        this.changes = new HashMap<>();

        JBTabbedPane tabbedPane = new JBTabbedPane();

        String componentName = folder.getName().replace(".wo", "");

                // Tab 1: HTML Editor
        VirtualFile htmlFile = folder.findChild(componentName + ".html");
        VirtualFile wodFile = folder.findChild(componentName + ".wod");

        if (!htmlFile.isDirectory()) {
            JComponent htmlEditor = createIntellijEditor(htmlFile);
            JComponent wodFileEditor = createIntellijEditor(wodFile);

            // Create a splitter with 80:20 ratio
            OnePixelSplitter splitter = new OnePixelSplitter(true, 0.8f);
            splitter.setFirstComponent(htmlEditor);
            splitter.setSecondComponent(wodFileEditor);

            tabbedPane.addTab("Component", splitter);
        }

        VirtualFile apiFile = folder.getParent().findChild(componentName + ".api");
        if(apiFile == null)
        {
            //TODO
//            apiFile = folder.getParent().createChildData(null, componentName + ".api");
        } else if (!apiFile.isDirectory()) {
            JComponent apiFileEditor = createIntellijEditor(apiFile);
            tabbedPane.addTab("API", apiFileEditor);
        }

        // Tab 2: Plain Text Editor
        VirtualFile wooFile = folder.findChild(componentName + ".woo");
        if(wooFile == null)
        {
           // TODO wooFile = folder.getParent().createChildData(null, componentName + ".woo");
        } else if (!wooFile.isDirectory()) {
            JComponent textEditor = createIntellijEditor(wooFile);
            tabbedPane.addTab("DisplayGroup", textEditor);
        }

        tabbedPane.setTabPlacement(JBTabbedPane.BOTTOM);
        tabbedPane.setSelectedIndex(0);

        this.component = JBUI.Panels.simplePanel(tabbedPane);
    }

    private JComponent createIntellijEditor(@NotNull VirtualFile file) {
        FileEditor htmlEditor = TextEditorProvider.getInstance().createEditor(project, file);
        return htmlEditor.getComponent(); // Get the Swing component of the HTML editor
    }

    @Override
    public @NotNull JComponent getComponent() {
        return component;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return component;
    }

    @Override
    public @NotNull String getName() {
        return "WO Folder Editor";
    }

    @Override
    public void dispose() {
        // Dispose of resources if needed
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
}
