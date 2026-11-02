package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.FakePsiElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WOHtmlComponentGotoDeclarationHandlerTest {

    @Test
    public void testNullSourceReturnsNull() {
        WOHtmlComponentGotoDeclarationHandler handler = new WOHtmlComponentGotoDeclarationHandler();
        assertNull(handler.getGotoDeclarationTargets(null, 0, null));
    }

    @Test
    public void testNonXmlTokenReturnsNull() {
        WOHtmlComponentGotoDeclarationHandler handler = new WOHtmlComponentGotoDeclarationHandler();
        FakePsiElement element = new FakePsiElement() {
            @Override
            public PsiElement getParent() {
                return null;
            }

            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public PsiFile getContainingFile() {
                return null;
            }
        };
        assertNull(handler.getGotoDeclarationTargets(element, 0, null));
    }
}
