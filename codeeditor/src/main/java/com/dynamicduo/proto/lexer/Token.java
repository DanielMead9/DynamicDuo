package com.dynamicduo.proto.lexer;

/**
 * A single token: (type, lexeme, line).
 * - type   : category (IDENTIFIER, ARROW, ENC, ...)
 * - lexeme : the exact text from the source that matched (e.g., "Alice")
 * - line   : line number (nice for error messages)
 */
public class Token {
    private final TokenType type;
    private final String lexeme;
    private final int line;

    public Token(TokenType type, String lexeme, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
    }

    public TokenType getType() { return type; }
    public String getLexeme() { return lexeme; }
    public int getLine() { return line; }

    @Override
    public String toString() {
        return String.format("%-10s '%s' (line %d)", type, lexeme, line);
    }
}
