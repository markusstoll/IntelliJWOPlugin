package org.wocommunity.plugins.intellij.runconfig;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiMethodUtil;
import org.jetbrains.annotations.Nullable;

/**
 * Utility for detecting and resolving WebObjects Application main classes,
 * supporting Java, Kotlin classes with @JvmStatic companion methods,
 * and Kotlin files with top-level main functions (facade classes like *Kt).
 */
public final class WOApplicationClassUtil {
    public static final String WO_APPLICATION = "com.webobjects.appserver.WOApplication";
    public static final String ERX_APPLICATION = "er.extensions.appserver.ERXApplication";

    private WOApplicationClassUtil() {}

    /**
     * Executes the given computation within a ReadAction if an Application instance is available.
     */
    public static <T> T computeReadAction(Computable<T> action) {
        Application app = ApplicationManager.getApplication();
        if (app != null) {
            return app.runReadAction(action);
        }
        return action.compute();
    }

    /**
     * Checks whether a class extends com.webobjects.appserver.WOApplication or er.extensions.appserver.ERXApplication.
     */
    public static boolean isWOApplicationClass(@Nullable PsiClass aClass) {
        if (aClass == null || aClass.isInterface() || aClass.isAnnotationType()) {
            return false;
        }
        return computeReadAction(() -> {
            String qName = aClass.getQualifiedName();
            if (qName == null) {
                return false;
            }
            if (WO_APPLICATION.equals(qName) || ERX_APPLICATION.equals(qName)) {
                return true;
            }
            Project project = aClass.getProject();
            if (project != null) {
                try {
                    if (InheritanceUtil.isInheritor(aClass, WO_APPLICATION) ||
                        InheritanceUtil.isInheritor(aClass, ERX_APPLICATION)) {
                        return true;
                    }
                } catch (Throwable ignored) {
                    // Ignore resolve issues, continue to fallback
                }
            }

            // Safe fallback hierarchy walk
            PsiClass current = aClass.getSuperClass();
            while (current != null) {
                String qn = current.getQualifiedName();
                String name = current.getName();
                if (WO_APPLICATION.equals(qn) || ERX_APPLICATION.equals(qn) ||
                    "WOApplication".equals(name) || "ERXApplication".equals(name)) {
                    return true;
                }
                current = current.getSuperClass();
            }
            return false;
        });
    }

    /**
     * Checks whether a class directly contains an executable main method
     * (standard Java main, Kotlin @JvmStatic main, or companion object main).
     */
    public static boolean hasExecutableMainMethod(@Nullable PsiClass aClass) {
        if (aClass == null) {
            return false;
        }
        return computeReadAction(() -> {
            Project project = aClass.getProject();
            if (project != null) {
                try {
                    if (PsiMethodUtil.hasMainMethod(aClass) || PsiMethodUtil.findMainMethod(aClass) != null) {
                        return true;
                    }
                } catch (Throwable ignored) {
                    // Ignore resolve issues, continue to checks below
                }
            }
            // Direct method check on class
            for (PsiMethod method : aClass.getMethods()) {
                if ("main".equals(method.getName())) {
                    return true;
                }
            }
            // Check companion object or inner classes for Kotlin
            for (PsiClass inner : aClass.getInnerClasses()) {
                String innerName = inner.getName();
                if (innerName != null && innerName.contains("Companion")) {
                    if (inner.getProject() != null) {
                        try {
                            if (PsiMethodUtil.hasMainMethod(inner) || PsiMethodUtil.findMainMethod(inner) != null) {
                                return true;
                            }
                        } catch (Throwable ignored) {
                            // Ignore
                        }
                    }
                    for (PsiMethod method : inner.getMethods()) {
                        if ("main".equals(method.getName())) {
                            return true;
                        }
                    }
                }
            }
            return false;
        });
    }

    /**
     * Checks whether the containing file defines an executable main method
     * in a sibling class (e.g. Kotlin facade class like ApplicationKt).
     */
    public static boolean fileHasMainMethod(@Nullable PsiClass aClass) {
        return findExecutableClassInFile(aClass) != null;
    }

    /**
     * Finds the class in the same file that has the executable main method.
     * If the class itself has it, returns the class. Otherwise, searches sibling classes in the same file.
     */
    @Nullable
    public static PsiClass findExecutableClassInFile(@Nullable PsiClass aClass) {
        if (aClass == null) {
            return null;
        }
        return computeReadAction(() -> {
            if (hasExecutableMainMethod(aClass)) {
                return aClass;
            }
            PsiFile file = aClass.getContainingFile();
            if (file instanceof PsiClassOwner) {
                for (PsiClass sibling : ((PsiClassOwner) file).getClasses()) {
                    if (sibling != aClass && hasExecutableMainMethod(sibling)) {
                        return sibling;
                    }
                }
            }
            // Fallback for Kotlin facade class (e.g. ApplicationKt for Application)
            String qName = aClass.getQualifiedName();
            if (qName != null && !qName.endsWith("Kt") && aClass.getProject() != null) {
                try {
                    JavaPsiFacade facade = JavaPsiFacade.getInstance(aClass.getProject());
                    GlobalSearchScope scope = aClass.getResolveScope();
                    PsiClass ktClass = facade.findClass(qName + "Kt", scope);
                    if (ktClass != null && hasExecutableMainMethod(ktClass)) {
                        return ktClass;
                    }
                } catch (Throwable ignored) {}
            }
            return null;
        });
    }

    /**
     * Checks if a class is associated with a WOApplication (either it is one itself,
     * or its containing file defines a WOApplication subclass).
     */
    public static boolean isAssociatedWithWOApplication(@Nullable PsiClass aClass) {
        if (aClass == null) {
            return false;
        }
        return computeReadAction(() -> {
            if (isWOApplicationClass(aClass)) {
                return true;
            }
            PsiFile file = aClass.getContainingFile();
            if (file instanceof PsiClassOwner) {
                for (PsiClass sibling : ((PsiClassOwner) file).getClasses()) {
                    if (sibling != aClass && isWOApplicationClass(sibling)) {
                        return true;
                    }
                }
            }
            return false;
        });
    }

    /**
     * Checks whether a class has a companion object with a main method that is missing @JvmStatic.
     */
    public static boolean hasCompanionMainWithoutJvmStatic(@Nullable PsiClass aClass) {
        if (aClass == null) {
            return false;
        }
        return computeReadAction(() -> {
            for (PsiClass inner : aClass.getInnerClasses()) {
                String innerName = inner.getName();
                if (innerName != null && innerName.contains("Companion")) {
                    for (PsiMethod method : inner.getMethods()) {
                        if ("main".equals(method.getName())) {
                            Project project = aClass.getProject();
                            if (project != null) {
                                try {
                                    return !PsiMethodUtil.hasMainMethod(aClass);
                                } catch (Throwable ignored) {}
                            }
                            return true;
                        }
                    }
                }
            }
            return false;
        });
    }

    /**
     * Filter predicate for selecting main classes in the run configuration dialog.
     */
    public static boolean isCandidateMainClass(@Nullable PsiClass aClass) {
        if (aClass == null) {
            return false;
        }
        return computeReadAction(() -> {
            String simpleName = aClass.getName();

            // Never show Kotlin synthetic facade classes (*Kt) in the chooser unless it directly extends WOApplication
            if (simpleName != null && simpleName.endsWith("Kt") && !isWOApplicationClass(aClass)) {
                return false;
            }

            if (simpleName != null) {
                if ("Application".equals(simpleName) || simpleName.endsWith("Application")) {
                    return true;
                }
            }
            // 1. Any direct WOApplication / ERXApplication class is ALWAYS an accepted candidate!
            if (isWOApplicationClass(aClass)) {
                return true;
            }
            // 2. Class with executable main method that is associated with a WOApplication
            if (hasExecutableMainMethod(aClass) && isAssociatedWithWOApplication(aClass)) {
                return true;
            }
            return false;
        });
    }

    /**
     * Resolves the actual executable main class name to pass to Java.
     * If the selected class does not directly contain a main method, but its file contains an executable facade
     * (e.g. ApplicationKt), the facade's qualified name is returned.
     */
    public static String resolveExecutableMainClassName(Project project, @Nullable Module module, String mainClassName) {
        if (mainClassName == null || mainClassName.isBlank() || project == null || project.isDisposed()) {
            return mainClassName;
        }
        return computeReadAction(() -> {
            GlobalSearchScope scope = module != null
                    ? GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
                    : GlobalSearchScope.allScope(project);
            JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
            PsiClass selectedClass = facade.findClass(mainClassName, scope);
            if (selectedClass != null && !PsiMethodUtil.hasMainMethod(selectedClass)) {
                PsiClass executableClass = findExecutableClassInFile(selectedClass);
                if (executableClass != null && executableClass.getQualifiedName() != null) {
                    return executableClass.getQualifiedName();
                }
                // Direct Kt fallback
                if (!mainClassName.endsWith("Kt")) {
                    PsiClass ktClass = facade.findClass(mainClassName + "Kt", scope);
                    if (ktClass != null && hasExecutableMainMethod(ktClass) && ktClass.getQualifiedName() != null) {
                        return ktClass.getQualifiedName();
                    }
                }
            }
            return mainClassName;
        });
    }
}
