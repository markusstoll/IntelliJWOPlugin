package org.wocommunity.plugins.intellij.wod;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static org.wocommunity.plugins.intellij.psi.wod.WODTypes.*;

%%

%{
  public _WODLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _WODLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

EOL=\R
WHITE_SPACE=\s+

IDENTIFIER=[a-zA-Z_][a-zA-Z0-9_\-]*
NUMBER=[0-9]+
BOOLEAN=true|false
STRING=\"([^\\\"\r\n]|\\[^\r\n])*\"
ASSIGNMENT_COMMENT="//".*|"/"\*([^*]|\*+[^*/])*\*+"/"

%%
<YYINITIAL> {
  {WHITE_SPACE}              { return WHITE_SPACE; }

  ";"                        { return SEMI; }
  "^"                        { return CARET; }
  ":"                        { return COLON; }
  "="                        { return ASSIGN; }
  "{"                        { return LBRACE; }
  "}"                        { return RBRACE; }
  "."                        { return DOT; }

  {BOOLEAN}                  { return BOOLEAN; }
  {IDENTIFIER}               { return IDENTIFIER; }
  {NUMBER}                   { return NUMBER; }
  {STRING}                   { return STRING; }
  {ASSIGNMENT_COMMENT}       { return ASSIGNMENT_COMMENT; }

}

[^] { return BAD_CHARACTER; }
