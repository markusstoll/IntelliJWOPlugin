package org.wocommunity.plugins.intellij.components;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.tools.WOIcons;

import java.util.Collection;
import java.util.Collections;

public class WOComponentNode extends ProjectViewNode<PsiDirectory> {

    public WOComponentNode(Project project, PsiDirectory value, ViewSettings viewSettings) {
        super(project, value, viewSettings);
    }

    @Override
    public void update(@NotNull PresentationData presentation) {
        PsiDirectory file = getValue();
        if (file != null) {
            // Set the name and custom icon for .wo folders
            String componentName = file.getName().replace(".wo", "");
            presentation.setPresentableText(componentName + " WO");
            presentation.setIcon(WOIcons.WOCOMPONENT_ICON); // Use your custom icon here
        }
    }

    @Override
    public @NotNull Collection<AbstractTreeNode<?>> getChildren() {
        return Collections.emptyList(); // Hide the folder's contents
    }

    @Override
    public boolean contains(@NotNull VirtualFile file) {
        PsiDirectory directory = getValue();
        if (directory == null) {
            return false;
        }
        VirtualFile woDirectory = directory.getVirtualFile();

        // Das .wo Verzeichnis selbst
        if (woDirectory.equals(file)) {
            return true;
        }

        // Prüfen ob file ein Kind des .wo Verzeichnisses ist
        VirtualFile parent = file.getParent();
        while (parent != null) {
            if (parent.equals(woDirectory)) {
                return true;
            }
            parent = parent.getParent();
        }

        // Prüfen ob file das zugehörige .api File im Komponenten-Ordner ist
        String componentName = directory.getName().replace(".wo", "");
        if (file.getName().equals(componentName + ".api")) {
            if (woDirectory.getParent() != null && woDirectory.getParent().equals(file.getParent())) {
                return true;
            }
            // Fallback für Ressourcen-Ordner
            String woPath = woDirectory.getPath();
            String apiPath = file.getPath();
            if (woPath.contains("/components/") && apiPath.contains("/resources/")) {
                String relWo = woPath.substring(woPath.indexOf("/components/") + "/components/".length());
                String relApi = apiPath.substring(apiPath.indexOf("/resources/") + "/resources/".length());
                if (relWo.equals(componentName + ".wo") && relApi.equals(componentName + ".api")) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean canRepresent(Object element) {
        if (super.canRepresent(element)) {
            return true;
        }
        if (element instanceof AbstractTreeNode<?> atn) {
            return canRepresent(atn.getValue());
        }
        if (element instanceof PsiElement psiElement) {
            if (getValue() != null && getValue().equals(psiElement)) {
                return true;
            }
            VirtualFile vFile = PsiUtilCore.getVirtualFile(psiElement);
            if (vFile != null && contains(vFile)) {
                return true;
            }
        }
        if (element instanceof VirtualFile vFile) {
            return contains(vFile);
        }
        if (element instanceof com.intellij.openapi.fileEditor.FileEditor fileEditor) {
            VirtualFile vFile = fileEditor.getFile();
            if (vFile != null && contains(vFile)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @Nullable VirtualFile getVirtualFile() {
        if (getValue() != null) {
            VirtualFile woDir = getValue().getVirtualFile();
            if (woDir != null) {
                String componentName = getValue().getName().replace(".wo", "");
                VirtualFile htmlFile = woDir.findChild(componentName + ".html");
                if (htmlFile != null && !htmlFile.isDirectory()) {
                    return htmlFile;
                }
                VirtualFile wodFile = woDir.findChild(componentName + ".wod");
                if (wodFile != null && !wodFile.isDirectory()) {
                    return wodFile;
                }
            }
        }
        return null;
    }

    @Override
    public boolean canNavigate() {
        return true; // Enable navigation if needed
    }

    @Override
    public boolean canNavigateToSource() {
        return true; // Enable navigation if needed
    }

    @Override
    public @Nullable com.intellij.platform.backend.navigation.NavigationRequest navigationRequest() {
        if (getValue() != null && getValue().getVirtualFile() != null) {
            VirtualFile woDir = getValue().getVirtualFile();
            String componentName = getValue().getName().replace(".wo", "");
            VirtualFile htmlFile = woDir.findChild(componentName + ".html");
            if (htmlFile != null && !htmlFile.isDirectory()) {
                return com.intellij.platform.backend.navigation.NavigationRequest.sourceNavigationRequest(myProject, htmlFile, 0);
            }
            VirtualFile wodFile = woDir.findChild(componentName + ".wod");
            if (wodFile != null && !wodFile.isDirectory()) {
                return com.intellij.platform.backend.navigation.NavigationRequest.sourceNavigationRequest(myProject, wodFile, 0);
            }
        }
        return null;
    }

    @Override
    public void navigate(boolean requestFocus) {
        if (getValue() != null && getValue().getVirtualFile() != null) {
            VirtualFile woDir = getValue().getVirtualFile();
            String componentName = getValue().getName().replace(".wo", "");
            VirtualFile htmlFile = woDir.findChild(componentName + ".html");
            if (htmlFile != null && !htmlFile.isDirectory()) {
                WOEditorUtils.openComponent(myProject, htmlFile);
            } else {
                VirtualFile wodFile = woDir.findChild(componentName + ".wod");
                if (wodFile != null && !wodFile.isDirectory()) {
                    WOEditorUtils.openComponent(myProject, wodFile);
                } else {
                    WOEditorUtils.openComponent(myProject, woDir);
                }
            }
        }
    }
}