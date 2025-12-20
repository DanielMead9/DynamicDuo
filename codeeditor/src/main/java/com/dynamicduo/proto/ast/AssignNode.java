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
 * AssignNode: target = value
 */
public class AssignNode extends SyntaxNode {
    private final IdentifierNode target;
    private final SyntaxNode value; // usually EncryptExprNode

    public AssignNode(IdentifierNode target, SyntaxNode value) {
        this.target = target;
        this.value = value;
    }

    public IdentifierNode getTarget() {
        return target;
    }

    public SyntaxNode getValue() {
        return value;
    }

    @Override
    public String label() {
        return "Assign(" + target.getName() + " = ...)";
    }

    @Override
    public List<SyntaxNode> children() {
        return List.of(value);
    }
}
