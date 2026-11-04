package org.wocommunity.plugins.intellij.wotemplate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WOHtmlWebObjectUtilTest {

    @Test
    public void testCleanWebObjectName() {
        // Plain identifier
        assertEquals("ShortTermLoginButton", WOHtmlWebObjectUtil.cleanWebObjectName("ShortTermLoginButton"));

        // Quoted variants
        assertEquals("ShortTermLoginButton", WOHtmlWebObjectUtil.cleanWebObjectName("\"ShortTermLoginButton\""));
        assertEquals("ShortTermLoginButton", WOHtmlWebObjectUtil.cleanWebObjectName("'ShortTermLoginButton'"));

        // Unquoted with self-closing trailing slash (e.g. <webobject name=ShortTermLoginButton/>)
        assertEquals("ShortTermLoginButton", WOHtmlWebObjectUtil.cleanWebObjectName("ShortTermLoginButton/"));
        assertEquals("ShortTermLoginButton", WOHtmlWebObjectUtil.cleanWebObjectName("ShortTermLoginButton /"));

        // Quoted with trailing slash (e.g. <webobject name="ShortTermLoginButton"/>)
        assertEquals("ShortTermLoginButton", WOHtmlWebObjectUtil.cleanWebObjectName("\"ShortTermLoginButton\"/"));
        assertEquals("ShortTermLoginButton", WOHtmlWebObjectUtil.cleanWebObjectName("\"ShortTermLoginButton\" /"));

        // Whitespace handling
        assertEquals("ShortTermLoginButton", WOHtmlWebObjectUtil.cleanWebObjectName("  ShortTermLoginButton  "));
        assertEquals("ShortTermLoginButton", WOHtmlWebObjectUtil.cleanWebObjectName("  \"ShortTermLoginButton\"  "));

        // Null and empty cases
        assertNull(WOHtmlWebObjectUtil.cleanWebObjectName(null));
        assertNull(WOHtmlWebObjectUtil.cleanWebObjectName(""));
        assertNull(WOHtmlWebObjectUtil.cleanWebObjectName("   "));
        assertNull(WOHtmlWebObjectUtil.cleanWebObjectName("/"));
        assertNull(WOHtmlWebObjectUtil.cleanWebObjectName("\"\""));
        assertNull(WOHtmlWebObjectUtil.cleanWebObjectName("''"));
    }

    @Test
    public void testIsSelfClosingSyntax() {
        // 1. Standard empty tag (tag.isEmpty() == true)
        assertTrue(WOHtmlInspectionSuppressor.isSelfClosingSyntax(true, "<webobject name=\"foo\"/>"));

        // 2. Unquoted self-closing without space: <webobject name=ShortTermLoginButton/>
        assertTrue(WOHtmlInspectionSuppressor.isSelfClosingSyntax(false, "<webobject name=ShortTermLoginButton/>", "ShortTermLoginButton/", "name=ShortTermLoginButton/"));

        // 3. Unquoted self-closing without space when followed by more content in the document
        assertTrue(WOHtmlInspectionSuppressor.isSelfClosingSyntax(false, "<webobject name=ShortTermLoginButton/>\n<div>test</div>", "ShortTermLoginButton/", "name=ShortTermLoginButton/"));

        // 4. Unquoted self-closing with space
        assertTrue(WOHtmlInspectionSuppressor.isSelfClosingSyntax(true, "<webobject name=ShortTermLoginButton />"));

        // 5. Container tag <webobject name=MyRepetition> ... </webobject>
        assertFalse(WOHtmlInspectionSuppressor.isSelfClosingSyntax(false, "<webobject name=MyRepetition>\n<tr></tr>\n</webobject>", "MyRepetition", "name=MyRepetition"));

        // 6. Inline wo-tag without space: <wo:String value=myVal/>
        assertTrue(WOHtmlInspectionSuppressor.isSelfClosingSyntax(false, "<wo:String value=myVal/>", "myVal/", "value=myVal/"));
    }
}

