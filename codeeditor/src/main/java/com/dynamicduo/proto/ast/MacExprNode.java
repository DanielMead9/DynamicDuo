// MacExprNode: Mac(k, m)
package com.dynamicduo.proto.ast;

import java.util.List;

public final class MacExprNode extends SyntaxNode {
    private final IdentifierNode key;
    private final SyntaxNode message;

    public MacExprNode(IdentifierNode key, SyntaxNode message) {
        this.key = key;
        this.message = message;
    }

    public IdentifierNode getKey()    { return key; }
    public SyntaxNode     getMessage(){ return message; }

    @Override
    public String label() {
        return "Mac(" + key.getName() + ", ...)";
    }

    @Override
    public List<SyntaxNode> children() {
        return List.of(key, message);
    }
}
