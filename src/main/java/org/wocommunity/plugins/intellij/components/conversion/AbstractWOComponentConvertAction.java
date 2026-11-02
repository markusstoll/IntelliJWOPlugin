package org.wocommunity.plugins.intellij.components.conversion;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.components.SwitchToWOComponentAction;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractWOComponentConvertAction extends AnAction {

    private static final List<String> SRC_SUFFIXES = Arrays.asList("java", "kt", "groovy");

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile woDir = findComponentFolder(e);
        boolean enabled = project != null && woDir != null && woDir.isDirectory() && woDir.getName().endsWith(".wo");
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        VirtualFile woDir = findComponentFolder(e);
        if (woDir == null || !woDir.isDirectory() || !woDir.getName().endsWith(".wo")) {
            return;
        }

        String compName = woDir.getName().substring(0, woDir.getName().length() - 3);
        VirtualFile htmlFile = findChildIgnoreCase(woDir, compName + ".html");
        if (htmlFile == null) {
            Notifications.Bus.notify(new Notification(
                    "WebObjects",
                    "WO Component Conversion",
                    "HTML template " + compName + ".html not found in " + woDir.getName(),
                    NotificationType.WARNING
            ), project);
            return;
        }

        VirtualFile wodFile = findChildIgnoreCase(woDir, compName + ".wod");
        if (wodFile == null && isConvertToWod()) {
            try {
                wodFile = woDir.createChildData(this, compName + ".wod");
            } catch (IOException ex) {
                Notifications.Bus.notify(new Notification(
                        "WebObjects",
                        "WO Component Conversion",
                        "Could not create " + compName + ".wod: " + ex.getMessage(),
                        NotificationType.ERROR
                ), project);
                return;
            }
        }

        Document htmlDoc = FileDocumentManager.getInstance().getDocument(htmlFile);
        if (htmlDoc == null) {
            Notifications.Bus.notify(new Notification(
                    "WebObjects",
                    "WO Component Conversion",
                    "Could not load document for " + htmlFile.getName(),
                    NotificationType.WARNING
            ), project);
            return;
        }

        Document wodDoc = (wodFile != null) ? FileDocumentManager.getInstance().getDocument(wodFile) : null;
        String htmlText = htmlDoc.getText();
        String wodText = (wodDoc != null) ? wodDoc.getText() : "";

        WOComponentConversionUtil.ConversionResult result = performConversion(htmlText, wodText);

        if (result.getConvertedCount() == 0) {
            Notifications.Bus.notify(new Notification(
                    "WebObjects",
                    "WO Component Conversion",
                    getNoConversionMessage(),
                    NotificationType.INFORMATION
            ), project);
            return;
        }

        if (wodDoc == null && !result.getWodText().isEmpty()) {
            try {
                wodFile = woDir.createChildData(this, compName + ".wod");
                wodDoc = FileDocumentManager.getInstance().getDocument(wodFile);
            } catch (IOException ex) {
                Notifications.Bus.notify(new Notification(
                        "WebObjects",
                        "WO Component Conversion",
                        "Could not create " + compName + ".wod: " + ex.getMessage(),
                        NotificationType.ERROR
                ), project);
                return;
            }
        }

        final Document finalWodDoc = wodDoc;
        WriteCommandAction.runWriteCommandAction(project, getActionTitle(), null, () -> {
            htmlDoc.setText(result.getHtmlText());
            if (finalWodDoc != null) {
                finalWodDoc.setText(result.getWodText());
            }
            FileDocumentManager.getInstance().saveDocument(htmlDoc);
            if (finalWodDoc != null) {
                FileDocumentManager.getInstance().saveDocument(finalWodDoc);
            }
            PsiDocumentManager.getInstance(project).commitDocument(htmlDoc);
            if (finalWodDoc != null) {
                PsiDocumentManager.getInstance(project).commitDocument(finalWodDoc);
            }
        });

        Notifications.Bus.notify(new Notification(
                "WebObjects",
                "WO Component Conversion",
                getSuccessMessage(result.getConvertedCount()),
                NotificationType.INFORMATION
        ), project);
    }

    protected abstract boolean isConvertToWod();

    protected abstract @NotNull String getActionTitle();

    protected abstract @NotNull WOComponentConversionUtil.ConversionResult performConversion(@NotNull String htmlText, @NotNull String wodText);

    protected abstract @NotNull String getNoConversionMessage();

    protected abstract @NotNull String getSuccessMessage(int count);

    public static @Nullable VirtualFile findComponentFolder(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file == null) {
            PsiElement element = e.getData(CommonDataKeys.PSI_ELEMENT);
            if (element instanceof PsiDirectory) {
                file = ((PsiDirectory) element).getVirtualFile();
            } else if (element instanceof PsiFile) {
                file = ((PsiFile) element).getVirtualFile();
            }
        }

        if (file == null && project != null) {
            PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
            if (psiFile != null) {
                file = psiFile.getVirtualFile();
            }
        }

        if (file == null) {
            return null;
        }

        // Direct .wo directory
        if (file.isDirectory() && file.getName().endsWith(".wo")) {
            return file;
        }

        // Inside a .wo directory (.html, .wod, .woo)
        VirtualFile parent = file.getParent();
        if (parent != null && parent.isDirectory() && parent.getName().endsWith(".wo")) {
            return parent;
        }

        // Associated .api file outside the .wo directory
        if (file.getName().endsWith(".api") && parent != null) {
            String compName = file.getName().substring(0, file.getName().length() - 4);
            VirtualFile woDir = parent.findChild(compName + ".wo");
            if (woDir != null && woDir.isDirectory()) {
                return woDir;
            }
        }

        // Source file (.java, .kt, .groovy)
        if (project != null && isSourceFile(file)) {
            String compName = FilenameUtils.getBaseName(file.getName());
            Module module = ModuleUtilCore.findModuleForFile(file, project);
            return SwitchToWOComponentAction.findResourceFolderByName(project, module, file, compName + ".wo");
        }

        return null;
    }

    private static boolean isSourceFile(@NotNull VirtualFile file) {
        String ext = file.getExtension();
        return ext != null && SRC_SUFFIXES.contains(ext.toLowerCase());
    }

    private static @Nullable VirtualFile findChildIgnoreCase(@NotNull VirtualFile dir, @NotNull String fileName) {
        VirtualFile direct = dir.findChild(fileName);
        if (direct != null) return direct;
        for (VirtualFile child : dir.getChildren()) {
            if (child.getName().equalsIgnoreCase(fileName)) {
                return child;
            }
        }
        return null;
    }
}

