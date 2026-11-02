package org.wocommunity.plugins.intellij.components;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.tools.WOFileUtil;
import org.wocommunity.plugins.intellij.tools.WOIcons;

import javax.swing.*;

public class WOComponentIconProvider extends IconProvider {
    @Override
    public @Nullable Icon getIcon(@NotNull PsiElement element, int flags) {
        if (element instanceof PsiDirectory psiDirectory) {
            VirtualFile vFile = psiDirectory.getVirtualFile();
            if (vFile != null && vFile.getName().endsWith(".wo")) {
                return WOIcons.WOCOMPONENT_ICON;
            }
        } else if (element instanceof PsiFile psiFile) {
            VirtualFile vFile = psiFile.getVirtualFile();
            if (vFile != null && (WOFileUtil.fileIsComponent(vFile) || WOFileUtil.fileIsInComponent(vFile))) {
                return WOIcons.WOCOMPONENT_ICON;
            }
        }

        return null;
    }
}