package org.wocommunity.plugins.intellij.wod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WodParserTest {

    @Test
    public void testParseWodModel() {
        String wodText = """
                /* WOD file */
                Conditional1 : WOConditional {
                    condition = hasExtraLargeLoginFields;
                    negate = false;
                }

                SubmitBtn : WOSubmitButton {
                    action = saveAction;
                    value = "Save Changes";
                }
                """;

        WodModel model = WodParser.parse(wodText);

        assertEquals(2, model.getElements().size());
        assertTrue(model.hasElement("Conditional1"));
        assertTrue(model.hasElement("SubmitBtn"));
        assertFalse(model.hasElement("NonExisting"));

        // Element 1
        WodElement cond = model.getElement("Conditional1");
        assertNotNull(cond);
        assertEquals("Conditional1", cond.getName());
        assertEquals("WOConditional", cond.getTypeName());
        assertNotNull(cond.getNameRange());
        assertNotNull(cond.getTypeRange());
        assertEquals("Conditional1", wodText.substring(cond.getNameRange().getStartOffset(), cond.getNameRange().getEndOffset()));
        assertEquals("WOConditional", wodText.substring(cond.getTypeRange().getStartOffset(), cond.getTypeRange().getEndOffset()));

        assertEquals(2, cond.getBindings().size());
        WodBinding bCond = cond.getBinding("condition");
        assertNotNull(bCond);
        assertEquals("condition", bCond.getKey());
        assertEquals("hasExtraLargeLoginFields", bCond.getValue());
        assertFalse(bCond.isLiteralString());

        WodBinding bNeg = cond.getBinding("negate");
        assertNotNull(bNeg);
        assertEquals("negate", bNeg.getKey());
        assertEquals("false", bNeg.getValue());
        assertFalse(bNeg.isLiteralString());

        // Element 2
        WodElement btn = model.getElement("SubmitBtn");
        assertNotNull(btn);
        assertEquals("SubmitBtn", btn.getName());
        assertEquals("WOSubmitButton", btn.getTypeName());

        WodBinding bAction = btn.getBinding("action");
        assertNotNull(bAction);
        assertEquals("action", bAction.getKey());
        assertEquals("saveAction", bAction.getValue());
        assertFalse(bAction.isLiteralString());

        WodBinding bVal = btn.getBinding("value");
        assertNotNull(bVal);
        assertEquals("value", bVal.getKey());
        assertEquals("\"Save Changes\"", bVal.getValue());
        assertTrue(bVal.isLiteralString());
    }

    @Test
    public void testParseTrailingSemicolonAndBooleanNO() {
        String wodText = """
                Frachtkostenhinweis: WOString
                {
                	value = session.localizer.AnfrageAuftrag_Frachtkostenhinweis;
                	escapeHTML = NO;
                };
                """;

        WodModel model = WodParser.parse(wodText);
        assertEquals(1, model.getElements().size());
        WodElement elem = model.getElement("Frachtkostenhinweis");
        assertNotNull(elem);
        assertEquals("WOString", elem.getTypeName());
        assertEquals(2, elem.getBindings().size());
        assertEquals("NO", elem.getBinding("escapeHTML").getValue());
        assertEquals(wodText.trim().length(), elem.getTotalRange().getEndOffset());
    }
}
