package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTagValue;
import com.intellij.util.ProcessingContext;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Attribute-name completion for {@code wo:*} tags in WO component HTML templates.
 *
 * HTML attribute completion does not always consult {@link com.intellij.xml.XmlElementDescriptor#getAttributesDescriptors(XmlTag)}
 * the same way validation does; this contributor fills that gap.
 */
public final class WOHtmlAttributeCompletionContributor extends CompletionContributor {

    public WOHtmlAttributeCompletionContributor() {
        CompletionProvider<CompletionParameters> provider = new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters,
                                          @NotNull ProcessingContext context,
                                          @NotNull CompletionResultSet result) {
                PsiFile file = parameters.getOriginalFile();
                if (!WOHtmlCompletionUtil.isWoComponentHtml(file)) {
                    return;
                }

                int offset = parameters.getOffset();
                if (offset < 0) {
                    return;
                }

                PsiElement leaf = file.findElementAt(offset > 0 ? offset - 1 : offset);
                if (leaf == null) {
                    leaf = file.findElementAt(offset);
                }
                if (leaf == null) {
                    return;
                }

                XmlTag tag = PsiTreeUtil.getParentOfType(leaf, XmlTag.class, false);
                if (tag == null) {
                    tag = PsiTreeUtil.getParentOfType(parameters.getPosition(), XmlTag.class, false);
                }
                if (tag == null || !WOHtmlCompletionUtil.isWoPrefixedTag(tag)) {
                    return;
                }

                XmlTagValue tv = tag.getValue();
                int bodyStart = tv != null ? tv.getTextRange().getStartOffset() : Integer.MAX_VALUE;
                XmlAttribute attr = PsiTreeUtil.getParentOfType(leaf, XmlAttribute.class, false);
                if (offset >= bodyStart && attr == null) {
                    return;
                }

                if (!isInOpeningTagAttributeArea(tag, leaf, offset, bodyStart)) {
                    return;
                }

                XmlElementDescriptor ed = new WOHtmlElementDescriptorProvider().getDescriptor(tag);
                if (!(ed instanceof WOXmlElementDescriptor wod)) {
                    return;
                }

                XmlAttributeDescriptor[] descriptors = wod.getAttributesDescriptors(tag);
                if (descriptors.length == 0) {
                    return;
                }

                String prefix = extractAttributeNamePrefix(file, attr, leaf, offset);
                CompletionResultSet out = prefix.isEmpty() ? result : result.withPrefixMatcher(prefix);

                Set<String> takenByOtherAttributes = new HashSet<>();
                for (XmlAttribute a : tag.getAttributes()) {
                    if (a == attr) {
                        continue;
                    }
                    String n = a.getName();
                    if (n != null && !n.isBlank()) {
                        takenByOtherAttributes.add(n);
                    }
                }

                for (XmlAttributeDescriptor d : descriptors) {
                    String name = d.getName();
                    if (name == null || name.isBlank()) {
                        continue;
                    }
                    if (takenByOtherAttributes.contains(name)) {
                        continue;
                    }
                    LookupElementBuilder le = LookupElementBuilder.create(name).withTypeText("WO binding", true);
                    PsiElement decl = d.getDeclaration();
                    if (decl != null) {
                        le = le.withPsiElement(decl);
                    }
                    out.addElement(le);
                }
            }
        };

        extend(CompletionType.BASIC, com.intellij.patterns.PlatformPatterns.psiElement(), provider);
        extend(CompletionType.SMART, com.intellij.patterns.PlatformPatterns.psiElement(), provider);
    }

    private static boolean isInOpeningTagAttributeArea(@NotNull XmlTag tag,
                                                       @NotNull PsiElement leaf,
                                                       int offset,
                                                       int bodyStart) {
        if (offset >= bodyStart) {
            return false;
        }
        // Inside an attribute (name or value): always OK for name completion when caret is in name.
        if (PsiTreeUtil.getParentOfType(leaf, XmlAttribute.class, false) != null) {
            return true;
        }
        // Whitespace / tokens in the opening tag before the tag body starts.
        for (PsiElement p = leaf; p != null; p = p.getParent()) {
            if (p == tag) {
                return true;
            }
        }
        return false;
    }

    private static @NotNull String extractAttributeNamePrefix(@NotNull PsiFile file,
                                                              @Nullable XmlAttribute attr,
                                                              @NotNull PsiElement leaf,
                                                              int offset) {
        if (attr == null) {
            return "";
        }
        PsiElement nameElement = attr.getNameElement();
        if (nameElement == null) {
            String n = attr.getName();
            return n != null ? n : "";
        }
        TextRange tr = nameElement.getTextRange();
        if (!tr.contains(offset) && offset != tr.getEndOffset()) {
            return "";
        }
        Document doc = com.intellij.psi.PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        if (doc == null) {
            return "";
        }
        int start = tr.getStartOffset();
        int end = Math.min(offset, tr.getEndOffset());
        if (end <= start) {
            return "";
        }
        return doc.getText(TextRange.create(start, end));
    }
}
