package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.Nullable;

/**
 * Utility for parsing and normalizing &lt;webobject&gt; tag attributes,
 * supporting both quoted (name="foo") and unquoted (name=foo or name=foo/&gt;) syntax.
 */
public final class WOHtmlWebObjectUtil {

    private WOHtmlWebObjectUtil() {}

    /**
     * Extracts and normalizes the WebObject element name from a &lt;webobject&gt; tag.
     * Handles both quoted and unquoted attributes, case insensitivity (name/NAME),
     * and self-closing tags without preceding whitespace (&lt;webobject name=ShortTermLoginButton/&gt;).
     */
    public static @Nullable String getWebObjectName(@Nullable XmlTag tag) {
        if (tag == null) {
            return null;
        }
        for (XmlAttribute attr : tag.getAttributes()) {
            if ("name".equalsIgnoreCase(attr.getName())) {
                return normalizeWebObjectName(attr);
            }
        }
        return null;
    }

    /**
     * Extracts and normalizes the WebObject name from a name XmlAttribute.
     */
    public static @Nullable String normalizeWebObjectName(@Nullable XmlAttribute nameAttr) {
        if (nameAttr == null) {
            return null;
        }
        String val = nameAttr.getValue();
        if (val == null || val.isBlank()) {
            XmlAttributeValue valElem = nameAttr.getValueElement();
            if (valElem != null) {
                val = valElem.getValue();
                if (val == null || val.isBlank()) {
                    val = valElem.getText();
                }
            }
        }
        if (val == null || val.isBlank()) {
            String text = nameAttr.getText();
            int eq = text.indexOf('=');
            if (eq >= 0 && eq + 1 < text.length()) {
                val = text.substring(eq + 1);
            }
        }
        return cleanWebObjectName(val);
    }

    /**
     * Cleans an attribute value string:
     * - Strips leading/trailing quotes (single or double)
     * - Strips trailing self-closing slash from unquoted syntax (&lt;webobject name=ShortTermLoginButton/&gt;)
     * - Trims whitespace
     */
    public static @Nullable String cleanWebObjectName(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();

        // Strip trailing slash from self-closing tags like <webobject name=Foo/> or name="Foo"/
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1).trim();
        }

        // Strip outer quotes if any
        if (s.length() >= 2 && ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))) {
            s = s.substring(1, s.length() - 1).trim();
        } else {
            if (s.startsWith("\"") || s.startsWith("'")) {
                s = s.substring(1).trim();
            }
            if (s.endsWith("\"") || s.endsWith("'")) {
                s = s.substring(0, s.length() - 1).trim();
            }
        }

        // In case there was a trailing slash after inner content like "Foo/"
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1).trim();
        }

        return s.isBlank() ? null : s;
    }
}
