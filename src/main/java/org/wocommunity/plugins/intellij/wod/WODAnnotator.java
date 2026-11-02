package org.wocommunity.plugins.intellij.wod;

import com.intellij.ide.highlighter.JavaHighlightingColors;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.psi.references.WODElementReference;
import org.wocommunity.plugins.intellij.tools.KeyValueCodingUtil;
import org.wocommunity.plugins.intellij.tools.WOPsiUtil;
import org.wocommunity.plugins.intellij.psi.api.APIFile;
import org.wocommunity.plugins.intellij.psi.wod.*;
import org.wocommunity.plugins.intellij.wotemplate.WOSystemBindingDefinitions;

import java.util.*;
import java.util.regex.Pattern;

/*
 * TODO: Most of these checks should be moved to the references.
 *  Then the references can be used to annotate the wod file.
 */
final class WODAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement psiElement, @NotNull AnnotationHolder holder) {
        switch (psiElement) {
            case WODElement element -> annotateElement(element, holder);
            case WODComponent component -> annotateComponent(component, holder);
            case WODBinding binding -> annotateBinding(binding, holder);
            case WODValue value -> annotateValue(value, holder);
            default -> {}
        }
    }

    private WODFile getWODFile(PsiElement element) {
        PsiElement iterElement = element;
        while (iterElement != null && !(iterElement instanceof WODFile)) {
            iterElement = iterElement.getParent();
        }
        return (WODFile) iterElement;
    }

    private WODDeclaration getDeclaration(PsiElement element) {
        if (element.getParent() instanceof WODDeclaration) {
            return (WODDeclaration) element.getParent();
        }
        return null;
    }
    private WODComponent getComponent(PsiElement element) {
        if (element.getParent() instanceof WODDeclaration) {
            return ((WODDeclaration) element.getParent()).getWODComponent();
        }
        return null;
    }

    private void annotateElement(@NotNull WODElement element, @NotNull AnnotationHolder holder) {
        WODDeclaration declaration = getDeclaration(element);
        WODComponent component = getComponent(element);
        if (declaration == null || component == null) {
            return;
        }

        // Normal color
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .textAttributes(TextAttributesKey.find("HTML_ATTRIBUTE_NAME"))
                .create();

        // Find out which elements are named in the HTML file and match against the identifier of this element
        //       -> Identifier is not used => weak warning
        PsiReference[] references = element.getReferences();

        for (PsiReference reference : references) {
            if (reference instanceof WODElementReference) {
                PsiElement resolved = reference.resolve();
                if (resolved == null) {
                    holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "There is no element named '" + element.getIdentifier().getText() + "' in your component HTML file")
                            .range(element)
                            .create();
                }
            }
        }

        // Check duplicate identifiers => error
        PsiElement previousDeclaration = declaration.getPrevSibling();
        while (previousDeclaration != null) {
            if (previousDeclaration instanceof WODDeclaration && ((WODDeclaration) previousDeclaration).getWODElement().getIdentifier().getText().equals(element.getIdentifier().getText())) {
                holder.newAnnotation(HighlightSeverity.ERROR, "Element '" + element.getIdentifier().getText() + "' is already defined above")
                        .range(element)
                        .create();
                break;
            }
            previousDeclaration = previousDeclaration.getPrevSibling();
        }

        // Check if all required bindings are provided
        Set<WOSystemBindingDefinitions.Binding> bindings = WOSystemBindingDefinitions.getBindingsForShortClassName(component.getIdentifier().getText());
        WODAssignmentList assignmentList = declaration.getWODAssignmentList();
        if (assignmentList != null) {
            Set<String> assignedBindings = new HashSet<>();
            for (@NotNull PsiElement psiElement : assignmentList.getChildren()) {
                if (psiElement instanceof WODAssignment assignment) {
                    assignedBindings.add(assignment.getWODBinding().getIdentifier().getText());
                }
            }
            for (WOSystemBindingDefinitions.Binding b : bindings) {
                if (b.required && !assignedBindings.contains(b.name)) {
                    holder.newAnnotation(HighlightSeverity.ERROR, "Missing required binding '" + b.name + "'")
                            .range(element)
                            .create();
                }
            }
        }
    }

    private void annotateComponent(@NotNull WODComponent component, @NotNull AnnotationHolder holder) {
        // Validate component name against WebObjects java classes
        //       -> Valid => normal color
        //       -> Invalid => error annotation
        // normal color
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(component)
                .textAttributes(TextAttributesKey.find("HTML_TAG_NAME"))
                .create();

        PsiReference[] references = component.getReferences();

        for (PsiReference reference : references) {
            boolean resolved;
            if (reference instanceof PsiPolyVariantReference poly) {
                resolved = poly.multiResolve(false).length > 0;
            } else {
                resolved = reference.resolve() != null;
            }
            if (!resolved) {
                String componentName = component.getIdentifier() != null ? component.getIdentifier().getText() : component.getText();
                holder.newAnnotation(HighlightSeverity.ERROR, "Cannot resolve component '" + componentName + "'")
                        .range(component)
                        .create();
            }
        }
    }

    private void annotateBinding(@NotNull WODBinding binding, @NotNull AnnotationHolder holder) {
        // TODO: Validate against known bindings? Does that make sense i.e. with components that take all  data-xxx bindings
        // normal color
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(binding)
                .textAttributes(JavaHighlightingColors.INSTANCE_FIELD_ATTRIBUTES)
                .create();


        // binding -> assignment -> assignmentList -> declaration
        PsiElement declaration = binding.getParent().getParent().getParent();
        if (!(declaration instanceof WODDeclaration)) {
            return;
        }

        WODComponent component = ((WODDeclaration) declaration).getWODComponent();
        if (component == null) {
            return;
        }

        String componentName = component.getIdentifier().getText();
        String bindingName = binding.getIdentifier().getText();

        // Check target components
        boolean found = false;
        // Check System components first
        Set<WOSystemBindingDefinitions.Binding> bindings = WOSystemBindingDefinitions.getBindingsForShortClassName(componentName);
        for (WOSystemBindingDefinitions.Binding b : bindings) {
            if (b.name.equals(bindingName)) {
                // binding found
                found = true;
                break;
            }
        }
        // Check custom components
        if (!found) {
            PsiDirectory componentFolder = WOPsiUtil.getComponentFolder(component);
            if (componentFolder != null) {
                APIFile apiFile = WOPsiUtil.getApiFile(componentFolder);
                if (apiFile != null) {
                    found = apiFile.getBindingNames().contains(bindingName);
                }
            }
            if (!found) {
                PsiClass compClass = null;
                try {
                    compClass = WOPsiUtil.getPsiClassForComponentName(componentName, component.getProject());
                } catch (Exception ignored) {
                }
                if (compClass == null) {
                    compClass = org.wocommunity.plugins.intellij.wotemplate.WOHtmlElementDescriptorProvider
                            .resolveWoTagClass(component.getProject(), componentName);
                }
                if (compClass != null) {
                    found = !org.wocommunity.plugins.intellij.wotemplate.WOComponentClassResolver
                            .findSingleKeyTargets(compClass, bindingName).isEmpty();
                }
            }
        }
        // binding not found
        if (!found) {
            holder.newAnnotation(HighlightSeverity.WARNING, "Binding '" + binding.getIdentifier().getText() + "' is not defined for component '" + component.getIdentifier().getText() + "'")
                    .range(binding)
                    .create();
        }
    }

    /** Writing "// valid" behind a valid skips validation. Matching is case-insensitive but the comment must start with "valid". */
    private final Pattern validCommentPattern = Pattern.compile("^// ?valid.*", Pattern.CASE_INSENSITIVE);
    private void annotateValue(@NotNull WODValue value, @NotNull AnnotationHolder holder) {
        PsiElement parent = value.getParent();
        if (parent instanceof WODAssignment assignment) {
            if (assignment.getWODAssignmentComment() != null && validCommentPattern.matcher(assignment.getWODAssignmentComment().getText()).matches()) {
                return;
            }
        }

        if (value.getString() != null) {
            // TODO: Check References for KeyPath Targets?
            return;
        }
        if (value.getNumber() != null) {
            // TODO Are there cases where simple numbers are invalid?
            return;
        }
        if (value.getBoolean() != null) {
            return;
        }
        if (value.getWODParentBinding() != null) {
            annotateParentBinding(value.getWODParentBinding(), holder);
            return;
        }
        PsiClass baseClass = WOPsiUtil.getPsiClass(value);
        if (baseClass == null) {
            return;
        }
        if (value.getWODKeyPath() != null) {
            String kpText = value.getWODKeyPath().getText();
            if ("YES".equalsIgnoreCase(kpText) || "NO".equalsIgnoreCase(kpText)
                    || "true".equalsIgnoreCase(kpText) || "false".equalsIgnoreCase(kpText)) {
                return;
            }
            annotateKeyPath(value, baseClass, value.getWODKeyPath(), holder);
        }
    }

    private void annotateParentBinding(@NotNull WODParentBinding parentBinding, @NotNull AnnotationHolder holder) {
        PsiDirectory componentFolder = WOPsiUtil.getComponentFolder(parentBinding);
        APIFile apiFile = componentFolder == null ? null : WOPsiUtil.getApiFile(componentFolder);
        String bindingName = parentBinding.getIdentifier().getText();

        if (apiFile == null || !apiFile.getBindingNames().contains(bindingName)) {
            holder.newAnnotation(HighlightSeverity.WARNING,
                            "Parent binding '" + bindingName + "' is not declared in this component's .api file")
                    .range(parentBinding)
                    .create();
        }
    }
    private void annotateKeyPath(@NotNull WODValue value, @NotNull PsiClass baseClass, @NotNull WODKeyPath keyPath, @NotNull AnnotationHolder holder) {
        PsiClass iterClass = baseClass;
        for (WODKeyPathElement element : keyPath.getWODKeyPathElementList()) {
            // Dieser Aufruf ZWINGT den ReferenceContributor anzuspringen!
            PsiReference[] references = element.getReferences();

            for (PsiReference reference : references) {
                PsiElement target = reference.resolve();

                if (target instanceof PsiField field) {
                    TextAttributesKey fieldKey;
                    if (field.hasModifierProperty(PsiModifier.STATIC)) {
                        fieldKey = field.hasModifierProperty(PsiModifier.FINAL)
                                ? JavaHighlightingColors.STATIC_FINAL_FIELD_ATTRIBUTES
                                : JavaHighlightingColors.STATIC_FIELD_ATTRIBUTES;
                    } else {
                        fieldKey = JavaHighlightingColors.INSTANCE_FIELD_ATTRIBUTES;
                    }

                    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                            .range(element)
                            .textAttributes(fieldKey)
                            .create();

                    iterClass = PsiTypesUtil.getPsiClass(field.getType());
                }
                else if (target instanceof PsiMethod method) {
                    TextAttributesKey methodKey = method.hasModifierProperty(PsiModifier.STATIC)
                            ? JavaHighlightingColors.STATIC_METHOD_ATTRIBUTES
                            : JavaHighlightingColors.METHOD_CALL_ATTRIBUTES;

                    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                            .range(element)
                            .textAttributes(methodKey)
                            .create();

                    iterClass = PsiTypesUtil.getPsiClass(method.getReturnType());
                }
                // Ignore if iterClass is baseClass or else every missing field would show this warning...
                else if (target == null && !baseClass.equals(iterClass) && implementsKeyValueCoding(iterClass)) {
                    holder.newAnnotation(HighlightSeverity.WARNING, "Unable to verify key '" + element.getText() + "' because the class " + iterClass.getName() + " implements dynamic Key Value Coding")
                            .range(element)
                            .create();
                }
                else if (target == null && iterClass != null /* is probably always true */) {
                    holder.newAnnotation(HighlightSeverity.ERROR, "There is no key '" + element.getText() + "' in " + iterClass.getName())
                            .range(element)
                            .create();
                }
                else {
                    holder.newAnnotation(HighlightSeverity.WARNING, "Error in plugin: Keypath resolved to " + target)
                            .range(element)
                            .create();
                }
            }
        }
    }

    public boolean implementsKeyValueCoding(PsiClass psiClass) {
        if (psiClass == null) return false;

        // 1. Locate the interface target in the project context / classpath
        PsiClass targetInterface = JavaPsiFacade.getInstance(psiClass.getProject())
                .findClass("com.webobjects.foundation.NSKeyValueCoding",
                        GlobalSearchScope.allScope(psiClass.getProject()));

        if (targetInterface == null) {
            // The WebObjects library isn't on the project's classpath
            return false;
        }

        // 2. Perform a deep structural check up the inheritance tree
        return psiClass.isInheritor(targetInterface, true);
    }

}
