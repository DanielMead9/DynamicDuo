package com.dynamicduo.proto.ast;

import java.util.List;

/** Represents: c = Enc(k, m)  (target = value) */
public class AssignNode extends SyntaxNode {
    private final IdentifierNode target;
    private final SyntaxNode value; // usually EncryptExprNode

    public AssignNode(IdentifierNode target, SyntaxNode value) {
        this.target = target;
        this.value = value;
    }

    public IdentifierNode getTarget() { return target; }
    public SyntaxNode getValue()      { return value; }

    @Override public String label() {
        return "Assign(" + target.getName() + " = ...)";
    }

    @Override public List<SyntaxNode> children() {
        return List.of(value);
    }
}

