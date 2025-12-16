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

public final class MacExprNode extends SyntaxNode {
    private final IdentifierNode key;
    private final SyntaxNode message;

    public MacExprNode(IdentifierNode key, SyntaxNode message) {
        this.key = key;
        this.message = message;
    }

    public IdentifierNode getKey()    { return key; }
    public SyntaxNode     getMessage(){ return message; }

    @Override
    public String label() {
        return "Mac(" + key.getName() + ", " + message.label() + ")";
    }

    @Override
    public List<SyntaxNode> children() {
        return List.of(key, message);
    }
}
