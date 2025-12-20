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


/**
 * VerifyExprNode: verify(pk, message, signature)
 */
public final class VerifyExprNode extends SyntaxNode {
    private final IdentifierNode pk;
    private final SyntaxNode message;
    private final SyntaxNode signature;

    public VerifyExprNode(IdentifierNode pk, SyntaxNode message, SyntaxNode signature) {
        this.pk = pk;
        this.message = message;
        this.signature = signature;
    }

    public IdentifierNode getPublicKey()     { return pk; }
    public SyntaxNode     getMessage(){ return message; }
    public SyntaxNode     getSignature(){ return signature; }

    @Override
    public String label() {
        return "Verify(" + pk.getName() + ", " + message.label() + ", " + signature.label() + ")";
    }

    @Override
    public List<SyntaxNode> children() {
        return List.of(pk, message, signature);
    }
}
