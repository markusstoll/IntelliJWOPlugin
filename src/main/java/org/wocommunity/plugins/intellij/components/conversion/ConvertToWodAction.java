package org.wocommunity.plugins.intellij.components.conversion;

import org.jetbrains.annotations.NotNull;

public class ConvertToWodAction extends AbstractWOComponentConvertAction {

    @Override
    protected boolean isConvertToWod() {
        return true;
    }

    @Override
    protected @NotNull String getActionTitle() {
        return "Convert to WOD";
    }

    @Override
    protected @NotNull WOComponentConversionUtil.ConversionResult performConversion(@NotNull String htmlText, @NotNull String wodText) {
        return WOComponentConversionUtil.convertToWod(htmlText, wodText);
    }

    @Override
    protected @NotNull String getNoConversionMessage() {
        return "No inline wo:* tags found to convert to WOD style.";
    }

    @Override
    protected @NotNull String getSuccessMessage(int count) {
        return "Successfully converted " + count + " element" + (count == 1 ? "" : "s") + " to WOD style.";
    }
}

