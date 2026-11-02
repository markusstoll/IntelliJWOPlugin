// This is a generated file. Not intended for manual editing.
package org.wocommunity.plugins.intellij.wod.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static org.wocommunity.plugins.intellij.psi.wod.WODTypes.*;
import static org.wocommunity.plugins.intellij.wod.parser.WODParserUtil.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class WODParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    r = parse_root_(t, b);
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    return parse_root_(t, b, 0);
  }

  static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return WODFile(b, l + 1);
  }

  /* ********************************************************** */
  // WODBinding ASSIGN WODValue SEMI WODAssignmentComment?
  public static boolean WODAssignment(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "WODAssignment")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, WOD_ASSIGNMENT, null);
    r = WODBinding(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, ASSIGN));
    r = p && report_error_(b, WODValue(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, SEMI)) && r;
    r = p && WODAssignment_4(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // WODAssignmentComment?
  private static boolean WODAssignment_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "WODAssignment_4")) return false;
    WODAssignmentComment(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // ASSIGNMENT_COMMENT
  public static boolean WODAssignmentComment(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "WODAssignmentComment")) return false;
    if (!nextTokenIs(b, ASSIGNMENT_COMMENT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ASSIGNMENT_COMMENT);
    exit_section_(b, m, WOD_ASSIGNMENT_COMMENT, r);
    return r;
  }

  /* ********************************************************** */
  // WODAssignment *
  public static boolean WODAssignmentList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "WODAssignmentList")) return false;
    Marker m = enter_section_(b, l, _NONE_, WOD_ASSIGNMENT_LIST, "<wod assignment list>");
    while (true) {
      int c = current_position_(b);
      if (consumeToken(b, ASSIGNMENT_COMMENT)) continue;
      if (!WODAssignment(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "WODAssignmentList", c)) break;
    }
    exit_section_(b, l, m, true, false, null);
    return true;
  }

  /* ********************************************************** */
  // IDENTIFIER
  public static boolean WODBinding(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "WODBinding")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IDENTIFIER);
    exit_section_(b, m, WOD_BINDING, r);
    return r;
  }

  /* ********************************************************** */
  // IDENTIFIER
  public static boolean WODComponent(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "WODComponent")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IDENTIFIER);
    exit_section_(b, m, WOD_COMPONENT, r);
    return r;
  }

  /* ********************************************************** */
  // WODElement COLON WODComponent LBRACE WODAssignmentList RBRACE SEMI?
  public static boolean WODDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "WODDeclaration")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, WOD_DECLARATION, null);
    r = WODElement(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, COLON));
    r = p && report_error_(b, WODComponent(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, LBRACE)) && r;
    r = p && report_error_(b, WODAssignmentList(b, l + 1)) && r;
    r = p && consumeToken(b, RBRACE) && r;
    consumeToken(b, SEMI);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // IDENTIFIER
  public static boolean WODElement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "WODElement")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IDENTIFIER);
    exit_section_(b, m, WOD_ELEMENT, r);
    return r;
  }

  /* ********************************************************** */
  // (WODDeclaration | SEMI) *
  static boolean WODFile(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "WODFile")) return false;
    while (true) {
      int c = current_position_(b);
      if (consumeToken(b, ASSIGNMENT_COMMENT)) continue;
      if (consumeToken(b, SEMI)) continue;
      if (!WODDeclaration(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "WODFile", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // WODKeyPathElement (DOT WODKeyPathElement)*
  public static boolean WODKeyPath(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "WODKeyPath")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = WODKeyPathElement(b, l + 1);
    r = r && WODKeyPath_1(b, l + 1);
    exit_section_(b, m, WOD_KEY_PATH, r);
    return r;
  }

  // (DOT WODKeyPathElement)*
  private static boolean WODKeyPath_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "WODKeyPath_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!WODKeyPath_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "WODKeyPath_1", c)) break;
    }
    return true;
  }

  // DOT WODKeyPathElement
  private static boolean WODKeyPath_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "WODKeyPath_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DOT);
    r = r && WODKeyPathElement(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // IDENTIFIER
  public static boolean WODKeyPathElement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "WODKeyPathElement")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IDENTIFIER);
    exit_section_(b, m, WOD_KEY_PATH_ELEMENT, r);
    return r;
  }

  /* ********************************************************** */
  // CARET IDENTIFIER
  public static boolean WODParentBinding(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "WODParentBinding")) return false;
    if (!nextTokenIs(b, CARET)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, CARET, IDENTIFIER);
    exit_section_(b, m, WOD_PARENT_BINDING, r);
    return r;
  }

  /* ********************************************************** */
  // STRING | WODParentBinding | WODKeyPath | NUMBER | BOOLEAN
  public static boolean WODValue(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "WODValue")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, WOD_VALUE, "<wod value>");
    r = consumeToken(b, STRING);
    if (!r) r = WODParentBinding(b, l + 1);
    if (!r) r = WODKeyPath(b, l + 1);
    if (!r) r = consumeToken(b, NUMBER);
    if (!r) r = consumeToken(b, BOOLEAN);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

}
