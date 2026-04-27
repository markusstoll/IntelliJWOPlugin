package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;

public final class WOHtmlTagCompletionContributor extends CompletionContributor {

    private static final String WO_PREFIX_WITH_COLON = "wo:";

    private static final String WO_COMPONENT_FQN = "com.webobjects.appserver.WOComponent";
    private static final String WO_ELEMENT_FQN = "com.webobjects.appserver.WOElement";
    private static final String WO_DYNAMIC_ELEMENT_FQN = "com.webobjects.appserver.WODynamicElement";

    public WOHtmlTagCompletionContributor() {
        extend(CompletionType.BASIC,
                com.intellij.patterns.PlatformPatterns.psiElement(),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters,
                                                  @NotNull ProcessingContext context,
                                                  @NotNull CompletionResultSet result) {
                        PsiFile file = parameters.getOriginalFile();
                        if (!isWoComponentHtml(file)) {
                            return;
                        }

                        // Only provide variants when user is likely typing a tag name starting with "wo:"
                        String prefix = result.getPrefixMatcher().getPrefix();
                        if (prefix == null || (!prefix.startsWith("wo") && !prefix.startsWith(WO_PREFIX_WITH_COLON))) {
                            return;
                        }

                        Set<String> localNames = new LinkedHashSet<>();
                        localNames.addAll(WOComponentTagAliasRegistry.getAliases().keySet());
                        if (!DumbService.isDumb(file.getProject())) {
                            localNames.addAll(collectWoInheritorNames(file));
                        }

                        CompletionResultSet woMatcher = result.withPrefixMatcher(result.getPrefixMatcher());
                        for (String localName : localNames) {
                            woMatcher.addElement(
                                    LookupElementBuilder.create(WO_PREFIX_WITH_COLON + localName)
                                            .withTypeText("WO", true)
                            );
                        }
                    }
                });
    }

    private static boolean isWoComponentHtml(@NotNull PsiFile file) {
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

    private static @NotNull Set<String> collectWoInheritorNames(@NotNull PsiFile file) {
        Set<String> out = new LinkedHashSet<>();
        var project = file.getProject();
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);

        PsiClass woComponent = JavaPsiFacade.getInstance(project).findClass(WO_COMPONENT_FQN, scope);
        PsiClass woElement = JavaPsiFacade.getInstance(project).findClass(WO_ELEMENT_FQN, scope);
        PsiClass woDynamicElement = JavaPsiFacade.getInstance(project).findClass(WO_DYNAMIC_ELEMENT_FQN, scope);

        addInheritors(out, woComponent, scope);
        addInheritors(out, woDynamicElement, scope);
        addInheritors(out, woElement, scope);

        return out;
    }

    private static void addInheritors(@NotNull Set<String> out, PsiClass base, @NotNull GlobalSearchScope scope) {
        if (base == null) {
            return;
        }
        for (PsiClass c : ClassInheritorsSearch.search(base, scope, true)) {
            String name = c.getName();
            if (name != null && !name.isBlank()) {
                out.add(name);
            }
        }
    }
}

