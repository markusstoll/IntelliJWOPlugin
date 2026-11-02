package org.wocommunity.plugins.intellij.wod;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.FakePsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Synthetic navigatable element returned when a `<webobject name="xxx">` has no definition in `.wod`.
 * When navigated (via Cmd+Click, Cmd+B, or Go to Declaration), it creates the definition
 * with default type `WOComponent` in the `.wod` file, scrolls to it, and selects `WOComponent`.
 */
public class CreateWodElementPlaceholder extends FakePsiElement {

    private final PsiElement parent;
    private final VirtualFile wodFile;
    private final VirtualFile woDir;
    private final String elementName;

    public CreateWodElementPlaceholder(@NotNull PsiElement parent,
                                       @Nullable VirtualFile wodFile,
                                       @NotNull VirtualFile woDir,
                                       @NotNull String elementName) {
        this.parent = parent;
        this.wodFile = wodFile;
        this.woDir = woDir;
        this.elementName = elementName;
    }

    @Override
    public PsiElement getParent() {
        return parent;
    }

    @Override
    public @NotNull Project getProject() {
        return parent.getProject();
    }

    @Override
    public PsiFile getContainingFile() {
        return parent.getContainingFile();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public String getName() {
        return elementName;
    }

    @Override
    public String getText() {
        return elementName;
    }

    @Override
    public boolean canNavigate() {
        return true;
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }

    @Override
    public PsiElement getNavigationElement() {
        return this;
    }

    @Override
    public void navigate(boolean requestFocus) {
        WodElementCreationUtil.createAndNavigateToElement(
                parent.getProject(), wodFile, woDir, elementName, "WOComponent"
        );
    }
}
