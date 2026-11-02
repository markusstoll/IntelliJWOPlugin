package org.wocommunity.plugins.intellij.wod;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.components.api.WOComponentApiBinding;
import org.wocommunity.plugins.intellij.psi.wod.WODFile;
import org.wocommunity.plugins.intellij.wotemplate.WOApiBindingDefinitions;
import org.wocommunity.plugins.intellij.wotemplate.WOSystemBindingDefinitions;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Inspection that flags missing required WebObjects bindings on elements in .wod files
 * based on Component API (.api) definitions and system WebObjectDefinitions.xml.
 */
public class WodRequiredBindingsInspection extends LocalInspectionTool {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull PsiFile file) {
                if (!(file instanceof WODFile) && !file.getName().endsWith(".wod")) {
                    return;
                }

                VirtualFile vFile = PsiUtilCore.getVirtualFile(file);
                if (vFile == null) {
                    return;
                }

                WodModel model = WodParser.getModel(vFile);
                Project project = file.getProject();

                for (WodElement elem : model.getElements()) {
                    checkElementRequiredBindings(file, elem, holder, project);
                }
            }
        };
    }

    private static void checkElementRequiredBindings(@NotNull PsiFile file,
                                                     @NotNull WodElement elem,
                                                     @NotNull ProblemsHolder holder,
                                                     @NotNull Project project) {
        String typeName = elem.getTypeName();
        if (typeName == null || typeName.isBlank()) {
            return;
        }

        Map<String, String> missingRequired = new LinkedHashMap<>();

        // 1. System binding definitions (e.g. WOConditional -> condition, WORepetition -> list)
        Set<String> sysRequired = WOSystemBindingDefinitions.getRequiredBindingsForShortClassName(typeName);
        for (String req : sysRequired) {
            if (!elem.hasBinding(req)) {
                missingRequired.put(req, "'" + req + "' is a required binding for " + typeName + ".");
            }
        }

        // 2. Component API (.api) definitions for custom components
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(typeName, scope);
        if (psiClass == null) {
            PsiClass[] classes = PsiShortNamesCache.getInstance(project).getClassesByName(typeName, scope);
            if (classes.length > 0) {
                psiClass = classes[0];
            }
        }

        if (psiClass != null) {
            Map<String, WOComponentApiBinding> apiBindings = WOApiBindingDefinitions.getApiBindingsForPsiClass(psiClass);
            for (WOComponentApiBinding b : apiBindings.values()) {
                if (b.isRequired() && !elem.hasBinding(b.getName())) {
                    String msg = (b.getRequiredMessage() != null && !b.getRequiredMessage().isBlank())
                            ? b.getRequiredMessage()
                            : "'" + b.getName() + "' is a required binding for " + typeName + ".";
                    missingRequired.put(b.getName(), msg);
                }
            }
        }

        if (missingRequired.isEmpty()) {
            return;
        }

        TextRange targetRange = elem.getNameRange();
        if (targetRange == null) {
            targetRange = elem.getTypeRange();
        }
        if (targetRange == null) {
            return;
        }

        PsiElement targetPsi = file.findElementAt(targetRange.getStartOffset());
        if (targetPsi == null) {
            targetPsi = file;
        }

        for (Map.Entry<String, String> entry : missingRequired.entrySet()) {
            String bindingName = entry.getKey();
            String message = entry.getValue();

            holder.registerProblem(
                    targetPsi,
                    message,
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    new AddMissingWodBindingQuickFix(elem.getName(), bindingName)
            );
        }
    }

    public static class AddMissingWodBindingQuickFix implements LocalQuickFix {
        private final String elementName;
        private final String bindingName;

        public AddMissingWodBindingQuickFix(String elementName, String bindingName) {
            this.elementName = elementName;
            this.bindingName = bindingName;
        }

        @Override
        public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String getName() {
            return "Add missing binding '" + bindingName + "'";
        }

        @Override
        public @NotNull String getFamilyName() {
            return "Add missing WebObjects binding";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiFile file = descriptor.getPsiElement().getContainingFile();
            if (file == null) {
                return;
            }
            VirtualFile vFile = PsiUtilCore.getVirtualFile(file);
            if (vFile == null) {
                return;
            }

            Document document = FileDocumentManager.getInstance().getDocument(vFile);
            if (document == null) {
                return;
            }

            WodModel model = WodParser.parse(document.getText());
            WodElement elem = model.getElement(elementName);
            if (elem == null) {
                return;
            }

            int insertOffset = -1;
            String text = document.getText();

            if (!elem.getBindings().isEmpty()) {
                int maxEnd = -1;
                for (WodBinding b : elem.getBindings().values()) {
                    if (b.getValueRange() != null && b.getValueRange().getEndOffset() > maxEnd) {
                        maxEnd = b.getValueRange().getEndOffset();
                    }
                }
                if (maxEnd > 0) {
                    int semi = text.indexOf(';', maxEnd);
                    insertOffset = (semi >= 0) ? semi + 1 : maxEnd;
                }
            }

            if (insertOffset < 0) {
                if (elem.getTypeRange() != null) {
                    int lBrace = text.indexOf('{', elem.getTypeRange().getEndOffset());
                    if (lBrace >= 0) {
                        insertOffset = lBrace + 1;
                    }
                }
            }

            if (insertOffset < 0) {
                return;
            }

            String toInsert = "\n    " + bindingName + " = ;";
            document.insertString(insertOffset, toInsert);
            FileDocumentManager.getInstance().saveDocument(document);
            PsiDocumentManager.getInstance(project).commitDocument(document);

            int caretTarget = insertOffset + "\n    ".length() + bindingName.length() + " = ".length();
            var editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (editor != null && editor.getDocument().equals(document)) {
                editor.getCaretModel().moveToOffset(caretTarget);
            }
        }
    }
}
