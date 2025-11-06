package com.dynamicduo.proto.parser;

import com.dynamicduo.proto.lexer.*;
import com.dynamicduo.proto.ast.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Recursive-descent parser for the minimal protocol grammar.
 *
 * Grammar:
 *   protocol     → rolesDecl message* EOF ;
 *   rolesDecl    → "roles" ":" IDENT ( "," IDENT )* ;
 *   message      → IDENT ARROW IDENT ":" stmt ;
 *   stmt         → IDENT "=" encExpr | encExpr ;
 *   encExpr      → "Enc" "(" IDENT "," IDENT ")" ;
 */
public class ProtocolParser {

    private final List<Token> tokens = new ArrayList<>();
    private int current = 0;

    public ProtocolParser(Lexer lexer) {
        // Pull all tokens up front so we can "peek" easily.
        Token t;
        do {
            t = lexer.nextToken();
            tokens.add(t);
        } while (t.getType() != TokenType.EOF);
    }

    /** Entry point: parse a ProtocolNode or throw ParseException on error. */
    public ProtocolNode parse() throws ParseException {
        RoleDeclNode roles = rolesDecl();
        ProtocolNode proto = new ProtocolNode(roles);

        while (peek().getType() != TokenType.EOF) {

            MessageSendNode msg = message();
            proto.addMessage(msg);
        }
        return proto;
    }

    // rolesDecl → "roles" ":" IDENT ( "," IDENT )* ;
    private RoleDeclNode rolesDecl() throws ParseException {
        consume(TokenType.ROLES, "Expected 'roles' declaration.");
        consume(TokenType.COLON, "Expected ':' after 'roles'.");

        RoleDeclNode roles = new RoleDeclNode();
        roles.addRole(identifier("Expected role name."));

        while (match(TokenType.COMMA)) {
            roles.addRole(identifier("Expected role name after ','."));
        }
        return roles;
    }

    // message → IDENT ARROW IDENT ":" stmt ;
    private MessageSendNode message() throws ParseException {
        IdentifierNode sender = identifier("Expected sender identifier.");
        consume(TokenType.ARROW, "Expected '->' after sender.");
        IdentifierNode receiver = identifier("Expected receiver identifier.");
        consume(TokenType.COLON, "Expected ':' after receiver.");
        SyntaxNode body = stmt();
        return new MessageSendNode(sender, receiver, body);
    }

    // stmt → IDENT "=" encExpr | encExpr ;
    private SyntaxNode stmt() throws ParseException {
        if (check(TokenType.IDENTIFIER) && checkNext(TokenType.EQUAL)) {
            IdentifierNode target = identifier("Expected variable name.");
            consume(TokenType.EQUAL, "Expected '=' after variable.");
            SyntaxNode value = encExpr();
            return new AssignNode(target, value);
        }
        return encExpr();
    }

    // encExpr → "Enc" "(" IDENT "," IDENT ")" ;
    private SyntaxNode encExpr() throws ParseException {
        consume(TokenType.ENC, "Expected 'Enc' for encryption expression.");
        consume(TokenType.LPAREN, "Expected '(' after 'Enc'.");
        IdentifierNode key = identifier("Expected key identifier.");
        consume(TokenType.COMMA, "Expected ',' between key and message.");
        IdentifierNode msg = identifier("Expected message identifier.");
        consume(TokenType.RPAREN, "Expected ')' after Enc(...).");
        return new EncryptExprNode(key, msg);
    }

    // --- helpers ---

    private IdentifierNode identifier(String err) throws ParseException {
        Token t = consume(TokenType.IDENTIFIER, err);
        return new IdentifierNode(t.getLexeme());
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private Token consume(TokenType type, String message) throws ParseException {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private ParseException error(Token token, String message) {
        return new ParseException(message + " Found: " + token, token.getLine());
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().getType() == type;
    }

    private boolean checkNext(TokenType type) {
        if (isAtEnd()) return false;
        if (tokens.get(current).getType() == TokenType.EOF) return false;
        return tokens.get(current + 1).getType() == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
}
