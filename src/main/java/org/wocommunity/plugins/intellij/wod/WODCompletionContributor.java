package org.wocommunity.plugins.intellij.wod;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.tools.KeyValueCodingUtil;
import org.wocommunity.plugins.intellij.tools.WOPsiUtil;
import org.wocommunity.plugins.intellij.psi.api.APIFile;
import org.wocommunity.plugins.intellij.psi.wod.*;
import org.wocommunity.plugins.intellij.wotemplate.WOSystemBindingDefinitions;

import java.util.Set;

import static org.wocommunity.plugins.intellij.tools.WOPsiUtil.WO_ELEMENT_FQN;

public class WODCompletionContributor extends CompletionContributor {
    public WODCompletionContributor() {
        // Add autocompletion for WOD tags
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement(),
                new CompletionProvider<CompletionParameters>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters,
                                                  @NotNull ProcessingContext context,
                                                  @NotNull CompletionResultSet result) {
                        // parameters: position, originalfile, completiontype, editor
                        // parameters.position = WODTokenType.IDENTIFIER
                        // parameters.position: parent CompositeElement mit type z.B. binding
                        // context: ? singleKye, singleValue, myMap, mySharedContext
                        // result: prefixMatcher, ...

                        PsiElement position = parameters.getPosition();
                        if (position instanceof LeafPsiElement leaf && leaf.getElementType() == WODTypes.IDENTIFIER) {
                            PsiElement parent = position.getParent();
                            switch (parent) {
                                case WODElement element -> completeIdentifier4Element(element, parameters, context, result);
                                case WODComponent component -> completeIdentifier4Component(component, parameters, context, result);
                                case WODBinding binding -> completeIdentifier4Binding(binding, parameters, context, result);
                                case WODParentBinding parentBinding -> completeIdentifier4ParentBinding(parentBinding, result);
                                case WODKeyPath keyPath -> completeIdentifier4KeyPath(keyPath, parameters, context, result);
                                case WODKeyPathElement keyPath -> completeIdentifier4KeyPath((WODKeyPath)keyPath.getParent(), parameters, context, result);
                                default -> {System.out.println("Unknown parent type: " + parent.getClass().getName());}
                            }
                        }
                    }
                }
        );

        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement(WODTypes.IDENTIFIER).withParent(WODComponent.class),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters,
                                                  @NotNull ProcessingContext context,
                                                  @NotNull CompletionResultSet result) {
                        PsiElement position = parameters.getPosition();
                        PsiElement parent = position.getParent();
                        completeIdentifier4Component((WODComponent) parent, parameters, context, result);
                    }
                }
        );
    }

    protected void completeIdentifier4ParentBinding(@NotNull WODParentBinding parentBinding,
                                                    @NotNull CompletionResultSet result) {
        PsiDirectory componentFolder = WOPsiUtil.getComponentFolder(parentBinding);
        if (componentFolder == null) {
            return;
        }
        APIFile api = WOPsiUtil.getApiFile(componentFolder);
        if (api != null) {
            api.getBindingNames().forEach(name -> result.addElement(LookupElementBuilder.create(name)));
        }
    }

    protected void completeIdentifier4Element(@NotNull WODElement element,
                                              @NotNull CompletionParameters parameters,
                                              @NotNull ProcessingContext context,
                                              @NotNull CompletionResultSet result) {

    }

    protected void completeIdentifier4Component(@NotNull WODComponent component,
                                                @NotNull CompletionParameters parameters,
                                                @NotNull ProcessingContext context,
                                                @NotNull CompletionResultSet result) {
        Project project = component.getProject();
        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);

        PsiClass baseClass = facade.findClass(WO_ELEMENT_FQN, scope);
        if (baseClass == null) {
            return;
        }

        ClassInheritorsSearch.search(baseClass, scope, true).forEach(clazz -> {
            String className = clazz.getName();
            if (className != null) {
                // 3. Add to completion lookup list
                result.addElement(
                        LookupElementBuilder.create(clazz, className)
                                .withIcon(AllIcons.Nodes.Class)
                                .withTypeText(clazz.getContainingFile().getName(), true)
                                .bold()
                );
            }
            return true;
        });
    }

    protected void completeIdentifier4Binding(@NotNull WODBinding binding,
                                              @NotNull CompletionParameters parameters,
                                              @NotNull ProcessingContext context,
                                              @NotNull CompletionResultSet result) {
        // Determine Classname
        WODAssignment wodAss = (WODAssignment) binding.getParent();
        WODAssignmentList wodList = (WODAssignmentList) wodAss.getParent();
        WODDeclaration wodDeclaration = (WODDeclaration) wodList.getParent();
        WODComponent wodComponent = wodDeclaration.getWODComponent();
        if (wodComponent == null) {
            return;
        }

        // WO Core
        Set<WOSystemBindingDefinitions.Binding> systemBinding = WOSystemBindingDefinitions.getBindingsForShortClassName(wodComponent.getIdentifier().getText());
        if (!systemBinding.isEmpty()) {
            systemBinding.forEach(sysBinding -> result.addElement(LookupElementBuilder.create(sysBinding.name)));
        }
        else {
            // Component with .api file
            PsiDirectory componentFolder = WOPsiUtil.getComponentFolder(wodComponent);
            if (componentFolder == null) {
                return;
            }
            APIFile api = WOPsiUtil.getApiFile(componentFolder);
            if (api != null) {
                api.getBindingNames().forEach(name -> result.addElement(LookupElementBuilder.create(name)));
            }
        }
    }

    protected void completeIdentifier4KeyPath(@NotNull WODKeyPath keyPath,
                                              @NotNull CompletionParameters parameters,
                                              @NotNull ProcessingContext context,
                                              @NotNull CompletionResultSet result) {
        PsiClass baseClass = WOPsiUtil.getPsiClass(keyPath);
        PsiMember lastMember = KeyValueCodingUtil.resolveWODKeyPath(baseClass, keyPath, true);
        if (!(lastMember instanceof PsiClass lastClass)) {
            return;
        }
        KeyValueCodingUtil.addCompletionSuggestions(result, lastClass);
    }
}
