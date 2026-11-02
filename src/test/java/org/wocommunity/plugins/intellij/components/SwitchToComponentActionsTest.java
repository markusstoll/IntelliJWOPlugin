package org.wocommunity.plugins.intellij.components;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.beans.PropertyChangeListener;

import static org.junit.jupiter.api.Assertions.*;

public class SwitchToComponentActionsTest {

    private LightVirtualFile htmlFile;
    private LightVirtualFile javaFile;

    private static class DummyFileEditor extends UserDataHolderBase implements FileEditor {
        private final VirtualFile file;

        DummyFileEditor(VirtualFile file) {
            this.file = file;
        }

        @Override
        public @NotNull JComponent getComponent() { return new JPanel(); }

        @Override
        public @Nullable JComponent getPreferredFocusedComponent() { return null; }

        @Override
        public @NotNull String getName() { return "Dummy"; }

        @Override
        public void setState(@NotNull FileEditorState state) {}

        @Override
        public boolean isModified() { return false; }

        @Override
        public boolean isValid() { return true; }

        @Override
        public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {}

        @Override
        public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {}

        @Override
        public @Nullable FileEditorLocation getCurrentLocation() { return null; }

        @Override
        public void dispose() {}

        @Override
        public @Nullable VirtualFile getFile() { return file; }
    }

    @BeforeEach
    public void setUp() {
        htmlFile = new LightVirtualFile("Main.html");
        javaFile = new LightVirtualFile("Main.java");
    }

    @Test
    public void testSwitchToSrcActionResolveWithDirectVirtualFile() {
        VirtualFile resolved = SwitchToSrcAction.resolveComponentFile(htmlFile, null, null, null);
        assertEquals(htmlFile, resolved);
    }

    @Test
    public void testSwitchToSrcActionResolveWithVirtualFileArrayFallback() {
        VirtualFile resolved = SwitchToSrcAction.resolveComponentFile(null, new VirtualFile[]{htmlFile}, null, null);
        assertEquals(htmlFile, resolved);
    }

    @Test
    public void testSwitchToSrcActionResolveWithFileEditorFallback() {
        DummyFileEditor editor = new DummyFileEditor(htmlFile);
        VirtualFile resolved = SwitchToSrcAction.resolveComponentFile(null, null, editor, null);
        assertEquals(htmlFile, resolved);
    }

    @Test
    public void testSwitchToSrcActionResolveWithLastActiveFileEditorFallback() {
        DummyFileEditor editor = new DummyFileEditor(htmlFile);
        VirtualFile resolved = SwitchToSrcAction.resolveComponentFile(null, null, null, editor);
        assertEquals(htmlFile, resolved);
    }

    @Test
    public void testSwitchToWOComponentActionResolveWithDirectVirtualFile() {
        VirtualFile resolved = SwitchToWOComponentAction.resolveSourceFile(javaFile, null, null, null);
        assertEquals(javaFile, resolved);
    }

    @Test
    public void testSwitchToWOComponentActionResolveWithVirtualFileArrayFallback() {
        VirtualFile resolved = SwitchToWOComponentAction.resolveSourceFile(null, new VirtualFile[]{javaFile}, null, null);
        assertEquals(javaFile, resolved);
    }

    @Test
    public void testSwitchToWOComponentActionResolveWithFileEditorFallback() {
        DummyFileEditor editor = new DummyFileEditor(javaFile);
        VirtualFile resolved = SwitchToWOComponentAction.resolveSourceFile(null, null, editor, null);
        assertEquals(javaFile, resolved);
    }
}
