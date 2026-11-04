package org.wocommunity.plugins.intellij.java.search;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.SearchRequestCollector;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.tools.KeyValueCodingUtil;

import java.util.Set;

/**
 * Enables IntelliJ's ReferencesSearch (used by Find Usages, Line Markers, and Rename Refactoring)
 * to find KVC-bound references in WebObjects templates (.wod and .html) for Java getters, setters,
 * and fields (e.g. finding '$currency' or 'currency' when searching for 'getCurrency()').
 */
public class WOKvcReferencesSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {

    public WOKvcReferencesSearcher() {
        super(true);
    }

    @Override
    public void processQuery(@NotNull ReferencesSearch.SearchParameters parameters,
                             @NotNull Processor<? super PsiReference> consumer) {
        PsiElement element = parameters.getElementToSearch();
        if (!(element instanceof PsiMethod || element instanceof PsiField)) {
            return;
        }

        Set<String> altNames = KeyValueCodingUtil.getAlternativeKvcNames(element);
        if (altNames.isEmpty()) {
            return;
        }

        SearchRequestCollector optimizer = parameters.getOptimizer();
        for (String altName : altNames) {
            optimizer.searchWord(
                    altName,
                    parameters.getEffectiveSearchScope(),
                    UsageSearchContext.ANY,
                    true,
                    element
            );
        }
    }
}
