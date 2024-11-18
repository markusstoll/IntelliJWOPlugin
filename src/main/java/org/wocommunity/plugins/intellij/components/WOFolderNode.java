package org.wocommunity.plugins.intellij.components;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.tools.WOIcons;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class WOFolderNode extends ProjectViewNode<PsiDirectory> {

    public WOFolderNode(Project project, PsiDirectory value, ViewSettings viewSettings) {
        super(project, value, viewSettings);
    }

    @Override
    public void update(@NotNull PresentationData presentation) {
        PsiDirectory file = getValue();
        if (file != null) {
            // Set the name and custom icon for .wo folders
            presentation.setPresentableText(file.getName());
            presentation.setIcon(WOIcons.WOCOMPONENT_ICON); // Use your custom icon here
        }
    }

    @Override
    public @NotNull Collection<AbstractTreeNode<?>> getChildren() {
        return Collections.emptyList(); // Hide the folder's contents
    }

    @Override
    public boolean contains(@NotNull VirtualFile file) {
        return false; // Prevent selection of nested files
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
    public void navigate(boolean requestFocus) {
        FileEditorManager.getInstance(myProject).openFile(getValue().getVirtualFile(), true);
    }
    public void navigate_old(boolean requestFocus) {
        PsiDirectory folder = getValue();

        if (folder != null) {
            // Specify the HTML file to open, for example, "index.html" inside the .wo folder
            String htmlFileName = folder.getName().replace(".wo", ".html");

            @Nullable PsiFile htmlFile = folder.findFile(htmlFileName);
            if (htmlFile != null) {
                openHtmlEditor(htmlFile);
            } else {
                // Optionally, show an error or feedback if the file is not found
                Notifications.Bus.notify(new Notification(
                        "WOFolder", "File Not Found",
                        htmlFile + " was not found in " + folder.getName(),
                        NotificationType.WARNING
                ));
            }
        }
    }

    private void openHtmlEditor(PsiFile htmlFile) {
        if (htmlFile != null && htmlFile.isValid()) {
            FileEditorManager.getInstance(myProject).openFile(htmlFile.getVirtualFile(), true);
        }
    }
}