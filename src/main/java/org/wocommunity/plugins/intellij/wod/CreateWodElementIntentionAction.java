package org.wocommunity.plugins.intellij.wod;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.wotemplate.WOHtmlWebObjectUtil;

/**
 * Quick-fix / Intention action (Alt+Enter) on `<webobject name="xxx">` to create the definition in `.wod`.
 */
public class CreateWodElementIntentionAction extends PsiElementBaseIntentionAction {

    public CreateWodElementIntentionAction() {
        setText("Create WebObject definition in WOD");
    }

    @Override
    public @NotNull String getText() {
        return "Create WebObject definition in WOD";
    }

    @Override
    public @NotNull String getFamilyName() {
        return "WebObjects";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        XmlTag tag = PsiTreeUtil.getParentOfType(element, XmlTag.class, false);
        if (tag == null || !"webobject".equalsIgnoreCase(tag.getLocalName())) {
            return false;
        }

        String name = WOHtmlWebObjectUtil.getWebObjectName(tag);
        if (name == null || name.isBlank()) {
            return false;
        }

        PsiFile file = tag.getContainingFile();
        if (file == null) {
            return false;
        }
        VirtualFile vFile = PsiUtilCore.getVirtualFile(file);
        if (vFile == null || vFile.getParent() == null || !vFile.getParent().getName().endsWith(".wo")) {
            return false;
        }

        VirtualFile woDir = vFile.getParent();
        String compName = woDir.getName().substring(0, woDir.getName().length() - 3);
        VirtualFile wodFile = woDir.findChild(compName + ".wod");
        if (wodFile != null && wodFile.isValid()) {
            WodModel model = WodParser.getModel(wodFile);
            if (model.hasElement(name)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        XmlTag tag = PsiTreeUtil.getParentOfType(element, XmlTag.class, false);
        if (tag == null) {
            return;
        }
        String name = WOHtmlWebObjectUtil.getWebObjectName(tag);
        if (name == null || name.isBlank()) {
            return;
        }

        PsiFile file = tag.getContainingFile();
        if (file == null) {
            return;
        }
        VirtualFile vFile = PsiUtilCore.getVirtualFile(file);
        if (vFile == null || vFile.getParent() == null) {
            return;
        }
        VirtualFile woDir = vFile.getParent();
        if (!woDir.getName().endsWith(".wo")) {
            return;
        }
        String compName = woDir.getName().substring(0, woDir.getName().length() - 3);
        VirtualFile wodFile = woDir.findChild(compName + ".wod");

        WodElementCreationUtil.createAndNavigateToElement(project, wodFile, woDir, name, "WOComponent");
    }
}
