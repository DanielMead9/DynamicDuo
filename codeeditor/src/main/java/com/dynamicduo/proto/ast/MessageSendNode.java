package com.dynamicduo.proto.ast;

import java.util.List;

/** Represents: Alice -> Bob : <stmt> */
public class MessageSendNode extends SyntaxNode {
    private final IdentifierNode sender;
    private final IdentifierNode receiver;
    private final SyntaxNode body; // either AssignNode or EncryptExprNode

    public MessageSendNode(IdentifierNode sender, IdentifierNode receiver, SyntaxNode body) {
        this.sender = sender;
        this.receiver = receiver;
        this.body = body;
    }

    public IdentifierNode getSender()   { return sender; }
    public IdentifierNode getReceiver() { return receiver; }
    public SyntaxNode getBody()         { return body; }

    @Override public String label() {
        return "MessageSend(" + sender.getName() + " -> " + receiver.getName() + ")";
    }

    @Override public List<SyntaxNode> children() {
        return List.of(body);
    }
}
