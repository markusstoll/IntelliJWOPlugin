package org.wocommunity.plugins.intellij.wod;

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.wocommunity.plugins.intellij.psi.wod.WODFile;

/**
 * Handles automatic smart indentation in WebObjects .wod files when pressing Enter.
 */
public class WodEnterHandlerDelegate extends EnterHandlerDelegateAdapter {

    @Override
    public Result preprocessEnter(@NotNull PsiFile file,
                                  @NotNull Editor editor,
                                  @NotNull Ref<Integer> caretOffset,
                                  @NotNull Ref<Integer> caretAdvance,
                                  @NotNull DataContext dataContext,
                                  @Nullable EditorActionHandler originalHandler) {
        if (!(file instanceof WODFile) && !file.getName().endsWith(".wod")) {
            return Result.Continue;
        }

        Document doc = editor.getDocument();
        int offset = caretOffset.get();
        if (offset <= 0 || offset >= doc.getTextLength()) {
            return Result.Continue;
        }

        CharSequence chars = doc.getCharsSequence();
        char prev = chars.charAt(offset - 1);
        char next = chars.charAt(offset);

        // When pressing Enter right between '{' and '}' -> expand into a block
        if (prev == '{' && next == '}') {
            doc.insertString(offset, "\n    \n");
            editor.getCaretModel().moveToOffset(offset + 5);
            return Result.Stop;
        }

        return Result.Continue;
    }

    @Override
    public Result postProcessEnter(@NotNull PsiFile file,
                                   @NotNull Editor editor,
                                   @NotNull DataContext dataContext) {
        if (!(file instanceof WODFile) && !file.getName().endsWith(".wod")) {
            return Result.Continue;
        }

        Document doc = editor.getDocument();
        int offset = editor.getCaretModel().getOffset();
        if (offset < 0 || offset > doc.getTextLength()) {
            return Result.Continue;
        }

        CharSequence chars = doc.getCharsSequence();
        int openBraces = 0;
        for (int i = 0; i < offset; i++) {
            char c = chars.charAt(i);
            if (c == '{') {
                openBraces++;
            } else if (c == '}') {
                openBraces--;
            }
        }

        if (openBraces > 0) {
            int lineNum = doc.getLineNumber(offset);
            int lineStart = doc.getLineStartOffset(lineNum);
            int lineEnd = doc.getLineEndOffset(lineNum);
            String lineText = chars.subSequence(lineStart, lineEnd).toString();

            // Do not indent if line starts with closing brace
            if (lineText.trim().startsWith("}")) {
                return Result.Continue;
            }

            // If line is empty or purely whitespace with less than desired indentation
            if (lineText.trim().isEmpty() && lineText.length() < openBraces * 4) {
                StringBuilder indent = new StringBuilder();
                for (int b = 0; b < openBraces; b++) {
                    indent.append("    ");
                }
                doc.replaceString(lineStart, lineEnd, indent.toString());
                editor.getCaretModel().moveToOffset(lineStart + indent.length());
                return Result.Stop;
            }
        }

        return Result.Continue;
    }
}
