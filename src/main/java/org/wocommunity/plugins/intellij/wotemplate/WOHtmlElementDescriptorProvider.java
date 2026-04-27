package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
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

    @Override
    public @Nullable XmlElementDescriptor getDescriptor(@NotNull XmlTag tag) {
        if (!isWoComponentHtml(tag)) {
            return null;
        }

        String name = tag.getName();
        if ("webobject".equalsIgnoreCase(name)) {
            return new WOXmlElementDescriptor("webobject", null);
        }

        int colon = name.indexOf(':');
        if (colon <= 0) {
            return null;
        }

        String prefix = name.substring(0, colon);
        if (!WO_PREFIX.equals(prefix)) {
            return null;
        }

        String localName = name.substring(colon + 1);
        PsiClass resolved = resolveClass(tag.getProject(), localName);
        return new WOXmlElementDescriptor(name, resolved);
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

    private static @Nullable PsiClass resolveClass(@NotNull Project project, @NotNull String localName) {
        GlobalSearchScope all = GlobalSearchScope.allScope(project); // includes project + libraries

        // Allow wo:com.example.Foo
        if (localName.indexOf('.') >= 0) {
            return JavaPsiFacade.getInstance(project).findClass(localName, all);
        }

        // Prefer project classes if possible, then fall back to libraries.
        PsiShortNamesCache cache = PsiShortNamesCache.getInstance(project);
        PsiClass[] projectMatches = cache.getClassesByName(localName, GlobalSearchScope.projectScope(project));
        if (projectMatches.length > 0) {
            return projectMatches[0];
        }
        PsiClass[] allMatches = cache.getClassesByName(localName, all);
        return allMatches.length > 0 ? allMatches[0] : null;
    }
}

