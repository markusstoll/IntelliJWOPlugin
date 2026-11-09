package org.wocommunity.plugins.intellij.eomodel;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.tools.WOIcons;

import java.util.Collection;
import java.util.Collections;

public class EOModelNode extends ProjectViewNode<PsiDirectory> {

	public EOModelNode(Project project, PsiDirectory value, ViewSettings viewSettings) {
		super(project, value, viewSettings);
	}

	@Override
	public void update(@NotNull PresentationData presentation) {
		PsiDirectory dir = getValue();
		if (dir != null) {
			String modelName = dir.getName().replace(".eomodeld", "");
			presentation.setPresentableText(modelName + " EOModel");
			presentation.setIcon(WOIcons.EOMODEL_ICON);
		}
	}

	@Override
	public @NotNull Collection<AbstractTreeNode<?>> getChildren() {
		return Collections.emptyList(); // Kapselt den Ordner
	}

	@Override
	public int getWeight() {
		return 30; // Matches EOGenNode so .eomodeld and .eogen are sorted at the bottom
	}

	@Override
	public boolean contains(@NotNull VirtualFile file) {
		PsiDirectory directory = getValue();
		if (directory == null) {
			return false;
		}
		VirtualFile modelDir = directory.getVirtualFile();
		if (modelDir.equals(file)) {
			return true;
		}
		VirtualFile parent = file.getParent();
		while (parent != null) {
			if (parent.equals(modelDir)) {
				return true;
			}
			parent = parent.getParent();
		}
		return false;
	}

	@Override
	public boolean canRepresent(Object element) {
		if (super.canRepresent(element)) {
			return true;
		}
		if (element instanceof AbstractTreeNode<?> atn) {
			return canRepresent(atn.getValue());
		}
		if (element instanceof PsiElement psiElement) {
			if (getValue() != null && getValue().equals(psiElement)) {
				return true;
			}
			VirtualFile vFile = PsiUtilCore.getVirtualFile(psiElement);
			if (vFile != null && contains(vFile)) {
				return true;
			}
		}
		if (element instanceof VirtualFile vFile) {
			return contains(vFile);
		}
		if (element instanceof FileEditor fileEditor) {
			VirtualFile vFile = fileEditor.getFile();
			if (vFile != null && contains(vFile)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public @Nullable VirtualFile getVirtualFile() {
		if (getValue() != null) {
			VirtualFile modelDir = getValue().getVirtualFile();
			if (modelDir != null) {
				VirtualFile indexFile = modelDir.findChild(EOModelFileUtil.INDEX_FILE_NAME);
				if (indexFile != null && !indexFile.isDirectory()) {
					return indexFile;
				}
				return modelDir;
			}
		}
		return null;
	}

	@Override
	public boolean canNavigate() {
		return true;
	}

	@Override
	public boolean canNavigateToSource() {
		return true;
	}

	@Override
	public void navigate(boolean requestFocus) {
		VirtualFile vf = getVirtualFile();
		if (vf != null && myProject != null) {
			EOModelEditorUtils.openModel(myProject, vf);
		}
	}
}

