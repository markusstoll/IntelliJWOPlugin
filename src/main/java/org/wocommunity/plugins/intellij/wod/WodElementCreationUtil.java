package org.wocommunity.plugins.intellij.wod;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public final class WodElementCreationUtil {

    private WodElementCreationUtil() {
    }

    public static void createAndNavigateToElement(@NotNull Project project,
                                                 @Nullable VirtualFile wodFile,
                                                 @NotNull VirtualFile woDir,
                                                 @NotNull String elementName,
                                                 @NotNull String defaultTypeName) {
        WriteCommandAction.runWriteCommandAction(project, "Create WebObject '" + elementName + "'", null, () -> {
            VirtualFile targetWodFile = wodFile;
            if (targetWodFile == null || !targetWodFile.isValid()) {
                String compName = woDir.getName().endsWith(".wo")
                        ? woDir.getName().substring(0, woDir.getName().length() - 3)
                        : woDir.getName();
                targetWodFile = woDir.findChild(compName + ".wod");
                if (targetWodFile == null || !targetWodFile.isValid()) {
                    try {
                        targetWodFile = woDir.createChildData(WodElementCreationUtil.class, compName + ".wod");
                    } catch (IOException e) {
                        return;
                    }
                }
            }

            Document document = FileDocumentManager.getInstance().getDocument(targetWodFile);
            if (document == null) {
                return;
            }

            String currentText = document.getText();
            WodModel currentModel = WodParser.parse(currentText);
            WodElement existing = currentModel.getElement(elementName);
            if (existing != null && existing.getNameRange() != null) {
                final VirtualFile finalWodFile = targetWodFile;
                ApplicationManager.getApplication().invokeLater(() -> {
                    WOHtmlToWodNavigationUtil.navigateToWodAndSelect(
                            project, finalWodFile, existing.getNameRange().getStartOffset(), existing.getNameRange().getEndOffset()
                    );
                });
                return;
            }

            String prefix;
            if (currentText.isEmpty()) {
                prefix = "";
            } else if (currentText.endsWith("\n\n")) {
                prefix = "";
            } else if (currentText.endsWith("\n")) {
                prefix = "\n";
            } else {
                prefix = "\n\n";
            }

            int insertOffset = currentText.length();
            String snippet = prefix + elementName + " : " + defaultTypeName + " {\n}\n";
            document.insertString(insertOffset, snippet);
            FileDocumentManager.getInstance().saveDocument(document);
            PsiDocumentManager.getInstance(project).commitDocument(document);
            WodParser.invalidateCache(targetWodFile);

            int typeStart = insertOffset + prefix.length() + elementName.length() + 3; // after "xxx : "
            int typeEnd = typeStart + defaultTypeName.length();

            final VirtualFile finalWodFile = targetWodFile;
            ApplicationManager.getApplication().invokeLater(() -> {
                WOHtmlToWodNavigationUtil.navigateToWodAndSelect(project, finalWodFile, typeStart, typeEnd);
            });
        });
    }
}
