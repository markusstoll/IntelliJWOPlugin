package org.wocommunity.plugins.intellij.psi.wod;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.wod.WODFileType;

public final class WODElementFactory {

    private WODElementFactory() {
    }

    public static @Nullable WODKeyPathElement createKeyPathElement(@NotNull Project project, @NotNull String name) {
        WODFile dummyFile = (WODFile) PsiFileFactory.getInstance(project).createFileFromText(
                "dummy.wod", WODFileType.INSTANCE, "Dummy: WOComponent { key = " + name + "; }"
        );
        return PsiTreeUtil.findChildOfType(dummyFile, WODKeyPathElement.class);
    }
}
