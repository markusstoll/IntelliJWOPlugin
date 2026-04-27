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
            return false;
        }
        if (!vFile.getName().endsWith(".html")) {
            return false;
        }
        var parent = vFile.getParent();
        return parent != null && parent.getName().endsWith(".wo");
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
