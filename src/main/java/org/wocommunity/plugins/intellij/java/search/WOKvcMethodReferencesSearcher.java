package org.wocommunity.plugins.intellij.java.search;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.SearchRequestCollector;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.tools.KeyValueCodingUtil;

import java.util.Set;

/**
 * Enables MethodReferencesSearch (used specifically by RenameJavaMethodProcessor during
 * Java method rename refactorings) to find KVC-bound references in WebObjects templates
 * (.wod and .html) for Java accessor methods (e.g. finding '$content' or 'content' when
 * renaming 'getContent()').
 */
public class WOKvcMethodReferencesSearcher extends QueryExecutorBase<PsiReference, MethodReferencesSearch.SearchParameters> {

    public WOKvcMethodReferencesSearcher() {
        super(true);
    }

    @Override
    public void processQuery(@NotNull MethodReferencesSearch.SearchParameters parameters,
                             @NotNull Processor<? super PsiReference> consumer) {
        PsiMethod method = parameters.getMethod();
        Set<String> altNames = KeyValueCodingUtil.getAlternativeKvcNames(method);
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
                    method
            );
        }
    }
}
