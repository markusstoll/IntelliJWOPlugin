package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlElement;
import com.intellij.xml.XmlAttributeDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Minimal attribute descriptor for WO bindings in HTML templates.
 *
 * IntelliJ uses {@link XmlAttributeDescriptor} for attribute name completion and for
 * marking unknown attributes.
 */
final class WOXmlAttributeDescriptor implements XmlAttributeDescriptor {

    private final @NotNull String name;
    private final @Nullable PsiElement declaration;

    WOXmlAttributeDescriptor(@NotNull String name, @Nullable PsiElement declaration) {
        this.name = name;
        this.declaration = declaration;
    }

    @Override
    public @NonNls String getName(@NotNull PsiElement context) {
        return name;
    }

    @Override
    public @NlsSafe String getName() {
        return name;
    }

    @Override
    public void init(@NotNull PsiElement element) {
        // stateless descriptor
    }

    @Override
    public @Nullable PsiElement getDeclaration() {
        return declaration;
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    public boolean isFixed() {
        return false;
    }

    @Override
    public boolean hasIdType() {
        return false;
    }

    @Override
    public boolean hasIdRefType() {
        return false;
    }

    @Override
    public @Nullable String getDefaultValue() {
        return null;
    }

    @Override
    public String @Nullable [] getEnumeratedValues() {
        return null;
    }

    @Override
    public @Nullable String validateValue(XmlElement context, String value) {
        return null;
    }

    @Override
    public boolean isEnumerated() {
        return false;
    }

    @Override
    public Object @NotNull [] getDependencies() {
        return declaration != null ? new Object[]{declaration} : new Object[0];
    }
}
