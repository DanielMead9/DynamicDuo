// ConcatNode: left || right
package com.dynamicduo.proto.ast;

import java.util.List;

public final class ConcatNode extends SyntaxNode {
    private final SyntaxNode left;
    private final SyntaxNode right;

    public ConcatNode(SyntaxNode left, SyntaxNode right) {
        this.left = left;
        this.right = right;
    }

    public SyntaxNode getLeft()  { return left; }
    public SyntaxNode getRight() { return right; }

    @Override
    public String label() {
        return "Concat";
    }

    @Override
    public List<SyntaxNode> children() {
        return List.of(left, right);
    }
}
