package org.wocommunity.plugins.intellij.components;

import com.intellij.ide.projectView.SelectableTreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WOComponentTreeStructureProvider implements SelectableTreeStructureProvider {

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
                    modified.add(new WOComponentNode(child.getProject(), file, settings));
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
    public @Nullable PsiElement getTopLevelElement(PsiElement element) {
        if (element == null) {
            return null;
        }
        PsiFile psiFile = element.getContainingFile();
        if (psiFile != null) {
            VirtualFile vFile = psiFile.getVirtualFile();
            if (vFile != null) {
                VirtualFile p = vFile.getParent();
                while (p != null) {
                    if (p.isDirectory() && p.getName().endsWith(".wo")) {
                        return psiFile.getManager().findDirectory(p);
                    }
                    p = p.getParent();
                }
                if (vFile.getName().endsWith(".api")) {
                    VirtualFile parent = vFile.getParent();
                    if (parent != null) {
                        String componentName = vFile.getName().substring(0, vFile.getName().length() - ".api".length());
                        VirtualFile woDir = parent.findChild(componentName + ".wo");
                        if (woDir != null && woDir.isDirectory()) {
                            return psiFile.getManager().findDirectory(woDir);
                        }
                    }
                }
            }
        } else if (element instanceof PsiDirectory dir) {
            VirtualFile vFile = dir.getVirtualFile();
            if (vFile != null && vFile.getName().endsWith(".wo")) {
                return dir;
            }
        }
        return null;
    }
}