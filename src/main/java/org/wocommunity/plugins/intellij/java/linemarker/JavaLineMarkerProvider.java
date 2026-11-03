/**
 * Adds WO icon to java members used in WO templates
 */
package org.wocommunity.plugins.intellij.java.linemarker;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.psi.references.WODComponentReference;
import org.wocommunity.plugins.intellij.psi.references.WODKeyPathReference;
import org.wocommunity.plugins.intellij.psi.wod.WODComponent;
import org.wocommunity.plugins.intellij.tools.WOIcons;

import java.util.Collection;
import java.util.List;

public class JavaLineMarkerProvider extends RelatedItemLineMarkerProvider {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element,
                                            @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        if (!(element instanceof PsiIdentifier)) return;

        PsiElement parent = element.getParent();
        if (parent instanceof PsiField || parent instanceof PsiMethod || parent instanceof PsiClass) {

            FileType wodFileType = FileTypeManager.getInstance().getFileTypeByExtension("wod");
            FileType htmlFileType = FileTypeManager.getInstance().getFileTypeByExtension("html");

            GlobalSearchScope projectScope = ProjectScope.getProjectScope(element.getProject());
            GlobalSearchScope typeScope = GlobalSearchScope.getScopeRestrictedByFileTypes(projectScope, wodFileType, htmlFileType);

            // Verwende Query-Lazy-Iteration anstelle von .findAll() wo möglich
            List<PsiElement> targets = ReferencesSearch.search(parent, typeScope).findAll().stream()
                    .filter(ref -> ref instanceof WODKeyPathReference || ref instanceof WODComponentReference)
                    .map(PsiReference::getElement)
                    .toList();

            if (!targets.isEmpty()) {
                NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(WOIcons.WOCOMPONENT_ICON)
                        .setTargets(targets)
                        .setNamer(el -> {
                            PsiFile file = el.getContainingFile();
                            return file != null ? file.getName() : el.getText();
                        })
                        .setTooltipText("Used in WO Component");

                result.add(builder.createLineMarkerInfo(element));
            }
        }
    }
}