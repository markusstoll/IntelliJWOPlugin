package org.wocommunity.plugins.intellij.wod;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;

public final class WodTokenTypes {
    private WodTokenTypes() {
    }

    public static final IFileElementType FILE = new IFileElementType(WODLanguage.INSTANCE);

    public static final IElementType ELEMENT_NAME = new WodElementType("WOD_ELEMENT_NAME");
    public static final IElementType TYPE_NAME = new WodElementType("WOD_TYPE_NAME");
    public static final IElementType BINDING_KEY = new WodElementType("WOD_BINDING_KEY");
    public static final IElementType CODE_VALUE = new WodElementType("WOD_CODE_VALUE");
    public static final IElementType STRING_LITERAL = new WodElementType("WOD_STRING_LITERAL");
    public static final IElementType NUMBER_LITERAL = new WodElementType("WOD_NUMBER_LITERAL");
    public static final IElementType BOOLEAN_LITERAL = new WodElementType("WOD_BOOLEAN_LITERAL");

    public static final IElementType COLON = new WodElementType("WOD_COLON");
    public static final IElementType EQUALS = new WodElementType("WOD_EQUALS");
    public static final IElementType SEMICOLON = new WodElementType("WOD_SEMICOLON");
    public static final IElementType LBRACE = new WodElementType("WOD_LBRACE");
    public static final IElementType RBRACE = new WodElementType("WOD_RBRACE");

    public static final IElementType LINE_COMMENT = new WodElementType("WOD_LINE_COMMENT");
    public static final IElementType BLOCK_COMMENT = new WodElementType("WOD_BLOCK_COMMENT");
    public static final IElementType BAD_CHARACTER = TokenType.BAD_CHARACTER;
    public static final IElementType WHITE_SPACE = TokenType.WHITE_SPACE;

    public static final TokenSet COMMENTS = TokenSet.create(LINE_COMMENT, BLOCK_COMMENT);
    public static final TokenSet STRINGS = TokenSet.create(STRING_LITERAL);
    public static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);

    public static class WodElementType extends IElementType {
        public WodElementType(String debugName) {
            super(debugName, WODLanguage.INSTANCE);
        }
    }
}
