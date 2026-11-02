package org.wocommunity.plugins.intellij.wod;

import com.intellij.lexer.FlexAdapter;
import com.intellij.psi.tree.IElementType;
import org.wocommunity.plugins.intellij.psi.wod.WODTypes;

public class WODLexerAdapter extends FlexAdapter {
    public WODLexerAdapter() {
        super(new _WODLexer(null));
    }

    @Override
    public IElementType getTokenType() {
        IElementType type = super.getTokenType();
        if (type == WODTypes.IDENTIFIER) {
            CharSequence text = getTokenSequence();
            if (isBooleanLiteral(text)) {
                return WODTypes.BOOLEAN;
            }
        }
        return type;
    }

    public static boolean isBooleanLiteral(CharSequence text) {
        if (text == null) return false;
        String s = text.toString();
        return "true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s)
                || "YES".equalsIgnoreCase(s) || "NO".equalsIgnoreCase(s);
    }
}
