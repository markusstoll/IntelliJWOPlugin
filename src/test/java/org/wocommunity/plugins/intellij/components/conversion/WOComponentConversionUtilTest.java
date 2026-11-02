package org.wocommunity.plugins.intellij.components.conversion;

import org.junit.jupiter.api.Test;
import org.wocommunity.plugins.intellij.wod.WodModel;
import org.wocommunity.plugins.intellij.wod.WodParser;

import static org.junit.jupiter.api.Assertions.*;

public class WOComponentConversionUtilTest {

    @Test
    public void testResolveTypeName() {
        assertEquals("WOConditional", WOComponentConversionUtil.resolveTypeName("if"));
        assertEquals("WOConditional", WOComponentConversionUtil.resolveTypeName("If"));
        assertEquals("WOConditional", WOComponentConversionUtil.resolveTypeName("conditional"));
        assertEquals("WOConditional", WOComponentConversionUtil.resolveTypeName("WOConditional"));
        assertEquals("WOString", WOComponentConversionUtil.resolveTypeName("str"));
        assertEquals("WOString", WOComponentConversionUtil.resolveTypeName("string"));
        assertEquals("WORepetition", WOComponentConversionUtil.resolveTypeName("foreach"));
        assertEquals("WORepetition", WOComponentConversionUtil.resolveTypeName("repeat"));
        assertEquals("WORepetition", WOComponentConversionUtil.resolveTypeName("repetition"));
        assertEquals("WOSubmitButton", WOComponentConversionUtil.resolveTypeName("submit"));
        assertEquals("WOSubmitButton", WOComponentConversionUtil.resolveTypeName("submitButton"));
        assertEquals("WOForm", WOComponentConversionUtil.resolveTypeName("form"));
        assertEquals("ERXLocalizedString", WOComponentConversionUtil.resolveTypeName("localized"));
        assertEquals("ERXElse", WOComponentConversionUtil.resolveTypeName("else"));
        assertEquals("MyCustomComponent", WOComponentConversionUtil.resolveTypeName("MyCustomComponent"));
    }

    @Test
    public void testResolveInlineTagName() {
        assertEquals("if", WOComponentConversionUtil.resolveInlineTagName("WOConditional"));
        assertEquals("str", WOComponentConversionUtil.resolveInlineTagName("WOString"));
        assertEquals("repeat", WOComponentConversionUtil.resolveInlineTagName("WORepetition"));
        assertEquals("submit", WOComponentConversionUtil.resolveInlineTagName("WOSubmitButton"));
        assertEquals("form", WOComponentConversionUtil.resolveInlineTagName("WOForm"));
        assertEquals("else", WOComponentConversionUtil.resolveInlineTagName("ERXElse"));
        assertEquals("localized", WOComponentConversionUtil.resolveInlineTagName("ERXLocalizedString"));
        assertEquals("content", WOComponentConversionUtil.resolveInlineTagName("WOComponentContent"));
        assertEquals("MyCustomComponent", WOComponentConversionUtil.resolveInlineTagName("MyCustomComponent"));
    }

    @Test
    public void testConvertToWodSimple() {
        String html = """
                <div>
                    <wo:if condition="$isLoggedIn">
                        <span>Hello <wo:str value="$userName"/></span>
                    </wo:if>
                </div>
                """;
        String wod = "";

        var result = WOComponentConversionUtil.convertToWod(html, wod);

        assertEquals(2, result.getConvertedCount());
        assertTrue(result.getHtmlText().contains("<webobject name=\"WOConditional1\">"));
        assertTrue(result.getHtmlText().contains("</webobject>"));
        assertTrue(result.getHtmlText().contains("<webobject name=\"WOString1\"/>"));

        WodModel model = WodParser.parse(result.getWodText());
        assertEquals(2, model.getElements().size());
        assertTrue(model.hasElement("WOConditional1"));
        assertTrue(model.hasElement("WOString1"));

        var cond = model.getElement("WOConditional1");
        assertNotNull(cond);
        assertEquals("WOConditional", cond.getTypeName());
        assertEquals("isLoggedIn", cond.getBinding("condition").getValue());
        assertFalse(cond.getBinding("condition").isLiteralString());

        var str = model.getElement("WOString1");
        assertNotNull(str);
        assertEquals("WOString", str.getTypeName());
        assertEquals("userName", str.getBinding("value").getValue());
    }

    @Test
    public void testConvertToWodAvoidDuplicateNames() {
        String html = "<wo:if condition=\"$cond2\"><wo:str value=\"Test\"/></wo:if>";
        String existingWod = """
                WOConditional1 : WOConditional {
                    condition = cond1;
                }
                """;

        var result = WOComponentConversionUtil.convertToWod(html, existingWod);

        assertEquals(2, result.getConvertedCount());
        assertTrue(result.getHtmlText().contains("<webobject name=\"WOConditional2\">"));
        assertTrue(result.getHtmlText().contains("<webobject name=\"WOString1\"/>"));

        WodModel model = WodParser.parse(result.getWodText());
        assertEquals(3, model.getElements().size());
        assertTrue(model.hasElement("WOConditional1"));
        assertTrue(model.hasElement("WOConditional2"));
        assertTrue(model.hasElement("WOString1"));

        var str = model.getElement("WOString1");
        assertNotNull(str);
        assertTrue(str.getBinding("value").isLiteralString());
        assertEquals("\"Test\"", str.getBinding("value").getValue());
    }

    @Test
    public void testConvertToInlineSimple() {
        String html = """
                <div>
                    <webobject name="Conditional1">
                        <span>Hello <webobject name="String1"/></span>
                    </webobject>
                </div>
                """;
        String wod = """
                Conditional1 : WOConditional {
                    condition = isLoggedIn;
                    negate = false;
                }

                String1 : WOString {
                    value = userName;
                }
                """;

        var result = WOComponentConversionUtil.convertToInline(html, wod);

        assertEquals(2, result.getConvertedCount());
        assertTrue(result.getHtmlText().contains("<wo:if condition=\"$isLoggedIn\" negate=\"$false\">"));
        assertTrue(result.getHtmlText().contains("</wo:if>"));
        assertTrue(result.getHtmlText().contains("<wo:str value=\"$userName\"/>"));

        WodModel model = WodParser.parse(result.getWodText());
        assertEquals(0, model.getElements().size());
    }

    @Test
    public void testRoundTrip() {
        String originalHtml = """
                <div>
                    <wo:if condition="$isLoggedIn">
                        <span>Welcome <wo:str value="Guest"/></span>
                    </wo:if>
                </div>
                """;
        String emptyWod = "";

        // 1. Convert to WOD
        var toWod = WOComponentConversionUtil.convertToWod(originalHtml, emptyWod);
        assertEquals(2, toWod.getConvertedCount());

        // 2. Convert back to Inline
        var toInline = WOComponentConversionUtil.convertToInline(toWod.getHtmlText(), toWod.getWodText());
        assertEquals(2, toInline.getConvertedCount());

        assertTrue(toInline.getHtmlText().contains("<wo:if condition=\"$isLoggedIn\">"));
        assertTrue(toInline.getHtmlText().contains("<wo:str value=\"Guest\"/>"));
        assertEquals("", toInline.getWodText().trim());
    }

    @Test
    public void testCommentsIgnoredInHtml() {
        String html = """
                <!-- <wo:if condition="$test"><wo:str value="$hidden"/></wo:if> -->
                <wo:str value="$visible"/>
                """;
        var result = WOComponentConversionUtil.convertToWod(html, "");
        assertEquals(1, result.getConvertedCount());
        assertTrue(result.getHtmlText().contains("<!-- <wo:if condition=\"$test\"><wo:str value=\"$hidden\"/></wo:if> -->"));
        assertTrue(result.getHtmlText().contains("<webobject name=\"WOString1\"/>"));
    }

    @Test
    public void testDeeplyNestedAndSameTypeTags() {
        String html = """
                <wo:if condition="$c1">
                    <wo:if condition="$c2">
                        <wo:if condition="$c3">
                            <wo:str value="Leaf"/>
                        </wo:if>
                    </wo:if>
                </wo:if>
                """;
        var result = WOComponentConversionUtil.convertToWod(html, "");
        assertEquals(4, result.getConvertedCount());
        assertTrue(result.getHtmlText().contains("<webobject name=\"WOConditional1\">"));
        assertTrue(result.getHtmlText().contains("<webobject name=\"WOConditional2\">"));
        assertTrue(result.getHtmlText().contains("<webobject name=\"WOConditional3\">"));
        assertTrue(result.getHtmlText().contains("<webobject name=\"WOString1\"/>"));

        WodModel model = WodParser.parse(result.getWodText());
        assertEquals(4, model.getElements().size());
        assertEquals("c1", model.getElement("WOConditional1").getBinding("condition").getValue());
        assertEquals("c2", model.getElement("WOConditional2").getBinding("condition").getValue());
        assertEquals("c3", model.getElement("WOConditional3").getBinding("condition").getValue());
    }

    @Test
    public void testPreserveUnrelatedDeclarationsWhenConvertingToInline() {
        String html = "<webobject name=\"String1\"/>";
        String wod = """
                UnrelatedDeclaration : WOConditional {
                    condition = true;
                }

                String1 : WOString {
                    value = "Hello";
                }
                """;

        var result = WOComponentConversionUtil.convertToInline(html, wod);
        assertEquals(1, result.getConvertedCount());
        assertEquals("<wo:str value=\"Hello\"/>", result.getHtmlText().trim());

        WodModel model = WodParser.parse(result.getWodText());
        assertEquals(1, model.getElements().size());
        assertTrue(model.hasElement("UnrelatedDeclaration"));
        assertFalse(model.hasElement("String1"));
    }

    @Test
    public void testOgnlExpressionQuoting() {
        String wod = """
                ReservedWidthValue : WOString {
                    value = "~(hasExtraLargeLoginFields? \\"335\\" : \\"215\\")";
                }
                """;
        String html = "<webobject name=\"ReservedWidthValue\"/>";

        // Convert WOD to Inline
        var inlineResult = WOComponentConversionUtil.convertToInline(html, wod);
        assertEquals(1, inlineResult.getConvertedCount());
        // Must use single quotes around the attribute so internal double quotes are not escaped to &quot;
        assertEquals("<wo:str value='~(hasExtraLargeLoginFields? \"335\" : \"215\")'/>", inlineResult.getHtmlText().trim());
        assertEquals("", inlineResult.getWodText().trim());

        // Convert back to WOD
        var wodResult = WOComponentConversionUtil.convertToWod(inlineResult.getHtmlText(), "");
        assertEquals(1, wodResult.getConvertedCount());
        assertEquals("<webobject name=\"WOString1\"/>", wodResult.getHtmlText().trim());

        WodModel model = WodParser.parse(wodResult.getWodText());
        var str = model.getElement("WOString1");
        assertNotNull(str);
        assertTrue(str.getBinding("value").isLiteralString());
        assertEquals("\"~(hasExtraLargeLoginFields? \\\"335\\\" : \\\"215\\\")\"", str.getBinding("value").getValue());
    }

    @Test
    public void testAutoRepairBrokenEntityQuotesInHtml() {
        String html = "<wo:WOString value=\"~(hasExtraLargeLoginFields? &quot;335&quot; : &quot;215&quot;)\"/>";

        var wodResult = WOComponentConversionUtil.convertToWod(html, "");
        assertEquals(1, wodResult.getConvertedCount());

        WodModel model = WodParser.parse(wodResult.getWodText());
        var str = model.getElement("WOString1");
        assertNotNull(str);
        assertEquals("\"~(hasExtraLargeLoginFields? \\\"335\\\" : \\\"215\\\")\"", str.getBinding("value").getValue());

        // Now convert back to Inline - should produce clean single quotes without &quot;
        var inlineResult = WOComponentConversionUtil.convertToInline(wodResult.getHtmlText(), wodResult.getWodText());
        assertEquals("<wo:str value='~(hasExtraLargeLoginFields? \"335\" : \"215\")'/>", inlineResult.getHtmlText().trim());
    }
}
