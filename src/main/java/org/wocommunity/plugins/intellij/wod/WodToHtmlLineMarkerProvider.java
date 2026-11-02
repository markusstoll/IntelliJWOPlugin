package org.wocommunity.plugins.intellij.wod;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.psi.wod.WODFile;
import org.wocommunity.plugins.intellij.tools.WOIcons;

import java.util.Collection;

public class WodToHtmlLineMarkerProvider implements LineMarkerProvider {

    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        PsiFile containingFile = element.getContainingFile();
        if (containingFile == null || (!(containingFile instanceof WODFile) && !containingFile.getName().endsWith(".wod"))) {
            return null;
        }

        VirtualFile wodVFile = PsiUtilCore.getVirtualFile(element.getContainingFile());
        if (wodVFile == null || wodVFile.getParent() == null) {
            return null;
        }

        VirtualFile woDir = wodVFile.getParent();
        if (!woDir.getName().endsWith(".wo")) {
            return null;
        }

        WodModel model = WodParser.getModel(wodVFile);
        String name = element.getText().trim();
        WodElement wodElement = model.getElement(name);
        if (wodElement == null || wodElement.getNameRange() == null) {
            return null;
        }

        // Check that element is at the start offset of this element's name
        if (element.getTextRange().getStartOffset() != wodElement.getNameRange().getStartOffset()) {
            return null;
        }

        String componentName = woDir.getName().substring(0, woDir.getName().length() - 3);
        VirtualFile htmlFile = woDir.findChild(componentName + ".html");
        if (htmlFile == null || !htmlFile.isValid()) {
            return null;
        }

        PsiFile psiHtml = PsiManager.getInstance(element.getProject()).findFile(htmlFile);
        if (!(psiHtml instanceof XmlFile xmlFile)) {
            return null;
        }

        XmlTag matchingTag = findMatchingTag(xmlFile, name);
        if (matchingTag == null) {
            return null;
        }

        final int targetOffset = matchingTag.getTextOffset();
        final String finalName = name;

        return new LineMarkerInfo<>(
                element,
                element.getTextRange(),
                WOIcons.WOCOMPONENT_ICON,
                e -> "Navigate to <webobject name=\"" + finalName + "\"> in " + htmlFile.getName(),
                (mouseEvent, elt) -> WOHtmlToWodNavigationUtil.navigateToHtml(elt.getProject(), htmlFile, targetOffset),
                GutterIconRenderer.Alignment.LEFT,
                () -> "Navigate to WebObject tag in HTML"
        );
    }

    private static XmlTag findMatchingTag(XmlFile xmlFile, String name) {
        Collection<XmlTag> tags = PsiTreeUtil.findChildrenOfType(xmlFile, XmlTag.class);
        for (XmlTag tag : tags) {
            if ("webobject".equalsIgnoreCase(tag.getLocalName())) {
                String val = tag.getAttributeValue("name");
                if (val == null) {
                    val = tag.getAttributeValue("NAME");
                }
                if (name.equals(val)) {
                    return tag;
                }
            }
        }
        return null;
    }
}
