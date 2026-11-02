package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.components.api.WOComponentApiBinding;
import org.wocommunity.plugins.intellij.components.api.WOComponentApiModel;
import org.wocommunity.plugins.intellij.components.api.WOComponentApiXmlParser;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads WO binding definitions from classpath resources or component directories:
 * {@code Resources/<SimpleClassName>.api}, {@code src/main/components/<SimpleClassName>.api}, etc.
 *
 * These definitions have precedence over reflective member inspection.
 */
public final class WOApiBindingDefinitions {

    private static final String RESOURCES_DIR = "Resources";

    private record CacheEntry(long modificationStamp, @NotNull Map<String, WOComponentApiBinding> bindings) {}
    private static final Map<String, CacheEntry> CACHE_BY_VFILE_URL = new ConcurrentHashMap<>();

    private WOApiBindingDefinitions() {
    }

    public static @NotNull Map<String, WOComponentApiBinding> getApiBindingsForPsiClass(@NotNull PsiClass psiClass) {
        String simpleName = psiClass.getName();
        if (simpleName == null || simpleName.isBlank()) {
            return Collections.emptyMap();
        }

        VirtualFile vFile = findApiVirtualFile(psiClass, simpleName);
        if (vFile == null) {
            return Collections.emptyMap();
        }

        long stamp = vFile.getModificationStamp();
        CacheEntry entry = CACHE_BY_VFILE_URL.get(vFile.getUrl());
        if (entry != null && entry.modificationStamp() == stamp) {
            return entry.bindings();
        }

        Map<String, WOComponentApiBinding> loaded = loadBindingsFromApiFile(vFile);
        CACHE_BY_VFILE_URL.put(vFile.getUrl(), new CacheEntry(stamp, loaded));
        return loaded;
    }

    static @NotNull Set<String> getBindingsForPsiClass(@NotNull PsiClass psiClass) {
        return getApiBindingsForPsiClass(psiClass).keySet();
    }

    private static VirtualFile findApiVirtualFile(@NotNull PsiClass psiClass, @NotNull String simpleName) {
        String relPath = RESOURCES_DIR + "/" + simpleName + ".api";
        String fileName = simpleName + ".api";
        Project project = psiClass.getProject();

        PsiFile containing = psiClass.getContainingFile();
        VirtualFile clsFile = containing != null ? containing.getVirtualFile() : null;

        Module module = ModuleUtilCore.findModuleForPsiElement(psiClass);

        if (clsFile != null) {
            // Prefer looking beside the class in the same JAR, if applicable.
            VirtualFile fromJar = findInSameJar(clsFile, relPath);
            if (fromJar != null) {
                return fromJar;
            }

            // If it's not a JAR class, it's part of the open project. In that case, prefer the unique
            // <SimpleName>.api found in the class' module (regardless of folder).
            if (!(clsFile.getFileSystem() instanceof JarFileSystem) && module != null && !DumbService.isDumb(project)) {
                // FilenameIndex is scope- and index-dependent. Start narrow, then expand.
                VirtualFile v = findUniqueApiByNameInScopes(fileName, relPath,
                        GlobalSearchScope.moduleScope(module),
                        GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module),
                        GlobalSearchScope.projectScope(project));
                if (v != null) {
                    return v;
                }
            }
        }

        // Fallback: look in module content roots (covers project sources + local resources).
        if (module != null) {
            for (VirtualFile root : ModuleRootManager.getInstance(module).getContentRoots()) {
                VirtualFile candidate = root.findFileByRelativePath(relPath);
                if (candidate != null && !candidate.isDirectory()) {
                    return candidate;
                }
                // Common Maven layout fallback:
                VirtualFile candidate2 = root.findFileByRelativePath("src/main/resources/" + relPath);
                if (candidate2 != null && !candidate2.isDirectory()) {
                    return candidate2;
                }
                VirtualFile candidate3 = root.findFileByRelativePath("src/main/components/" + fileName);
                if (candidate3 != null && !candidate3.isDirectory()) {
                    return candidate3;
                }
                VirtualFile candidate4 = root.findFileByRelativePath("src/main/resources/" + fileName);
                if (candidate4 != null && !candidate4.isDirectory()) {
                    return candidate4;
                }
            }
        }

        return null;
    }

    private static VirtualFile findUniqueApiByNameInScopes(@NotNull String fileName,
                                                           @NotNull String relPath,
                                                           @NotNull GlobalSearchScope... scopes) {
        for (GlobalSearchScope scope : scopes) {
            var matches = FilenameIndex.getVirtualFilesByName(fileName, scope)
                    .stream()
                    .filter(v -> v != null && v.isValid() && !v.isDirectory())
                    .toList();
            if (matches.isEmpty()) {
                continue;
            }
            if (matches.size() == 1) {
                return matches.get(0);
            }
            // If multiple matches exist, prefer Resources/<SimpleName>.api or src/main/components/<SimpleName>.api if present.
            String preferredSuffix = "/" + relPath;
            for (VirtualFile v : matches) {
                if (v.getPath().endsWith(preferredSuffix) || v.getPath().contains("/components/" + fileName)) {
                    return v;
                }
            }
        }
        return null;
    }

    private static VirtualFile findInSameJar(@NotNull VirtualFile classOrJavaFile, @NotNull String relPath) {
        if (!(classOrJavaFile.getFileSystem() instanceof JarFileSystem)) {
            return null;
        }
        VirtualFile localJar = JarFileSystem.getInstance().getVirtualFileForJar(classOrJavaFile);
        if (localJar == null) {
            return null;
        }
        VirtualFile jarRoot = JarFileSystem.getInstance().getJarRootForLocalFile(localJar);
        if (jarRoot == null) {
            return null;
        }
        VirtualFile candidate = jarRoot.findFileByRelativePath(relPath);
        return (candidate != null && !candidate.isDirectory()) ? candidate : null;
    }

    private static @NotNull Map<String, WOComponentApiBinding> loadBindingsFromApiFile(@NotNull VirtualFile apiFile) {
        try {
            String content = new String(apiFile.contentsToByteArray(), StandardCharsets.UTF_8);
            WOComponentApiModel model = WOComponentApiXmlParser.parse(content);
            Map<String, WOComponentApiBinding> map = new LinkedHashMap<>();
            for (WOComponentApiBinding b : model.getBindings()) {
                if (b.getName() != null && !b.getName().isBlank()) {
                    map.put(b.getName(), b);
                }
            }
            return map.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(map);
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }
}

