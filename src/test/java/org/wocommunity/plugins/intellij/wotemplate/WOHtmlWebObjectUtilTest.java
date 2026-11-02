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
}

