package org.wocommunity.plugins.intellij.tools;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import org.apache.commons.lang3.StringUtils;

public class JavaFileCreator {

    /**
     * Creates a new Java file in the specified module with the given package, class name, and content.
     *
     * @param module      The module where the file should be created.
     * @param packageName The package name for the class.
     * @param className   The name of the Java class.
     * @param fileContent The content to populate in the new file.
     * @throws Exception if the file creation fails.
     */
    public static void createJavaFile(Module module, String packageName, String className, String fileContent) throws Exception {
        if (module == null || packageName == null || className == null || fileContent == null) {
            throw new IllegalArgumentException("Module, package name, class name, and file content must not be null.");
        }

        Project project = module.getProject();

        // Find the source root directory
        VirtualFile[] sourceRoots = ModuleRootManager.getInstance(module).getSourceRoots();
        if (sourceRoots.length == 0) {
            throw new Exception("No source roots found in the module.");
        }

        VirtualFile sourceRoot = null;

        for (VirtualFile file : sourceRoots) {
            if(file.getName().contains("esources"))
                continue;
            if(file.getName().contains("test"))
                continue;

            sourceRoot = file;
            break;
        }

        // Use the first source root as the base directory
        if (sourceRoot == null) {
            throw new Exception("No source roots found in the module.");
        }

        // Get the target package directory, creating it if necessary
        PsiManager psiManager = PsiManager.getInstance(project);
        PsiDirectory sourceRootPsi = psiManager.findDirectory(sourceRoot);
        if (sourceRootPsi == null) {
            throw new Exception("Source root directory is not accessible.");
        }

        PsiDirectory targetDirectory = StringUtils.isEmpty(packageName)
                ? sourceRootPsi
                : createOrFindPackageDirectory(sourceRootPsi, packageName);

        // Check if the file already exists
        String fileName = className.endsWith(".java") ? className : className + ".java";

        if (targetDirectory.findFile(fileName) != null) {
            throw new Exception("File already exists: " + fileName);
        }

        PsiFile file = PsiFileFactory.getInstance(project).createFileFromText(fileName, fileContent);
        // Add the file to the target directory
        targetDirectory.add(file);

        System.out.println("Java file created successfully: " + fileName);
    }

    /**
     * Creates or finds the package directory for a given package name.
     *
     * @param rootDirectory The root directory to start from.
     * @param packageName   The package name (e.g., "com.example.myapp").
     * @return The PsiDirectory representing the package.
     */
    private static PsiDirectory createOrFindPackageDirectory(PsiDirectory rootDirectory, String packageName) throws Exception {
        PsiDirectory currentDirectory = rootDirectory;

        String[] packageParts = packageName.split("\\.");
        for (String part : packageParts) {
            PsiDirectory subdirectory = currentDirectory.findSubdirectory(part);
            if (subdirectory == null) {
                subdirectory = currentDirectory.createSubdirectory(part);
            }
            currentDirectory = subdirectory;
        }

        return currentDirectory;
    }
}
