package org.wocommunity.plugins.intellij.wod;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WodLexer extends LexerBase {

    private static final int STATE_EXPECT_ELEMENT_NAME = 0;
    private static final int STATE_EXPECT_COLON = 1;
    private static final int STATE_EXPECT_TYPE_NAME = 2;
    private static final int STATE_EXPECT_LBRACE = 3;
    private static final int STATE_INSIDE_BLOCK = 4;
    private static final int STATE_EXPECT_EQUALS = 5;
    private static final int STATE_EXPECT_VALUE = 6;
    private static final int STATE_EXPECT_SEMICOLON = 7;

    private CharSequence myBuffer;
    private int myStartOffset;
    private int myEndOffset;
    private int myCurrentOffset;
    private int myTokenStart;
    private int myTokenEnd;
    private IElementType myTokenType;
    private int myState;
    private int myBraceDepth;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        myBuffer = buffer;
        myStartOffset = startOffset;
        myEndOffset = endOffset;
        myCurrentOffset = startOffset;
        myState = (initialState & 0xFF);
        myBraceDepth = (initialState >> 8);
        advance();
    }

    @Override
    public int getState() {
        return (myState & 0xFF) | (myBraceDepth << 8);
    }

    @Override
    public @Nullable IElementType getTokenType() {
        return myTokenType;
    }

    @Override
    public int getTokenStart() {
        return myTokenStart;
    }

    @Override
    public int getTokenEnd() {
        return myTokenEnd;
    }

    @Override
    public @NotNull CharSequence getBufferSequence() {
        return myBuffer;
    }

    @Override
    public int getBufferEnd() {
        return myEndOffset;
    }

    @Override
    public void advance() {
        if (myCurrentOffset >= myEndOffset) {
            myTokenType = null;
            myTokenStart = myEndOffset;
            myTokenEnd = myEndOffset;
            return;
        }

        myTokenStart = myCurrentOffset;
        char c = myBuffer.charAt(myCurrentOffset);

        // 1. Whitespace
        if (Character.isWhitespace(c)) {
            while (myCurrentOffset < myEndOffset && Character.isWhitespace(myBuffer.charAt(myCurrentOffset))) {
                myCurrentOffset++;
            }
            myTokenEnd = myCurrentOffset;
            myTokenType = WodTokenTypes.WHITE_SPACE;
            return;
        }

        // 2. Comments
        if (c == '/' && myCurrentOffset + 1 < myEndOffset) {
            char next = myBuffer.charAt(myCurrentOffset + 1);
            if (next == '/') {
                // Line comment
                myCurrentOffset += 2;
                while (myCurrentOffset < myEndOffset && myBuffer.charAt(myCurrentOffset) != '\n' && myBuffer.charAt(myCurrentOffset) != '\r') {
                    myCurrentOffset++;
                }
                myTokenEnd = myCurrentOffset;
                myTokenType = WodTokenTypes.LINE_COMMENT;
                return;
            } else if (next == '*') {
                // Block comment
                myCurrentOffset += 2;
                while (myCurrentOffset + 1 < myEndOffset) {
                    if (myBuffer.charAt(myCurrentOffset) == '*' && myBuffer.charAt(myCurrentOffset + 1) == '/') {
                        myCurrentOffset += 2;
                        break;
                    }
                    myCurrentOffset++;
                }
                myTokenEnd = myCurrentOffset;
                myTokenType = WodTokenTypes.BLOCK_COMMENT;
                return;
            }
        }

        // 3. Punctuation
        if (c == ':') {
            myCurrentOffset++;
            myTokenEnd = myCurrentOffset;
            myTokenType = WodTokenTypes.COLON;
            myState = STATE_EXPECT_TYPE_NAME;
            return;
        }
        if (c == '=') {
            myCurrentOffset++;
            myTokenEnd = myCurrentOffset;
            myTokenType = WodTokenTypes.EQUALS;
            myState = STATE_EXPECT_VALUE;
            return;
        }
        if (c == ';') {
            myCurrentOffset++;
            myTokenEnd = myCurrentOffset;
            myTokenType = WodTokenTypes.SEMICOLON;
            myState = myBraceDepth > 0 ? STATE_INSIDE_BLOCK : STATE_EXPECT_ELEMENT_NAME;
            return;
        }
        if (c == '{') {
            myCurrentOffset++;
            myTokenEnd = myCurrentOffset;
            myTokenType = WodTokenTypes.LBRACE;
            myBraceDepth++;
            myState = STATE_INSIDE_BLOCK;
            return;
        }
        if (c == '}') {
            myCurrentOffset++;
            myTokenEnd = myCurrentOffset;
            myTokenType = WodTokenTypes.RBRACE;
            if (myBraceDepth > 0) {
                myBraceDepth--;
            }
            myState = myBraceDepth > 0 ? STATE_INSIDE_BLOCK : STATE_EXPECT_ELEMENT_NAME;
            return;
        }

        // 4. String literals (double or single quotes)
        if (c == '"' || c == '\'') {
            char quote = c;
            myCurrentOffset++;
            boolean escaped = false;
            while (myCurrentOffset < myEndOffset) {
                char ch = myBuffer.charAt(myCurrentOffset);
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == quote) {
                    myCurrentOffset++;
                    break;
                }
                myCurrentOffset++;
            }
            myTokenEnd = myCurrentOffset;
            myTokenType = WodTokenTypes.STRING_LITERAL;
            if (myState == STATE_EXPECT_VALUE) {
                myState = STATE_EXPECT_SEMICOLON;
            }
            return;
        }

        // 5. Numbers
        if (Character.isDigit(c) && myState == STATE_EXPECT_VALUE) {
            while (myCurrentOffset < myEndOffset) {
                char ch = myBuffer.charAt(myCurrentOffset);
                if (Character.isDigit(ch) || ch == '.') {
                    myCurrentOffset++;
                } else {
                    break;
                }
            }
            myTokenEnd = myCurrentOffset;
            myTokenType = WodTokenTypes.NUMBER_LITERAL;
            myState = STATE_EXPECT_SEMICOLON;
            return;
        }

        // 6. Identifiers / Types / Words / Keypaths
        if (isIdentifierStart(c)) {
            while (myCurrentOffset < myEndOffset && isIdentifierPart(myBuffer.charAt(myCurrentOffset))) {
                myCurrentOffset++;
            }
            myTokenEnd = myCurrentOffset;
            String text = myBuffer.subSequence(myTokenStart, myTokenEnd).toString();

            if (myState == STATE_EXPECT_ELEMENT_NAME) {
                myTokenType = WodTokenTypes.ELEMENT_NAME;
                myState = STATE_EXPECT_COLON;
            } else if (myState == STATE_EXPECT_TYPE_NAME) {
                myTokenType = WodTokenTypes.TYPE_NAME;
                myState = STATE_EXPECT_LBRACE;
            } else if (myState == STATE_INSIDE_BLOCK) {
                myTokenType = WodTokenTypes.BINDING_KEY;
                myState = STATE_EXPECT_EQUALS;
            } else if (myState == STATE_EXPECT_VALUE) {
                if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text) ||
                        "YES".equals(text) || "NO".equals(text)) {
                    myTokenType = WodTokenTypes.BOOLEAN_LITERAL;
                } else {
                    myTokenType = WodTokenTypes.CODE_VALUE;
                }
                myState = STATE_EXPECT_SEMICOLON;
            } else {
                myTokenType = WodTokenTypes.CODE_VALUE;
            }
            return;
        }

        // Fallback: Bad character
        myCurrentOffset++;
        myTokenEnd = myCurrentOffset;
        myTokenType = WodTokenTypes.BAD_CHARACTER;
    }

    private static boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '$' || c == '~' || c == '^';
    }

    private static boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '.' || c == '-' ||
                c == '(' || c == ')' || c == '@' || c == '~' || c == '^';
    }
}
