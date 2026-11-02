package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

/**
 * Shared helpers for HTML completion in WO component templates ({@code *.wo/*.html}).
 */
public final class WOHtmlCompletionUtil {

    private static final String WO_PREFIX = "wo";

    private WOHtmlCompletionUtil() {
    }

    public static boolean isWoComponentHtml(@NotNull PsiFile file) {
        var vFile = PsiUtilCore.getVirtualFile(file);
        if (vFile == null) {
            PsiFile orig = file.getOriginalFile();
            if (orig != null) {
                vFile = PsiUtilCore.getVirtualFile(orig);
            }
        }
        if (vFile == null) {
            vFile = file.getVirtualFile();
        }
        if (vFile == null) {
            return false;
        }
        String fileName = vFile.getName().toLowerCase(java.util.Locale.ROOT);
        boolean isHtml = fileName.endsWith(".html") || fileName.endsWith(".htm") || fileName.endsWith(".xhtml");
        if (!isHtml) {
            return false;
        }
        var parent = vFile.getParent();
        if (parent != null && parent.getName().endsWith(".wo")) {
            return true;
        }
        return WOComponentClassResolver.findComponentClass(file) != null;
    }

    public static boolean isWoPrefixedTag(@NotNull XmlTag tag) {
        String prefix = tag.getNamespacePrefix();
        if (prefix == null || prefix.isEmpty()) {
            String qn = tag.getName();
            int colon = qn.indexOf(':');
            if (colon > 0) {
                prefix = qn.substring(0, colon);
            }
        }
        return WO_PREFIX.equals(prefix);
    }
}
