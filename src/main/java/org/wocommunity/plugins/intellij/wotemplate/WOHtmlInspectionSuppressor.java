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
            "XmlUnboundNsPrefix",
            "HtmlMissingClosingTag",
            "CheckEmptyTag"
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

        if (!isWo) {
            return super.isSuppressedFor(element, toolId);
        }

        if ("HtmlMissingClosingTag".equals(toolId) || "CheckEmptyTag".equals(toolId)) {
            return isSelfClosingWoTag(tag) || super.isSuppressedFor(element, toolId);
        }

        return true;
    }

    /**
     * Checks if a WebObjects tag was intended as self-closing.
     * Handles standard empty tags ({@code <webobject name="x"/>} or {@code <webobject name=x />})
     * as well as unquoted self-closing tags without preceding whitespace ({@code <webobject name=x/>}),
     * where the HTML parser consumes the trailing slash as part of the attribute value.
     */
    public static boolean isSelfClosingWoTag(@NotNull XmlTag tag) {
        if (tag.isEmpty()) {
            return true;
        }
        String text = tag.getText();
        com.intellij.psi.xml.XmlAttribute[] attrs = tag.getAttributes();
        String[] values = new String[attrs.length * 2];
        int idx = 0;
        for (com.intellij.psi.xml.XmlAttribute attr : attrs) {
            values[idx++] = attr.getValue();
            values[idx++] = attr.getText();
        }
        return isSelfClosingSyntax(false, text, values);
    }

    public static boolean isSelfClosingSyntax(boolean isEmpty, @Nullable String tagText, String @Nullable ... attributeValues) {
        if (isEmpty) {
            return true;
        }
        if (tagText != null) {
            String trimmed = tagText.trim();
            if (trimmed.endsWith("/>")) {
                return true;
            }
            int firstGt = trimmed.indexOf('>');
            if (firstGt > 0 && trimmed.substring(0, firstGt).trim().endsWith("/")) {
                return true;
            }
        }
        if (attributeValues != null) {
            for (String val : attributeValues) {
                if (val != null && (val.endsWith("/") || val.endsWith("/>"))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public SuppressQuickFix @NotNull [] getSuppressActions(@Nullable PsiElement element, @NotNull String toolId) {
        return SuppressQuickFix.EMPTY_ARRAY;
    }
}
