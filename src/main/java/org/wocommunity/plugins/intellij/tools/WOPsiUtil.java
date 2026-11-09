package org.wocommunity.plugins.intellij.tools;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.components.SwitchToWOComponentAction;
import org.wocommunity.plugins.intellij.psi.api.APIFile;
import org.wocommunity.plugins.intellij.psi.wod.*;
import org.wocommunity.plugins.intellij.wotemplate.WOComponentClassResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

// TODO: Components may also be in a Language.lproj folder inside the components folder.
//       The .api file however is in the components folder even in that case...
//       e.g. https://github.com/wocommunity/wonder/tree/master/Frameworks/Core/ERExtensions/Components

// TODO: The .api file can also exist without a component folder if it's a WODynamicElement.
//       In that case the .api file should not be hidden in the file tree... ;)

// TODO: CONFUSION... Getting the class may refer to the class of a declaration e.g. WOHYperlink or the
//       class of the component we are editing... argh

public class WOPsiUtil {
    public static final @NonNls @NotNull String WO_ELEMENT_FQN = "com.webobjects.appserver.WOElement";

    public static boolean itemIsComponentFolder(PsiFileSystemItem item) {
        return item instanceof PsiDirectory && WOFileUtil.COMPONENT_EXTENSION.equalsIgnoreCase(((PsiDirectory) item).getVirtualFile().getExtension());
    }

    public static boolean itemIsInComponentFolder(@NotNull PsiFileSystemItem item) {
        return itemIsComponentTemplateFile(item) || itemIsComponentWodFile(item) || itemIsComponentApiFile(item) || itemIsComponentWooFile(item);
    }

    public static boolean itemIsComponentTemplateFile(@NotNull PsiFileSystemItem item) {
        return item instanceof PsiFile && WOFileUtil.TEMPLATE_EXTENSION.equalsIgnoreCase(((PsiFile) item).getVirtualFile().getExtension()) && itemIsComponentFolder(item.getParent());
    }

    public static boolean itemIsComponentWodFile(@NotNull PsiFileSystemItem item) {
        return item instanceof WODFile
                && WOFileUtil.DECLARATION_EXTENSION.equalsIgnoreCase(((WODFile) item).getVirtualFile().getExtension())
                && itemIsComponentFolder(item.getParent());
    }

    public static boolean itemIsComponentApiFile(@NotNull PsiFileSystemItem item) {
        return item instanceof APIFile || (item instanceof XmlFile && WOFileUtil.API_EXTENSION.equalsIgnoreCase(((PsiFile) item).getVirtualFile().getExtension()) /* TODO: For whatever reason the api file is parallel to the .wo folder and not inside --- because there can be multiple .wo folders i.e. for localized components */);
    }

    public static boolean itemIsComponentWooFile(@NotNull PsiFileSystemItem item) {
        return item instanceof PsiFile && WOFileUtil.WOO_EXTENSION.equalsIgnoreCase(((PsiFile) item).getVirtualFile().getExtension()) && itemIsComponentFolder(item.getParent());
    }

    public static String getComponentName(@NotNull PsiElement item) {
        PsiDirectory component = getComponentFolder(item);
        if (component != null) {
            return component.getVirtualFile().getNameWithoutExtension();
        }
        return null;
    }

    public static PsiDirectory getComponentFolder(@NotNull PsiElement element) {
        if (element instanceof PsiFileSystemItem item) {
            if (itemIsComponentFolder(item)) {
                return (PsiDirectory) element;
            }
            if (itemIsInComponentFolder(item)) {
                return ((PsiFile) element).getParent();
            }
        }
        else {
            // We need to distinguish between getting the component of the component we are editing
            // and the component that's declared.

            // get the component we are editing for values
            WODValue value = element instanceof WODValue ? (WODValue) element : PsiTreeUtil.getParentOfType(element, WODValue.class);
            if (value != null) {
                return value.getContainingFile().getOriginalFile().getContainingDirectory();
            }
            // or go to the declared component
            else {
                element = PsiTreeUtil.getParentOfType(element, WODDeclaration.class);
                if (element instanceof WODDeclaration wodDeclaration) {
                    element = wodDeclaration.getWODComponent();
                }
            }

            if (element instanceof WODComponent wodComponent) {
                return getComponentFolderForComponentName(wodComponent.getIdentifier().getText(), wodComponent.getProject());
            }
        }
        return null;
    }

    public static PsiFile getTemplateFile(@NotNull PsiFileSystemItem item) {
        if (itemIsComponentTemplateFile(item)) {
            return (PsiFile) item;
        }

        PsiDirectory component = getComponentFolder(item);
        if (component != null) {
            String componentName = component.getVirtualFile().getNameWithoutExtension();
            return component.findFile(componentName + "." + WOFileUtil.TEMPLATE_EXTENSION);
        }
        return null;
    }

    public static WODFile getWodFile(@NotNull PsiFileSystemItem item) {
        if (itemIsComponentWodFile(item)) {
            return (WODFile) item;
        }

        PsiDirectory component = getComponentFolder(item);
        if (component != null) {
            String componentName = component.getVirtualFile().getNameWithoutExtension();
            PsiFile file = component.findFile(componentName + "." + WOFileUtil.DECLARATION_EXTENSION);
            if (file instanceof WODFile wodFile) {
                return wodFile;
            }
        }
        return null;
    }

    public static APIFile getApiFile(@NotNull PsiFileSystemItem item) {
        if (itemIsComponentApiFile(item)) {
            if (item instanceof XmlFile xmlFile) {
                return new APIFile(xmlFile);
            }
            return (APIFile) item;
        }

        PsiDirectory component = getComponentFolder(item);
        if (component != null) {
            String componentName = component.getVirtualFile().getNameWithoutExtension();
            assert component.getParent() != null;
            PsiFile foundFile = component.getParent().findFile(componentName + "." + WOFileUtil.API_EXTENSION);
            if (foundFile == null) {
                return null;
            }
            if (foundFile instanceof APIFile apiFile) {
                return apiFile;
            }
            if (foundFile instanceof XmlFile xmlFile) {
                return new APIFile(xmlFile);
            }
            try {
                // 1. Load the raw text of the .api file
                String fileContent = VfsUtilCore.loadText(foundFile.getVirtualFile());

                // 2. Force IntelliJ to create an in-memory XmlFile from this text
                PsiFile factoryFile = PsiFileFactory.getInstance(component.getProject()).createFileFromText(
                        foundFile.getName(),
                        XmlFileType.INSTANCE, // This forces the XML language parser
                        fileContent
                );

                // 3. This is now safely castable to XmlFile!
                if (factoryFile instanceof XmlFile xmlFile) {
                    return new APIFile(xmlFile);
                }
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    public static PsiFile getWoo(@NotNull PsiFileSystemItem item) {
        if (itemIsComponentWooFile(item)) {
            return (PsiFile) item;
        }

        PsiDirectory component = getComponentFolder(item);
        if (component != null) {
            String componentName = component.getVirtualFile().getNameWithoutExtension();
            return component.findFile(componentName + "." + WOFileUtil.WOO_EXTENSION);
        }
        return null;
    }


    // Find other Components

    public static PsiClass getPsiClass(@NotNull PsiElement item) {
        PsiFile containingFile = item.getContainingFile();
        if (containingFile != null) {
            PsiClass resolved = WOComponentClassResolver.findComponentClass(containingFile);
            if (resolved != null) {
                return resolved;
            }
        }

        String componentName = getComponentName(item);
        if (componentName != null) {
            try {
                return getPsiClassForComponentName(componentName, item.getProject());
            } catch (Exception ignored) {
                // Return null when unresolvable to avoid breaking annotators / completion
                return null;
            }
        }
        return null;
    }

    public static PsiClass getPsiClassForComponentName(@NotNull String className, @NotNull Project project) throws Exception {
        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);

        PsiClass baseClass = facade.findClass(WO_ELEMENT_FQN, scope);
        if (baseClass == null) {
            baseClass = facade.findClass("com.webobjects.appserver.WOComponent", scope);
        }
        if (baseClass == null) {
            throw new Exception(WO_ELEMENT_FQN + " not found in project classpath");
        }

        PsiShortNamesCache cache = PsiShortNamesCache.getInstance(project);
        PsiClass[] candidateClasses = cache.getClassesByName(className, scope);

        if (candidateClasses.length == 0) {
            throw new Exception("The class for '" + className + "' is missing");
        }

        List<PsiClass> matchingClasses = new ArrayList<>();
        for (PsiClass candidateClass : candidateClasses) {
            if (candidateClass.isInheritor(baseClass, true)) {
                matchingClasses.add(candidateClass);
            }
        }

        if (matchingClasses.isEmpty()) {
            throw new Exception("The class for '" + className + "' does not extend " + baseClass.getName());
        }

        if (matchingClasses.size() == 1) {
            return matchingClasses.get(0);
        }

        // If multiple classes inherit from WOElement/WOComponent:
        // Filter out build artifacts (target/, build/, out/) and prefer project source classes
        List<PsiClass> sourceCandidates = new ArrayList<>();
        for (PsiClass candidate : matchingClasses) {
            if (candidate.getContainingFile() != null) {
                VirtualFile vf = candidate.getContainingFile().getVirtualFile();
                if (vf != null) {
                    String path = vf.getPath();
                    if (!path.contains("/target/") && !path.contains("/build/") && !path.contains("/out/") && !path.contains("/.gradle/")) {
                        sourceCandidates.add(candidate);
                    }
                }
            }
        }

        if (sourceCandidates.size() == 1) {
            return sourceCandidates.get(0);
        }

        List<PsiClass> toReport = sourceCandidates.isEmpty() ? matchingClasses : sourceCandidates;
        throw new Exception("The class for '" + className + "' is ambiguous (more than 1: " +
                toReport.stream().map(c -> c.getQualifiedName() != null ? c.getQualifiedName() : c.getName()).collect(Collectors.joining(", ")) + ")");
    }

    public static @Nullable PsiDirectory getComponentFolderForClass(@NotNull PsiClass componentClass) {
        try {
            Project project = componentClass.getProject();
            PsiFile containingFile = componentClass.getContainingFile();
            if (containingFile == null) return null;
            VirtualFile classFile = containingFile.getVirtualFile();
            if (classFile == null) return null;

            String className = componentClass.getName();
            if (className == null) return null;
            String folderName = className + "." + WOFileUtil.COMPONENT_EXTENSION;

            // 1. Check parent hierarchy for 'main' dir
            VirtualFile current = classFile.getParent();
            VirtualFile mainDir = null;
            while (current != null) {
                if ("main".equals(current.getName())) {
                    mainDir = current;
                    break;
                }
                current = current.getParent();
            }
            if (mainDir != null) {
                VirtualFile componentsDir = mainDir.findChild("components");
                if (componentsDir != null) {
                    VirtualFile componentFolder = componentsDir.findChild(folderName);
                    if (componentFolder != null && componentFolder.isDirectory()) {
                        return PsiManager.getInstance(project).findDirectory(componentFolder);
                    }
                }
                VirtualFile resourcesDir = mainDir.findChild("resources");
                if (resourcesDir != null) {
                    VirtualFile componentFolder = resourcesDir.findChild(folderName);
                    if (componentFolder != null && componentFolder.isDirectory()) {
                        return PsiManager.getInstance(project).findDirectory(componentFolder);
                    }
                }
            }

            // 2. Check module using SwitchToWOComponentAction.findResourceFolderByName
            Module module = ModuleUtilCore.findModuleForFile(classFile, project);
            VirtualFile targetFolder = SwitchToWOComponentAction.findResourceFolderByName(project, module, classFile, folderName);
            if (targetFolder != null && targetFolder.isDirectory()) {
                return PsiManager.getInstance(project).findDirectory(targetFolder);
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static @Nullable PsiDirectory getComponentFolderForComponentName(@NotNull String componentName, @NotNull Project project) {
        try {
            PsiClass componentClass = null;
            try {
                componentClass = getPsiClassForComponentName(componentName, project);
            } catch (Exception ignored) {
            }

            if (componentClass != null) {
                PsiDirectory dir = getComponentFolderForClass(componentClass);
                if (dir != null) {
                    return dir;
                }
            }

            // Fallback: search project for componentName.wo directly
            String simpleName = componentName.contains(".") ? componentName.substring(componentName.lastIndexOf('.') + 1) : componentName;
            String folderName = simpleName + "." + WOFileUtil.COMPONENT_EXTENSION;
            Collection<VirtualFile> matches = FilenameIndex.getVirtualFilesByName(folderName, GlobalSearchScope.projectScope(project));
            for (VirtualFile match : matches) {
                if (WOComponentClassResolver.isValidComponentFolder(project, match)) {
                    return PsiManager.getInstance(project).findDirectory(match);
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
