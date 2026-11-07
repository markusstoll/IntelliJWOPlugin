package org.wocommunity.plugins.intellij.eomodel;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.tools.WOIcons;

import javax.swing.*;

public class EOModelIconProvider extends IconProvider {
	@Override
	public @Nullable Icon getIcon(@NotNull PsiElement element, int flags) {
		if (element instanceof PsiDirectory psiDirectory) {
			VirtualFile vFile = psiDirectory.getVirtualFile();
			if (EOModelFileUtil.fileIsModel(vFile)) {
				return WOIcons.EOMODEL_ICON;
			}
		} else if (element instanceof PsiFile psiFile) {
			VirtualFile vFile = psiFile.getVirtualFile();
			if (vFile != null && EOModelFileUtil.fileIsInModel(vFile)) {
				return WOIcons.EOMODEL_ICON;
			}
		}
		return null;
	}
}

