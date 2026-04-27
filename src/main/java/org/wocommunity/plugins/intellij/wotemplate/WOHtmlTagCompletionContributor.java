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
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
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
                        if (!WOHtmlCompletionUtil.isWoComponentHtml(file)) {
                            return;
                        }

                        String localPrefix = getWoLocalPrefix(parameters, file);
                        if (localPrefix == null) {
                            return;
                        }

                        Set<String> localNames = new LinkedHashSet<>();
                        localNames.addAll(WOComponentTagAliasRegistry.getAliases().keySet());
                        if (!DumbService.isDumb(file.getProject())) {
                            localNames.addAll(collectWoInheritorNames(file));
                        }

                        // Match against full "wo:<localPrefix>" so completion works even if IntelliJ's computed prefix
                        // is only the part after "wo:".
                        CompletionResultSet woMatcher = result.withPrefixMatcher(WO_PREFIX_WITH_COLON + localPrefix);
                        for (String localName : localNames) {
                            woMatcher.addElement(
                                    LookupElementBuilder.create(WO_PREFIX_WITH_COLON + localName)
                                            .withTypeText("WO", true)
                            );
                        }
                    }
                });
    }

    /**
     * @return the already typed prefix after "wo:" (may be empty), or null if caret is not in a WO tag-name context.
     */
    private static String getWoLocalPrefix(@NotNull CompletionParameters parameters, @NotNull PsiFile file) {
        var doc = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        if (doc == null) {
            return null;
        }
        int offset = parameters.getOffset();
        if (offset < 0 || offset > doc.getTextLength()) {
            return null;
        }

        CharSequence text = doc.getCharsSequence();

        // Find the start of the current token-ish segment (letters/digits/_/-) after "wo:"
        int i = offset;
        while (i > 0) {
            char c = text.charAt(i - 1);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
                i--;
                continue;
            }
            break;
        }
        String typedLocal = text.subSequence(i, offset).toString();

        // Now check that immediately before that, we have "wo:" and that we're inside a tag name ("<wo:...").
        int woEnd = i;
        int woStart = woEnd - WO_PREFIX_WITH_COLON.length();
        if (woStart < 0) {
            return null;
        }
        if (!text.subSequence(woStart, woEnd).toString().equalsIgnoreCase(WO_PREFIX_WITH_COLON)) {
            // Special case: caret is right after ":" with no local part typed yet.
            if (offset >= WO_PREFIX_WITH_COLON.length()
                    && text.subSequence(offset - WO_PREFIX_WITH_COLON.length(), offset).toString().equalsIgnoreCase(WO_PREFIX_WITH_COLON)) {
                typedLocal = "";
                woStart = offset - WO_PREFIX_WITH_COLON.length();
            } else {
                return null;
            }
        }

        // Ensure there's a '<' before "wo:" without intervening whitespace or '>'
        int j = woStart - 1;
        while (j >= 0) {
            char c = text.charAt(j);
            if (c == '<') {
                return typedLocal;
            }
            if (c == '>' || Character.isWhitespace(c)) {
                return null;
            }
            j--;
        }
        return null;
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

