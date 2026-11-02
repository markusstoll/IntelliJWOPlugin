package org.wocommunity.plugins.intellij.wod;

import com.intellij.lexer.Lexer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.wocommunity.plugins.intellij.psi.wod.WODTypes.*;

class WODLexerTest {
    @Test
    void lexesBooleanLiterals() {
        Lexer lexer = new WODLexerAdapter();
        lexer.start("true;false;YES;NO;yes;no;TRUE;FALSE");

        assertEquals(BOOLEAN, lexer.getTokenType());
        assertEquals("true", lexer.getTokenText());
        lexer.advance();
        assertEquals(";", lexer.getTokenText());
        lexer.advance();
        assertEquals(BOOLEAN, lexer.getTokenType());
        assertEquals("false", lexer.getTokenText());
        lexer.advance();
        assertEquals(";", lexer.getTokenText());
        lexer.advance();
        assertEquals(BOOLEAN, lexer.getTokenType());
        assertEquals("YES", lexer.getTokenText());
        lexer.advance();
        assertEquals(";", lexer.getTokenText());
        lexer.advance();
        assertEquals(BOOLEAN, lexer.getTokenType());
        assertEquals("NO", lexer.getTokenText());
        lexer.advance();
        assertEquals(";", lexer.getTokenText());
        lexer.advance();
        assertEquals(BOOLEAN, lexer.getTokenType());
        assertEquals("yes", lexer.getTokenText());
        lexer.advance();
        assertEquals(";", lexer.getTokenText());
        lexer.advance();
        assertEquals(BOOLEAN, lexer.getTokenType());
        assertEquals("no", lexer.getTokenText());
        lexer.advance();
        assertEquals(";", lexer.getTokenText());
        lexer.advance();
        assertEquals(BOOLEAN, lexer.getTokenType());
        assertEquals("TRUE", lexer.getTokenText());
        lexer.advance();
        assertEquals(";", lexer.getTokenText());
        lexer.advance();
        assertEquals(BOOLEAN, lexer.getTokenType());
        assertEquals("FALSE", lexer.getTokenText());
    }

    @Test
    void keepsBooleanPrefixesInIdentifiers() {
        Lexer lexer = new WODLexerAdapter();
        lexer.start("trueValue");

        assertToken(lexer, "trueValue");
        assertNull(lexer.getTokenType());
    }

    @Test
    void distinguishesAssignmentCommentFromNormalComment() {
        Lexer lexer = new WODLexerAdapter();
        String input = "foo = \"bar\"; // same line\n// next line\nbaz = 1;";
        lexer.start(input);

        // foo
        assertEquals(IDENTIFIER, lexer.getTokenType());
        lexer.advance();
        // =
        lexer.advance(); // whitespace
        assertEquals(ASSIGN, lexer.getTokenType());
        lexer.advance();
        // "bar"
        lexer.advance(); // whitespace
        assertEquals(STRING, lexer.getTokenType());
        lexer.advance();
        // ;
        assertEquals(SEMI, lexer.getTokenType());
        lexer.advance();
        // whitespace
        lexer.advance();
        // // same line -> ASSIGNMENT_COMMENT
        assertEquals(ASSIGNMENT_COMMENT, lexer.getTokenType());
        assertEquals("// same line", lexer.getTokenText());
        lexer.advance();
        // whitespace (\n)
        lexer.advance();
        // // next line -> ASSIGNMENT_COMMENT
        assertEquals(ASSIGNMENT_COMMENT, lexer.getTokenType());
        assertEquals("// next line", lexer.getTokenText());
    }

    private static void assertToken(Lexer lexer, String text) {
        assertEquals(text, lexer.getTokenText());
        lexer.advance();
    }
}
