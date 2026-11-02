package org.wocommunity.plugins.intellij.psi.references;

import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.psi.wod.WODComponent;
import org.wocommunity.plugins.intellij.psi.wod.WODKeyPathElement;
import org.wocommunity.plugins.intellij.psi.wod.impl.WODKeyPathElementMixin;
import org.wocommunity.plugins.intellij.tools.KeyValueCodingUtil;
import org.wocommunity.plugins.intellij.tools.WOPsiUtil;

import java.util.ArrayList;
import java.util.List;

public final class WODComponentReference extends PsiPolyVariantReferenceBase<WODComponent> {

    public WODComponentReference(@NotNull WODComponent element) {
        super(element, ElementManipulators.getValueTextRange(element));
    }

    @Override
    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
        String componentName = myElement.getIdentifier().getText();
        if (componentName.isEmpty()) {
            return new ResolveResult[0];
        }

        List<ResolveResult> results = new ArrayList<>();

        // Find matching Java Classes
        try {
            PsiClass clazz = WOPsiUtil.getPsiClassForComponentName(componentName, myElement.getProject());
            if (clazz != null) {
                results.add(new PsiElementResolveResult(clazz));
            }
        } catch (Exception ignored) {
            System.out.println("Error resolving Java Class for component: " + componentName);
        }

        // If not found via WOPsiUtil, also check WOHtmlElementDescriptorProvider.resolveWoTagClass
        if (results.isEmpty()) {
            PsiClass clazz = org.wocommunity.plugins.intellij.wotemplate.WOHtmlElementDescriptorProvider
                    .resolveWoTagClass(myElement.getProject(), componentName);
            if (clazz != null) {
                results.add(new PsiElementResolveResult(clazz));
            }
        }

        // Find matching .wo Folders
        PsiDirectory directory = WOPsiUtil.getComponentFolderForComponentName(componentName, myElement.getProject());
        if (directory != null) {
            results.add(new PsiElementResolveResult(directory));
        }

        return results.toArray(new ResolveResult[0]);
    }
}