package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.codeInspection.SuppressQuickFix;
import com.intellij.codeInspection.XmlInspectionSuppressor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Suppresses HTML inspections (such as empty script tag warnings or extra closing tag warnings)
 * on WebObjects template tags ({@code <webobject>} and {@code <wo:*>}).
 */
public final class WOHtmlInspectionSuppressor extends XmlInspectionSuppressor {

    private static final Set<String> SUPPRESSED_TOOL_IDS = Set.of(
            "CheckEmptyScriptTag",
            "HtmlExtraClosingTag",
            "XmlUnboundNsPrefix"
    );

    @Override
    public boolean isSuppressedFor(@NotNull PsiElement element, @NotNull String toolId) {
        if (!SUPPRESSED_TOOL_IDS.contains(toolId)) {
            return super.isSuppressedFor(element, toolId);
        }

        if (element.getContainingFile() == null || !WOHtmlCompletionUtil.isWoComponentHtml(element.getContainingFile())) {
            return super.isSuppressedFor(element, toolId);
        }

        XmlTag tag = element instanceof XmlTag
                ? (XmlTag) element
                : PsiTreeUtil.getParentOfType(element, XmlTag.class, false);

        if (tag == null) {
            return super.isSuppressedFor(element, toolId);
        }

        String localName = tag.getLocalName();
        boolean isWo = "webobject".equalsIgnoreCase(localName)
                || (localName != null && "wo".equalsIgnoreCase(tag.getNamespacePrefix()));

        return isWo || super.isSuppressedFor(element, toolId);
    }

    @Override
    public SuppressQuickFix @NotNull [] getSuppressActions(@Nullable PsiElement element, @NotNull String toolId) {
        return SuppressQuickFix.EMPTY_ARRAY;
    }
}
