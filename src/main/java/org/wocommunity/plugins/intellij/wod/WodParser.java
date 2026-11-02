package org.wocommunity.plugins.intellij.wod;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WodParser {

    private static class CachedModel {
        final long modStamp;
        final WodModel model;

        CachedModel(long modStamp, WodModel model) {
            this.modStamp = modStamp;
            this.model = model;
        }
    }

    private static final Map<String, CachedModel> CACHE = new ConcurrentHashMap<>();

    public static void invalidateCache(@Nullable VirtualFile file) {
        if (file != null) {
            CACHE.remove(file.getUrl());
        }
    }

    public static @NotNull WodModel getModel(@Nullable VirtualFile file) {
        if (file == null || !file.isValid()) {
            return new WodModel();
        }

        Document doc = FileDocumentManager.getInstance().getCachedDocument(file);
        if (doc != null) {
            long stamp = doc.getModificationStamp();
            String url = file.getUrl();
            CachedModel cached = CACHE.get(url);
            if (cached != null && cached.modStamp == stamp) {
                return cached.model;
            }
            WodModel parsed = parse(doc.getImmutableCharSequence());
            CACHE.put(url, new CachedModel(stamp, parsed));
            return parsed;
        }

        long stamp = file.getModificationStamp();
        String url = file.getUrl();
        CachedModel cached = CACHE.get(url);
        if (cached != null && cached.modStamp == stamp) {
            return cached.model;
        }

        try {
            String text = VfsUtilCore.loadText(file);
            WodModel parsed = parse(text);
            CACHE.put(url, new CachedModel(stamp, parsed));
            return parsed;
        } catch (IOException e) {
            return new WodModel();
        }
    }

    public static @NotNull WodModel parse(@NotNull CharSequence text) {
        WodModel model = new WodModel();
        WodLexer lexer = new WodLexer();
        lexer.start(text, 0, text.length(), 0);

        while (lexer.getTokenType() != null) {
            skipWhitespaceAndComments(lexer);
            if (lexer.getTokenType() == null) {
                break;
            }

            IElementType type = lexer.getTokenType();
            if (type == WodTokenTypes.ELEMENT_NAME) {
                int elemStart = lexer.getTokenStart();
                int elemEnd = lexer.getTokenEnd();
                String elemName = text.subSequence(elemStart, elemEnd).toString().trim();
                TextRange nameRange = new TextRange(elemStart, elemEnd);

                lexer.advance();
                skipWhitespaceAndComments(lexer);

                // Expect colon
                if (lexer.getTokenType() == WodTokenTypes.COLON) {
                    lexer.advance();
                    skipWhitespaceAndComments(lexer);
                }

                // Expect type name
                String typeName = "";
                TextRange typeRange = null;
                if (lexer.getTokenType() == WodTokenTypes.TYPE_NAME || lexer.getTokenType() == WodTokenTypes.CODE_VALUE) {
                    int tStart = lexer.getTokenStart();
                    int tEnd = lexer.getTokenEnd();
                    typeName = text.subSequence(tStart, tEnd).toString().trim();
                    typeRange = new TextRange(tStart, tEnd);
                    lexer.advance();
                    skipWhitespaceAndComments(lexer);
                }

                // Expect LBRACE '{'
                int totalStart = elemStart;
                int totalEnd = elemEnd;
                WodElement element = new WodElement(elemName, typeName, nameRange, typeRange, null);

                if (lexer.getTokenType() == WodTokenTypes.LBRACE) {
                    lexer.advance();
                    skipWhitespaceAndComments(lexer);

                    // Parse bindings inside '{' ... '}'
                    while (lexer.getTokenType() != null && lexer.getTokenType() != WodTokenTypes.RBRACE) {
                        skipWhitespaceAndComments(lexer);
                        if (lexer.getTokenType() == null || lexer.getTokenType() == WodTokenTypes.RBRACE) {
                            break;
                        }

                        if (lexer.getTokenType() == WodTokenTypes.BINDING_KEY || lexer.getTokenType() == WodTokenTypes.CODE_VALUE) {
                            int kStart = lexer.getTokenStart();
                            int kEnd = lexer.getTokenEnd();
                            String key = text.subSequence(kStart, kEnd).toString().trim();
                            TextRange keyRange = new TextRange(kStart, kEnd);

                            lexer.advance();
                            skipWhitespaceAndComments(lexer);

                            // Expect equals '='
                            if (lexer.getTokenType() == WodTokenTypes.EQUALS) {
                                lexer.advance();
                                skipWhitespaceAndComments(lexer);
                            }

                            // Value token
                            if (lexer.getTokenType() != null && lexer.getTokenType() != WodTokenTypes.SEMICOLON && lexer.getTokenType() != WodTokenTypes.RBRACE) {
                                int vStart = lexer.getTokenStart();
                                int vEnd = lexer.getTokenEnd();
                                IElementType valType = lexer.getTokenType();
                                String val = text.subSequence(vStart, vEnd).toString().trim();
                                TextRange valRange = new TextRange(vStart, vEnd);
                                boolean isLiteral = (valType == WodTokenTypes.STRING_LITERAL);

                                element.addBinding(new WodBinding(key, val, keyRange, valRange, isLiteral));

                                lexer.advance();
                                skipWhitespaceAndComments(lexer);
                            }

                            // Optional semicolon ';'
                            if (lexer.getTokenType() == WodTokenTypes.SEMICOLON) {
                                lexer.advance();
                                skipWhitespaceAndComments(lexer);
                            }
                        } else {
                            // Recover from unexpected token inside block
                            lexer.advance();
                        }
                    }

                    if (lexer.getTokenType() == WodTokenTypes.RBRACE) {
                        totalEnd = lexer.getTokenEnd();
                        lexer.advance();
                    }
                }

                TextRange totalRange = new TextRange(totalStart, totalEnd);
                WodElement finalElement = new WodElement(elemName, typeName, nameRange, typeRange, totalRange);
                for (WodBinding b : element.getBindings().values()) {
                    finalElement.addBinding(b);
                }
                model.addElement(finalElement);
            } else {
                lexer.advance();
            }
        }

        return model;
    }

    private static void skipWhitespaceAndComments(WodLexer lexer) {
        while (lexer.getTokenType() != null) {
            IElementType t = lexer.getTokenType();
            if (t == WodTokenTypes.WHITE_SPACE || t == WodTokenTypes.LINE_COMMENT || t == WodTokenTypes.BLOCK_COMMENT) {
                lexer.advance();
            } else {
                break;
            }
        }
    }
}
