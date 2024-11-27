package org.wocommunity.plugins.intellij.components;

import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.tools.WOIcons;

public class WOComponentEditorTabTitleProvider implements EditorTabTitleProvider {
    @Override
    public @Nullable String getEditorTabTitle(@NotNull Project project, @NotNull VirtualFile file) {
        if (file.getName().endsWith(".wo"))
            return file.getName().replace(".wo", " WO");

        return null; // Return null to use the default title
    }
}
