package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.psi.api.APIFile;
import org.wocommunity.plugins.intellij.tools.WOPsiUtil;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Annotator for WebObjects HTML component templates.
 * Validates:
 * 1. Inline keypath bindings ($property, $user.name) against the component Java class.
 * 2. Parent bindings (^bindingName) against the component's .api file.
 * 3. Component binding names on &lt;wo:*&gt; tags against the target component's API/Java members.
 */
public class WOHtmlAnnotator implements Annotator {

    private static final Set<String> COMMON_HTML_ATTRS = Set.of(
            "id", "class", "style", "name", "title", "dir", "lang", "tabindex", "accesskey", "role",
            "target", "rel", "type", "value", "disabled", "readonly", "checked", "selected",
            "multiple", "placeholder", "autocomplete", "autofocus", "width", "height", "src",
            "href", "alt", "rows", "cols", "action", "method", "enctype", "onclick", "onchange",
            "onsubmit", "onkeyup", "onkeydown", "onmouseover", "onmouseout", "onfocus", "onblur"
    );

    @Override
    public void annotate(@NotNull PsiElement psiElement, @NotNull AnnotationHolder holder) {
        PsiFile containingFile = psiElement.getContainingFile();
        if (containingFile == null || !WOHtmlCompletionUtil.isWoComponentHtml(containingFile)) {
            return;
        }

        if (psiElement instanceof XmlAttribute attribute) {
            annotateAttribute(attribute, holder);
        } else if (psiElement instanceof XmlAttributeValue attributeValue) {
            annotateAttributeValue(attributeValue, holder);
        }
    }

    private void annotateAttribute(@NotNull XmlAttribute attribute, @NotNull AnnotationHolder holder) {
        PsiElement parent = attribute.getParent();
        if (!(parent instanceof XmlTag tag) || !WOHtmlCompletionUtil.isWoPrefixedTag(tag)) {
            return;
        }

        String attrName = attribute.getName();
        if (attrName == null || attrName.isBlank() || attrName.startsWith("xmlns")) {
            return;
        }

        // Pass-through HTML attributes or data-*/aria-* attributes are always valid
        if (attrName.startsWith("data-") || attrName.startsWith("aria-") || COMMON_HTML_ATTRS.contains(attrName.toLowerCase(Locale.ROOT))) {
            return;
        }

        String componentName = WOComponentTagAliasRegistry.resolveAlias(tag.getName());
        if (componentName == null) {
            componentName = tag.getLocalName();
        }

        // Validate attribute against tag descriptor if available
        XmlElementDescriptor desc = tag.getDescriptor();
        if (desc instanceof WOXmlElementDescriptor woDesc) {
            XmlAttributeDescriptor ad = woDesc.getAttributeDescriptor(attrName, tag);
            if (ad != null) {
                return; // Valid binding
            }
        }

        // Check system bindings
        if (WOSystemBindingDefinitions.getBindingNamesForShortClassName(componentName).contains(attrName)) {
            return;
        }

        // If target component class is resolved, check if binding exists
        Project project = attribute.getProject();
        PsiClass compClass = WOHtmlElementDescriptorProvider.resolveWoTagClass(project, componentName);
        if (compClass != null) {
            if (!WOComponentClassResolver.findSingleKeyTargets(compClass, attrName).isEmpty()) {
                return;
            }
            // Binding is not defined on the component
            PsiElement nameElem = attribute.getFirstChild();
            TextRange range = nameElem != null ? nameElem.getTextRange() : attribute.getTextRange();
            holder.newAnnotation(HighlightSeverity.WARNING, "Binding '" + attrName + "' is not defined for component '" + componentName + "'")
                    .range(range)
                    .create();
        }
    }

    private void annotateAttributeValue(@NotNull XmlAttributeValue attrValue, @NotNull AnnotationHolder holder) {
        String text = attrValue.getText();
        if (text == null || text.isBlank()) {
            return;
        }

        // 1. Check parent bindings: ^bindingName
        String rawValue = text.startsWith("\"") || text.startsWith("'")
                ? text.substring(1, Math.max(1, text.length() - 1))
                : text;
        if (rawValue.startsWith("^")) {
            String bindingName = rawValue.substring(1).trim();
            if (!bindingName.isEmpty()) {
                PsiDirectory compFolder = WOPsiUtil.getComponentFolder(attrValue);
                APIFile apiFile = compFolder != null ? WOPsiUtil.getApiFile(compFolder) : null;
                if (apiFile != null && !apiFile.getBindingNames().contains(bindingName)) {
                    holder.newAnnotation(HighlightSeverity.WARNING, "Parent binding '" + bindingName + "' is not declared in this component's .api file")
                            .range(attrValue.getTextRange())
                            .create();
                }
            }
            return;
        }

        // 2. Check inline keypath bindings: $property, $user.name, $method()
        List<WOHtmlBindingReferenceContributor.BindingSegment> segments =
                WOHtmlBindingReferenceContributor.parseBindingSegments(text);
        if (segments.isEmpty()) {
            return;
        }

        PsiFile file = attrValue.getContainingFile();
        if (file == null) {
            return;
        }
        PsiClass componentClass = WOComponentClassResolver.findComponentClass(file);
        if (componentClass == null) {
            return;
        }

        // Validate keypath segments
        PsiClass currentClass = componentClass;
        int valueStart = attrValue.getTextRange().getStartOffset();

        for (int i = 0; i < segments.size(); i++) {
            WOHtmlBindingReferenceContributor.BindingSegment seg = segments.get(i);
            String key = seg.key();

            // Ignore boolean literals
            if ("YES".equalsIgnoreCase(key) || "NO".equalsIgnoreCase(key)
                    || "true".equalsIgnoreCase(key) || "false".equalsIgnoreCase(key)) {
                continue;
            }

            List<PsiElement> targets = WOComponentClassResolver.findSingleKeyTargets(currentClass, key);
            if (targets.isEmpty()) {
                TextRange segmentRange = new TextRange(
                        valueStart + seg.range().getStartOffset(),
                        valueStart + seg.range().getEndOffset()
                );
                holder.newAnnotation(HighlightSeverity.WARNING, "There is no key '" + key + "' in " + currentClass.getName())
                        .range(segmentRange)
                        .create();
                break; // Stop evaluating sub-segments if previous segment was invalid
            }

            // Resolve next class for subsequent segments
            PsiClass nextClass = WOComponentClassResolver.resolveTypeToClass(targets.get(0));
            if (nextClass == null) {
                break; // Can't resolve type further (e.g. primitive, void, or unknown type)
            }
            currentClass = nextClass;
        }
    }
}
