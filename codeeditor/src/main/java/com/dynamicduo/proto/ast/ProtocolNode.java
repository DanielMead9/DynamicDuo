package com.dynamicduo.proto.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Root of the AST.
 * Holds the role declarations and the list of message sends.
 */
public class ProtocolNode extends SyntaxNode {
    private final RoleDeclNode roles;
    private final List<MessageSendNode> messages = new ArrayList<>();

    public ProtocolNode(RoleDeclNode roles) {
        this.roles = roles;
    }

    public void addMessage(MessageSendNode msg) { messages.add(msg); }
    public RoleDeclNode getRoles() { return roles; }
    public List<MessageSendNode> getMessages() { return messages; }

    @Override public String label() { return "Protocol"; }

    @Override
    public List<SyntaxNode> children() {
        List<SyntaxNode> c = new ArrayList<>();
        c.add(roles);
        c.addAll(messages);
        return c;
    }
}
