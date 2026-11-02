package org.wocommunity.plugins.intellij.wod;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.wocommunity.plugins.intellij.psi.wod.WODTypes;

public class WodPairedBraceMatcher implements PairedBraceMatcher {

    private static final BracePair[] PAIRS = new BracePair[]{
            new BracePair(WODTypes.LBRACE, WODTypes.RBRACE, true)
    };

    @Override
    public BracePair @NotNull [] getPairs() {
        return PAIRS;
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType, @Nullable IElementType contextType) {
        return true;
    }

    @Override
    public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
        return openingBraceOffset;
    }
}
