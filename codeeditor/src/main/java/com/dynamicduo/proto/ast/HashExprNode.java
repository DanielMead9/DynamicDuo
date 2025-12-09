// HashExprNode: H(m)
package com.dynamicduo.proto.ast;

import java.util.List;

public final class HashExprNode extends SyntaxNode {
    private final SyntaxNode inner;

    public HashExprNode(SyntaxNode inner) {
        this.inner = inner;
    }

    public SyntaxNode getInner() { return inner; }

    @Override
    public String label() {
        return "H(...)";
    }

    @Override
    public List<SyntaxNode> children() {
        return List.of(inner);
    }
}
