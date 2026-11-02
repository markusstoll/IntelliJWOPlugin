package org.wocommunity.plugins.intellij.components.conversion;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.wod.WodBinding;
import org.wocommunity.plugins.intellij.wod.WodElement;
import org.wocommunity.plugins.intellij.wod.WodModel;
import org.wocommunity.plugins.intellij.wod.WodParser;
import org.wocommunity.plugins.intellij.wotemplate.WOComponentTagAliasRegistry;

import java.util.*;

/**
 * Utility for bidirectional template conversion:
 * 1. Convert inline {@code <wo:*>} tags to classic WOD declarations and {@code <webobject>} tags.
 * 2. Convert classic {@code <webobject>} tags and their WOD declarations to modern inline {@code <wo:*>} tags.
 */
public final class WOComponentConversionUtil {

    private WOComponentConversionUtil() {
    }

    public static class ConversionResult {
        private final String htmlText;
        private final String wodText;
        private final int convertedCount;

        public ConversionResult(@NotNull String htmlText, @NotNull String wodText, int convertedCount) {
            this.htmlText = htmlText;
            this.wodText = wodText;
            this.convertedCount = convertedCount;
        }

        public @NotNull String getHtmlText() {
            return htmlText;
        }

        public @NotNull String getWodText() {
            return wodText;
        }

        public int getConvertedCount() {
            return convertedCount;
        }
    }

    /**
     * Resolves a tag local name (e.g. "if", "str", "WOConditional") to its canonical short class name.
     */
    private static final Map<String, String> CANONICAL_SHORTHANDS = new HashMap<>();
    static {
        CANONICAL_SHORTHANDS.put("WOConditional", "if");
        CANONICAL_SHORTHANDS.put("WOString", "str");
        CANONICAL_SHORTHANDS.put("WORepetition", "repeat");
        CANONICAL_SHORTHANDS.put("ERXElse", "else");
        CANONICAL_SHORTHANDS.put("ERXLocalizedString", "localized");
        CANONICAL_SHORTHANDS.put("WOComponentContent", "content");
        CANONICAL_SHORTHANDS.put("WOForm", "form");
        CANONICAL_SHORTHANDS.put("WOSubmitButton", "submit");
        CANONICAL_SHORTHANDS.put("WOTextField", "textfield");
        CANONICAL_SHORTHANDS.put("WOCheckBox", "checkbox");
        CANONICAL_SHORTHANDS.put("WOHyperlink", "link");
        CANONICAL_SHORTHANDS.put("WOImage", "img");
        CANONICAL_SHORTHANDS.put("WOPopUpButton", "popUpButton");
        CANONICAL_SHORTHANDS.put("WOPasswordField", "password");
        CANONICAL_SHORTHANDS.put("WORadioButton", "radio");
        CANONICAL_SHORTHANDS.put("WOResetButton", "reset");
        CANONICAL_SHORTHANDS.put("WOSwitchComponent", "switch");
        CANONICAL_SHORTHANDS.put("WOActionURL", "actionURL");
        CANONICAL_SHORTHANDS.put("WOResourceURL", "resourceURL");
        CANONICAL_SHORTHANDS.put("WOFileUpload", "upload");
        CANONICAL_SHORTHANDS.put("WOHiddenField", "hidden");
        CANONICAL_SHORTHANDS.put("WOSearchField", "search");
        CANONICAL_SHORTHANDS.put("WOText", "text");
        CANONICAL_SHORTHANDS.put("WOBody", "body");
    }

    public static @NotNull String resolveInlineTagName(@NotNull String typeName) {
        String simpleName = typeName.contains(".") ? typeName.substring(typeName.lastIndexOf('.') + 1) : typeName;
        String shorthand = CANONICAL_SHORTHANDS.get(simpleName);
        if (shorthand != null) {
            return shorthand;
        }
        return typeName;
    }

    public static @NotNull String resolveTypeName(@Nullable String localName) {
        if (localName == null || localName.isBlank()) {
            return "WOComponent";
        }
        String mapped = WOComponentTagAliasRegistry.resolveAlias(localName);
        if (mapped.equals(localName)) {
            String lower = localName.toLowerCase(Locale.ROOT);
            String mappedLower = WOComponentTagAliasRegistry.resolveAlias(lower);
            if (!mappedLower.equals(lower)) {
                mapped = mappedLower;
            }
        }
        int lastDot = mapped.lastIndexOf('.');
        if (lastDot >= 0) {
            return mapped.substring(lastDot + 1);
        }
        return mapped;
    }

    /**
     * Converts all inline {@code <wo:*>} tags in the HTML to {@code <webobject>} tags
     * and appends the corresponding declarations to the WOD text.
     */
    public static @NotNull ConversionResult convertToWod(@NotNull String htmlText, @NotNull String wodText) {
        List<HtmlTagMatch> matches = findInlineWoTags(htmlText);
        if (matches.isEmpty()) {
            return new ConversionResult(htmlText, wodText, 0);
        }

        WodModel existingModel = WodParser.parse(wodText);
        Set<String> allocatedNames = new HashSet<>();
        for (WodElement elem : existingModel.getElements()) {
            allocatedNames.add(elem.getName());
        }

        List<TextReplacement> replacements = new ArrayList<>();
        StringBuilder newWodDeclarations = new StringBuilder();

        for (HtmlTagMatch match : matches) {
            String typeName = resolveTypeName(match.localName);
            int idx = 1;
            while (allocatedNames.contains(typeName + idx)) {
                idx++;
            }
            String elemName = typeName + idx;
            allocatedNames.add(elemName);

            // Build WOD declaration block
            if (newWodDeclarations.length() > 0) {
                newWodDeclarations.append("\n");
            }
            newWodDeclarations.append(elemName).append(" : ").append(typeName).append(" {\n");
            for (Map.Entry<String, String> attr : match.attributes.entrySet()) {
                String key = attr.getKey();
                String rawVal = attr.getValue();
                String wodVal;
                if (rawVal == null) {
                    wodVal = "true";
                } else if (rawVal.startsWith("$")) {
                    wodVal = rawVal.substring(1);
                } else if (rawVal.startsWith("~")) {
                    wodVal = "\"" + escapeWodLiteral(rawVal) + "\"";
                } else {
                    wodVal = "\"" + escapeWodLiteral(rawVal) + "\"";
                }
                newWodDeclarations.append("    ").append(key).append(" = ").append(wodVal).append(";\n");
            }
            newWodDeclarations.append("}\n");

            // Plan HTML replacements
            boolean treatAsSelfClosing = match.isSelfClosing;
            if (!treatAsSelfClosing && match.closeTagStart >= 0) {
                String inner = htmlText.substring(match.openTagEnd, match.closeTagStart);
                if (inner.trim().isEmpty()) {
                    treatAsSelfClosing = true;
                }
            }

            if (treatAsSelfClosing) {
                int endOffset = (match.closeTagEnd > 0) ? match.closeTagEnd : match.openTagEnd;
                replacements.add(new TextReplacement(match.openTagStart, endOffset, "<webobject name=\"" + elemName + "\"/>"));
            } else {
                replacements.add(new TextReplacement(match.openTagStart, match.openTagEnd, "<webobject name=\"" + elemName + "\">"));
                if (match.closeTagStart >= 0 && match.closeTagEnd > match.closeTagStart) {
                    replacements.add(new TextReplacement(match.closeTagStart, match.closeTagEnd, "</webobject>"));
                }
            }
        }

        String updatedHtml = applyReplacements(htmlText, replacements);

        StringBuilder updatedWod = new StringBuilder(wodText);
        if (newWodDeclarations.length() > 0) {
            if (updatedWod.length() > 0) {
                if (!updatedWod.toString().endsWith("\n\n")) {
                    if (updatedWod.toString().endsWith("\n")) {
                        updatedWod.append("\n");
                    } else {
                        updatedWod.append("\n\n");
                    }
                }
            }
            updatedWod.append(newWodDeclarations);
        }

        return new ConversionResult(updatedHtml, updatedWod.toString(), matches.size());
    }

    /**
     * Converts all {@code <webobject>} tags in HTML (whose definitions exist in WOD) to inline
     * {@code <wo:TypeName>} tags, and removes those definitions from WOD.
     */
    public static @NotNull ConversionResult convertToInline(@NotNull String htmlText, @NotNull String wodText) {
        WodModel wodModel = WodParser.parse(wodText);
        if (wodModel.getElements().isEmpty()) {
            return new ConversionResult(htmlText, wodText, 0);
        }

        List<HtmlTagMatch> matches = findWebObjectTags(htmlText);
        if (matches.isEmpty()) {
            return new ConversionResult(htmlText, wodText, 0);
        }

        List<TextReplacement> htmlReplacements = new ArrayList<>();
        Set<String> convertedElemNames = new LinkedHashSet<>();

        for (HtmlTagMatch match : matches) {
            String elemName = match.attributes.get("name");
            if (elemName == null || elemName.isBlank()) {
                elemName = match.attributes.get("NAME");
            }
            if (elemName == null || elemName.isBlank()) {
                continue;
            }

            WodElement element = wodModel.getElement(elemName);
            if (element == null) {
                // Not in WOD, cannot resolve type/bindings
                continue;
            }

            convertedElemNames.add(elemName);
            String typeName = element.getTypeName();
            String inlineTagName = resolveInlineTagName(typeName);

            // Build inline tag attributes
            StringBuilder attrs = new StringBuilder();
            for (WodBinding binding : element.getBindings().values()) {
                String key = binding.getKey();
                String val = binding.getValue();
                String attrVal;
                if (binding.isLiteralString()) {
                    String unquoted = unescapeWodLiteral(val);
                    attrVal = unquoted;
                } else {
                    if (val.startsWith("~")) {
                        attrVal = val;
                    } else {
                        attrVal = "$" + val;
                    }
                }
                attrs.append(" ").append(formatHtmlAttribute(key, attrVal));
            }

            boolean treatAsSelfClosing = match.isSelfClosing;
            if (!treatAsSelfClosing && match.closeTagStart >= 0) {
                String inner = htmlText.substring(match.openTagEnd, match.closeTagStart);
                if (inner.trim().isEmpty()) {
                    treatAsSelfClosing = true;
                }
            }

            if (treatAsSelfClosing) {
                int endOffset = (match.closeTagEnd > 0) ? match.closeTagEnd : match.openTagEnd;
                htmlReplacements.add(new TextReplacement(match.openTagStart, endOffset, "<wo:" + inlineTagName + attrs + "/>"));
            } else {
                htmlReplacements.add(new TextReplacement(match.openTagStart, match.openTagEnd, "<wo:" + inlineTagName + attrs + ">"));
                if (match.closeTagStart >= 0 && match.closeTagEnd > match.closeTagStart) {
                    htmlReplacements.add(new TextReplacement(match.closeTagStart, match.closeTagEnd, "</wo:" + inlineTagName + ">"));
                }
            }
        }

        if (htmlReplacements.isEmpty()) {
            return new ConversionResult(htmlText, wodText, 0);
        }

        String updatedHtml = applyReplacements(htmlText, htmlReplacements);

        // Remove converted declarations from WOD
        List<TextReplacement> wodReplacements = new ArrayList<>();
        for (String name : convertedElemNames) {
            WodElement elem = wodModel.getElement(name);
            if (elem != null && elem.getTotalRange() != null) {
                int start = elem.getTotalRange().getStartOffset();
                int end = elem.getTotalRange().getEndOffset();
                // Include trailing whitespace / newlines
                while (end < wodText.length()) {
                    char c = wodText.charAt(end);
                    if (c == ' ' || c == '\t') {
                        end++;
                    } else if (c == '\r' || c == '\n') {
                        if (c == '\r' && end + 1 < wodText.length() && wodText.charAt(end + 1) == '\n') {
                            end += 2;
                        } else {
                            end += 1;
                        }
                        break;
                    } else {
                        break;
                    }
                }
                wodReplacements.add(new TextReplacement(start, end, ""));
            }
        }

        String updatedWod = applyReplacements(wodText, wodReplacements);
        // Clean up redundant blank lines
        updatedWod = updatedWod.replaceAll("(?m)^[ \t]*\r?\n[ \t]*\r?\n[ \t]*\r?\n+", "\n\n").trim();
        if (!updatedWod.isEmpty()) {
            updatedWod += "\n";
        }

        return new ConversionResult(updatedHtml, updatedWod, convertedElemNames.size());
    }

    public static class HtmlTagMatch {
        public final String tagName;
        public final String localName;
        public final int openTagStart;
        public final int openTagEnd;
        public int closeTagStart = -1;
        public int closeTagEnd = -1;
        public final boolean isSelfClosing;
        public final Map<String, String> attributes = new LinkedHashMap<>();

        public HtmlTagMatch(String tagName, String localName, int openTagStart, int openTagEnd, boolean isSelfClosing) {
            this.tagName = tagName;
            this.localName = localName;
            this.openTagStart = openTagStart;
            this.openTagEnd = openTagEnd;
            this.isSelfClosing = isSelfClosing;
        }
    }

    public static class TextReplacement {
        public final int startOffset;
        public final int endOffset;
        public final String replacement;

        public TextReplacement(int startOffset, int endOffset, String replacement) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.replacement = replacement;
        }
    }

    public static String applyReplacements(@NotNull String text, @NotNull List<TextReplacement> replacements) {
        if (replacements.isEmpty()) {
            return text;
        }
        // Sort descending by startOffset so earlier replacements don't shift later ones
        replacements.sort((r1, r2) -> Integer.compare(r2.startOffset, r1.startOffset));

        StringBuilder sb = new StringBuilder(text);
        for (TextReplacement r : replacements) {
            int start = Math.max(0, Math.min(r.startOffset, sb.length()));
            int end = Math.max(start, Math.min(r.endOffset, sb.length()));
            sb.replace(start, end, r.replacement);
        }
        return sb.toString();
    }

    public static @NotNull List<HtmlTagMatch> findInlineWoTags(@NotNull String html) {
        List<HtmlTagMatch> result = new ArrayList<>();
        Deque<HtmlTagMatch> openStack = new ArrayDeque<>();
        int len = html.length();
        int i = 0;

        while (i < len) {
            if (html.startsWith("<!--", i)) {
                int end = html.indexOf("-->", i + 4);
                i = (end >= 0) ? end + 3 : len;
                continue;
            }
            if (html.startsWith("<![CDATA[", i)) {
                int end = html.indexOf("]]>", i + 9);
                i = (end >= 0) ? end + 3 : len;
                continue;
            }

            if (html.charAt(i) == '<') {
                if (i + 1 < len && html.charAt(i + 1) == '/') {
                    // Closing tag: </wo:...>
                    int start = i;
                    i += 2;
                    while (i < len && Character.isWhitespace(html.charAt(i))) i++;
                    int nameStart = i;
                    while (i < len && !Character.isWhitespace(html.charAt(i)) && html.charAt(i) != '>') i++;
                    String closeName = html.substring(nameStart, i);
                    while (i < len && html.charAt(i) != '>') i++;
                    if (i < len && html.charAt(i) == '>') i++;
                    int end = i;

                    if (closeName.toLowerCase(Locale.ROOT).startsWith("wo:")) {
                        // Find matching open tag in stack
                        HtmlTagMatch matched = null;
                        Iterator<HtmlTagMatch> it = openStack.iterator();
                        while (it.hasNext()) {
                            HtmlTagMatch candidate = it.next();
                            if (candidate.tagName.equalsIgnoreCase(closeName)) {
                                matched = candidate;
                                it.remove();
                                break;
                            }
                        }
                        if (matched != null) {
                            matched.closeTagStart = start;
                            matched.closeTagEnd = end;
                        }
                    }
                    continue;
                }

                // Potential opening tag: <wo:...
                int tagStart = i;
                i++;
                while (i < len && Character.isWhitespace(html.charAt(i))) i++;
                int nameStart = i;
                while (i < len && !Character.isWhitespace(html.charAt(i)) && html.charAt(i) != '>' && html.charAt(i) != '/') i++;
                String tagName = html.substring(nameStart, i);

                if (tagName.toLowerCase(Locale.ROOT).startsWith("wo:")) {
                    String localName = tagName.substring(3);
                    Map<String, String> attrs = new LinkedHashMap<>();
                    boolean selfClosing = false;

                    while (i < len) {
                        while (i < len && Character.isWhitespace(html.charAt(i))) i++;
                        if (i >= len) break;
                        if (html.charAt(i) == '>') {
                            i++;
                            break;
                        }
                        if (html.charAt(i) == '/' && i + 1 < len && html.charAt(i + 1) == '>') {
                            selfClosing = true;
                            i += 2;
                            break;
                        }

                        // Read attribute name
                        int attrNameStart = i;
                        while (i < len && !Character.isWhitespace(html.charAt(i)) && html.charAt(i) != '=' && html.charAt(i) != '>' && html.charAt(i) != '/') {
                            i++;
                        }
                        String attrName = html.substring(attrNameStart, i);
                        while (i < len && Character.isWhitespace(html.charAt(i))) i++;

                        String attrVal = null;
                        if (i < len && html.charAt(i) == '=') {
                            i++;
                            while (i < len && Character.isWhitespace(html.charAt(i))) i++;
                            if (i < len && (html.charAt(i) == '"' || html.charAt(i) == '\'')) {
                                char quote = html.charAt(i);
                                i++;
                                int valStart = i;
                                while (i < len && html.charAt(i) != quote) i++;
                                attrVal = html.substring(valStart, i);
                                if (i < len && html.charAt(i) == quote) i++;
                            } else {
                                int valStart = i;
                                while (i < len && !Character.isWhitespace(html.charAt(i)) && html.charAt(i) != '>' && html.charAt(i) != '/') {
                                    i++;
                                }
                                attrVal = html.substring(valStart, i);
                            }
                        }

                        if (!attrName.isBlank()) {
                            attrs.put(attrName, unescapeHtmlAttr(attrVal));
                        }
                    }

                    int openTagEnd = i;
                    HtmlTagMatch match = new HtmlTagMatch(tagName, localName, tagStart, openTagEnd, selfClosing);
                    match.attributes.putAll(attrs);
                    result.add(match);
                    if (!selfClosing) {
                        openStack.push(match);
                    }
                    continue;
                }
            }

            i++;
        }

        return result;
    }

    public static @NotNull List<HtmlTagMatch> findWebObjectTags(@NotNull String html) {
        List<HtmlTagMatch> result = new ArrayList<>();
        Deque<HtmlTagMatch> openStack = new ArrayDeque<>();
        int len = html.length();
        int i = 0;

        while (i < len) {
            if (html.startsWith("<!--", i)) {
                int end = html.indexOf("-->", i + 4);
                i = (end >= 0) ? end + 3 : len;
                continue;
            }
            if (html.startsWith("<![CDATA[", i)) {
                int end = html.indexOf("]]>", i + 9);
                i = (end >= 0) ? end + 3 : len;
                continue;
            }

            if (html.charAt(i) == '<') {
                if (i + 1 < len && html.charAt(i + 1) == '/') {
                    // Closing tag: </webobject> or </wo>
                    int start = i;
                    i += 2;
                    while (i < len && Character.isWhitespace(html.charAt(i))) i++;
                    int nameStart = i;
                    while (i < len && !Character.isWhitespace(html.charAt(i)) && html.charAt(i) != '>') i++;
                    String closeName = html.substring(nameStart, i);
                    while (i < len && html.charAt(i) != '>') i++;
                    if (i < len && html.charAt(i) == '>') i++;
                    int end = i;

                    if ("webobject".equalsIgnoreCase(closeName) || "wo".equalsIgnoreCase(closeName)) {
                        if (!openStack.isEmpty()) {
                            HtmlTagMatch matched = openStack.pop();
                            matched.closeTagStart = start;
                            matched.closeTagEnd = end;
                        }
                    }
                    continue;
                }

                // Potential opening tag: <webobject ... or <wo ...
                int tagStart = i;
                i++;
                while (i < len && Character.isWhitespace(html.charAt(i))) i++;
                int nameStart = i;
                while (i < len && !Character.isWhitespace(html.charAt(i)) && html.charAt(i) != '>' && html.charAt(i) != '/') i++;
                String tagName = html.substring(nameStart, i);

                if ("webobject".equalsIgnoreCase(tagName) || "wo".equalsIgnoreCase(tagName)) {
                    Map<String, String> attrs = new LinkedHashMap<>();
                    boolean selfClosing = false;

                    while (i < len) {
                        while (i < len && Character.isWhitespace(html.charAt(i))) i++;
                        if (i >= len) break;
                        if (html.charAt(i) == '>') {
                            i++;
                            break;
                        }
                        if (html.charAt(i) == '/' && i + 1 < len && html.charAt(i + 1) == '>') {
                            selfClosing = true;
                            i += 2;
                            break;
                        }

                        // Read attribute name
                        int attrNameStart = i;
                        while (i < len && !Character.isWhitespace(html.charAt(i)) && html.charAt(i) != '=' && html.charAt(i) != '>' && html.charAt(i) != '/') {
                            i++;
                        }
                        String attrName = html.substring(attrNameStart, i);
                        while (i < len && Character.isWhitespace(html.charAt(i))) i++;

                        String attrVal = null;
                        if (i < len && html.charAt(i) == '=') {
                            i++;
                            while (i < len && Character.isWhitespace(html.charAt(i))) i++;
                            if (i < len && (html.charAt(i) == '"' || html.charAt(i) == '\'')) {
                                char quote = html.charAt(i);
                                i++;
                                int valStart = i;
                                while (i < len && html.charAt(i) != quote) i++;
                                attrVal = html.substring(valStart, i);
                                if (i < len && html.charAt(i) == quote) i++;
                            } else {
                                int valStart = i;
                                while (i < len && !Character.isWhitespace(html.charAt(i)) && html.charAt(i) != '>' && html.charAt(i) != '/') {
                                    i++;
                                }
                                attrVal = html.substring(valStart, i);
                            }
                        }

                        if (!attrName.isBlank()) {
                            attrs.put(attrName, unescapeHtmlAttr(attrVal));
                        }
                    }

                    int openTagEnd = i;
                    HtmlTagMatch match = new HtmlTagMatch(tagName, tagName, tagStart, openTagEnd, selfClosing);
                    match.attributes.putAll(attrs);
                    result.add(match);
                    if (!selfClosing) {
                        openStack.push(match);
                    }
                    continue;
                }
            }

            i++;
        }

        return result;
    }

    public static @NotNull String escapeWodLiteral(@NotNull String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static @NotNull String unescapeWodLiteral(@NotNull String s) {
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    public static @NotNull String formatHtmlAttribute(@NotNull String key, @NotNull String value) {
        if (value.contains("\"") && !value.contains("'")) {
            return key + "='" + value + "'";
        } else if (value.contains("'") && !value.contains("\"")) {
            return key + "=\"" + value + "\"";
        } else {
            return key + "=\"" + escapeHtmlAttr(value) + "\"";
        }
    }

    public static @NotNull String escapeHtmlAttr(@NotNull String s) {
        return s.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public static @Nullable String unescapeHtmlAttr(@Nullable String s) {
        if (s == null) return null;
        return s.replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
    }
}
