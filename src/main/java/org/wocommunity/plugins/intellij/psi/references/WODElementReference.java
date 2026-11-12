package org.wocommunity.plugins.intellij.psi.references;

import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.psi.wod.WODElement;
import org.wocommunity.plugins.intellij.tools.WOPsiUtil;
import org.wocommunity.plugins.intellij.wotemplate.WOHtmlWebObjectUtil;

import java.util.Collection;

public final class WODElementReference extends PsiReferenceBase<WODElement> {

    public WODElementReference(@NotNull WODElement element) {
        super(element, ElementManipulators.getValueTextRange(element));
    }

    @Override
    public @Nullable PsiElement resolve() {
        try {
            return getTagWithName(myElement.getContainingFile(), myElement.getIdentifier().getText());
        } catch (Exception e) {
            return null;
        }
    }

    private PsiElement getTagWithName(PsiFile containingFile, @NlsSafe String elementName) {
        PsiFile htmlFile = WOPsiUtil.getTemplateFile(containingFile);
        if (htmlFile == null) {
            return null;
        }

        Collection<XmlTag> tags = PsiTreeUtil.findChildrenOfType(htmlFile, XmlTag.class);

        for (XmlTag tag : tags) {
            if (WOHtmlWebObjectUtil.isWebObjectTag(tag)) {
                String val = WOHtmlWebObjectUtil.getWebObjectName(tag);
                if (elementName.equals(val)) {
                    return tag;
                }
            }
        }

        return null;
    }
}