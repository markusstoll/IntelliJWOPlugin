package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.psi.util.PsiUtilCore;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlFileNSInfoProvider;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides default namespace prefix bindings for WO component HTML templates.
 *
 * This prevents "Namespace wo is not bound" inspections for {@code wo:*} tags,
 * even if the document does not contain an explicit {@code xmlns:wo="..."} declaration.
 */
public final class WOHtmlFileNSInfoProvider implements XmlFileNSInfoProvider {

    public static final @NonNls String WO_PREFIX = "wo";
    public static final @NonNls String WO_NAMESPACE_URI = "urn:webobjects";

    @Override
    public @NonNls String[] @Nullable [] getDefaultNamespaces(@NotNull XmlFile file) {
        var vFile = PsiUtilCore.getVirtualFile(file);
        if (vFile == null) {
            return null;
        }
        if (!vFile.getName().endsWith(".html")) {
            return null;
        }
        var parent = vFile.getParent();
        if (parent == null || !parent.getName().endsWith(".wo")) {
            return null;
        }

        return new String[][]{
                {WO_PREFIX, WO_NAMESPACE_URI}
        };
    }

    @Override
    public boolean overrideNamespaceFromDocType(@NotNull XmlFile file) {
        return false;
    }
}

