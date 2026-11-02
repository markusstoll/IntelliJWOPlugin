package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.tools.WOFileUtil;
import org.wocommunity.plugins.intellij.tools.WOPsiUtil;
import org.wocommunity.plugins.intellij.wod.CreateWodElementPlaceholder;
import org.wocommunity.plugins.intellij.wod.WodElement;
import org.wocommunity.plugins.intellij.wod.WodModel;
import org.wocommunity.plugins.intellij.wod.WodParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles Go to Declaration (Cmd-click / Ctrl-click / Ctrl+B) on WebObjects tags in template HTML files:
 * 1. Inline component tags (&lt;wo:MyComponent&gt;): returns both code (PsiClass) and component folder (PsiDirectory),
 *    allowing the user to choose between opening Code or Component.
 * 2. WebObject tags (&lt;webobject name="xxx"&gt;): navigates directly to the declaration in the .wod file,
 *    switching to the WOD tab and selecting the element (identical to gutter icon behavior).
 */
public final class WOHtmlComponentGotoDeclarationHandler implements GotoDeclarationHandler {

    private static final String WO_PREFIX = "wo";

    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement sourceElement, int offset, Editor editor) {
        if (sourceElement == null) {
            return null;
        }

        PsiFile file = sourceElement.getContainingFile();
        if (file == null || !WOHtmlCompletionUtil.isWoComponentHtml(file)) {
            return null;
        }

        XmlTag tag = PsiTreeUtil.getParentOfType(sourceElement, XmlTag.class);
        if (tag == null) {
            return null;
        }

        // 1. Check <webobject name="..."> tag
        if ("webobject".equalsIgnoreCase(tag.getLocalName())) {
            return resolveWebObjectGotoTargets(tag, sourceElement);
        }

        // 2. For inline <wo:...> tags: do not intercept if cursor is inside an XML attribute
        if (PsiTreeUtil.getParentOfType(sourceElement, XmlAttribute.class) != null) {
            return null;
        }

        if (!(sourceElement instanceof XmlToken token)) {
            return null;
        }
        IElementType tokenType = token.getTokenType();
        if (tokenType != XmlTokenType.XML_NAME && tokenType != XmlTokenType.XML_TAG_NAME) {
            return null;
        }
        if (!(token.getParent() instanceof XmlTag)) {
            return null;
        }

        String qualifiedName = tag.getName();
        String localTagName = tag.getLocalName();

        String prefix = tag.getNamespacePrefix();
        if (prefix == null || prefix.isEmpty()) {
            int colon = qualifiedName.indexOf(':');
            if (colon > 0) {
                prefix = qualifiedName.substring(0, colon);
                localTagName = qualifiedName.substring(colon + 1);
            }
        }

        if (!WO_PREFIX.equalsIgnoreCase(prefix)) {
            return null;
        }

        Project project = tag.getProject();
        String resolvedName = WOComponentTagAliasRegistry.resolveAlias(localTagName);

        List<PsiElement> targets = new ArrayList<>(2);

        // 1. Resolve Java class (Code)
        PsiClass componentClass = WOHtmlElementDescriptorProvider.resolveWoTagClass(project, localTagName);
        if (componentClass == null) {
            try {
                componentClass = WOPsiUtil.getPsiClassForComponentName(resolvedName, project);
            } catch (Exception ignored) {
            }
        }
        if (componentClass != null) {
            targets.add(componentClass);
        }

        // 2. Resolve component directory (.wo) (Component)
        PsiDirectory componentFolder = null;
        if (componentClass != null) {
            componentFolder = WOPsiUtil.getComponentFolderForClass(componentClass);
        }
        if (componentFolder == null) {
            componentFolder = WOPsiUtil.getComponentFolderForComponentName(resolvedName, project);
        }
        if (componentFolder != null && !targets.contains(componentFolder)) {
            targets.add(componentFolder);
        }

        if (targets.isEmpty()) {
            return null;
        }

        return targets.toArray(PsiElement.EMPTY_ARRAY);
    }

    private static PsiElement @Nullable [] resolveWebObjectGotoTargets(@NotNull XmlTag tag, @NotNull PsiElement sourceElement) {
        String name = WOHtmlWebObjectUtil.getWebObjectName(tag);
        if (name == null || name.isBlank()) {
            return null;
        }

        Project project = tag.getProject();
        PsiFile psiFile = tag.getContainingFile();
        if (psiFile == null) {
            return null;
        }
        VirtualFile vFile = PsiUtilCore.getVirtualFile(psiFile);
        if (vFile == null && psiFile.getOriginalFile() != null) {
            vFile = PsiUtilCore.getVirtualFile(psiFile.getOriginalFile());
        }
        if (vFile == null) {
            vFile = psiFile.getViewProvider().getVirtualFile();
        }
        if (vFile == null) {
            return null;
        }

        VirtualFile componentDir = WOFileUtil.getComponent(vFile);
        if (componentDir == null) {
            componentDir = vFile.getParent();
        }
        if (componentDir == null) {
            return null;
        }

        String componentName = componentDir.getName().endsWith(".wo")
                ? componentDir.getName().substring(0, componentDir.getName().length() - 3)
                : vFile.getNameWithoutExtension();
        VirtualFile wodFile = componentDir.findChild(componentName + ".wod");
        if (wodFile == null || !wodFile.isValid()) {
            return null;
        }

        WodModel model = WodParser.getModel(wodFile);
        WodElement element = model.getElement(name);
        if (element != null && element.getNameRange() != null) {
            PsiFile wodPsiFile = PsiManager.getInstance(project).findFile(wodFile);
            if (wodPsiFile != null) {
                PsiElement target = wodPsiFile.findElementAt(element.getNameRange().getStartOffset());
                if (target != null) {
                    return new PsiElement[] {
                            new WODNavigatableElement(target, wodFile, element.getNameRange().getStartOffset(), element.getNameRange().getEndOffset())
                    };
                }
            }
        }

        return new PsiElement[] {
                new CreateWodElementPlaceholder(tag, wodFile, componentDir, name)
        };
    }
}

