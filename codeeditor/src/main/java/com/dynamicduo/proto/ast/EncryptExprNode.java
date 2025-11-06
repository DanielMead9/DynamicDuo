package com.dynamicduo.proto.ast;

import java.util.List;

/** Represents: Enc(k, m) */
public class EncryptExprNode extends SyntaxNode {
    private final IdentifierNode key;
    private final IdentifierNode message;

    public EncryptExprNode(IdentifierNode key, IdentifierNode message) {
        this.key = key;
        this.message = message;
    }

    public IdentifierNode getKey()     { return key; }
    public IdentifierNode getMessage() { return message; }

    @Override public String label() {
        return "Enc(key=" + key.getName() + ", msg=" + message.getName() + ")";
    }

    @Override public List<SyntaxNode> children() {
        return List.of(key, message);
    }
}
