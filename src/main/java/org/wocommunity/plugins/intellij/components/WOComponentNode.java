package org.wocommunity.plugins.intellij.components;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.tools.WOIcons;

import java.util.Collection;
import java.util.Collections;

public class WOComponentNode extends ProjectViewNode<PsiDirectory> {

    public WOComponentNode(Project project, PsiDirectory value, ViewSettings viewSettings) {
        super(project, value, viewSettings);
    }

    @Override
    public void update(@NotNull PresentationData presentation) {
        PsiDirectory file = getValue();
        if (file != null) {
            // Set the name and custom icon for .wo folders
            String componentName = file.getName().replace(".wo", "");
            presentation.setPresentableText(componentName + " WO");
            presentation.setIcon(WOIcons.WOCOMPONENT_ICON); // Use your custom icon here
        }
    }

    @Override
    public @NotNull Collection<AbstractTreeNode<?>> getChildren() {
        return Collections.emptyList(); // Hide the folder's contents
    }

    @Override
    public boolean contains(@NotNull VirtualFile file) {
        PsiDirectory directory = getValue();
        if (directory == null) {
            return false;
        }
        VirtualFile woDirectory = directory.getVirtualFile();

        // Das .wo Verzeichnis selbst
        if (woDirectory.equals(file)) {
            return true;
        }

        // Prüfen ob file ein Kind des .wo Verzeichnisses ist
        VirtualFile parent = file.getParent();
        while (parent != null) {
            if (parent.equals(woDirectory)) {
                return true;
            }
            parent = parent.getParent();
        }

        return false;
    }

    @Override
    public boolean canNavigate() {
        return true; // Enable navigation if needed
    }

    @Override
    public boolean canNavigateToSource() {
        return true; // Enable navigation if needed
    }

    @Override
    public void navigate(boolean requestFocus) {
        FileEditorManager.getInstance(myProject).openFile(getValue().getVirtualFile(), true);
    }
}