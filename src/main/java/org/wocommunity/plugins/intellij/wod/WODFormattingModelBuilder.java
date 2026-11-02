package org.wocommunity.plugins.intellij.wod;

import com.intellij.formatting.Block;
import com.intellij.formatting.FormattingContext;
import com.intellij.formatting.FormattingModel;
import com.intellij.formatting.FormattingModelBuilder;
import com.intellij.formatting.FormattingModelProvider;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.psi.wod.WODTypes;

public final class WODFormattingModelBuilder implements FormattingModelBuilder {
    @Override
    public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
        CodeStyleSettings settings = formattingContext.getCodeStyleSettings();
        PsiElement element = formattingContext.getPsiElement();
        SpacingBuilder spacingBuilder = new SpacingBuilder(settings, WODLanguage.INSTANCE);
        spacingBuilder.around(WODTypes.COLON).spaces(1);
        spacingBuilder.around(WODTypes.ASSIGN).spaces(1);
        spacingBuilder.before(WODTypes.WOD_ASSIGNMENT_COMMENT).spaces(1);
        spacingBuilder.between(WODTypes.RBRACE, WODTypes.SEMI).spaces(0);
        spacingBuilder.after(WODTypes.LBRACE).lineBreakInCode();
        spacingBuilder.between(WODTypes.WOD_ASSIGNMENT, WODTypes.WOD_ASSIGNMENT).lineBreakInCode();
        spacingBuilder.before(WODTypes.RBRACE).lineBreakInCode();
        spacingBuilder.between(WODTypes.WOD_DECLARATION, WODTypes.WOD_DECLARATION).blankLines(1);

        Block block = new WODBlock(element.getNode(), null, null, spacingBuilder);
        return FormattingModelProvider.createFormattingModelForPsiFile(formattingContext.getContainingFile(), block, settings);
    }
}
