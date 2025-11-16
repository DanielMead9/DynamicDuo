package com.dynamicduo.proto.render;

import com.dynamicduo.proto.ast.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DotBuilder (simple sequence diagram)
 *
 * - Roles are boxes across the top.
 * - Each message is on its own row (time step) with a horizontal arrow
 *   from sender to receiver.
 * - We use invisible "points" to align columns and rows; no lifelines yet.
 */
public final class DotBuilder {
    private DotBuilder() {}

    /** Build DOT text and write to the given path; returns the absolute .dot path. */
    public static Path buildAndWrite(ProtocolNode root, String dotFilePath) throws IOException {
        String dot = buildDot(root);
        Path path = Path.of(dotFilePath).toAbsolutePath();
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        Files.writeString(path, dot);
        System.out.println("[DotBuilder] Wrote DOT file: " + path);
        return path;
    }

    /** AST → DOT string. */
    private static String buildDot(ProtocolNode root) {
        StringBuilder sb = new StringBuilder();

        // Roles in declaration order
        List<String> roles = root.getRoles().getRoles().stream()
                .map(IdentifierNode::getName)
                .collect(Collectors.toList());

        // Messages in order define "time steps"
        List<MessageSendNode> messages = root.getMessages();
        int rows = messages.size(); // one row per message

        sb.append("digraph Protocol {\n");
        sb.append("  rankdir=TB;\n"); // time flows top -> bottom
        sb.append("  splines=polyline;\n");
        sb.append("  node [fontsize=12];\n");
        sb.append("  graph [nodesep=0.8, ranksep=0.8];\n\n");

        // 1) Header nodes (roles across the top)
        for (String role : roles) {
            sb.append("  ").append(headerId(role))
              .append(" [label=\"").append(escape(role))
              .append("\", shape=box, style=rounded];\n");
        }
        sb.append("\n");

        // 2) Invisible points for each (role, row)
        for (int r = 0; r < rows; r++) {
            for (String role : roles) {
                sb.append("  ").append(pointId(role, r))
                  .append(" [label=\"\", shape=point, width=0.02, height=0.02];\n");
            }
        }
        sb.append("\n");

        // 3) Rank constraints: headers on same row; each message row aligned
        sb.append("  { rank=same; ");
        for (String role : roles) sb.append(headerId(role)).append(" ");
        sb.append("}\n");

        for (int r = 0; r < rows; r++) {
            sb.append("  { rank=same; ");
            for (String role : roles) sb.append(pointId(role, r)).append(" ");
            sb.append("}\n");
        }
        sb.append("\n");

        // 4) Invisible links to keep columns aligned (header -> first row point)
        for (String role : roles) {
            sb.append("  ").append(headerId(role))
              .append(" -> ").append(pointId(role, 0))
              .append(" [style=invis];\n");
        }
        sb.append("\n");

        // 5) Actual messages: horizontal arrows on each row
        for (int i = 0; i < messages.size(); i++) {
            MessageSendNode msg = messages.get(i);
            String fromRole = msg.getSender().getName();
            String toRole   = msg.getReceiver().getName();
            String label    = labelFor(msg.getBody());

            sb.append("  ").append(pointId(fromRole, i))
              .append(" -> ").append(pointId(toRole, i))
              .append(" [label=\"").append(escape(label))
              .append("\", constraint=false];\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    // ---- helpers ----

    private static String labelFor(SyntaxNode body) {
        if (body instanceof AssignNode a) {
            return a.getTarget().getName() + " = " + labelFor(a.getValue());
        }
        if (body instanceof EncryptExprNode e) {
            return "Enc(" + e.getKey().getName() + ", " + e.getMessage().getName() + ")";
        }
        return body.label();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String headerId(String role) {
        return "hdr_" + sanitize(role);
    }

    private static String pointId(String role, int row) {
        return "pt_" + sanitize(role) + "_" + row;
    }

    private static String sanitize(String name) {
        String base = name.replaceAll("[^A-Za-z0-9_]", "_");
        return base.isEmpty() ? "Role" : base;
    }
}

