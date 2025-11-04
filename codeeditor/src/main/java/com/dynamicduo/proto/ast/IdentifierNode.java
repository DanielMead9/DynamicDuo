package com.dynamicduo.proto.ast;

/** Simple identifier wrapper (e.g., Alice, Bob, k, m, c). */
public class IdentifierNode extends SyntaxNode {
    private final String name;

    public IdentifierNode(String name) { this.name = name; }
    public String getName() { return name; }

    @Override public String label() { return "Id(" + name + ")"; }
}
