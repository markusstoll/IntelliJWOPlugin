package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlElementsGroup;
import com.intellij.xml.XmlNSDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

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
        Map<String, XmlAttributeDescriptor> map = getOrBuildAttributeDescriptors();
        return map.values().toArray(XmlAttributeDescriptor.EMPTY);
    }

    @Override
    public @Nullable XmlAttributeDescriptor getAttributeDescriptor(@NonNls String attributeName, @Nullable XmlTag context) {
        if (attributeName == null || attributeName.isBlank()) {
            return null;
        }
        return getOrBuildAttributeDescriptors().get(attributeName);
    }

    @Override
    public @Nullable XmlAttributeDescriptor getAttributeDescriptor(XmlAttribute attribute) {
        if (attribute == null) {
            return null;
        }
        return getAttributeDescriptor(attribute.getName(), attribute.getParent());
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

    private volatile @Nullable Map<String, XmlAttributeDescriptor> cachedAttributeDescriptors;

    private @NotNull Map<String, XmlAttributeDescriptor> getOrBuildAttributeDescriptors() {
        Map<String, XmlAttributeDescriptor> cached = cachedAttributeDescriptors;
        if (cached != null) {
            return cached;
        }
        Map<String, XmlAttributeDescriptor> built = buildAttributeDescriptors();
        cachedAttributeDescriptors = built;
        return built;
    }

    private @NotNull Map<String, XmlAttributeDescriptor> buildAttributeDescriptors() {
        if (!(declaration instanceof PsiClass psiClass)) {
            return Map.of();
        }

        Map<String, XmlAttributeDescriptor> out = new LinkedHashMap<>();

        // 1) public instance fields
        for (PsiField f : psiClass.getAllFields()) {
            if (!f.hasModifierProperty(PsiModifier.PUBLIC)) {
                continue;
            }
            if (f.hasModifierProperty(PsiModifier.STATIC)) {
                continue;
            }
            String fieldName = f.getName();
            if (fieldName == null || fieldName.isBlank()) {
                continue;
            }
            // If a matching property descriptor already exists, prefer the method-based one (more semantically correct).
            out.putIfAbsent(fieldName, new WOXmlAttributeDescriptor(fieldName, f));
        }

        // 2) public bean properties with getter+setter
        Map<String, PsiMethod> getters = new LinkedHashMap<>();
        Map<String, PsiMethod> setters = new LinkedHashMap<>();
        for (PsiMethod m : psiClass.getAllMethods()) {
            if (!m.hasModifierProperty(PsiModifier.PUBLIC)) {
                continue;
            }
            if (m.hasModifierProperty(PsiModifier.STATIC)) {
                continue;
            }
            String methodName = m.getName();
            if (methodName == null) {
                continue;
            }

            PsiParameterList params = m.getParameterList();
            if (isGetterLike(methodName, params)) {
                String prop = getterPropertyName(m);
                if (prop != null && !prop.isBlank()) {
                    getters.putIfAbsent(prop, m);
                }
            } else if (isSetterLike(methodName, params)) {
                String prop = setterPropertyName(m);
                if (prop != null && !prop.isBlank()) {
                    setters.putIfAbsent(prop, m);
                }
            }
        }

        for (Map.Entry<String, PsiMethod> e : getters.entrySet()) {
            String prop = e.getKey();
            PsiMethod getter = e.getValue();
            PsiMethod setter = setters.get(prop);
            if (setter == null) {
                continue; // require getter + setter (as requested)
            }
            out.put(prop, new WOXmlAttributeDescriptor(prop, getter));
        }

        return out;
    }

    private static boolean isGetterLike(@NotNull String methodName, @NotNull PsiParameterList params) {
        if (params.getParametersCount() != 0) {
            return false;
        }
        // "getX" or "isX" for boolean.
        if (methodName.length() > 3 && methodName.startsWith("get")) {
            return true;
        }
        return methodName.length() > 2 && methodName.startsWith("is");
    }

    private static boolean isSetterLike(@NotNull String methodName, @NotNull PsiParameterList params) {
        if (params.getParametersCount() != 1) {
            return false;
        }
        return methodName.length() > 3 && methodName.startsWith("set");
    }

    private static @Nullable String getterPropertyName(@NotNull PsiMethod getter) {
        String name = getter.getName();
        String prop = PropertyUtilBase.getPropertyName(getter);
        if (prop != null) {
            return prop;
        }
        // Very defensive fallback.
        if (name.startsWith("get") && name.length() > 3) {
            return decapitalizeAscii(name.substring(3));
        }
        if (name.startsWith("is") && name.length() > 2) {
            return decapitalizeAscii(name.substring(2));
        }
        return null;
    }

    private static @Nullable String setterPropertyName(@NotNull PsiMethod setter) {
        String name = setter.getName();
        String prop = PropertyUtilBase.getPropertyName(setter);
        if (prop != null) {
            return prop;
        }
        if (name.startsWith("set") && name.length() > 3) {
            return decapitalizeAscii(name.substring(3));
        }
        return null;
    }

    private static @NotNull String decapitalizeAscii(@NotNull String s) {
        if (s.isEmpty()) {
            return s;
        }
        // Similar to Introspector.decapitalize, but without locale-sensitive behavior.
        if (s.length() > 1 && Character.isUpperCase(s.charAt(0)) && Character.isUpperCase(s.charAt(1))) {
            return s;
        }
        return s.substring(0, 1).toLowerCase(Locale.ROOT) + s.substring(1);
    }
}

