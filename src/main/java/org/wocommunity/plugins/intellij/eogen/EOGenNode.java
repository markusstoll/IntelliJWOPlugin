package org.wocommunity.plugins.intellij.eogen;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.tools.WOIcons;

import java.util.Collection;
import java.util.Collections;

public class EOGenNode extends ProjectViewNode<PsiFile> {

	public EOGenNode(Project project, PsiFile value, ViewSettings viewSettings) {
		super(project, value, viewSettings);
	}

	@Override
	public void update(@NotNull PresentationData presentation) {
		PsiFile file = getValue();
		if (file != null) {
			presentation.setPresentableText(file.getName());
			presentation.setIcon(WOIcons.EOGEN_ICON);
		}
	}

	@Override
	public @NotNull Collection<AbstractTreeNode<?>> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public int getWeight() {
		return 30; // Matches EOModelNode so .eogen files are sorted with EOModels at the bottom
	}

	@Override
	public boolean contains(@NotNull VirtualFile file) {
		VirtualFile vf = getVirtualFile();
		return vf != null && vf.equals(file);
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
		PsiFile file = getValue();
		return file != null ? file.getVirtualFile() : null;
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
			new OpenFileDescriptor(myProject, vf).navigate(requestFocus);
		}
	}
}
