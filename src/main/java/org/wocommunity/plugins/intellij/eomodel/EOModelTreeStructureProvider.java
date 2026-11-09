package org.wocommunity.plugins.intellij.eomodel;

import com.intellij.ide.projectView.SelectableTreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.eogen.EOGenNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EOModelTreeStructureProvider implements SelectableTreeStructureProvider {

	@Override
	public @NotNull Collection<AbstractTreeNode<?>> modify(
			@NotNull AbstractTreeNode<?> parent,
			@NotNull Collection<AbstractTreeNode<?>> children,
			@NotNull ViewSettings settings) {

		List<AbstractTreeNode<?>> modified = new ArrayList<>();

		for (AbstractTreeNode<?> child : children) {
			Object value = child.getValue();

			if (value instanceof PsiDirectory dir) {
				VirtualFile vf = dir.getVirtualFile();
				if (EOModelFileUtil.fileIsModel(vf)) {
					modified.add(new EOModelNode(child.getProject(), dir, settings));
				} else {
					modified.add(child);
				}
			} else if (value instanceof PsiFile psiFile) {
				VirtualFile vf = psiFile.getVirtualFile();
				if (vf != null && "eogen".equalsIgnoreCase(vf.getExtension())) {
					modified.add(new EOGenNode(child.getProject(), psiFile, settings));
				} else {
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
				if ("eogen".equalsIgnoreCase(vFile.getExtension())) {
					return psiFile;
				}
				VirtualFile p = vFile.getParent();
				while (p != null) {
					if (EOModelFileUtil.fileIsModel(p)) {
						return psiFile.getManager().findDirectory(p);
					}
					p = p.getParent();
				}
			}
		} else if (element instanceof PsiDirectory dir) {
			VirtualFile vFile = dir.getVirtualFile();
			if (EOModelFileUtil.fileIsModel(vFile)) {
				return dir;
			}
		}
		return null;
	}
}

