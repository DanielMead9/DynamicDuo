package com.dynamicduo.proto.lexer;

/**
 * Token categories produced by the lexer.
 * Keep this tiny and specific to today's grammar.
 */
public enum TokenType {
    // Keywords
    ROLES,
    ENC,
    DEC, // reserved for later, not used today

    // Symbols
    ARROW,   // ->
    COLON,   // :
    COMMA,   // ,
    EQUAL,   // =
    LPAREN,  // (
    RPAREN,  // )
    EOF,     // end of input

    // Identifiers: names like Alice, Bob, k, m, c...
    IDENTIFIER
}
