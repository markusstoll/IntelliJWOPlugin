package org.wocommunity.plugins.intellij.wod;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.ParsingTestCase;
import org.junit.Test;
import org.wocommunity.plugins.intellij.psi.wod.WODAssignment;
import org.wocommunity.plugins.intellij.psi.wod.WODAssignmentComment;
import org.wocommunity.plugins.intellij.psi.wod.WODDeclaration;
import org.wocommunity.plugins.intellij.psi.wod.WODFile;

import java.util.ArrayList;
import java.util.List;

public class WODParsingTest extends ParsingTestCase {
    public WODParsingTest() {
        super("", "wod", new WODParserDefinition());
    }

    @Override
    protected String getTestDataPath() {
        return "";
    }

    @Override
    protected boolean includeRanges() {
        return true;
    }

    @Test
    public void testAssignmentCommentNestedInAssignment() {
        String code = "Main: MyComponent {\n" +
                "    foo = \"bar\"; // some comment\n" +
                "    // standalone comment\n" +
                "    baz = 123;\n" +
                "}\n";

        var psiFile = createPsiFile("test.wod", code);
        assertTrue(psiFile instanceof WODFile);

        List<WODDeclaration> declarations = new ArrayList<>(PsiTreeUtil.findChildrenOfType(psiFile, WODDeclaration.class));
        assertEquals(1, declarations.size());
        WODDeclaration declaration = declarations.get(0);

        assertNotNull(declaration.getWODAssignmentList());
        List<WODAssignment> assignments = declaration.getWODAssignmentList().getWODAssignmentList();
        assertEquals(2, assignments.size());

        WODAssignment firstAssignment = assignments.get(0);
        assertEquals("foo", firstAssignment.getWODBinding().getText());
        WODAssignmentComment assignmentComment = firstAssignment.getWODAssignmentComment();
        assertNotNull("WODAssignmentComment should be present in the first assignment", assignmentComment);
        assertEquals("// some comment", assignmentComment.getText());

        WODAssignment secondAssignment = assignments.get(1);
        assertEquals("baz", secondAssignment.getWODBinding().getText());
        assertNull("Second assignment should not have WODAssignmentComment", secondAssignment.getWODAssignmentComment());
    }

    @Test
    public void testBlockCommentOnSameLineAsAssignment() {
        String code = "Main: MyComponent {\n" +
                "    foo = \"bar\"; /* inline block comment */\n" +
                "}\n";

        var psiFile = createPsiFile("test.wod", code);
        List<WODDeclaration> declarations = new ArrayList<>(PsiTreeUtil.findChildrenOfType(psiFile, WODDeclaration.class));
        assertEquals(1, declarations.size());
        WODAssignment assignment = declarations.get(0).getWODAssignmentList().getWODAssignmentList().get(0);

        WODAssignmentComment comment = assignment.getWODAssignmentComment();
        assertNotNull(comment);
        assertEquals("/* inline block comment */", comment.getText());
    }

    @Test
    public void testValidCommentOnAssignment() {
        String code = "Main: MyComponent {\n" +
                "    foo = \"bar\"; // valid\n" +
                "}\n";

        var psiFile = createPsiFile("test.wod", code);
        List<WODDeclaration> declarations = new ArrayList<>(PsiTreeUtil.findChildrenOfType(psiFile, WODDeclaration.class));
        assertEquals(1, declarations.size());
        WODAssignment assignment = declarations.get(0).getWODAssignmentList().getWODAssignmentList().get(0);

        WODAssignmentComment comment = assignment.getWODAssignmentComment();
        assertNotNull(comment);
        assertEquals("// valid", comment.getText());
        assertTrue(comment.getText().contains("valid"));
    }

    @Test
    public void testNoSpaceBeforeAssignmentComment() {
        String code = "Main: MyComponent {\n" +
                "    foo = \"bar\";// comment without space\n" +
                "}\n";

        var psiFile = createPsiFile("test.wod", code);
        List<WODDeclaration> declarations = new ArrayList<>(PsiTreeUtil.findChildrenOfType(psiFile, WODDeclaration.class));
        assertEquals(1, declarations.size());
        WODAssignment assignment = declarations.get(0).getWODAssignmentList().getWODAssignmentList().get(0);

        WODAssignmentComment comment = assignment.getWODAssignmentComment();
        assertNotNull(comment);
        assertEquals("// comment without space", comment.getText());
    }

    @Test
    public void testDeclarationWithTrailingSemicolonAndBooleanLiterals() {
        String code = "Frachtkostenhinweis: WOString\n" +
                "{\n" +
                "\tvalue = session.localizer.AnfrageAuftrag_Frachtkostenhinweis;\n" +
                "\tescapeHTML = NO;\n" +
                "};\n";

        var psiFile = createPsiFile("test.wod", code);
        assertFalse(PsiTreeUtil.hasErrorElements(psiFile));

        List<WODDeclaration> declarations = new ArrayList<>(PsiTreeUtil.findChildrenOfType(psiFile, WODDeclaration.class));
        assertEquals(1, declarations.size());
        WODDeclaration decl = declarations.get(0);
        assertEquals("Frachtkostenhinweis", decl.getWODElement().getText());
        assertEquals("WOString", decl.getWODComponent().getText());

        List<WODAssignment> assignments = decl.getWODAssignmentList().getWODAssignmentList();
        assertEquals(2, assignments.size());
        assertEquals("value", assignments.get(0).getWODBinding().getText());
        assertEquals("escapeHTML", assignments.get(1).getWODBinding().getText());
        assertNotNull(assignments.get(1).getWODValue().getBoolean());
        assertEquals("NO", assignments.get(1).getWODValue().getBoolean().getText());
    }
}
