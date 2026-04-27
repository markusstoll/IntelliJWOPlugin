package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.impl.source.xml.XmlElementDescriptorProvider;
import com.intellij.xml.XmlElementDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Makes WO template tags "known" to the XML/HTML PSI.
 *
 * This enables proper highlighting (no unknown-tag errors) and allows
 * "Go to Declaration" on tag names via {@link com.intellij.psi.impl.source.xml.TagNameReference},
 * which resolves to {@link XmlElementDescriptor#getDeclaration()}.
 */
public final class WOHtmlElementDescriptorProvider implements XmlElementDescriptorProvider {

    private static final String WO_PREFIX = "wo";
    private static final String WO_COMPONENT_FQN = "com.webobjects.appserver.WOComponent";
    private static final String WO_ELEMENT_FQN = "com.webobjects.appserver.WOElement";
    private static final String WO_DYNAMIC_ELEMENT_FQN = "com.webobjects.appserver.WODynamicElement";

    @Override
    public @Nullable XmlElementDescriptor getDescriptor(@NotNull XmlTag tag) {
        if (!isWoComponentHtml(tag)) {
            return null;
        }

        String qualifiedName = tag.getName();
        String localTagName = tag.getLocalName();

        if ("webobject".equalsIgnoreCase(localTagName)) {
            return new WOXmlElementDescriptor("webobject", null);
        }

        String prefix = tag.getNamespacePrefix();
        if (prefix == null || prefix.isEmpty()) {
            // In HTML PSI, tags may still be represented as "wo:If" in the raw name even if the namespace isn't bound
            // via an explicit xmlns attribute. Fall back to parsing the qualified tag name.
            int colon = qualifiedName.indexOf(':');
            if (colon > 0) {
                prefix = qualifiedName.substring(0, colon);
                localTagName = qualifiedName.substring(colon + 1);
            }
        }
        if (prefix == null || prefix.isEmpty()) {
            return null;
        }

        if (!WO_PREFIX.equals(prefix)) {
            return null;
        }

        PsiClass resolved = resolveWoTagClass(tag.getProject(), localTagName);
        return new WOXmlElementDescriptor(qualifiedName, resolved);
    }

    private static boolean isWoComponentHtml(@NotNull XmlTag tag) {
        PsiFile file = tag.getContainingFile();
        if (file == null) {
            return false;
        }
        var vFile = PsiUtilCore.getVirtualFile(file);
        if (vFile == null) {
            return false;
        }
        if (!vFile.getName().endsWith(".html")) {
            return false;
        }
        var parent = vFile.getParent();
        return parent != null && parent.getName().endsWith(".wo");
    }

    private static @Nullable PsiClass resolveWoTagClass(@NotNull Project project, @NotNull String rawLocalName) {
        GlobalSearchScope all = GlobalSearchScope.allScope(project); // includes project + libraries
        if (DumbService.isDumb(project)) {
            return null;
        }

        PsiClass woComponent;
        PsiClass woElement;
        PsiClass woDynamicElement;
        try {
            woComponent = JavaPsiFacade.getInstance(project).findClass(WO_COMPONENT_FQN, all);
            woElement = JavaPsiFacade.getInstance(project).findClass(WO_ELEMENT_FQN, all);
            woDynamicElement = JavaPsiFacade.getInstance(project).findClass(WO_DYNAMIC_ELEMENT_FQN, all);
        } catch (IndexNotReadyException e) {
            return null;
        }

        // At minimum, one WO base type must be resolvable.
        if (woComponent == null && woElement == null && woDynamicElement == null) {
            return null;
        }

        String localName = WOComponentTagAliasRegistry.resolveAlias(rawLocalName);

        // Allow wo:com.example.Foo
        if (localName.indexOf('.') >= 0) {
            PsiClass cls = JavaPsiFacade.getInstance(project).findClass(localName, all);
            return cls != null && isAllowedWoTagClass(cls, woComponent, woElement, woDynamicElement) ? cls : null;
        }

        // Prefer project classes if possible, then fall back to libraries.
        PsiShortNamesCache cache = PsiShortNamesCache.getInstance(project);
        PsiClass[] projectMatches = cache.getClassesByName(localName, GlobalSearchScope.projectScope(project));
        for (PsiClass c : projectMatches) {
            if (isAllowedWoTagClass(c, woComponent, woElement, woDynamicElement)) {
                return c;
            }
        }
        PsiClass[] allMatches = cache.getClassesByName(localName, all);
        for (PsiClass c : allMatches) {
            if (isAllowedWoTagClass(c, woComponent, woElement, woDynamicElement)) {
                return c;
            }
        }
        return null;
    }

    private static boolean isAllowedWoTagClass(@NotNull PsiClass candidate,
                                               @Nullable PsiClass woComponent,
                                               @Nullable PsiClass woElement,
                                               @Nullable PsiClass woDynamicElement) {
        // Most WO template tags map to dynamic elements (WODynamicElement), some map to WOComponent.
        // We accept any inheritor of the available WO base types.
        if (woComponent != null && InheritanceUtil.isInheritorOrSelf(candidate, woComponent, true)) {
            return true;
        }
        if (woDynamicElement != null && InheritanceUtil.isInheritorOrSelf(candidate, woDynamicElement, true)) {
            return true;
        }
        return woElement != null && InheritanceUtil.isInheritorOrSelf(candidate, woElement, true);
    }
}

