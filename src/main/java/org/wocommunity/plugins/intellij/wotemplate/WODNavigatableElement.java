package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.FakePsiElement;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.wod.WOHtmlToWodNavigationUtil;

public class WODNavigatableElement extends FakePsiElement {

    private final PsiElement delegate;
    private final VirtualFile wodFile;
    private final int startOffset;
    private final int endOffset;

    public WODNavigatableElement(@NotNull PsiElement delegate,
                                 @NotNull VirtualFile wodFile,
                                 int startOffset,
                                 int endOffset) {
        this.delegate = delegate;
        this.wodFile = wodFile;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    @Override
    public PsiElement getParent() {
        return delegate.getParent();
    }

    @Override
    public @NotNull Project getProject() {
        return delegate.getProject();
    }

    @Override
    public PsiFile getContainingFile() {
        return delegate.getContainingFile();
    }

    @Override
    public boolean isValid() {
        return delegate.isValid();
    }

    @Override
    public String getName() {
        return delegate.getText();
    }

    @Override
    public String getText() {
        return delegate.getText();
    }

    @Override
    public TextRange getTextRange() {
        return delegate.getTextRange();
    }

    @Override
    public int getTextOffset() {
        return startOffset;
    }

    @Override
    public boolean canNavigate() {
        return true;
    }

    @Override
    public boolean canNavigateToSource() {
        return true;
    }

    @Override
    public PsiElement getNavigationElement() {
        return this;
    }

    @Override
    public void navigate(boolean requestFocus) {
        WOHtmlToWodNavigationUtil.navigateToWodAndSelect(getProject(), wodFile, startOffset, endOffset);
    }
}
