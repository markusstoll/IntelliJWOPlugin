package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Autocompletion contributor for HTML attribute values in WebObjects component templates.
 * Triggers when the user types '$' in an attribute value and suggests properties, getters,
 * setters, and methods from the associated component class (Java/Kotlin/Groovy).
 */
public class WOHtmlBindingCompletionContributor extends CompletionContributor {

    public WOHtmlBindingCompletionContributor() {
        CompletionProvider<CompletionParameters> provider = new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters,
                                          @NotNull ProcessingContext context,
                                          @NotNull CompletionResultSet result) {
                PsiFile file = parameters.getOriginalFile();
                if (!WOHtmlCompletionUtil.isWoComponentHtml(file)) {
                    return;
                }

                PsiElement position = parameters.getPosition();
                XmlAttributeValue attrValue = PsiTreeUtil.getParentOfType(position, XmlAttributeValue.class, false);
                XmlAttribute attr = PsiTreeUtil.getParentOfType(position, XmlAttribute.class, false);

                if (attrValue == null && attr == null) {
                    return;
                }

                PsiElement container = attrValue != null ? attrValue : attr;
                int offset = parameters.getOffset();
                int containerStart = container.getTextRange().getStartOffset();
                if (offset < containerStart) {
                    return;
                }

                Document doc = parameters.getEditor().getDocument();
                String textBeforeCaret = doc.getText(new TextRange(containerStart, offset));

                int dollarIdx = textBeforeCaret.lastIndexOf('$');
                if (dollarIdx < 0) {
                    return;
                }

                String afterDollar = textBeforeCaret.substring(dollarIdx + 1);
                // If there are spaces or quotes between '$' and the caret, it's not an active binding expression
                if (afterDollar.contains("\"") || afterDollar.contains("'") ||
                    afterDollar.contains(" ") || afterDollar.contains("\t") || afterDollar.contains("\n")) {
                    return;
                }

                int lastDot = afterDollar.lastIndexOf('.');
                String leadingPath = lastDot >= 0 ? afterDollar.substring(0, lastDot) : "";
                String currentPrefix = lastDot >= 0 ? afterDollar.substring(lastDot + 1) : afterDollar;

                PsiClass componentClass = WOComponentClassResolver.findComponentClass(file);
                if (componentClass == null) {
                    return;
                }

                PsiClass targetClass = componentClass;
                if (!leadingPath.isEmpty()) {
                    targetClass = WOComponentClassResolver.resolvePathToClass(componentClass, leadingPath);
                    if (targetClass == null) {
                        return;
                    }
                }

                List<LookupElement> variants = WOComponentClassResolver.collectBindingVariants(targetClass);
                if (variants.isEmpty()) {
                    return;
                }

                CompletionResultSet matcher = result.withPrefixMatcher(currentPrefix);
                for (LookupElement el : variants) {
                    matcher.addElement(el);
                }
            }
        };

        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), provider);
        extend(CompletionType.SMART, PlatformPatterns.psiElement(), provider);
    }
}

