package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlElementsGroup;
import com.intellij.xml.XmlNSDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class WOXmlElementDescriptor implements XmlElementDescriptor {

    private final @NotNull String name;
    private final @Nullable PsiElement declaration;

    WOXmlElementDescriptor(@NotNull String name, @Nullable PsiElement declaration) {
        this.name = name;
        this.declaration = declaration;
    }

    @Override
    public PsiElement getDeclaration() {
        return declaration;
    }

    @Override
    public @NonNls String getName(PsiElement context) {
        return name;
    }

    @Override
    public @NlsSafe String getName() {
        return name;
    }

    @Override
    public void init(PsiElement element) {
        // stateless descriptor
    }

    @Override
    public @NonNls String getQualifiedName() {
        return name;
    }

    @Override
    public @NonNls String getDefaultName() {
        return name;
    }

    @Override
    public XmlElementDescriptor @NotNull [] getElementsDescriptors(XmlTag context) {
        return EMPTY_ARRAY;
    }

    @Override
    public @Nullable XmlElementDescriptor getElementDescriptor(XmlTag childTag, XmlTag contextTag) {
        return null;
    }

    @Override
    public XmlAttributeDescriptor @NotNull [] getAttributesDescriptors(@Nullable XmlTag context) {
        return XmlAttributeDescriptor.EMPTY;
    }

    @Override
    public @Nullable XmlAttributeDescriptor getAttributeDescriptor(@NonNls String attributeName, @Nullable XmlTag context) {
        return null;
    }

    @Override
    public @Nullable XmlAttributeDescriptor getAttributeDescriptor(XmlAttribute attribute) {
        return null;
    }

    @Override
    public @Nullable XmlNSDescriptor getNSDescriptor() {
        return null;
    }

    @Override
    public @Nullable XmlElementsGroup getTopGroup() {
        return null;
    }

    @Override
    public int getContentType() {
        return CONTENT_TYPE_ANY;
    }

    @Override
    public @Nullable String getDefaultValue() {
        return null;
    }

    @Override
    public Object @NotNull [] getDependencies() {
        return declaration != null ? new Object[]{declaration} : new Object[0];
    }
}

