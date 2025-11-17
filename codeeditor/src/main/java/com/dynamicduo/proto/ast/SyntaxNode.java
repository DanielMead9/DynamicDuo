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
 * Base class for all AST nodes.
 * Provides a tiny "pretty()" printer to show a tree on the console.
 */
public abstract class SyntaxNode {
    /** Give a short label for this node in the tree (e.g., "MessageSend"). */
    public abstract String label();

    /** Child nodes for tree printing (override to expose structure). */
    public List<SyntaxNode> children() {
        return List.of();
    }

    /** Pretty-print as an indented tree (great for demos). */
    public String pretty() {
        StringBuilder sb = new StringBuilder();
        buildPretty(sb, "", true);
        return sb.toString();
    }

    private void buildPretty(StringBuilder sb, String indent, boolean last) {
        sb.append(indent).append(last ? "└─ " : "├─ ").append(label()).append('\n');
        var kids = children();
        for (int i = 0; i < kids.size(); i++) {
            boolean isLast = (i == kids.size() - 1);
            kids.get(i).buildPretty(sb, indent + (last ? "   " : "│  "), isLast);
        }
    }
}
