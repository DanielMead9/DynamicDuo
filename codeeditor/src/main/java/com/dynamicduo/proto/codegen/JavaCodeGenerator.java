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

/**
 * Generate starter Java code from a ProtocolNode.
 *
 * - One class: ProtocolDemo with a main method.
 * - Imports BouncyCastle + CryptoUtil.
 * - Uses:
 *     * AES-GCM for Enc(shared key, ...)
 *     * ElGamal for Enc(public key, ...)  (if key is declared PUBLIC)
 *     * HMAC-SHA256 for Mac
 *     * SHA-256 for Hash
 *     * SHA256withRSA for Sign / Verify
 *
 * The generated code compiles, but still leaves application-specific
 * initialization (like real key loading) for the student.
 */
public final class JavaCodeGenerator {

    private JavaCodeGenerator() {
        // utility class
    }

    public static String fromProtocol(ProtocolNode proto) {

        StringBuilder sb = new StringBuilder();

        // ------------------------------------------------------------------
        // Imports + class header
        // ------------------------------------------------------------------
        sb.append("import java.security.*;\n");
        sb.append("import javax.crypto.SecretKey;\n");
        sb.append("import com.dynamicduo.proto.codegen.CryptoUtil;\n");
        sb.append("import org.bouncycastle.jce.provider.BouncyCastleProvider;\n\n");

        sb.append("public class ProtocolDemo {\n\n");

        // Static block to register BouncyCastle
        sb.append("    static {\n");
        sb.append("        Security.addProvider(new BouncyCastleProvider());\n");
        sb.append("    }\n\n");

        sb.append("    public static void main(String[] args) throws Exception {\n\n");

        // ------------------------------------------------------------------
        // Roles comment
        // ------------------------------------------------------------------
        sb.append("        // Roles in this protocol:\n");
        List<IdentifierNode> roles = proto.getRoles().getRoles();
        sb.append("        //   ");
        for (int i = 0; i < roles.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(roles.get(i).getName());
        }
        sb.append("\n\n");

        // ------------------------------------------------------------------
        // Key declarations
        // ------------------------------------------------------------------
        Map<String, KeyKind> keyKinds = new LinkedHashMap<>();
        for (KeyDeclNode kd : proto.getKeyDecls()) {
            // Your KeyDeclNode API: getKeyName() returns String, getOwners() -> List<String>
            String keyName = kd.getKeyName();
            keyKinds.put(keyName, kd.getKind());

            sb.append("        // ")
              .append(kd.getKind().name().toLowerCase())
              .append(" key ")
              .append(keyName)
              .append(": ");

            List<String> owners = kd.getOwners();
            for (int i = 0; i < owners.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(owners.get(i));
            }
            sb.append("\n");
        }
        sb.append("\n");

        // Concrete key variables
        for (KeyDeclNode kd : proto.getKeyDecls()) {
            String keyName = kd.getKeyName();
            KeyKind kind   = kd.getKind();

            switch (kind) {
                case SHARED -> {
                    // Shared key: generate a fresh AES key.
                    sb.append("        SecretKey ").append(keyName)
                      .append(" = CryptoUtil.generateAesKey();\n");
                }
                case PUBLIC -> {
                    // Public key: declare and leave for initialization.
                    sb.append("        PublicKey ").append(keyName)
                      .append(" = null; // TODO: initialize or load the public key\n");
                }
                case PRIVATE -> {
                    // Private key: declare and leave for initialization.
                    sb.append("        PrivateKey ").append(keyName)
                      .append(" = null; // TODO: initialize or load the private key\n");
                }
            }
        }
        if (!proto.getKeyDecls().isEmpty()) {
            sb.append("\n");
        }

        // ------------------------------------------------------------------
        // Message symbols (inputs) as byte[]
        //   - used somewhere in expressions
        //   - NOT used as assignment targets (so we avoid c1, c2, c3)
        //   - NOT keys
        // ------------------------------------------------------------------
        Set<String> usedIds      = new LinkedHashSet<>();
        Set<String> assignedIds  = new LinkedHashSet<>();

        for (MessageSendNode msg : proto.getMessages()) {
            collectMessageSymbols(msg.getBody(), usedIds, assignedIds);
        }

        Set<String> messageSymbols = new LinkedHashSet<>(usedIds);
        // Remove anything that is an assignment target (computed variables)
        messageSymbols.removeAll(assignedIds);
        // Remove keys
        messageSymbols.removeAll(keyKinds.keySet());

        if (!messageSymbols.isEmpty()) {
            sb.append("        // Message symbols used in the protocol.\n");
            sb.append("        // Replace the placeholder initializations as needed.\n");
            for (String sym : messageSymbols) {
                sb.append("        byte[] ").append(sym)
                  .append(" = \"").append(sym).append("\".getBytes();\n");
            }
            sb.append("\n");
        }

        // ------------------------------------------------------------------
        // Emit one block per protocol message
        // ------------------------------------------------------------------
        int step = 1;
        for (MessageSendNode msg : proto.getMessages()) {
            String sender   = msg.getSender().getName();
            String receiver = msg.getReceiver().getName();

            sb.append("        // Step ").append(step++).append(": ")
              .append(sender).append(" -> ").append(receiver).append(": ")
              .append(msg.getBody().label())
              .append("\n");

            if (msg.getBody() instanceof AssignNode assign) {
                String varName = assign.getTarget().getName();
                String exprCode = generateExpr(assign.getValue(), keyKinds);
                sb.append("        byte[] ").append(varName)
                  .append(" = ").append(exprCode).append(";\n\n");
            } else {
                String exprCode = generateExpr(msg.getBody(), keyKinds);
                // If it's not an assignment, just evaluate the expression (side-effect-less)
                sb.append("        ").append(exprCode).append(";\n\n");
            }
        }

        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    // ----------------------------------------------------------------------
    // Collect identifiers for message symbols:
    //   - usedIds: identifiers that appear as *uses* (Expr)
    //   - assignedIds: identifiers that appear as LHS of assignments
    // ----------------------------------------------------------------------
    private static void collectMessageSymbols(SyntaxNode node,
                                              Set<String> usedIds,
                                              Set<String> assignedIds) {
        if (node instanceof AssignNode a) {
            // LHS is an "output" variable, we don't want to treat it as input/plaintext
            assignedIds.add(a.getTarget().getName());
            collectMessageSymbols(a.getValue(), usedIds, assignedIds);
        } else if (node instanceof IdentifierNode id) {
            usedIds.add(id.getName());
        } else if (node instanceof EncryptExprNode enc) {
            usedIds.add(enc.getKey().getName());
            collectMessageSymbols(enc.getMessage(), usedIds, assignedIds);
        } else if (node instanceof ConcatNode cat) {
            collectMessageSymbols(cat.getLeft(), usedIds, assignedIds);
            collectMessageSymbols(cat.getRight(), usedIds, assignedIds);
        } else if (node instanceof MacExprNode mac) {
            usedIds.add(mac.getKey().getName());
            collectMessageSymbols(mac.getMessage(), usedIds, assignedIds);
        } else if (node instanceof HashExprNode h) {
            collectMessageSymbols(h.getInner(), usedIds, assignedIds);
        } else if (node instanceof SignExprNode s) {
            usedIds.add(s.getSigningKey().getName());
            collectMessageSymbols(s.getMessage(), usedIds, assignedIds);
        } else if (node instanceof VerifyExprNode v) {
            usedIds.add(v.getPublicKey().getName());
            collectMessageSymbols(v.getMessage(), usedIds, assignedIds);
            collectMessageSymbols(v.getSignature(), usedIds, assignedIds);
        }
        // No default; if new node types appear, we can extend this.
    }

    // ----------------------------------------------------------------------
    // Turn an expression AST into a Java expression (as a String).
    // We treat everything as byte[] except Verify (boolean).
    // ----------------------------------------------------------------------
    private static String generateExpr(SyntaxNode node, Map<String, KeyKind> keyKinds) {

        // Bare identifier -> variable name (assumed byte[])
        if (node instanceof IdentifierNode id) {
            return id.getName();
        }

        // Concatenation: left || right  -->  CryptoUtil.concat(left, right)
        if (node instanceof ConcatNode cat) {
            String left  = generateExpr(cat.getLeft(), keyKinds);
            String right = generateExpr(cat.getRight(), keyKinds);
            return "CryptoUtil.concat(" + left + ", " + right + ")";
        }

        // Enc(k, m): choose AES-GCM for shared keys, ElGamal for public keys
        if (node instanceof EncryptExprNode enc) {
            String keyName = enc.getKey().getName();
            String msgExpr = generateExpr(enc.getMessage(), keyKinds);
            KeyKind kind   = keyKinds.getOrDefault(keyName, KeyKind.SHARED);

            if (kind == KeyKind.PUBLIC) {
                // Public-key encryption (ElGamal)
                return "CryptoUtil.elGamalEncrypt(" + keyName + ", " + msgExpr + ")";
            } else {
                // Shared-key encryption (AES-GCM)
                return "CryptoUtil.encryptAESGCM(" + keyName + ", " + msgExpr + ")";
            }
        }

        // Mac(K, m) -> HMAC-SHA256 with K
        if (node instanceof MacExprNode mac) {
            String keyName = mac.getKey().getName();
            String msgExpr = generateExpr(mac.getMessage(), keyKinds);
            return "CryptoUtil.hmacSha256(" + keyName + ".getEncoded(), " + msgExpr + ")";
        }

        // Hash(m) -> SHA-256
        if (node instanceof HashExprNode h) {
            String inner = generateExpr(h.getInner(), keyKinds);
            return "CryptoUtil.sha256(" + inner + ")";
        }

        // Sign(sk, m) -> RSA SHA256 signature
        if (node instanceof SignExprNode s) {
            String skName  = s.getSigningKey().getName();
            String msgExpr = generateExpr(s.getMessage(), keyKinds);
            return "CryptoUtil.sign(" + skName + ", " + msgExpr + ")";
        }

        // Verify(pk, m, sig) -> boolean
        if (node instanceof VerifyExprNode v) {
            String pkName   = v.getPublicKey().getName();
            String msgExpr  = generateExpr(v.getMessage(), keyKinds);
            String sigExpr  = generateExpr(v.getSignature(), keyKinds);
            // Note: this is a boolean expression; caller must handle it.
            return "CryptoUtil.verify(" + pkName + ", " + msgExpr + ", " + sigExpr + ")";
        }

        // Fallback: encode the label string as bytes
        return ("\"" + node.label() + "\".getBytes()");
    }
}
