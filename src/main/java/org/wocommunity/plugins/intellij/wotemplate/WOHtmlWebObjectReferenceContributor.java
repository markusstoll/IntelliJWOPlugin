package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.psi.wod.WODElement;
import org.wocommunity.plugins.intellij.psi.wod.WODFile;
import org.wocommunity.plugins.intellij.tools.WOFileUtil;
import org.wocommunity.plugins.intellij.wod.WodModel;
import org.wocommunity.plugins.intellij.wod.WodParser;

/** Resolves {@code <webobject name="...">} names to their WOD declarations. */
public final class WOHtmlWebObjectReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(XmlAttributeValue.class),
                new PsiReferenceProvider() {
                    @Override
                    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                                            @NotNull ProcessingContext context) {
                        if (!(element instanceof XmlAttributeValue value)
                                || !(value.getParent() instanceof XmlAttribute attribute)
                                || !isWebObjectName(attribute)) {
                            return PsiReference.EMPTY_ARRAY;
                        }
                        return new PsiReference[]{new WODNameReference(value,
                                ElementManipulators.getValueTextRange(value))};
                    }
                }, PsiReferenceRegistrar.HIGHER_PRIORITY);
    }

    private static boolean isWebObjectName(@NotNull XmlAttribute attribute) {
        PsiElement parent = attribute.getParent();
        return parent instanceof XmlTag tag
                && WOHtmlWebObjectUtil.isWebObjectTag(tag)
                && "name".equalsIgnoreCase(attribute.getName());
    }

    private static final class WODNameReference extends PsiReferenceBase<PsiElement> {
        private WODNameReference(@NotNull PsiElement element, @NotNull com.intellij.openapi.util.TextRange range) {
            super(element, range);
        }

        @Override
        public @NotNull com.intellij.openapi.util.TextRange getRangeInElement() {
            com.intellij.openapi.util.TextRange range = super.getRangeInElement();
            String text = myElement.getText();
            if (text != null && text.endsWith("/")) {
                return new com.intellij.openapi.util.TextRange(range.getStartOffset(), Math.max(range.getStartOffset(), range.getEndOffset() - 1));
            }
            return range;
        }

        @Override
        public @Nullable PsiElement resolve() {
            String name = null;
            if (myElement instanceof XmlAttributeValue val) {
                name = val.getValue();
            }
            if (name == null || name.isBlank()) {
                name = myElement.getText();
            }
            name = WOHtmlWebObjectUtil.cleanWebObjectName(name);
            if (name == null || name.isBlank()) {
                return null;
            }
            final String finalName = name;

            PsiFile psiFile = myElement.getContainingFile();
            if (psiFile == null) {
                return null;
            }
            VirtualFile htmlFile = PsiUtilCore.getVirtualFile(psiFile);
            if (htmlFile == null && psiFile.getOriginalFile() != null) {
                htmlFile = PsiUtilCore.getVirtualFile(psiFile.getOriginalFile());
            }
            if (htmlFile == null) {
                htmlFile = psiFile.getViewProvider().getVirtualFile();
            }
            if (htmlFile == null) {
                return null;
            }

            VirtualFile componentDirectory = WOFileUtil.getComponent(htmlFile);
            if (componentDirectory == null) {
                componentDirectory = htmlFile.getParent();
            }
            if (componentDirectory == null) {
                return null;
            }

            String componentName = componentDirectory.getName().endsWith(".wo")
                    ? componentDirectory.getName().substring(0, componentDirectory.getName().length() - 3)
                    : htmlFile.getNameWithoutExtension();
            VirtualFile wodVirtualFile = componentDirectory.findChild(componentName + ".wod");
            if (wodVirtualFile == null || !wodVirtualFile.isValid()) {
                return null;
            }

            WodModel model = WodParser.getModel(wodVirtualFile);
            org.wocommunity.plugins.intellij.wod.WodElement element = model.getElement(name);
            if (element != null && element.getNameRange() != null) {
                PsiFile wodPsiFile = PsiManager.getInstance(myElement.getProject()).findFile(wodVirtualFile);
                if (wodPsiFile != null) {
                    PsiElement target = wodPsiFile.findElementAt(element.getNameRange().getStartOffset());
                    if (target != null) {
                        return new WODNavigatableElement(target, wodVirtualFile,
                                element.getNameRange().getStartOffset(), element.getNameRange().getEndOffset());
                    }
                }
            }

            PsiElement file = PsiManager.getInstance(myElement.getProject()).findFile(wodVirtualFile);
            if (!(file instanceof WODFile wod)) {
                return null;
            }
            return PsiTreeUtil.findChildrenOfType(wod, WODElement.class).stream()
                    .filter(el -> finalName.equals(el.getIdentifier().getText()))
                    .findFirst()
                    .orElse(null);
        }

        private static String stripQuotes(String text) {
            if (text.length() >= 2
                    && ((text.startsWith("\"") && text.endsWith("\""))
                    || (text.startsWith("'") && text.endsWith("'")))) {
                return text.substring(1, text.length() - 1);
            }
            return text;
        }
    }
}
