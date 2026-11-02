package org.wocommunity.plugins.intellij.components.conversion;

import org.jetbrains.annotations.NotNull;

public class ConvertToInlineAction extends AbstractWOComponentConvertAction {

    @Override
    protected boolean isConvertToWod() {
        return false;
    }

    @Override
    protected @NotNull String getActionTitle() {
        return "Convert to Inline";
    }

    @Override
    protected @NotNull WOComponentConversionUtil.ConversionResult performConversion(@NotNull String htmlText, @NotNull String wodText) {
        return WOComponentConversionUtil.convertToInline(htmlText, wodText);
    }

    @Override
    protected @NotNull String getNoConversionMessage() {
        return "No webobject tags with matching WOD declarations found to convert to Inline style.";
    }

    @Override
    protected @NotNull String getSuccessMessage(int count) {
        return "Successfully converted " + count + " element" + (count == 1 ? "" : "s") + " to Inline style.";
    }
}

