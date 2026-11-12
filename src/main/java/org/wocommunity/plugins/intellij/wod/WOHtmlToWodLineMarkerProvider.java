package org.wocommunity.plugins.intellij.wod;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.tools.WOIcons;
import org.wocommunity.plugins.intellij.wotemplate.WOHtmlWebObjectUtil;

public class WOHtmlToWodLineMarkerProvider implements LineMarkerProvider {

    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        if (!(element instanceof XmlToken token)) {
            return null;
        }
        if (token.getTokenType() != XmlTokenType.XML_NAME) {
            return null;
        }
        if (!(token.getParent() instanceof XmlTag tag)) {
            return null;
        }
        if (!WOHtmlWebObjectUtil.isWebObjectTag(tag)) {
            return null;
        }

        // Only add line marker on opening tag name token
        PsiElement firstToken = tag.getFirstChild();
        while (firstToken != null && !(firstToken instanceof XmlToken && ((XmlToken) firstToken).getTokenType() == XmlTokenType.XML_NAME)) {
            firstToken = firstToken.getNextSibling();
        }
        if (!element.equals(firstToken)) {
            return null;
        }

        String name = WOHtmlWebObjectUtil.getWebObjectName(tag);
        if (name == null || name.isBlank()) {
            return null;
        }

        PsiFile file = tag.getContainingFile();
        if (file == null) {
            return null;
        }
        VirtualFile htmlVFile = PsiUtilCore.getVirtualFile(file);
        if (htmlVFile == null && file.getOriginalFile() != null) {
            htmlVFile = PsiUtilCore.getVirtualFile(file.getOriginalFile());
        }
        if (htmlVFile == null) {
            htmlVFile = file.getViewProvider().getVirtualFile();
        }
        if (htmlVFile == null || htmlVFile.getParent() == null) {
            return null;
        }

        VirtualFile woDir = htmlVFile.getParent();
        if (!woDir.getName().endsWith(".wo")) {
            return null;
        }
        String componentName = woDir.getName().substring(0, woDir.getName().length() - 3);
        VirtualFile wodFile = woDir.findChild(componentName + ".wod");
        final String finalName = name;
        final VirtualFile finalWodFile = wodFile;
        final VirtualFile finalWoDir = woDir;
        final String wodDisplayName = wodFile != null ? wodFile.getName() : (componentName + ".wod");

        if (wodFile != null && wodFile.isValid()) {
            WodModel model = WodParser.getModel(wodFile);
            WodElement wodElement = model.getElement(name);
            if (wodElement != null && wodElement.getNameRange() != null) {
                final String typeName = wodElement.getTypeName();

                return new LineMarkerInfo<>(
                        element,
                        element.getTextRange(),
                        WOIcons.WOCOMPONENT_ICON,
                        e -> "Navigate to '" + finalName + " : " + typeName + "' in " + wodFile.getName(),
                        (mouseEvent, elt) -> {
                            WodModel currentModel = WodParser.getModel(finalWodFile);
                            WodElement currentElement = currentModel.getElement(finalName);
                            if (currentElement != null && currentElement.getNameRange() != null) {
                                WOHtmlToWodNavigationUtil.navigateToWodAndSelect(
                                        elt.getProject(),
                                        finalWodFile,
                                        currentElement.getNameRange().getStartOffset(),
                                        currentElement.getNameRange().getEndOffset()
                                );
                            } else {
                                WodElementCreationUtil.createAndNavigateToElement(
                                        elt.getProject(), finalWodFile, finalWoDir, finalName, "WOComponent"
                                );
                            }
                        },
                        GutterIconRenderer.Alignment.LEFT,
                        () -> "Navigate to WebObject in WOD"
                );
            }
        }

        // Definition does not exist in .wod yet: provide gutter icon to create it
        return new LineMarkerInfo<>(
                element,
                element.getTextRange(),
                WOIcons.WOCOMPONENT_ICON,
                e -> "Create '" + finalName + " : WOComponent' in " + wodDisplayName,
                (mouseEvent, elt) -> WodElementCreationUtil.createAndNavigateToElement(
                        elt.getProject(), finalWodFile, finalWoDir, finalName, "WOComponent"
                ),
                GutterIconRenderer.Alignment.LEFT,
                () -> "Create WebObject definition in WOD"
        );
    }
}

