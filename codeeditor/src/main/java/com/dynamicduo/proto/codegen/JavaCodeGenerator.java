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

package com.dynamicduo.proto.codegen;

import com.dynamicduo.proto.ast.*;

import java.util.*;

public final class JavaCodeGenerator {

    private JavaCodeGenerator() {}

    public static String fromProtocol(ProtocolNode proto) {
        StringBuilder sb = new StringBuilder();

        // ---------- Imports ----------
        sb.append("import edu.merrimack.cs.crypto.CryptoUtil;\n"); // placeholder util
        sb.append("import org.bouncycastle.jce.provider.BouncyCastleProvider;\n");
        sb.append("import java.security.Security;\n");
        sb.append("\n");

        // ---------- Class header ----------
        sb.append("public class ProtocolDemo {\n\n");
        sb.append("    static {\n");
        sb.append("        // Ensure BouncyCastle is available\n");
        sb.append("        Security.addProvider(new BouncyCastleProvider());\n");
        sb.append("    }\n\n");

        // ---------- Key summary as comments ----------
        if (!proto.getKeyDecls().isEmpty()) {
            sb.append("    // Key declarations from protocol:\n");
            for (KeyDeclNode kd : proto.getKeyDecls()) {
                sb.append("    // ")
                  .append(keyDeclSummary(kd))
                  .append("\n");
            }
            sb.append("\n");
        }

        // ---------- Group messages by sender ----------
        Map<String, List<MessageSendNode>> bySender = new LinkedHashMap<>();
        for (IdentifierNode roleId : proto.getRoles().getRoles()) {
            bySender.put(roleId.getName(), new ArrayList<>());
        }
        for (MessageSendNode msg : proto.getMessages()) {
            String sender = msg.getSender().getName();
            bySender.computeIfAbsent(sender, k -> new ArrayList<>()).add(msg);
        }

        // ---------- One inner class per role ----------
        for (IdentifierNode roleId : proto.getRoles().getRoles()) {
            String roleName = roleId.getName();
            sb.append("    public static class ").append(roleName).append(" {\n");
            sb.append("        // TODO: initialize keys and I/O channels for ").append(roleName).append("\n\n");

            List<MessageSendNode> sends = bySender.getOrDefault(roleName, List.of());
            int step = 1;
            for (MessageSendNode msg : sends) {
                sb.append("        public void step").append(step).append("() throws Exception {\n");
                sb.append("            // ")
                  .append(msg.getSender().getName())
                  .append(" -> ")
                  .append(msg.getReceiver().getName())
                  .append(": ")
                  .append(renderMessageBody(msg))
                  .append("\n");

                // Insert very simple TODO crypto mapping
                appendCryptoStub(sb, msg.getBody());

                sb.append("        }\n\n");
                step++;
            }

            if (sends.isEmpty()) {
                sb.append("        // No outgoing messages for ").append(roleName).append("\n\n");
            }

            sb.append("    }\n\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    private static String keyDeclSummary(KeyDeclNode kd) {
        StringBuilder sb = new StringBuilder();
        switch (kd.getKind()) {
            case SHARED:
                sb.append("shared key ").append(kd.getKeyName()).append(": ");
                break;
            case PUBLIC:
                sb.append("public key ").append(kd.getKeyName()).append(": ");
                break;
            case PRIVATE:
                sb.append("private key ").append(kd.getKeyName()).append(": ");
                break;
        }
        sb.append(String.join(", ", kd.getOwners()));
        return sb.toString();
    }

    private static String renderMessageBody(MessageSendNode msg) {
        // We just use the statement's label to show something meaningful
        return msg.getBody().label();
    }

    private static void appendCryptoStub(StringBuilder sb, SyntaxNode body) {
        // VERY minimal mapping for now; we keep everything as TODO comments
        if (body instanceof AssignNode assign) {
            String varName = assign.getTarget().getName();
            SyntaxNode value = assign.getValue();
            if (value instanceof EncryptExprNode enc) {
                String keyName = enc.getKey().getName();
                sb.append("            // TODO: define plaintext for ")
                  .append(enc.getMessage().label())
                  .append("\n");
                sb.append("            // byte[] ").append(varName)
                  .append(" = CryptoUtil.encryptShared(")
                  .append(keyName)
                  .append(", /* plaintext */);\n");
            } else {
                sb.append("            // TODO: implement assignment: ")
                  .append(varName)
                  .append(" = ")
                  .append(value.label())
                  .append("\n");
            }
        } else if (body instanceof EncryptExprNode enc) {
            sb.append("            // TODO: send encrypted value: Enc(")
              .append(enc.getKey().getName())
              .append(", ")
              .append(enc.getMessage().label())
              .append(")\n");
        } else {
            sb.append("            // TODO: send value: ")
              .append(body.label())
              .append("\n");
        }
    }
}
