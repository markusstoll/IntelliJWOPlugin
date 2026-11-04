package org.wocommunity.plugins.intellij.tools;

import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.jvm.JvmMember;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiFormatUtilBase;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.PlatformIcons;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.psi.wod.WODKeyPath;
import org.wocommunity.plugins.intellij.psi.wod.WODKeyPathElement;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class KeyValueCodingUtil {

    /**
     * Warning: we can't resolve key paths that go through collections
     * We might to communicate this to the caller somehow.
     */
    public static PsiMember resolveWODKeyPath(PsiClass baseClass, WODKeyPath keyPath) {
        return resolveWODKeyPath(baseClass, keyPath, false);
    }
    public static PsiMember resolveWODKeyPath(PsiClass baseClass, WODKeyPath keyPath, WODKeyPathElement elementToResolve) {
        return resolveWODKeyPath(baseClass, keyPath, elementToResolve, false);
    }
    public static PsiMember resolveWODKeyPath(PsiClass baseClass, WODKeyPath keyPath, boolean returnLastMatch) {
        return resolveWODKeyPath(baseClass, keyPath, null, returnLastMatch);
    }
    public static PsiMember resolveWODKeyPath(PsiClass baseClass, WODKeyPath keyPath, WODKeyPathElement elementToResolve, boolean returnLastMatch) {
        PsiClass lastClass = baseClass;
        List<WODKeyPathElement> pathElements = keyPath.getWODKeyPathElementList();
        for (WODKeyPathElement element : pathElements) {
            String key = element.getText();
            key = key.replace(CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED, "");
            if (key.isBlank()) {
                break;
            }
            PsiMember resolved = resolveSegment(lastClass, key);
            if (element.equals(elementToResolve)) {
                return resolved;
            }

            PsiClass newClass = resolveMemberToClass(resolved);
            if (newClass == null) {
                if (returnLastMatch) {
                    return lastClass;
                }
                return null;
            }
            lastClass = newClass;
        }
        return lastClass;
    }

    public static @Nullable PsiElement resolveKeyPathElement(WODKeyPathElement elementToResolve) {
        PsiClass baseClass = WOPsiUtil.getPsiClass(elementToResolve);
        if (baseClass == null) {
            return null;
        }

        PsiElement keyPathElem = elementToResolve.getParent();
        if (!(keyPathElem instanceof WODKeyPath keyPath)) {
            return null;
        }
        return resolveWODKeyPath(baseClass, keyPath, elementToResolve);
    }

    public static PsiMember resolveSegment(PsiClass baseClass, String key) {
        String capKey = StringUtils.capitalize(key);

        // 1. Public Accessor Methods (getKey(), key(), isKey())
        PsiMethod[] m = baseClass.findMethodsByName("get" + capKey, true);
        if (m.length > 0 && m[0].hasModifierProperty(PsiModifier.PUBLIC)) { return m[0]; }
        m = baseClass.findMethodsByName(key, true);
        if (m.length > 0 && m[0].hasModifierProperty(PsiModifier.PUBLIC)) { return m[0]; }
        m = baseClass.findMethodsByName("is" + capKey, true);
        if (m.length > 0 && m[0].hasModifierProperty(PsiModifier.PUBLIC)) { return m[0]; }

        // 2. Private Accessor Methods (_getKey(), _key(), _isKey())
        m = baseClass.findMethodsByName("_get" + capKey, true);
        if (m.length > 0 && m[0].hasModifierProperty(PsiModifier.PUBLIC)) { return m[0]; }
        m = baseClass.findMethodsByName("_" + key, true);
        if (m.length > 0 && m[0].hasModifierProperty(PsiModifier.PUBLIC)) { return m[0]; }
        m = baseClass.findMethodsByName("_is" + capKey, true);
        if (m.length > 0 && m[0].hasModifierProperty(PsiModifier.PUBLIC)) { return m[0]; }

        // 3. Direct Field / Instance Variable Access (_key, _isKey, key, isKey)
        PsiField field = baseClass.findFieldByName("_" + key, true);
        if (field != null && field.hasModifierProperty(PsiModifier.PUBLIC)) { return field; }
        field = baseClass.findFieldByName("_is" + capKey, true);
        if (field != null && field.hasModifierProperty(PsiModifier.PUBLIC)) { return field; }
        field = baseClass.findFieldByName(key, true);
        if (field != null && field.hasModifierProperty(PsiModifier.PUBLIC)) { return field; }
        field = baseClass.findFieldByName("is" + capKey, true);
        if (field != null && field.hasModifierProperty(PsiModifier.PUBLIC)) { return field; }

        return null;
    }

    private static PsiClass resolveMemberToClass(PsiMember member) {
        if (member instanceof PsiField field) {
            return PsiTypesUtil.getPsiClass(field.getType());
        }
        if (member instanceof PsiMethod method) {
            return PsiTypesUtil.getPsiClass(method.getReturnType());
        }
        if (member instanceof PsiClass clazz) {
            return clazz;
        }

        return null;
    }

    public static void addCompletionSuggestions(CompletionResultSet result, PsiClass baseClass) {
        Set<String> keys = new HashSet<String>();

        PsiMethod[] methods = baseClass.getAllMethods();
        for (PsiMethod method : methods) {
            if (method.isConstructor()) {
                continue;
            }
            if ("java.lang.Object".equals(Objects.requireNonNull(method.getContainingClass()).getQualifiedName())) {
                continue;
            }
            if (!method.hasModifierProperty(PsiModifier.PUBLIC)) {
                continue;
            }
            if (!method.getParameterList().isEmpty()) {
                continue;
            }
            if (PsiTypes.voidType().equals(method.getReturnType())) {
                continue;
            }
            String normalizedName = method.getName();
            if (normalizedName.startsWith("_")) {
                normalizedName = normalizedName.substring(1);
            }
            if (normalizedName.startsWith("get")) {
                normalizedName = normalizedName.substring(3);
                normalizedName = StringUtils.uncapitalize(normalizedName);
            }
            else if (normalizedName.startsWith("is")) {
                normalizedName = normalizedName.substring(2);
                normalizedName = StringUtils.uncapitalize(normalizedName);
            }
            if (keys.contains(normalizedName)) {
                continue;
            }

            keys.add(normalizedName);

            LookupElementBuilder lookup = LookupElementBuilder.create(method, method.getName())
                    .withIcon(PlatformIcons.METHOD_ICON) // Setzt das schicke "m"-Icon
                    .withTypeText(method.getReturnType() != null ? method.getReturnType().getPresentableText() : "void") // Zeigt den Rückgabetyp rechts an
                    .withTailText(PsiFormatUtil.formatMethod(method, PsiSubstitutor.EMPTY, PsiFormatUtilBase.SHOW_PARAMETERS, PsiFormatUtilBase.SHOW_TYPE)); // Zeigt Parameter an (z.B. "(String arg)")
            result.addElement(lookup);
        }

        PsiField[] fields = baseClass.getFields();
        for (PsiField field : fields) {
            if (!field.hasModifierProperty(PsiModifier.PUBLIC)) {
                continue;
            }
            String normalizedName = field.getName();
            if (normalizedName.startsWith("_")) {
                normalizedName = normalizedName.substring(1);
            }
            if (normalizedName.startsWith("is")) {
                normalizedName = normalizedName.substring(2);
                normalizedName = StringUtils.uncapitalize(normalizedName);
            }
            if (keys.contains(normalizedName)) {
                continue;
            }

            keys.add(normalizedName);
            LookupElementBuilder lookup = LookupElementBuilder.create(field, field.getName())
                    .withIcon(PlatformIcons.FIELD_ICON) // Setzt das "f"-Icon
                    .withTypeText(field.getType().getPresentableText()) // Zeigt den Feld-Typ rechts an
                    .bold(); // Macht den Namen fett, weil es z.B. ein direktes Attribut ist
            result.addElement(lookup);
        }
    }

    public static @NotNull String decapitalize(@NotNull String name) {
        if (name.isEmpty()) return name;
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) && Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Extracts the KVC property name from an accessor method or field name.
     * Examples:
     * - "getCurrency" -> "currency"
     * - "isAvailable" -> "available"
     * - "setCurrency" -> "currency"
     * - "_getCurrency" -> "currency"
     * - "_currency" -> "currency"
     */
    public static @Nullable String extractKvcPropertyName(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        if (name.startsWith("get") && name.length() > 3 && Character.isUpperCase(name.charAt(3))) {
            return decapitalize(name.substring(3));
        }
        if (name.startsWith("is") && name.length() > 2 && Character.isUpperCase(name.charAt(2))) {
            return decapitalize(name.substring(2));
        }
        if (name.startsWith("set") && name.length() > 3 && Character.isUpperCase(name.charAt(3))) {
            return decapitalize(name.substring(3));
        }
        if (name.startsWith("_get") && name.length() > 4 && Character.isUpperCase(name.charAt(4))) {
            return decapitalize(name.substring(4));
        }
        if (name.startsWith("_is") && name.length() > 3 && Character.isUpperCase(name.charAt(3))) {
            return decapitalize(name.substring(3));
        }
        if (name.startsWith("_set") && name.length() > 4 && Character.isUpperCase(name.charAt(4))) {
            return decapitalize(name.substring(4));
        }
        if (name.startsWith("_") && name.length() > 1 && !name.startsWith("__")) {
            return name.substring(1);
        }
        return null;
    }

    /**
     * Returns alternative KVC names for a PSI member to search for in templates (.wod and .html).
     * For example, for 'getCurrency()' this returns ['currency'].
     */
    public static @NotNull Set<String> getAlternativeKvcNames(@NotNull PsiElement element) {
        if (element instanceof PsiNamedElement named) {
            String name = named.getName();
            if (name == null || name.isBlank()) {
                return Collections.emptySet();
            }
            String kvcName = extractKvcPropertyName(name);
            if (kvcName != null && !kvcName.equals(name)) {
                return Set.of(kvcName);
            }
        }
        return Collections.emptySet();
    }

    /**
     * Computes the new key/binding name when refactoring/renaming a Java method or field.
     * Preserves leading '$', trailing '()', and converts method names (like "getEuro")
     * back to KVC property names (like "euro") if the original binding was a KVC property name.
     */
    public static @NotNull String computeRenamedKey(@NotNull String currentText, @NotNull String newElementName) {
        boolean hasDollar = currentText.startsWith("$");
        String rawCurrent = hasDollar ? currentText.substring(1) : currentText;
        boolean hasParens = rawCurrent.endsWith("()");
        if (hasParens) {
            rawCurrent = rawCurrent.substring(0, rawCurrent.length() - 2);
        }

        String newKvcName = extractKvcPropertyName(newElementName);
        boolean originalWasKvc = !rawCurrent.startsWith("get")
                && !rawCurrent.startsWith("set")
                && !rawCurrent.startsWith("is")
                && !rawCurrent.startsWith("_");

        String renamed;
        if (originalWasKvc && newKvcName != null) {
            renamed = newKvcName;
        } else {
            renamed = newElementName;
        }

        StringBuilder result = new StringBuilder();
        if (hasDollar) {
            result.append("$");
        }
        result.append(renamed);
        if (hasParens && !renamed.endsWith("()")) {
            result.append("()");
        }
        return result.toString();
    }
}
