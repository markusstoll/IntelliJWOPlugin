package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;

/**
 * Inserts ` name=""` when `<webobject>` is completed and positions the caret inside `name="<caret>"`.
 */
public class WebObjectTagInsertHandler implements InsertHandler<LookupElement> {

    public static final WebObjectTagInsertHandler INSTANCE = new WebObjectTagInsertHandler();

    @Override
    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
        Editor editor = context.getEditor();
        Document doc = editor.getDocument();
        int tailOffset = context.getTailOffset();

        CharSequence chars = doc.getCharsSequence();
        boolean hasName = false;
        boolean hasClosing = false;
        int i = tailOffset;
        while (i < chars.length() && chars.charAt(i) != '>' && chars.charAt(i) != '\n') {
            char c = chars.charAt(i);
            if (c == 'n' || c == 'N') {
                int end = Math.min(chars.length(), i + 4);
                if ("name".equalsIgnoreCase(chars.subSequence(i, end).toString())) {
                    hasName = true;
                }
            }
            i++;
        }
        if (i < chars.length() && chars.charAt(i) == '>') {
            hasClosing = true;
        }

        if (!hasName) {
            String toInsert = " name=\"\"" + (hasClosing ? "" : "/>");
            doc.insertString(tailOffset, toInsert);
            int caretOffset = tailOffset + " name=\"".length();
            editor.getCaretModel().moveToOffset(caretOffset);

            // Trigger auto-popup to immediately suggest available WOD element names!
            AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(editor);
        }
    }
}

