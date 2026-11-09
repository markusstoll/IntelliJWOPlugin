package org.wocommunity.plugins.intellij.components;

import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.FakePsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.tools.WOIcons;

import javax.swing.*;

/**
 * A navigatable FakePsiElement representing a .wo component folder.
 * When navigated to (e.g. from Go to Declaration target chooser popup),
 * it opens the component in the WOComponentEditor tab.
 */
public class WOComponentNavigatableElement extends FakePsiElement {

	private final PsiDirectory directory;

	public WOComponentNavigatableElement(@NotNull PsiDirectory directory) {
		this.directory = directory;
	}

	public @NotNull PsiDirectory getDirectory() {
		return directory;
	}

	public @NotNull VirtualFile getVirtualFile() {
		return directory.getVirtualFile();
	}

	@Override
	public PsiElement getParent() {
		return directory.getParent();
	}

	@Override
	public @NotNull Project getProject() {
		return directory.getProject();
	}

	@Override
	public PsiFile getContainingFile() {
		return directory.getContainingFile();
	}

	@Override
	public boolean isValid() {
		return directory.isValid();
	}

	@Override
	public String getName() {
		return directory.getName();
	}

	@Override
	public String getText() {
		return directory.getName();
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
	public PsiElement getNavigationElement() {
		return this;
	}

	@Override
	public void navigate(boolean requestFocus) {
		VirtualFile vFile = directory.getVirtualFile();
		if (vFile != null) {
			WOEditorUtils.openComponent(getProject(), vFile);
		}
	}

	@Override
	public ItemPresentation getPresentation() {
		return new ItemPresentation() {
			@Override
			public @NlsSafe String getPresentableText() {
				return directory.getName();
			}

			@Override
			public @Nullable String getLocationString() {
				VirtualFile vf = directory.getVirtualFile();
				VirtualFile parent = vf != null ? vf.getParent() : null;
				return parent != null ? parent.getPath() : null;
			}

			@Override
			public @Nullable Icon getIcon(boolean unused) {
				return WOIcons.WOCOMPONENT_ICON;
			}
		};
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof WOComponentNavigatableElement other) {
			return directory.equals(other.directory);
		}
		if (obj instanceof PsiDirectory otherDir) {
			return directory.equals(otherDir);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return directory.hashCode();
	}

	@Override
	public String toString() {
		return "WOComponentNavigatableElement:" + directory.getName();
	}
}
