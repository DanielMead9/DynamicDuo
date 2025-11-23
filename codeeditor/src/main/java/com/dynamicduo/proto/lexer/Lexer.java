/*
*
* Copyright (C) 2025 Owen Forsyth and Daniel Mead
*
* This program is free software: you can redistribute it and/or modify 
* it under the terms of the GNU General Public License as published by 
* the Free Software Foundation, either version 3 of the License, or 
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful, 
* but WITHOUT ANY WARRANTY; without even the implied warranty of 
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
* General Public License for more details.
*
* You should have received a copy of the GNU General Public License 
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*
*/

package com.dynamicduo.proto.lexer;

/**
 * Super small hand-written lexer.
 * - Skips whitespace/newlines.
 * - Recognizes keywords, identifiers, and punctuation.
 * - Produces a stream of Token objects consumed by the parser.
 */
public class Lexer {
    private final String src;
    private final int len;
    private int pos = 0;
    private int line = 1;

    public Lexer(String source) {
        this.src = source;
        this.len = source.length();
    }

    public Token nextToken() {
        skipWhitespace();

        if (isAtEnd()) {
            return new Token(TokenType.EOF, "", line);
        }

        char c = advance();

        // Single-char symbols
        switch (c) {
            case ':':
                return new Token(TokenType.COLON, ":", line);
            case ',':
                return new Token(TokenType.COMMA, ",", line);
            case '=':
                return new Token(TokenType.EQUAL, "=", line);
            case '(':
                return new Token(TokenType.LPAREN, "(", line);
            case ')':
                return new Token(TokenType.RPAREN, ")", line);
            case '-':
                if (match('>'))
                    return new Token(TokenType.ARROW, "->", line);
                // fall-through to identifier handling if lone '-'
                break;
        }

        if (Character.isLetter(c)) {
            return identifier(c);
        }

        // Unknown character: treat as IDENTIFIER lexeme so demo doesn't crash
        return new Token(TokenType.IDENTIFIER, String.valueOf(c), line);
    }

    // --- helpers ---

    private Token identifier(char first) {
        StringBuilder sb = new StringBuilder();
        sb.append(first);

        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            sb.append(advance());
        }

        String text = sb.toString();

        // Recognize keywords
        switch (text) {
            case "roles":
                return new Token(TokenType.ROLES, text, line);
            case "Enc":
                return new Token(TokenType.ENC, text, line);
            case "Dec":
                return new Token(TokenType.DEC, text, line);
            default:
                return new Token(TokenType.IDENTIFIER, text, line);
        }
    }

    private void skipWhitespace() {
        while (!isAtEnd()) {
            char c = peek();
            switch (c) {
                case ' ':
                case '\r':
                case '\t':
                    advance();
                    break;
                case '\n':
                    line++;
                    advance();
                    break;
                default:
                    return;
            }
        }
    }

    private boolean isAtEnd() {
        return pos >= len;
    }

    private char peek() {
        return isAtEnd() ? '\0' : src.charAt(pos);
    }

    private char advance() {
        return src.charAt(pos++);
    }

    private boolean match(char expected) {
        if (isAtEnd())
            return false;
        if (src.charAt(pos) != expected)
            return false;
        pos++;
        return true;
    }
}
