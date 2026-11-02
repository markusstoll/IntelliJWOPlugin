package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.XmlElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.util.XmlTagUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Inspection that flags missing required WebObjects bindings on {@code wo:*} tags
 * based on Component API (.api) definitions.
 */
public class WORequiredBindingsInspection extends LocalInspectionTool {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new XmlElementVisitor() {
            @Override
            public void visitXmlTag(@NotNull XmlTag tag) {
                if (!WOHtmlCompletionUtil.isWoComponentHtml(tag.getContainingFile())) {
                    return;
                }
                if (!WOHtmlCompletionUtil.isWoPrefixedTag(tag)) {
                    return;
                }

                XmlElementDescriptor descriptor = tag.getDescriptor();
                if (!(descriptor instanceof WOXmlElementDescriptor wod)) {
                    return;
                }

                XmlAttributeDescriptor[] attrDescriptors = wod.getAttributesDescriptors(tag);
                if (attrDescriptors == null || attrDescriptors.length == 0) {
                    return;
                }

                Set<String> presentAttributes = new HashSet<>();
                for (XmlAttribute attr : tag.getAttributes()) {
                    String attrName = attr.getName();
                    if (attrName != null && !attrName.isBlank()) {
                        presentAttributes.add(attrName);
                    }
                }

                for (XmlAttributeDescriptor ad : attrDescriptors) {
                    if (ad.isRequired()) {
                        String requiredName = ad.getName();
                        if (requiredName != null && !requiredName.isBlank() && !presentAttributes.contains(requiredName)) {
                            String message;
                            if (ad instanceof WOXmlAttributeDescriptor woad
                                    && woad.getRequiredMessage() != null
                                    && !woad.getRequiredMessage().isBlank()) {
                                message = woad.getRequiredMessage();
                            } else {
                                message = "'" + requiredName + "' is a required binding.";
                            }

                            PsiElement targetElement = XmlTagUtil.getStartTagNameElement(tag);
                            if (targetElement == null) {
                                targetElement = tag;
                            }

                            holder.registerProblem(
                                    targetElement,
                                    message,
                                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                    new AddMissingBindingQuickFix(requiredName)
                            );
                        }
                    }
                }
            }
        };
    }

    private static class AddMissingBindingQuickFix implements LocalQuickFix {
        private final String bindingName;

        AddMissingBindingQuickFix(String bindingName) {
            this.bindingName = bindingName;
        }

        @Override
        public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String getName() {
            return "Add required binding '" + bindingName + "'";
        }

        @Override
        public @NotNull String getFamilyName() {
            return "Add missing WO binding";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            XmlTag tag = element instanceof XmlTag ? (XmlTag) element : PsiTreeUtil.getParentOfType(element, XmlTag.class);
            if (tag != null) {
                tag.setAttribute(bindingName, "");
                XmlAttribute created = tag.getAttribute(bindingName);
                if (created != null) {
                    XmlAttributeValue val = created.getValueElement();
                    if (val != null) {
                        FileEditorManager fem = FileEditorManager.getInstance(project);
                        Editor editor = fem.getSelectedTextEditor();
                        if (editor != null) {
                            int offset = val.getTextOffset();
                            String text = val.getText();
                            if (text != null && (text.startsWith("\"") || text.startsWith("'"))) {
                                offset++;
                            }
                            editor.getCaretModel().moveToOffset(offset);
                        }
                    }
                }
            }
        }
    }
}

