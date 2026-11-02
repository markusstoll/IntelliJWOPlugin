package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.tools.WOPsiUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles Go to Declaration (Cmd-click / Ctrl-click / Ctrl+B) on inline WebObjects component tags.
 * When both a Java code class and a component (.wo) folder exist, returns both targets,
 * allowing the user to choose between opening Code or Component.
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

        // Do not intercept if cursor is inside an XML attribute
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
        if (!(token.getParent() instanceof XmlTag tag)) {
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
}

