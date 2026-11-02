package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.openapi.project.DumbService;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlExtension;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Registers {@code wo:*} tag support and {@code <webobject>} tag support
 * for the IntelliJ HTML/XML subsystem.
 */
public final class WOHtmlXmlExtension extends XmlExtension {

    private static final String WO_COMPONENT_FQN = "com.webobjects.appserver.WOComponent";

    @Override
    public boolean isAvailable(PsiFile file) {
        return file != null && WOHtmlCompletionUtil.isWoComponentHtml(file);
    }

    @Override
    public @NotNull List<TagInfo> getAvailableTagNames(@NotNull XmlFile file, @NotNull XmlTag context) {
        Set<String> localNames = new LinkedHashSet<>();
        localNames.addAll(WOComponentTagAliasRegistry.getAliases().keySet());
        localNames.addAll(WOComponentCollector.collectAllComponentAndElementNames(file.getProject()));

        List<TagInfo> out = new ArrayList<>(localNames.size() + 1);
        out.add(new TagInfo("webobject", null));
        for (String localName : localNames) {
            out.add(new TagInfo(localName, WOHtmlFileNSInfoProvider.WO_NAMESPACE_URI));
        }
        return out;
    }

    @Override
    public @org.jetbrains.annotations.Nullable com.intellij.psi.impl.source.xml.SchemaPrefix getPrefixDeclaration(@NotNull XmlTag context, String namespacePrefix) {
        return null;
    }

    @Override
    public boolean isCustomTagAllowed(final XmlTag tag) {
        PsiFile file = tag.getContainingFile();
        if (file == null || !isAvailable(file)) {
            return false;
        }
        if (isLegacyWebObjectTag(tag)) {
            return true;
        }
        String prefix = tag.getNamespacePrefix();
        if (prefix == null || prefix.isEmpty()) {
            String qn = tag.getName();
            int colon = qn.indexOf(':');
            if (colon > 0) {
                prefix = qn.substring(0, colon);
            }
        }
        return "wo".equalsIgnoreCase(prefix);
    }

    @Override
    public boolean isSelfClosingTagAllowed(@NotNull XmlTag tag) {
        if (!isAvailable(tag.getContainingFile())) {
            return super.isSelfClosingTagAllowed(tag);
        }

        if (isLegacyWebObjectTag(tag)) {
            return true;
        }

        String prefix = tag.getNamespacePrefix();
        if (prefix == null || prefix.isEmpty()) {
            String qn = tag.getName();
            int colon = qn.indexOf(':');
            if (colon > 0) {
                prefix = qn.substring(0, colon);
            }
        }
        return "wo".equalsIgnoreCase(prefix) || super.isSelfClosingTagAllowed(tag);
    }

    @Override
    public boolean isCollapsibleTag(@NotNull XmlTag tag) {
        if (!isAvailable(tag.getContainingFile())) {
            return super.isCollapsibleTag(tag);
        }

        if (isLegacyWebObjectTag(tag)) {
            return true;
        }

        String prefix = tag.getNamespacePrefix();
        if (prefix == null || prefix.isEmpty()) {
            String qn = tag.getName();
            int colon = qn.indexOf(':');
            if (colon > 0) {
                prefix = qn.substring(0, colon);
            }
        }
        return "wo".equalsIgnoreCase(prefix) || super.isCollapsibleTag(tag);
    }

    private static boolean isLegacyWebObjectTag(@NotNull XmlTag tag) {
        return "webobject".equalsIgnoreCase(tag.getName())
                || "webobject".equalsIgnoreCase(tag.getLocalName());
    }
}
