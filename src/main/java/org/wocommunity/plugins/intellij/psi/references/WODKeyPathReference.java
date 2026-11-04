package org.wocommunity.plugins.intellij.psi.references;

import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.psi.wod.WODElementFactory;
import org.wocommunity.plugins.intellij.psi.wod.WODKeyPathElement;
import org.wocommunity.plugins.intellij.tools.KeyValueCodingUtil;

import java.util.Objects;

public final class WODKeyPathReference extends PsiReferenceBase<WODKeyPathElement> {

    public WODKeyPathReference(@NotNull WODKeyPathElement element) {
        super(element, ElementManipulators.getValueTextRange(element));
    }

    @Override
    public @Nullable PsiElement resolve() {
        try {
            return KeyValueCodingUtil.resolveKeyPathElement(myElement);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        if (super.isReferenceTo(element)) {
            return true;
        }
        if (element instanceof PsiNamedElement named) {
            String altName = KeyValueCodingUtil.extractKvcPropertyName(named.getName());
            String myKey = myElement.getText();
            if (myKey.equals(altName) || myKey.equals(named.getName())) {
                PsiElement resolved = resolve();
                if (resolved instanceof PsiMember resolvedMember && element instanceof PsiMember targetMember) {
                    return Objects.equals(resolvedMember.getContainingClass(), targetMember.getContainingClass());
                }
            }
        }
        return false;
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        String currentText = myElement.getText();
        String newKey = KeyValueCodingUtil.computeRenamedKey(currentText, newElementName);
        WODKeyPathElement replacement = WODElementFactory.createKeyPathElement(myElement.getProject(), newKey);
        if (replacement != null) {
            myElement.getNode().replaceAllChildrenToChildrenOf(replacement.getNode());
            return myElement;
        }
        return myElement;
    }
}
