package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registers PSI reference providers for HTML attribute values containing '$' bindings
 * ($attribute, $method, $user.name) in WebObjects component templates.
 */
public class WOHtmlBindingReferenceContributor extends PsiReferenceContributor {

    public record BindingSegment(@NotNull TextRange range, @NotNull String fullPath, @NotNull String key) {}

    /**
     * Parses all '$' binding expressions from raw attribute value text (e.g. "\"$user.name\"").
     * Returns the relative text ranges and keypaths for each segment.
     */
    public static @NotNull List<BindingSegment> parseBindingSegments(@Nullable String rawText) {
        if (rawText == null || !rawText.contains("$")) {
            return Collections.emptyList();
        }

        List<BindingSegment> segmentsList = new ArrayList<>();
        int len = rawText.length();
        int dollarIdx = rawText.indexOf('$');

        while (dollarIdx >= 0 && dollarIdx < len) {
            int exprStart = dollarIdx + 1;
            int exprEnd = exprStart;
            while (exprEnd < len) {
                char ch = rawText.charAt(exprEnd);
                if (Character.isJavaIdentifierPart(ch) || ch == '.') {
                    exprEnd++;
                } else {
                    break;
                }
            }

            // Trim trailing dots (e.g. while editing "$user.")
            while (exprEnd > exprStart && rawText.charAt(exprEnd - 1) == '.') {
                exprEnd--;
            }

            if (exprEnd > exprStart) {
                // If followed immediately by empty parentheses "()", include them in the clickable range
                int rangeEnd = exprEnd;
                if (rangeEnd + 1 < len && rawText.charAt(rangeEnd) == '(' && rawText.charAt(rangeEnd + 1) == ')') {
                    rangeEnd += 2;
                }

                String expression = rawText.substring(exprStart, exprEnd);
                String[] segments = expression.split("\\.", -1);
                int segOffsetInRaw = exprStart;
                StringBuilder fullPath = new StringBuilder();

                for (int i = 0; i < segments.length; i++) {
                    String segment = segments[i];
                    if (i > 0) {
                        fullPath.append(".");
                    }
                    fullPath.append(segment);

                    if (!segment.isEmpty()) {
                        // Include '$' in the first segment's range so Cmd-clicking '$' or the property resolves it
                        int start = (i == 0) ? dollarIdx : segOffsetInRaw;
                        // Include "()" on the last segment if present
                        int end = (i == segments.length - 1) ? rangeEnd : (segOffsetInRaw + segment.length());
                        TextRange range = new TextRange(start, end);
                        segmentsList.add(new BindingSegment(range, fullPath.toString(), segment));
                    }
                    segOffsetInRaw += segment.length() + 1;
                }
            }

            dollarIdx = rawText.indexOf('$', exprEnd);
        }

        return segmentsList;
    }

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
                XmlPatterns.xmlAttributeValue(),
                new PsiReferenceProvider() {
                    @Override
                    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                                          @NotNull ProcessingContext context) {
                        if (!(element instanceof XmlAttributeValue attrValue)) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        PsiFile file = attrValue.getContainingFile();
                        if (file == null || !WOHtmlCompletionUtil.isWoComponentHtml(file)) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        String rawText = attrValue.getText();
                        List<BindingSegment> segments = parseBindingSegments(rawText);
                        if (segments.isEmpty()) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        PsiReference[] refs = new PsiReference[segments.size()];
                        for (int i = 0; i < segments.size(); i++) {
                            BindingSegment seg = segments.get(i);
                            refs[i] = new WOHtmlBindingReference(attrValue, seg.range(), seg.fullPath(), seg.key());
                        }
                        return refs;
                    }
                }
        );
    }
}
