package org.wocommunity.plugins.intellij.components;

import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WOFolderTreeStructureProvider implements TreeStructureProvider {

    @Override
    public @NotNull Collection<AbstractTreeNode<?>> modify(
            @NotNull AbstractTreeNode<?> parent,
            @NotNull Collection<AbstractTreeNode<?>> children,
            @NotNull ViewSettings settings) {

        List<AbstractTreeNode<?>> modified = new ArrayList<>();

        for (AbstractTreeNode<?> child : children) {
            Object value = child.getValue();

            if (value instanceof PsiDirectory) {
                PsiDirectory file = (PsiDirectory) value;

                // Check for the .wo suffix and whether it's a directory
                if (file.isDirectory() && file.getName().endsWith(".wo")) {
                    // Replace this folder node with a custom node
                    modified.add(new WOFolderNode(child.getProject(), file, settings));
                } else {
                    // Keep other nodes as they are
                    modified.add(child);
                }
            } else if (value instanceof PsiFile){
                PsiFile file = (PsiFile) value;

                if(!file.getName().endsWith(".api"))
                {
                    modified.add(child);
                }
            } else {
                modified.add(child);
            }
        }

        return modified;
    }

    @Override
    public @Nullable Object getData(
            @NotNull Collection<? extends AbstractTreeNode<?>> selected,
            @NotNull String dataId) {
        return null; // Handle additional data if needed
    }
}