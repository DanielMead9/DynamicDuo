package com.dynamicduo.proto.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Represents: roles: Alice, Bob */
public class RoleDeclNode extends SyntaxNode {
    private final List<IdentifierNode> roles = new ArrayList<>();

    public void addRole(IdentifierNode id) { roles.add(id); }
    public List<IdentifierNode> getRoles() { return roles; }

    @Override public String label() {
        String names = roles.stream().map(IdentifierNode::getName).collect(Collectors.joining(", "));
        return "Roles: [" + names + "]";
    }
}
