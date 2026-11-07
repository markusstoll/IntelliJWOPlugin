package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.wocommunity.plugins.intellij.tools.KeyValueCodingUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A PSI reference from an HTML attribute binding ($property, $method, $user.name)
 * to its declaration in the Java/Kotlin/Groovy component class.
 */
public class WOHtmlBindingReference extends PsiReferenceBase<XmlAttributeValue> implements PsiPolyVariantReference {

    private final String fullPathUpToSegment;
    private final String segmentKey;

    public WOHtmlBindingReference(@NotNull XmlAttributeValue element,
                                  @NotNull TextRange range,
                                  @NotNull String fullPathUpToSegment,
                                  @NotNull String segmentKey) {
        super(element, range, true);
        this.fullPathUpToSegment = fullPathUpToSegment;
        this.segmentKey = segmentKey;
    }

    @Override
    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
        PsiFile file = myElement.getContainingFile();
        if (file == null) {
            return ResolveResult.EMPTY_ARRAY;
        }
        if (file.getOriginalFile() != null) {
            file = file.getOriginalFile();
        }

        List<PsiClass> candidateClasses = WOComponentClassResolver.findCandidateComponentClasses(file);
        if (candidateClasses.isEmpty()) {
            return ResolveResult.EMPTY_ARRAY;
        }

        List<ResolveResult> results = new ArrayList<>();
        for (PsiClass componentClass : candidateClasses) {
            List<PsiElement> targets = WOComponentClassResolver.findBindingTargets(componentClass, fullPathUpToSegment);
            for (PsiElement target : targets) {
                results.add(new PsiElementResolveResult(target));
            }
            if (!results.isEmpty()) {
                break;
            }
        }

        return results.toArray(new ResolveResult[0]);
    }

    @Override
    public @Nullable PsiElement resolve() {
        ResolveResult[] results = multiResolve(false);
        return results.length > 0 ? results[0].getElement() : null;
    }

    @Override
    public @NotNull String getCanonicalText() {
        return segmentKey;
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        TextRange range = getRangeInElement();
        String currentText = range.substring(myElement.getText());
        String renamed = KeyValueCodingUtil.computeRenamedKey(currentText, newElementName);
        return super.handleElementRename(renamed);
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        if (super.isReferenceTo(element)) {
            return true;
        }
        for (ResolveResult result : multiResolve(false)) {
            PsiElement target = result.getElement();
            if (target != null && getElement().getManager().areElementsEquivalent(target, element)) {
                return true;
            }
        }
        if (element instanceof PsiNamedElement named) {
            String altName = KeyValueCodingUtil.extractKvcPropertyName(named.getName());
            if (segmentKey.equals(altName) || segmentKey.equals(named.getName())) {
                for (ResolveResult result : multiResolve(false)) {
                    PsiElement target = result.getElement();
                    if (target instanceof PsiMember targetMember && element instanceof PsiMember searchMember) {
                        if (Objects.equals(targetMember.getContainingClass(), searchMember.getContainingClass())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public Object @NotNull [] getVariants() {
        return EMPTY_ARRAY;
    }
}
