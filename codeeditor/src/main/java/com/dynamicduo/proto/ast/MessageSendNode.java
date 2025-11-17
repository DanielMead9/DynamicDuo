/*
*
* Copyright (C) 2025 Owen Forsyth and Daniel Mead
*
* This program is free software: you can redistribute it and/or modify 
* it under the terms of the GNU General Public License as published by 
* the Free Software Foundation, either version 3 of the License, or 
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful, 
* but WITHOUT ANY WARRANTY; without even the implied warranty of 
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
* General Public License for more details.
*
* You should have received a copy of the GNU General Public License 
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*
*/

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

    public IdentifierNode getSender() {
        return sender;
    }

    public IdentifierNode getReceiver() {
        return receiver;
    }

    public SyntaxNode getBody() {
        return body;
    }

    @Override
    public String label() {
        return "MessageSend(" + sender.getName() + " -> " + receiver.getName() + ")";
    }

    @Override
    public List<SyntaxNode> children() {
        return List.of(body);
    }
}
