package org.wocommunity.plugins.intellij.components;

import com.intellij.ide.IconProvider;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.tools.WOIcons;

import javax.swing.*;

public class WOComponentIconProvider extends IconProvider {
    @Override
    public @Nullable Icon getIcon(@NotNull PsiElement element, int flags) {
        if(element instanceof PsiDirectory) {
            PsiDirectory psiDirectory = (PsiDirectory) element;
            if (psiDirectory.getVirtualFile().getName().endsWith(".wo"))
                return WOIcons.WOCOMPONENT_ICON;
        }

        return null;
    }
}