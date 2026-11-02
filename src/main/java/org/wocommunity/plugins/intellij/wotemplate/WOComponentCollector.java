package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Centrally collects all available WebObjects element and component names for completion:
 * - System WebObject elements from WebObjectDefinitions.xml
 * - Tag aliases from wo-tag-aliases.properties
 * - Subclasses of WOComponent, ERXComponent, WODynamicElement, and WOElement
 * - Custom components found in the project via .wo directories and .api files
 */
public final class WOComponentCollector {

    public static final String WO_COMPONENT_FQN = "com.webobjects.appserver.WOComponent";
    public static final String ERX_COMPONENT_FQN = "er.extensions.components.ERXComponent";
    public static final String ERX_STATELESS_COMPONENT_FQN = "er.extensions.components.ERXStatelessComponent";
    public static final String WO_ELEMENT_FQN = "com.webobjects.appserver.WOElement";
    public static final String WO_DYNAMIC_ELEMENT_FQN = "com.webobjects.appserver.WODynamicElement";

    private WOComponentCollector() {
    }

    public static @NotNull Set<String> collectAllComponentAndElementNames(@NotNull Project project) {
        Set<String> names = new LinkedHashSet<>();

        // 1. All system definitions
        names.addAll(WOSystemBindingDefinitions.getAllShortClassNames());

        // 2. All alias targets (short names)
        for (String target : WOComponentTagAliasRegistry.getAliases().values()) {
            if (target != null && !target.isBlank()) {
                int lastDot = target.lastIndexOf('.');
                String shortName = lastDot >= 0 ? target.substring(lastDot + 1) : target;
                if (!shortName.isBlank()) {
                    names.add(shortName);
                }
            }
        }

        // 3. Project .wo component directories
        try {
            Collection<VirtualFile> woFiles = FilenameIndex.getAllFilesByExt(project, "wo", GlobalSearchScope.projectScope(project));
            for (VirtualFile vf : woFiles) {
                if (vf.isDirectory() && vf.getName().endsWith(".wo") && !WOComponentClassResolver.isBuildArtifact(vf)) {
                    String compName = vf.getName().substring(0, vf.getName().length() - 3);
                    if (!compName.isBlank()) {
                        names.add(compName);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // 4. Project .api files
        try {
            Collection<VirtualFile> apiFiles = FilenameIndex.getAllFilesByExt(project, "api", GlobalSearchScope.projectScope(project));
            for (VirtualFile vf : apiFiles) {
                if (!vf.isDirectory() && vf.getName().endsWith(".api") && !WOComponentClassResolver.isBuildArtifact(vf)) {
                    String compName = vf.getName().substring(0, vf.getName().length() - 4);
                    if (!compName.isBlank()) {
                        names.add(compName);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // 5. Classes inheriting from WOComponent, ERXComponent, WODynamicElement in project and libraries
        if (!DumbService.isDumb(project)) {
            names.addAll(collectWoInheritorNames(project));
        }

        return names;
    }

    public static @NotNull Set<String> collectWoInheritorNames(@NotNull Project project) {
        Set<String> out = new LinkedHashSet<>();
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);

        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
        PsiClass woComponent = facade.findClass(WO_COMPONENT_FQN, scope);
        PsiClass erxComponent = facade.findClass(ERX_COMPONENT_FQN, scope);
        PsiClass erxStateless = facade.findClass(ERX_STATELESS_COMPONENT_FQN, scope);
        PsiClass woElement = facade.findClass(WO_ELEMENT_FQN, scope);
        PsiClass woDynamicElement = facade.findClass(WO_DYNAMIC_ELEMENT_FQN, scope);

        addInheritors(out, woComponent, scope);
        addInheritors(out, erxComponent, scope);
        addInheritors(out, erxStateless, scope);
        addInheritors(out, woDynamicElement, scope);
        addInheritors(out, woElement, scope);

        return out;
    }

    private static void addInheritors(@NotNull Set<String> out, PsiClass base, @NotNull GlobalSearchScope scope) {
        if (base == null) {
            return;
        }
        try {
            ClassInheritorsSearch.search(base, scope, true).forEach((Processor<PsiClass>) c -> {
                String name = c.getName();
                if (name != null && !name.isBlank()) {
                    out.add(name);
                }
                return true;
            });
        } catch (Exception ignored) {
        }
    }
}
