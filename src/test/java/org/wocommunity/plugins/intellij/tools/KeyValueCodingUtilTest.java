package org.wocommunity.plugins.intellij.tools;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class KeyValueCodingUtilTest {

    @Test
    public void testDecapitalize() {
        assertEquals("", KeyValueCodingUtil.decapitalize(""));
        assertEquals("currency", KeyValueCodingUtil.decapitalize("Currency"));
        assertEquals("c", KeyValueCodingUtil.decapitalize("C"));
        assertEquals("URL", KeyValueCodingUtil.decapitalize("URL"));
        assertEquals("XMLParser", KeyValueCodingUtil.decapitalize("XMLParser"));
    }

    @Test
    public void testExtractKvcPropertyName() {
        // Getters
        assertEquals("currency", KeyValueCodingUtil.extractKvcPropertyName("getCurrency"));
        assertEquals("URL", KeyValueCodingUtil.extractKvcPropertyName("getURL"));
        assertNull(KeyValueCodingUtil.extractKvcPropertyName("get"));
        assertNull(KeyValueCodingUtil.extractKvcPropertyName("getup")); // 'u' is not uppercase

        // Boolean is-getters
        assertEquals("available", KeyValueCodingUtil.extractKvcPropertyName("isAvailable"));
        assertNull(KeyValueCodingUtil.extractKvcPropertyName("is"));
        assertNull(KeyValueCodingUtil.extractKvcPropertyName("island"));

        // Setters
        assertEquals("currency", KeyValueCodingUtil.extractKvcPropertyName("setCurrency"));
        assertNull(KeyValueCodingUtil.extractKvcPropertyName("set"));

        // Private / internal getters & setters
        assertEquals("currency", KeyValueCodingUtil.extractKvcPropertyName("_getCurrency"));
        assertEquals("available", KeyValueCodingUtil.extractKvcPropertyName("_isAvailable"));
        assertEquals("currency", KeyValueCodingUtil.extractKvcPropertyName("_setCurrency"));

        // Backing fields
        assertEquals("currency", KeyValueCodingUtil.extractKvcPropertyName("_currency"));
        assertNull(KeyValueCodingUtil.extractKvcPropertyName("_"));
        assertNull(KeyValueCodingUtil.extractKvcPropertyName("__doubleUnderscore"));

        // Normal methods / fields should return null
        assertNull(KeyValueCodingUtil.extractKvcPropertyName("currency"));
        assertNull(KeyValueCodingUtil.extractKvcPropertyName("calculateTotal"));
    }

    @Test
    public void testComputeRenamedKeyForHtmlTemplate() {
        // Getter renamed -> KVC property name in HTML
        assertEquals("$euro", KeyValueCodingUtil.computeRenamedKey("$currency", "getEuro"));

        // Method call with parens and getter name preserved
        assertEquals("$getEuro()", KeyValueCodingUtil.computeRenamedKey("$getCurrency()", "getEuro"));

        // Method call with parens without getter
        assertEquals("$euro()", KeyValueCodingUtil.computeRenamedKey("$currency()", "euro"));

        // Normal method renamed
        assertEquals("$submitAction", KeyValueCodingUtil.computeRenamedKey("$saveAction", "submitAction"));
        assertEquals("$submitAction()", KeyValueCodingUtil.computeRenamedKey("$saveAction()", "submitAction"));

        // Boolean getter renamed
        assertEquals("$enabled", KeyValueCodingUtil.computeRenamedKey("$available", "isEnabled"));
        assertEquals("$isEnabled()", KeyValueCodingUtil.computeRenamedKey("$isAvailable()", "isEnabled"));

        // Setter renamed
        assertEquals("$euro", KeyValueCodingUtil.computeRenamedKey("$currency", "setEuro"));

        // Underscore field renamed
        assertEquals("$euro", KeyValueCodingUtil.computeRenamedKey("$currency", "_euro"));
    }

    @Test
    public void testComputeRenamedKeyForWod() {
        // Getter renamed -> KVC property name in WOD
        assertEquals("euro", KeyValueCodingUtil.computeRenamedKey("currency", "getEuro"));

        // Setter renamed -> KVC property name in WOD
        assertEquals("euro", KeyValueCodingUtil.computeRenamedKey("currency", "setEuro"));

        // Boolean getter renamed -> KVC property name in WOD
        assertEquals("enabled", KeyValueCodingUtil.computeRenamedKey("available", "isEnabled"));

        // Explicit method call preserved
        assertEquals("getEuro", KeyValueCodingUtil.computeRenamedKey("getCurrency", "getEuro"));
        assertEquals("isEnabled", KeyValueCodingUtil.computeRenamedKey("isAvailable", "isEnabled"));

        // Normal property renamed
        assertEquals("euro", KeyValueCodingUtil.computeRenamedKey("currency", "euro"));
    }
}
