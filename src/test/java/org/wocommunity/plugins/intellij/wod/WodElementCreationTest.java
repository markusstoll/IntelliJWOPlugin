package org.wocommunity.plugins.intellij.wod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WodElementCreationTest {

    @Test
    public void testAppendSnippetFormatting() {
        String initialEmpty = "";
        String elementName = "MyHeader";
        String defaultType = "WOComponent";

        String snippetEmpty = formatSnippet(initialEmpty, elementName, defaultType);
        assertEquals("MyHeader : WOComponent {\n}\n", snippetEmpty);

        String initialWithNewline = "Foo : WOString {\n}\n";
        String snippetNewline = formatSnippet(initialWithNewline, elementName, defaultType);
        assertEquals("\nMyHeader : WOComponent {\n}\n", snippetNewline);

        String initialWithTwoNewlines = "Foo : WOString {\n}\n\n";
        String snippetTwoNewlines = formatSnippet(initialWithTwoNewlines, elementName, defaultType);
        assertEquals("MyHeader : WOComponent {\n}\n", snippetTwoNewlines);

        String initialWithoutNewline = "Foo : WOString {}";
        String snippetWithoutNewline = formatSnippet(initialWithoutNewline, elementName, defaultType);
        assertEquals("\n\nMyHeader : WOComponent {\n}\n", snippetWithoutNewline);
    }

    @Test
    public void testCreatedElementParsesCleanly() {
        String existing = """
                Old1 : WOString {
                    value = "hello";
                }
                """;
        String snippet = formatSnippet(existing, "NewField", "WOComponent");
        String combined = existing + snippet;

        WodModel model = WodParser.parse(combined);
        assertTrue(model.hasElement("Old1"));
        assertTrue(model.hasElement("NewField"));

        WodElement created = model.getElement("NewField");
        assertNotNull(created);
        assertEquals("NewField", created.getName());
        assertEquals("WOComponent", created.getTypeName());
        assertTrue(created.getBindings().isEmpty());
        assertFalse(created.hasBinding("condition"));
    }

    @Test
    public void testSystemRequiredBindings() {
        var condReq = org.wocommunity.plugins.intellij.wotemplate.WOSystemBindingDefinitions.getRequiredBindingsForShortClassName("WOConditional");
        assertTrue(condReq.contains("condition"));

        var strReq = org.wocommunity.plugins.intellij.wotemplate.WOSystemBindingDefinitions.getRequiredBindingsForShortClassName("WOString");
        assertTrue(strReq.contains("value"));
    }

    private static String formatSnippet(String currentText, String elementName, String defaultTypeName) {
        String prefix;
        if (currentText.isEmpty()) {
            prefix = "";
        } else if (currentText.endsWith("\n\n")) {
            prefix = "";
        } else if (currentText.endsWith("\n")) {
            prefix = "\n";
        } else {
            prefix = "\n\n";
        }
        return prefix + elementName + " : " + defaultTypeName + " {\n}\n";
    }
}

