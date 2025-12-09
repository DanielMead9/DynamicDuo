// SignExprNode: Sign(sk, m)
package com.dynamicduo.proto.ast;

import java.util.List;

public final class SignExprNode extends SyntaxNode {
    private final IdentifierNode sk;
    private final SyntaxNode message;

    public SignExprNode(IdentifierNode sk, SyntaxNode message) {
        this.sk = sk;
        this.message = message;
    }

    public IdentifierNode getSk()     { return sk; }
    public SyntaxNode     getMessage(){ return message; }

    @Override
    public String label() {
        return "Sign(" + sk.getName() + ", ...)";
    }

    @Override
    public List<SyntaxNode> children() {
        return List.of(sk, message);
    }
}
