package org.wocommunity.plugins.intellij.wod;

import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler;

import org.wocommunity.plugins.intellij.psi.wod.WODTypes;

public class WodQuoteHandler extends SimpleTokenSetQuoteHandler {

    public WodQuoteHandler() {
        super(WODTypes.STRING);
    }
}
