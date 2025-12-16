package com.dynamicduo.proto.ast;

import java.util.List;

public final class KeyDeclNode extends SyntaxNode {
    private final KeyKind kind;
    private final String keyName;
    private final List<String> owners; // e.g. ["Alice", "Bob"] or ["Alice"]

    public KeyDeclNode(KeyKind kind, String keyName, List<String> owners) {
        this.kind = kind;
        this.keyName = keyName;
        this.owners = owners;
    }

    public KeyKind getKind() { return kind; }

    public String getKeyName() { return keyName; }

    public List<String> getOwners() { return owners; }

    @Override
    public String label() { return keyName; }
}
