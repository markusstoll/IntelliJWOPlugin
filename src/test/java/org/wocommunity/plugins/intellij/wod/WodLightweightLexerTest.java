package org.wocommunity.plugins.intellij.wod;

import com.intellij.psi.tree.IElementType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WodLightweightLexerTest {

    private record Token(IElementType type, String text) {}

    private List<Token> tokenize(String input) {
        WodLexer lexer = new WodLexer();
        lexer.start(input, 0, input.length(), 0);
        List<Token> tokens = new ArrayList<>();
        while (lexer.getTokenType() != null) {
            String text = input.substring(lexer.getTokenStart(), lexer.getTokenEnd());
            tokens.add(new Token(lexer.getTokenType(), text));
            lexer.advance();
        }
        return tokens;
    }

    @Test
    public void testBasicWodLexing() {
        String wod = """
                // Component definition
                Conditional1 : WOConditional {
                    condition = hasExtraLargeLoginFields;
                    negate = false;
                    label = "Sign in";
                    count = 42;
                }
                """;

        List<Token> tokens = tokenize(wod);
        List<Token> nonWs = tokens.stream()
                .filter(t -> t.type != WodTokenTypes.WHITE_SPACE)
                .toList();

        assertEquals(WodTokenTypes.LINE_COMMENT, nonWs.get(0).type);
        assertEquals("// Component definition", nonWs.get(0).text);

        assertEquals(WodTokenTypes.ELEMENT_NAME, nonWs.get(1).type);
        assertEquals("Conditional1", nonWs.get(1).text);

        assertEquals(WodTokenTypes.COLON, nonWs.get(2).type);

        assertEquals(WodTokenTypes.TYPE_NAME, nonWs.get(3).type);
        assertEquals("WOConditional", nonWs.get(3).text);

        assertEquals(WodTokenTypes.LBRACE, nonWs.get(4).type);

        assertEquals(WodTokenTypes.BINDING_KEY, nonWs.get(5).type);
        assertEquals("condition", nonWs.get(5).text);

        assertEquals(WodTokenTypes.EQUALS, nonWs.get(6).type);

        assertEquals(WodTokenTypes.CODE_VALUE, nonWs.get(7).type);
        assertEquals("hasExtraLargeLoginFields", nonWs.get(7).text);

        assertEquals(WodTokenTypes.SEMICOLON, nonWs.get(8).type);

        assertEquals(WodTokenTypes.BINDING_KEY, nonWs.get(9).type);
        assertEquals("negate", nonWs.get(9).text);

        assertEquals(WodTokenTypes.EQUALS, nonWs.get(10).type);

        assertEquals(WodTokenTypes.BOOLEAN_LITERAL, nonWs.get(11).type);
        assertEquals("false", nonWs.get(11).text);

        assertEquals(WodTokenTypes.SEMICOLON, nonWs.get(12).type);

        assertEquals(WodTokenTypes.BINDING_KEY, nonWs.get(13).type);
        assertEquals("label", nonWs.get(13).text);

        assertEquals(WodTokenTypes.EQUALS, nonWs.get(14).type);

        assertEquals(WodTokenTypes.STRING_LITERAL, nonWs.get(15).type);
        assertEquals("\"Sign in\"", nonWs.get(15).text);

        assertEquals(WodTokenTypes.SEMICOLON, nonWs.get(16).type);

        assertEquals(WodTokenTypes.BINDING_KEY, nonWs.get(17).type);
        assertEquals("count", nonWs.get(17).text);

        assertEquals(WodTokenTypes.EQUALS, nonWs.get(18).type);

        assertEquals(WodTokenTypes.NUMBER_LITERAL, nonWs.get(19).type);
        assertEquals("42", nonWs.get(19).text);

        assertEquals(WodTokenTypes.SEMICOLON, nonWs.get(20).type);

        assertEquals(WodTokenTypes.RBRACE, nonWs.get(21).type);
    }
}
