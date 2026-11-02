package org.wocommunity.plugins.intellij.components.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WOComponentApiXmlParserTest {

    @Test
    public void testParseUserExampleXml() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<wodefinitions>\n" +
                "    <wo class=\"P17_Main\" wocomponentcontent=\"true\">\n" +
                "        <binding name=\"loginPageMode\"/>\n" +
                "        <binding defaults=\"Boolean\" name=\"stageName\"/>\n" +
                "        <validation message=\"'stageName' is a required binding.\">\n" +
                "            <unbound name=\"stageName\"/>\n" +
                "        </validation>\n" +
                "        <validation message=\"'stageName' must be bound to a settable value\">\n" +
                "            <unsettable name=\"stageName\"/>\n" +
                "        </validation>\n" +
                "    </wo>\n" +
                "</wodefinitions>";

        WOComponentApiModel model = WOComponentApiXmlParser.parse(xml);
        assertNotNull(model);
        assertEquals("P17_Main", model.getComponentClassName());
        assertTrue(model.isComponentContent());
        assertEquals(2, model.getBindings().size());

        WOComponentApiBinding b1 = model.getBindings().get(0);
        assertEquals("loginPageMode", b1.getName());
        assertNull(b1.getDefaults());
        assertFalse(b1.isRequired());
        assertFalse(b1.isWillSet());

        WOComponentApiBinding b2 = model.getBindings().get(1);
        assertEquals("stageName", b2.getName());
        assertEquals("Boolean", b2.getDefaults());
        assertTrue(b2.isRequired());
        assertTrue(b2.isWillSet());
        assertEquals("'stageName' is a required binding.", b2.getRequiredMessage());
    }

    @Test
    public void testRoundtripXmlGeneration() {
        WOComponentApiModel model = new WOComponentApiModel("MyComp", true);
        model.addBinding(new WOComponentApiBinding("loginPageMode", null, false, false));
        model.addBinding(new WOComponentApiBinding("stageName", "Boolean", true, true));

        String generated = WOComponentApiXmlParser.toXml(model);
        assertTrue(generated.contains("class=\"MyComp\""));
        assertTrue(generated.contains("wocomponentcontent=\"true\""));
        assertTrue(generated.contains("<binding name=\"loginPageMode\"/>"));
        assertTrue(generated.contains("<binding defaults=\"Boolean\" name=\"stageName\"/>"));
        assertTrue(generated.contains("<unbound name=\"stageName\"/>"));
        assertTrue(generated.contains("<unsettable name=\"stageName\"/>"));
        assertTrue(generated.contains("message=\"'stageName' is a required binding.\""));

        // Parse it back and verify equality
        WOComponentApiModel parsed = WOComponentApiXmlParser.parse(generated);
        assertEquals(model.getComponentClassName(), parsed.getComponentClassName());
        assertEquals(model.isComponentContent(), parsed.isComponentContent());
        assertEquals(model.getBindings().size(), parsed.getBindings().size());
        assertEquals(model.getBindings().get(0), parsed.getBindings().get(0));
        assertEquals(model.getBindings().get(1), parsed.getBindings().get(1));
    }

    @Test
    public void testEmptyOrInvalidXml() {
        WOComponentApiModel model = WOComponentApiXmlParser.parse("");
        assertNotNull(model);
        assertEquals("", model.getComponentClassName());
        assertFalse(model.isComponentContent());
        assertTrue(model.getBindings().isEmpty());

        WOComponentApiModel model2 = WOComponentApiXmlParser.parse("<invalid>xml</invalid>");
        assertNotNull(model2);
        assertTrue(model2.getBindings().isEmpty());
    }

    @Test
    public void testPreserveExtraValidations() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<wodefinitions>\n" +
                "    <wo class=\"TestComp\" wocomponentcontent=\"false\">\n" +
                "        <binding name=\"myBinding\"/>\n" +
                "        <validation message=\"Custom validation rule\">\n" +
                "            <count min=\"1\"/>\n" +
                "        </validation>\n" +
                "    </wo>\n" +
                "</wodefinitions>";

        WOComponentApiModel model = WOComponentApiXmlParser.parse(xml);
        assertEquals(1, model.getBindings().size());
        assertEquals(1, model.getExtraValidationsXml().size());
        assertTrue(model.getExtraValidationsXml().get(0).contains("Custom validation rule"));

        String reserialized = WOComponentApiXmlParser.toXml(model);
        assertTrue(reserialized.contains("Custom validation rule"));
        assertTrue(reserialized.contains("<count min=\"1\"/>"));
    }
}
