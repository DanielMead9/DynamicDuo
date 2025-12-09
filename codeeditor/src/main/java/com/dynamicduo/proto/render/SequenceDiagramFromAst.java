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

package com.dynamicduo.proto.render;

import com.dynamicduo.proto.ast.*;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SequenceDiagramFromAst
 *
 * Bridges our Protocol AST into the existing SVG.java two-party sequence
 * diagram
 * 
 *
 * Assumptions:
 * - The protocol declares exactly TWO roles.
 * - Each message is represented as a MessageSendNode with
 * sender, receiver, and a body expression.
 *
 * Output:
 * - An SVG file with Alice/Bob style vertical lifelines and
 * horizontal arrows for each message.
 */
public final class SequenceDiagramFromAst {

    private SequenceDiagramFromAst() {
    }

    /**
     * Render a two-party sequence diagram using your partner's SVG.java.
     *
     * @param proto  Root AST (ProtocolNode)
     * @param outSvg Output SVG path, e.g. "pretty_protocol.svg"
     */
    public static String renderTwoParty(ProtocolNode proto) throws Exception {
        // 1) Extract roles in declaration order
        List<String> roles = proto.getRoles().getRoles().stream()
                .map(IdentifierNode::getName)
                .collect(Collectors.toList());

        if (roles.size() != 2) {
            throw new IllegalArgumentException(
                    "Two-party renderer requires exactly 2 roles, found: " + roles.size());
        }

        String p1 = roles.get(0);
        String p2 = roles.get(1);

        // 2) Messages define the rows
        List<MessageSendNode> msgs = proto.getMessages();
        int numNodes = msgs.size() + 1; // lifeline points = messages + 1

        String[] messages = new String[msgs.size()];
        String[] passer = new String[msgs.size()];

        for (int i = 0; i < msgs.size(); i++) {
            MessageSendNode m = msgs.get(i);
            messages[i] = labelFor(m.getBody()); // text on the arrow
            passer[i] = m.getSender().getName(); // who sends this message
        }

        // 3) Build the graph using your partner's SVG helper
        SVG svgBuilder = new SVG(numNodes, p1, p2, messages, passer);

        // 4) Render that graph to an SVG file using graphviz-java
        String outSvg = Graphviz.fromGraph(svgBuilder.getGraph())
                .render(Format.SVG)
                .toString();

        // System.out.println("[SequenceDiagramFromAst] Wrote SVG: "
        // + new File(outSvg).getAbsolutePath());
        return outSvg;
    }

    // === label helper: reuse our encryption labeling logic ===

    private static String labelFor(SyntaxNode body) {
        if (body instanceof AssignNode a) {
            return a.getTarget().getName() + " = " + labelFor(a.getValue());
        }
        if (body instanceof EncryptExprNode e) {
            return "Enc(" + e.getKey().getName() + ", " + e.getMessage().label() + ")";
        }

        return body.label(); // generic fallback
    }
}
