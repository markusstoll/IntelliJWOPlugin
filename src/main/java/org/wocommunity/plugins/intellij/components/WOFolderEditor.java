package org.wocommunity.plugins.intellij.components;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.runconfig.WORunConfigType;

import javax.swing.*;
        import java.awt.*;
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

    public WOFolderEditor(@NotNull Project project, @NotNull VirtualFile folder) {
        this.project = project;
        this.folder = folder;
        this.changes = new HashMap<>();

        JTabbedPane tabbedPane = new JTabbedPane();

        // Tab 1: HTML Editor
        VirtualFile htmlFile = folder.findChild(folder.getName().replace(".wo", ".html"));
        if (htmlFile != null && !htmlFile.isDirectory()) {
            JComponent htmlEditor = createIntellijEditor(htmlFile);
            tabbedPane.addTab("Component", htmlEditor);
        }

        // Tab 2: Plain Text Editor
        VirtualFile wooFile = folder.findChild(folder.getName().replace(".wo", ".woo"));
        if (wooFile != null && !wooFile.isDirectory()) {
            JComponent textEditor = createIntellijEditor(wooFile);
            tabbedPane.addTab("DisplayGroup", textEditor);
        }

        this.component = JBUI.Panels.simplePanel(tabbedPane);
    }

    private JComponent createIntellijEditor(@NotNull VirtualFile file) {
        FileEditor htmlEditor = TextEditorProvider.getInstance().createEditor(project, file);
        return htmlEditor.getComponent(); // Get the Swing component of the HTML editor
    }

    private String loadFileContents(@NotNull VirtualFile file) {
        try {
            return new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "Error loading file: " + e.getMessage();
        }
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
        return FILE_KEY.get(this);
    }
}
