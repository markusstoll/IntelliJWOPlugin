package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.psi.search.GlobalSearchScopesCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Resolves WebObjects component classes and their bindable properties/methods
 * across Java, Kotlin, and Groovy implementations.
 */
public final class WOComponentClassResolver {

    private static final List<String> COMPONENT_BASE_FQNS = List.of(
            "com.webobjects.appserver.WOComponent",
            "er.extensions.components.ERXComponent",
            "er.extensions.components.ERXStatelessComponent",
            "er.extensions.components.ERXDirectAction",
            "com.webobjects.appserver.WODirectAction"
    );

    private static final Set<String> OBJECT_METHOD_NAMES = Set.of(
            "equals", "hashCode", "toString", "getClass", "notify", "notifyAll", "wait"
    );

    private WOComponentClassResolver() {
    }

    /**
     * Extracts the simple component name (e.g. "Main") from a WO component file (.wo, .html, .wod, etc.).
     */
    public static @Nullable String getComponentName(@NotNull VirtualFile file) {
        String name = file.getName();
        if (name.endsWith(".wo")) {
            return name.substring(0, name.length() - 3);
        }
        VirtualFile parent = file.getParent();
        if (parent != null && parent.getName().endsWith(".wo")) {
            String parentName = parent.getName();
            return parentName.substring(0, parentName.length() - 3);
        }
        if (name.endsWith(".html")) {
            return name.substring(0, name.length() - 5);
        }
        int dot = name.indexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /**
     * Checks if a class is likely a WO component by checking its inheritance or name hierarchy.
     */
    public static boolean isComponentClass(@NotNull PsiClass psiClass) {
        try {
            Project project = psiClass.getProject();
            if (project != null) {
                JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
                for (String fqn : COMPONENT_BASE_FQNS) {
                    PsiClass base = facade.findClass(fqn, GlobalSearchScope.allScope(project));
                    if (base != null && InheritanceUtil.isInheritorOrSelf(psiClass, base, true)) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        PsiClass current = psiClass;
        while (current != null && !"java.lang.Object".equals(current.getQualifiedName())) {
            String name = current.getName();
            if (name != null && (name.contains("Component") || name.contains("DirectAction"))) {
                return true;
            }
            current = current.getSuperClass();
        }
        return false;
    }

    /**
     * Checks if a virtual file is located within a build artifact directory (target/, build/, out/, etc.).
     */
    public static boolean isBuildArtifact(@Nullable VirtualFile file) {
        if (file == null) {
            return false;
        }
        String path = file.getPath();
        return path.contains("/target/") || path.endsWith("/target") ||
               path.contains("/build/") || path.endsWith("/build") ||
               path.contains("/out/") || path.endsWith("/out") ||
               path.contains("/.gradle/") || path.contains("/.idea/");
    }

    /**
     * Checks if a virtual file is a valid project source file (.java, .kt, .groovy) and not a build artifact.
     */
    public static boolean isValidSourceFile(@NotNull Project project, @Nullable VirtualFile file) {
        if (file == null || !file.isValid() || file.isDirectory() || isBuildArtifact(file)) {
            return false;
        }
        String ext = file.getExtension();
        if (ext == null || (!ext.equalsIgnoreCase("java") && !ext.equalsIgnoreCase("kt") && !ext.equalsIgnoreCase("groovy"))) {
            return false;
        }
        try {
            ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
            return !fileIndex.isExcluded(file);
        } catch (Exception ignored) {
            return true;
        }
    }

    /**
     * Checks if a PsiClass belongs to a valid project source file and is not a compiled .class or build artifact.
     */
    public static boolean isValidSourceClass(@NotNull PsiClass psiClass) {
        VirtualFile vFile = PsiUtilCore.getVirtualFile(psiClass);
        if (vFile == null) {
            return false;
        }
        Project project = psiClass.getProject();
        if (project == null) {
            return false;
        }
        return isValidSourceFile(project, vFile);
    }

    /**
     * Checks if a directory is a valid WebObjects component directory and not in build artifacts.
     */
    public static boolean isValidComponentFolder(@NotNull Project project, @Nullable VirtualFile folder) {
        if (folder == null || !folder.isValid() || !folder.isDirectory()) {
            return false;
        }
        if (!folder.getName().endsWith(".wo")) {
            return false;
        }
        if (isBuildArtifact(folder)) {
            return false;
        }
        try {
            ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
            return !fileIndex.isExcluded(folder);
        } catch (Exception ignored) {
            return true;
        }
    }

    /**
     * Recursively searches for a file by name under a directory, strictly ignoring build artifacts.
     */
    public static @Nullable VirtualFile findFileByNameRecursive(@Nullable VirtualFile dir, @NotNull String fileName) {
        if (dir == null || !dir.isValid() || !dir.isDirectory() || isBuildArtifact(dir)) {
            return null;
        }
        for (VirtualFile child : dir.getChildren()) {
            if (child.isDirectory()) {
                if (!isBuildArtifact(child)) {
                    VirtualFile found = findFileByNameRecursive(child, fileName);
                    if (found != null) {
                        return found;
                    }
                }
            } else if (child.getName().equals(fileName)) {
                return child;
            }
        }
        return null;
    }

    /**
     * Recursively searches for a folder by name under a directory, strictly ignoring build artifacts.
     */
    public static @Nullable VirtualFile findFolderByNameRecursive(@Nullable VirtualFile dir, @NotNull String folderName) {
        if (dir == null || !dir.isValid() || !dir.isDirectory() || isBuildArtifact(dir)) {
            return null;
        }
        for (VirtualFile child : dir.getChildren()) {
            if (child.isDirectory()) {
                if (child.getName().equals(folderName)) {
                    return child;
                }
                if (!isBuildArtifact(child)) {
                    VirtualFile found = findFolderByNameRecursive(child, folderName);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Checks if a class belongs to the project's source code (not an external library or JDK class).
     */
    public static boolean isProjectSourceClass(@NotNull PsiClass psiClass) {
        return isValidSourceClass(psiClass);
    }

    /**
     * Returns candidate component classes for a context file, prioritizing project source classes.
     */
    public static @NotNull List<PsiClass> findCandidateComponentClasses(@NotNull PsiFile contextFile) {
        VirtualFile vFile = PsiUtilCore.getVirtualFile(contextFile);
        if (vFile == null) {
            PsiFile orig = contextFile.getOriginalFile();
            if (orig != null) {
                vFile = PsiUtilCore.getVirtualFile(orig);
            }
        }
        if (vFile == null) {
            vFile = contextFile.getVirtualFile();
        }
        if (vFile == null && contextFile.getViewProvider() != null) {
            vFile = contextFile.getViewProvider().getVirtualFile();
        }
        if (vFile == null) {
            return Collections.emptyList();
        }
        return findCandidateComponentClasses(contextFile.getProject(), vFile);
    }

    /**
     * Finds all candidate component classes for a given virtual file (.wo, .html, etc.).
     * Strictly limits searching to the same module if the file belongs to a module,
     * and strictly ignores any build artifacts (target/, build/, out/, etc.).
     */
    public static @NotNull List<PsiClass> findCandidateComponentClasses(@NotNull Project project, @NotNull VirtualFile file) {
        String componentName = getComponentName(file);
        if (componentName == null || componentName.isBlank()) {
            return Collections.emptyList();
        }

        Module module = ModuleUtilCore.findModuleForFile(file, project);
        PsiShortNamesCache cache = PsiShortNamesCache.getInstance(project);

        List<PsiClass> candidates = new ArrayList<>();
        Set<PsiClass> seen = new HashSet<>();

        if (module != null) {
            // Strictly search in this module only - never cross into other modules!
            GlobalSearchScope moduleScope = GlobalSearchScope.moduleScope(module);

            // 1. Search classes in module
            for (PsiClass cls : cache.getClassesByName(componentName, moduleScope)) {
                if (isValidSourceClass(cls) && seen.add(cls)) {
                    candidates.add(cls);
                }
            }

            // 2. Search files by extension in module (.java, .kt, .groovy)
            for (String ext : List.of(".java", ".kt", ".groovy")) {
                String fileName = componentName + ext;
                Collection<VirtualFile> foundFiles = FilenameIndex.getVirtualFilesByName(fileName, moduleScope);
                for (VirtualFile vf : foundFiles) {
                    if (isValidSourceFile(project, vf)) {
                        PsiFile psiFile = PsiManager.getInstance(project).findFile(vf);
                        if (psiFile instanceof PsiClassOwner classOwner) {
                            for (PsiClass cls : classOwner.getClasses()) {
                                if (componentName.equals(cls.getName()) && isValidSourceClass(cls) && seen.add(cls)) {
                                    candidates.add(cls);
                                }
                            }
                        }
                    }
                }
            }

            // 3. Fallback: manual source roots in module (in case indexing is incomplete)
            if (candidates.isEmpty()) {
                VirtualFile[] sourceRoots = ModuleRootManager.getInstance(module).getSourceRoots();
                for (VirtualFile sRoot : sourceRoots) {
                    if (isBuildArtifact(sRoot)) continue;
                    for (String ext : List.of(".java", ".kt", ".groovy")) {
                        VirtualFile vf = findFileByNameRecursive(sRoot, componentName + ext);
                        if (vf != null && isValidSourceFile(project, vf)) {
                            PsiFile psiFile = PsiManager.getInstance(project).findFile(vf);
                            if (psiFile instanceof PsiClassOwner classOwner) {
                                for (PsiClass cls : classOwner.getClasses()) {
                                    if (componentName.equals(cls.getName()) && isValidSourceClass(cls) && seen.add(cls)) {
                                        candidates.add(cls);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Final fallback: search across project scope if module search yielded nothing
            if (candidates.isEmpty()) {
                GlobalSearchScope projectScope = GlobalSearchScope.projectScope(project);
                for (PsiClass cls : cache.getClassesByName(componentName, projectScope)) {
                    if (isValidSourceClass(cls) && seen.add(cls)) {
                        candidates.add(cls);
                    }
                }
            }
        } else {
            // Module could not be determined: search strictly in content root of file
            VirtualFile contentRoot = null;
            try {
                contentRoot = ProjectRootManager.getInstance(project).getFileIndex().getContentRootForFile(file);
            } catch (Exception ignored) {
            }
            GlobalSearchScope searchScope = (contentRoot != null && !isBuildArtifact(contentRoot))
                    ? GlobalSearchScopesCore.directoryScope(project, contentRoot, true)
                    : GlobalSearchScope.projectScope(project);

            for (PsiClass cls : cache.getClassesByName(componentName, searchScope)) {
                if (isValidSourceClass(cls) && seen.add(cls)) {
                    candidates.add(cls);
                }
            }

            for (String ext : List.of(".java", ".kt", ".groovy")) {
                String fileName = componentName + ext;
                Collection<VirtualFile> foundFiles = FilenameIndex.getVirtualFilesByName(fileName, searchScope);
                for (VirtualFile vf : foundFiles) {
                    if (isValidSourceFile(project, vf)) {
                        PsiFile psiFile = PsiManager.getInstance(project).findFile(vf);
                        if (psiFile instanceof PsiClassOwner classOwner) {
                            for (PsiClass cls : classOwner.getClasses()) {
                                if (componentName.equals(cls.getName()) && isValidSourceClass(cls) && seen.add(cls)) {
                                    candidates.add(cls);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sort candidates: isComponentClass() == true comes first
        candidates.sort((c1, c2) -> {
            boolean comp1 = isComponentClass(c1);
            boolean comp2 = isComponentClass(c2);
            if (comp1 != comp2) return comp1 ? -1 : 1;
            return 0;
        });

        return candidates;
    }

    /**
     * Finds the primary component class for a given WO template file (HTML/WOD/etc.).
     */
    public static @Nullable PsiClass findComponentClass(@NotNull PsiFile contextFile) {
        List<PsiClass> candidates = findCandidateComponentClasses(contextFile);
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    /**
     * Finds the primary component class for a given virtual file.
     */
    public static @Nullable PsiClass findComponentClass(@NotNull Project project, @NotNull VirtualFile file) {
        List<PsiClass> candidates = findCandidateComponentClasses(project, file);
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    /**
     * Resolves a keypath (e.g. "title", "user.name") starting from the given class
     * to the target PsiElements (methods, getters, setters, fields).
     */
    public static @NotNull List<PsiElement> findBindingTargets(@NotNull PsiClass rootClass, @NotNull String path) {
        if (path.isBlank()) {
            return Collections.emptyList();
        }

        String[] parts = path.split("\\.", -1);
        PsiClass currentClass = rootClass;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) {
                return Collections.emptyList();
            }
            List<PsiElement> targets = findSingleKeyTargets(currentClass, part);
            if (targets.isEmpty()) {
                return Collections.emptyList();
            }
            PsiClass nextClass = resolveTypeToClass(targets.get(0));
            if (nextClass == null) {
                return Collections.emptyList();
            }
            currentClass = nextClass;
        }

        String lastKey = parts[parts.length - 1].trim();
        return findSingleKeyTargets(currentClass, lastKey);
    }

    /**
     * Resolves a prefix keypath (e.g. "user.") starting from the given class
     * to the target class on which the remaining property resides.
     */
    public static @Nullable PsiClass resolvePathToClass(@NotNull PsiClass rootClass, @NotNull String path) {
        String trimmed = path.endsWith(".") ? path.substring(0, path.length() - 1) : path;
        if (trimmed.isBlank()) {
            return rootClass;
        }

        String[] parts = trimmed.split("\\.", -1);
        PsiClass currentClass = rootClass;

        for (String part : parts) {
            String key = part.trim();
            if (key.isEmpty()) {
                return null;
            }
            List<PsiElement> targets = findSingleKeyTargets(currentClass, key);
            if (targets.isEmpty()) {
                return null;
            }
            PsiClass nextClass = resolveTypeToClass(targets.get(0));
            if (nextClass == null) {
                return null;
            }
            currentClass = nextClass;
        }

        return currentClass;
    }

    /**
     * Finds target methods and fields on the class matching the given single key
     * according to WebObjects KVC rules (getter, boolean getter, method, setter, field).
     */
    public static @NotNull List<PsiElement> findSingleKeyTargets(@NotNull PsiClass psiClass, @NotNull String key) {
        if (key.isBlank()) {
            return Collections.emptyList();
        }

        List<PsiElement> results = new ArrayList<>();
        String capitalized = key.length() > 1
                ? Character.toUpperCase(key.charAt(0)) + key.substring(1)
                : key.toUpperCase(Locale.ROOT);

        String getterName = "get" + capitalized;
        String isGetterName = "is" + capitalized;
        String setterName = "set" + capitalized;

        // 1. Exact getter (getFoo()) via findMethodsByName
        for (PsiMethod m : safeFindMethodsByName(psiClass, getterName)) {
            if (m.getParameterList() != null && m.getParameterList().isEmpty() && !results.contains(m)) {
                results.add(m);
            }
        }

        // 2. Exact boolean getter (isFoo()) via findMethodsByName
        for (PsiMethod m : safeFindMethodsByName(psiClass, isGetterName)) {
            if (m.getParameterList() != null && m.getParameterList().isEmpty() && !results.contains(m)) {
                results.add(m);
            }
        }

        // 3. Exact method name (foo()) - zero-arg preferred
        for (PsiMethod m : safeFindMethodsByName(psiClass, key)) {
            if (m.getParameterList() != null && m.getParameterList().isEmpty() && !results.contains(m)) {
                results.add(m);
            }
        }
        for (PsiMethod m : safeFindMethodsByName(psiClass, key)) {
            if (!results.contains(m)) {
                results.add(m);
            }
        }

        // 4. Exact field (foo)
        PsiField exactField = safeFindFieldByName(psiClass, key);
        if (exactField != null && !results.contains(exactField)) {
            results.add(exactField);
        }

        // 5. Setter (setFoo(val))
        for (PsiMethod m : safeFindMethodsByName(psiClass, setterName)) {
            if (m.getParameterList() != null && m.getParameterList().getParametersCount() == 1 && !results.contains(m)) {
                results.add(m);
            }
        }

        // 6. Backing field with underscore (_foo)
        PsiField backingField = safeFindFieldByName(psiClass, "_" + key);
        if (backingField != null && !results.contains(backingField)) {
            results.add(backingField);
        }

        // 7. Full scan fallback via getAllMethods() / getAllFields()
        PsiMethod[] allMethods = safeGetAllMethods(psiClass);
        PsiField[] allFields = safeGetAllFields(psiClass);

        for (PsiMethod m : allMethods) {
            String mName = m.getName();
            if ((mName.equals(getterName) || mName.equals(isGetterName)) && m.getParameterList() != null && m.getParameterList().isEmpty()) {
                if (!results.contains(m)) results.add(m);
            } else if (mName.equals(key)) {
                if (!results.contains(m)) results.add(m);
            } else if (mName.equals(setterName) && m.getParameterList() != null && m.getParameterList().getParametersCount() == 1) {
                if (!results.contains(m)) results.add(m);
            }
        }

        for (PsiField f : allFields) {
            String fName = f.getName();
            if (fName.equals(key) || fName.equals("_" + key)) {
                if (!results.contains(f)) results.add(f);
            }
        }

        // 8. Case-insensitive fallback
        if (results.isEmpty()) {
            for (PsiMethod m : allMethods) {
                if (m.getName().equalsIgnoreCase(getterName) ||
                    m.getName().equalsIgnoreCase(isGetterName) ||
                    m.getName().equalsIgnoreCase(key)) {
                    if (!results.contains(m)) results.add(m);
                }
            }
            for (PsiField f : allFields) {
                if (f.getName().equalsIgnoreCase(key) || f.getName().equalsIgnoreCase("_" + key)) {
                    if (!results.contains(f)) results.add(f);
                }
            }
        }

        return results;
    }

    private static PsiMethod[] safeFindMethodsByName(PsiClass psiClass, String name) {
        try {
            PsiMethod[] methods = psiClass.findMethodsByName(name, true);
            return methods != null ? methods : PsiMethod.EMPTY_ARRAY;
        } catch (Exception ignored) {
            return PsiMethod.EMPTY_ARRAY;
        }
    }

    private static PsiField safeFindFieldByName(PsiClass psiClass, String name) {
        try {
            return psiClass.findFieldByName(name, true);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static PsiMethod[] safeGetAllMethods(PsiClass psiClass) {
        try {
            PsiMethod[] methods = psiClass.getAllMethods();
            return methods != null ? methods : PsiMethod.EMPTY_ARRAY;
        } catch (Exception ignored) {
            return PsiMethod.EMPTY_ARRAY;
        }
    }

    private static PsiField[] safeGetAllFields(PsiClass psiClass) {
        try {
            PsiField[] fields = psiClass.getAllFields();
            return fields != null ? fields : PsiField.EMPTY_ARRAY;
        } catch (Exception ignored) {
            return PsiField.EMPTY_ARRAY;
        }
    }

    /**
     * Resolves the return type of a method or type of a field to its PsiClass.
     */
    public static @Nullable PsiClass resolveTypeToClass(@NotNull PsiElement member) {
        PsiType type = null;
        if (member instanceof PsiMethod method) {
            if (PsiTypes.voidType().equals(method.getReturnType()) &&
                    method.getParameterList().getParametersCount() == 1) {
                type = method.getParameterList().getParameters()[0].getType();
            } else {
                type = method.getReturnType();
            }
        } else if (member instanceof PsiField field) {
            type = field.getType();
        }

        if (type instanceof PsiClassType classType) {
            return classType.resolve();
        }
        return null;
    }

    /**
     * Collects all bindable member variants (getters, methods, fields) from a target class
     * for auto-completion.
     */
    public static @NotNull List<LookupElement> collectBindingVariants(@NotNull PsiClass targetClass) {
        List<LookupElement> elements = new ArrayList<>();
        Set<String> seenPropertyNames = new HashSet<>();

        PsiMethod[] allMethods = targetClass.getAllMethods();
        PsiField[] allFields = targetClass.getAllFields();

        // 1. Process getters (getFoo() -> "foo", isFoo() -> "foo")
        for (PsiMethod method : allMethods) {
            if (method.hasModifierProperty(PsiModifier.STATIC)) continue;
            if (method.hasModifierProperty(PsiModifier.PRIVATE)) continue;
            if (!method.getParameterList().isEmpty()) continue;

            String name = method.getName();
            if (OBJECT_METHOD_NAMES.contains(name)) continue;

            String propName = null;
            if (name.startsWith("get") && name.length() > 3) {
                propName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
            } else if (name.startsWith("is") && name.length() > 2) {
                propName = Character.toLowerCase(name.charAt(2)) + name.substring(3);
            }

            if (propName != null && seenPropertyNames.add(propName)) {
                seenPropertyNames.add(name);
                String typeText = method.getReturnType() != null ? method.getReturnType().getPresentableText() : "";
                LookupElementBuilder builder = LookupElementBuilder.create(propName)
                        .withLookupString(name)
                        .withIcon(method.getIcon(0))
                        .withTypeText(typeText, true)
                        .withTailText(" (via " + name + "())", true)
                        .withBoldness(targetClass.equals(method.getContainingClass()));
                builder = attachPsiElement(builder, method);
                elements.add(builder);
            }
        }

        // 2. Process zero-arg methods and actions (e.g. save(), title())
        for (PsiMethod method : allMethods) {
            if (method.hasModifierProperty(PsiModifier.STATIC)) continue;
            if (method.hasModifierProperty(PsiModifier.PRIVATE)) continue;

            String name = method.getName();
            if (OBJECT_METHOD_NAMES.contains(name)) continue;
            if (seenPropertyNames.contains(name)) continue;

            if (method.getParameterList().isEmpty()) {
                seenPropertyNames.add(name);
                String typeText = method.getReturnType() != null ? method.getReturnType().getPresentableText() : "";
                LookupElementBuilder builder = LookupElementBuilder.create(name)
                        .withIcon(method.getIcon(0))
                        .withTypeText(typeText, true)
                        .withTailText("()", true)
                        .withBoldness(targetClass.equals(method.getContainingClass()));
                builder = attachPsiElement(builder, method);
                elements.add(builder);
            }
        }

        // 3. Process accessible fields
        for (PsiField field : allFields) {
            if (field.hasModifierProperty(PsiModifier.STATIC)) continue;
            if (field.hasModifierProperty(PsiModifier.PRIVATE)) continue;

            String name = field.getName();
            if (name.startsWith("_")) {
                name = name.substring(1);
            }

            if (!name.isEmpty() && seenPropertyNames.add(name)) {
                String typeText = field.getType() != null ? field.getType().getPresentableText() : "";
                LookupElementBuilder builder = LookupElementBuilder.create(name)
                        .withIcon(field.getIcon(0))
                        .withTypeText(typeText, true)
                        .withBoldness(targetClass.equals(field.getContainingClass()));
                builder = attachPsiElement(builder, field);
                elements.add(builder);
            }
        }

        return elements;
    }

    private static LookupElementBuilder attachPsiElement(LookupElementBuilder builder, PsiElement element) {
        try {
            if (element != null && element.getProject() != null) {
                return builder.withPsiElement(element);
            }
        } catch (Exception ignored) {
        }
        return builder;
    }
}
