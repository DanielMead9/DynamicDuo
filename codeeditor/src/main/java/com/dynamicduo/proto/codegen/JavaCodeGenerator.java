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
 * Generate working Java code from a ProtocolNode.
 *
 * Output:
 *  - Single file: ProtocolDemo.java
 *  - TCP sockets (one role listens; the other connects)
 *  - JSON messages via MerrimackUtil JSONObject + JsonIO.readObject(String)
 *  - Payloads are Base64 encoded byte[]
 *
 * Crypto selection:
 *  - Enc(shared key, ...) -> AES-GCM
 *  - Enc(public key, ...) -> ElGamal
 *  - Mac(shared key, ...) -> HMAC-SHA256
 *  - Hash(...) -> SHA-256
 *  - Sign(private key, ...) / Verify(public key, ...) -> RSA signatures
 *
 * Key initialization:
 *  - shared keys are generated (AES)
 *  - pk sk  pairs are auto-generated as RSA keypairs for signing
 *  - any PUBLIC key used inside Enc(...) is treated as ElGamal encryption key
 */
public final class JavaCodeGenerator {

    private JavaCodeGenerator() {}

    // ----------------- Helper: key kinds and uses -----------------
    private enum KeyUse {
        PK_ENCRYPT,   // used as Enc(pkX, ...)
        SIGN,         // used as Sign(skX, ...)
        VERIFY        // used as Verify(pkX, ...)
    }   

    private enum JType { BYTES, BOOL }

    private static JType typeOfRhs(SyntaxNode rhs) {
        return (rhs instanceof VerifyExprNode) ? JType.BOOL : JType.BYTES;
    }

    private static JType typeOfExpr(SyntaxNode expr) {
        return (expr instanceof VerifyExprNode) ? JType.BOOL : JType.BYTES;
    }



    public static String fromProtocol(ProtocolNode proto) {

        // 1) Gather roles
        List<IdentifierNode> roles = proto.getRoles().getRoles();
        List<String> roleNames = new ArrayList<>();
        for (IdentifierNode r : roles) roleNames.add(r.getName());

        // 2) Gather key kinds from declarations
        Map<String, KeyKind> keyKinds = new LinkedHashMap<>();
        List<KeyDeclNode> keyDecls = proto.getKeyDecls();
        for (KeyDeclNode kd : keyDecls) {
            keyKinds.put(kd.getKeyName(), kd.getKind());
        }

        // 3) Prepare to track key uses
                Map<String, EnumSet<KeyUse>> uses = new LinkedHashMap<>();
        for (String k : keyKinds.keySet()) {
            uses.put(k, EnumSet.noneOf(KeyUse.class));
        }
        for (MessageSendNode msg : proto.getMessages()) {
            markKeyUses(msg.getBody(), uses, keyKinds);
        }

        // 4) Determine how keys are used (signing vs encryption)
        KeyUsage usage = analyzeKeyUsage(proto, keyKinds);


        StringBuilder sb = new StringBuilder();

        // ------------------------------------------------------------------
        // Imports + class header
        // ------------------------------------------------------------------
        sb.append("import java.io.*;\n");
        sb.append("import java.net.*;\n");
        sb.append("import java.security.*;\n");
        sb.append("import javax.crypto.SecretKey;\n");
        sb.append("import java.util.*;\n");
        sb.append("import java.util.Base64;\n\n");

        sb.append("import org.bouncycastle.jce.provider.BouncyCastleProvider;\n");
        sb.append("import com.dynamicduo.proto.codegen.CryptoUtil;\n\n");

        sb.append("import merrimackutil.json.JsonIO;\n");
        sb.append("import merrimackutil.json.InvalidJSONException;\n");
        sb.append("import merrimackutil.json.types.JSONObject;\n\n");

        sb.append("public class ProtocolDemo {\n\n");

        sb.append("    static {\n");
        sb.append("        Security.addProvider(new BouncyCastleProvider());\n");
        sb.append("    }\n\n");

        // ------------------------------------------------------------------
        // Main + usage
        // ------------------------------------------------------------------
        sb.append("    public static void main(String[] args) throws Exception {\n");
        sb.append("        // Run in TWO terminals.\n");
        sb.append("        // Example (Bob listens on 5000):\n");
        sb.append("        //   java ProtocolDemo Bob 5000\n");
        sb.append("        // Example (Alice connects to Bob):\n");
        sb.append("        //   java ProtocolDemo Alice localhost 5000\n\n");

        sb.append("        if (args.length < 2) {\n");
        sb.append("            System.out.println(\"Usage:\\n\" +\n");
        sb.append("                \"  Listener: java ProtocolDemo <Role> <port>\\n\" +\n");
        sb.append("                \"  Connector: java ProtocolDemo <Role> <host> <port>\\n\");\n");
        sb.append("            return;\n");
        sb.append("        }\n\n");

        sb.append("        String me = args[0];\n");
        sb.append("        boolean iListen = (args.length == 2);\n");
        sb.append("        String host = iListen ? null : args[1];\n");
        sb.append("        int port = Integer.parseInt(iListen ? args[1] : args[2]);\n\n");

        // Decide who listens: simplest rule = the receiver of step 1 listens
        String listenerRole = proto.getMessages().isEmpty()
                ? (roleNames.isEmpty() ? "Bob" : roleNames.get(0))
                : proto.getMessages().get(0).getReceiver().getName();

        sb.append("        // TCP setup: one role listens because TCP requires an accept().\n");
        sb.append("        // Listener role chosen from the first message receiver: ").append(listenerRole).append("\n");
        sb.append("        String listener = \"").append(listenerRole).append("\";\n");
        sb.append("        if (me.equals(listener)) {\n");
        sb.append("            iListen = true;\n");
        sb.append("        }\n\n");

        sb.append("        Socket socket;\n");
        sb.append("        if (iListen) {\n");
        sb.append("            try (ServerSocket server = new ServerSocket(port)) {\n");
        sb.append("                System.out.println(me + \" listening on port \" + port + \"...\");\n");
        sb.append("                socket = server.accept();\n");
        sb.append("                System.out.println(me + \" accepted connection from \" + socket.getRemoteSocketAddress());\n");
        sb.append("            }\n");
        sb.append("        } else {\n");
        sb.append("            socket = new Socket(host, port);\n");
        sb.append("            System.out.println(me + \" connected to \" + host + \":\" + port);\n");
        sb.append("        }\n\n");

        sb.append("        try (\n");
        sb.append("            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));\n");
        sb.append("            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)\n");
        sb.append("        ) {\n\n");

        // ------------------------------------------------------------------
        // Print roles + key decl comments
        // ------------------------------------------------------------------
        sb.append("            // Roles in this protocol:\n");
        sb.append("            //   ").append(String.join(", ", roleNames)).append("\n\n");

        for (KeyDeclNode kd : keyDecls) {
            sb.append("            // ")
              .append(kd.getKind().name().toLowerCase())
              .append(" key ")
              .append(kd.getKeyName())
              .append(": ");
            List<String> owners = kd.getOwners();
            for (int i = 0; i < owners.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(owners.get(i));
            }
            sb.append("\n");
        }
        sb.append("\n");

        // ------------------------------------------------------------------
        // Key material initialization
        // ------------------------------------------------------------------
        sb.append("            // --- Key initialization (auto-generated for demo) ---\n");

        // Shared keys -> AES keys
        for (KeyDeclNode kd : keyDecls) {
            if (kd.getKind() == KeyKind.SHARED) {
                sb.append("            SecretKey ").append(kd.getKeyName())
                  .append(" = CryptoUtil.generateAesKey();\n");
            }
        }

        // RSA keypairs for pk*/sk* used for signing/verifying
        // We pair by suffix: pkA <-> skA
        Set<String> rsaPairsDone = new HashSet<>();
        for (String pk : usage.rsaPublicKeys) {
            String suffix = pk.startsWith("pk") ? pk.substring(2) : pk;
            String sk = "sk" + suffix;
            if (usage.rsaPrivateKeys.contains(sk) && !rsaPairsDone.contains(suffix)) {
                rsaPairsDone.add(suffix);
                sb.append("            KeyPair rsa").append(suffix).append(" = CryptoUtil.generateRsaKeyPair(2048);\n");
                sb.append("            PublicKey ").append(pk).append(" = rsa").append(suffix).append(".getPublic();\n");
                sb.append("            PrivateKey ").append(sk).append(" = rsa").append(suffix).append(".getPrivate();\n");
            }
        }

        // ElGamal keypairs for PUBLIC keys used for encryption
        // Pair pkX <-> skX if both exist, else still generate pair and keep pk.
        Set<String> elgPairsDone = new HashSet<>();
        for (String pk : usage.elgamalPublicKeys) {
            String suffix = pk.startsWith("pk") ? pk.substring(2) : pk;
            if (elgPairsDone.contains(suffix)) continue;
            elgPairsDone.add(suffix);

            String sk = "sk" + suffix;
            sb.append("            KeyPair elg").append(suffix).append(" = CryptoUtil.generateElGamalKeyPair(2048);\n");
            sb.append("            PublicKey ").append(pk).append(" = elg").append(suffix).append(".getPublic();\n");
            if (keyKinds.containsKey(sk) && keyKinds.get(sk) == KeyKind.PRIVATE) {
                sb.append("            PrivateKey ").append(sk).append(" = elg").append(suffix).append(".getPrivate();\n");
            }
        }

        sb.append("\n");

        
        // Message symbols used as plaintext inputs (byte[])
        Set<String> usedIds = new LinkedHashSet<>();
        Set<String> assignedIds = new LinkedHashSet<>();
        for (MessageSendNode msg : proto.getMessages()) {
            collectMessageSymbols(msg.getBody(), usedIds, assignedIds);
        }

        Set<String> messageSymbols = new LinkedHashSet<>(usedIds);
        messageSymbols.removeAll(assignedIds);       // don't create c1, c2, etc as plaintext
        messageSymbols.removeAll(keyKinds.keySet()); // don't create keys as byte[]
        // also remove role names if they appear
        messageSymbols.removeAll(roleNames);

        if (!messageSymbols.isEmpty()) {
            sb.append("            // --- Plaintext symbols (demo bytes) ---\n");
            for (String sym : messageSymbols) {
                sb.append("            byte[] ").append(sym)
                  .append(" = \"").append(sym).append("\".getBytes();\n");
            }
            sb.append("\n");
        }

      
        // Predeclare all assignment targets so later steps can reuse them
        Map<String, JType> varTypes = new LinkedHashMap<>();
        for (MessageSendNode m : proto.getMessages()) {
            if (m.getBody() instanceof AssignNode a) {
                varTypes.put(a.getTarget().getName(), typeOfRhs(a.getValue()));
            }
        }

        if (!varTypes.isEmpty()) {
            sb.append("            // --- Protocol variables (predeclared) ---\n");
            for (Map.Entry<String, JType> e : varTypes.entrySet()) {
                if (e.getValue() == JType.BOOL) {
                    sb.append("            boolean ").append(e.getKey()).append(" = false;\n");
                } else {
                    sb.append("            byte[] ").append(e.getKey()).append(" = null;\n");
                }
            }
            sb.append("\n");
        }

        // ------------------------------------------------------------------
        // Protocol execution: sender computes + sends JSON;
        // receiver reads JSON and assigns into predeclared variables.
        // BYTES use Base64 payload, BOOL use payloadBool.
        // ------------------------------------------------------------------
        int step = 1;
        for (MessageSendNode msg : proto.getMessages()) {
                String sender = msg.getSender().getName();
                String receiver = msg.getReceiver().getName();

            sb.append("            // Step ").append(step).append(": ")
            .append(sender).append(" -> ").append(receiver).append("\n");

            if (msg.getBody() instanceof AssignNode assign) {
                String varName = assign.getTarget().getName();
                JType rhsType = typeOfRhs(assign.getValue());
                String exprCode = generateExpr(assign.getValue(), keyKinds);

                // Sender side
                sb.append("            if (me.equals(\"").append(sender).append("\")) {\n");
                sb.append("                ").append(varName).append(" = ").append(exprCode).append(";\n");
                sb.append("                JSONObject msg").append(step).append(" = new JSONObject();\n");
                sb.append("                msg").append(step).append(".put(\"from\", \"").append(sender).append("\");\n");
                sb.append("                msg").append(step).append(".put(\"to\", \"").append(receiver).append("\");\n");
                sb.append("                msg").append(step).append(".put(\"label\", \"").append(varName).append("\");\n");

                if (rhsType == JType.BOOL) {
                    sb.append("                msg").append(step)
                    .append(".put(\"payloadBool\", Boolean.valueOf(")
                    .append(varName).append("));\n");
                } else {
                    sb.append("                msg").append(step).append(".put(\"payload\", Base64.getEncoder().encodeToString(")
                    .append(varName).append("));\n");
                }

                sb.append("                String json").append(step).append(" = msg").append(step).append(".toJSON();\n");
                sb.append("                out.println(json").append(step).append(");\n");
                sb.append("                System.out.println(me + \" SENT: \" + json").append(step).append(");\n");

                // Receiver side
                sb.append("            } else if (me.equals(\"").append(receiver).append("\")) {\n");
                sb.append("                String line = in.readLine();\n");
                sb.append("                if (line == null) throw new EOFException(\"Connection closed during step ").append(step).append("\");\n");
                sb.append("                JSONObject obj;\n");
                sb.append("                try {\n");
                sb.append("                    obj = JsonIO.readObject(line);\n");
                sb.append("                } catch (InvalidJSONException ex) {\n");
                sb.append("                    throw new IllegalArgumentException(\"Invalid JSON received: \" + line, ex);\n");
                sb.append("                }\n");

                if (rhsType == JType.BOOL) {
                    sb.append("                ").append(varName).append(" = obj.getBoolean(\"payloadBool\");\n");
                } else {
                    sb.append("                String payloadB64 = obj.getString(\"payload\");\n");
                    sb.append("                ").append(varName).append(" = Base64.getDecoder().decode(payloadB64);\n");
                }

                sb.append("                System.out.println(me + \" RECV: \" + line);\n");
                sb.append("            }\n\n");

            } else {
                // Non-assignment message: compute and send as either BYTES or BOOL
                JType exprType = typeOfExpr(msg.getBody());
                String exprCode = generateExpr(msg.getBody(), keyKinds);

                sb.append("            if (me.equals(\"").append(sender).append("\")) {\n");
                sb.append("                JSONObject m = new JSONObject();\n");
                sb.append("                m.put(\"from\", \"").append(sender).append("\");\n");
                sb.append("                m.put(\"to\", \"").append(receiver).append("\");\n");
                sb.append("                m.put(\"label\", \"expr\");\n");

                if (exprType == JType.BOOL) {
                    sb.append("                boolean payloadBool = ").append(exprCode).append(";\n");
                    sb.append("                m.put(\"payloadBool\", payloadBool);\n");
                } else {
                    sb.append("                byte[] payload = ").append(exprCode).append(";\n");
                    sb.append("                m.put(\"payload\", Base64.getEncoder().encodeToString(payload));\n");
                }

                sb.append("                out.println(m.toJSON());\n");
                sb.append("            } else if (me.equals(\"").append(receiver).append("\")) {\n");
                sb.append("                String line = in.readLine();\n");
                sb.append("                if (line == null) throw new EOFException(\"Connection closed\");\n");
                sb.append("                System.out.println(me + \" RECV: \" + line);\n");
                sb.append("            }\n\n");
            }
            step++;
        }

        sb.append("        }\n"); // end try-with-resources
        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    // ----------------- Helper: key usage analysis -----------------

    private static final class KeyUsage {
        final Set<String> rsaPublicKeys = new LinkedHashSet<>();
        final Set<String> rsaPrivateKeys = new LinkedHashSet<>();
        final Set<String> elgamalPublicKeys = new LinkedHashSet<>();
    }

    private static KeyUsage analyzeKeyUsage(ProtocolNode proto, Map<String, KeyKind> keyKinds) {
        KeyUsage usage = new KeyUsage();
        for (MessageSendNode msg : proto.getMessages()) {
            collectKeyUsage(msg.getBody(), usage, keyKinds);
        }
        return usage;
    }


    private static void collectKeyUsage(SyntaxNode node, KeyUsage usage, Map<String, KeyKind> keyKinds) {
        if (node instanceof AssignNode a) {
            collectKeyUsage(a.getValue(), usage, keyKinds);

        } else if (node instanceof EncryptExprNode enc) {
            String k = enc.getKey().getName();

            // Only treat Enc(pkX, ...) as ElGamal if the key is declared PUBLIC
            if (keyKinds.getOrDefault(k, KeyKind.SHARED) == KeyKind.PUBLIC) {
                usage.elgamalPublicKeys.add(k);
            }

            collectKeyUsage(enc.getMessage(), usage, keyKinds);

        } else if (node instanceof SignExprNode s) {
            usage.rsaPrivateKeys.add(s.getSigningKey().getName());
            collectKeyUsage(s.getMessage(), usage, keyKinds);

        } else if (node instanceof VerifyExprNode v) {
            usage.rsaPublicKeys.add(v.getPublicKey().getName());
            collectKeyUsage(v.getMessage(), usage, keyKinds);
            collectKeyUsage(v.getSignature(), usage, keyKinds);

        } else if (node instanceof MacExprNode mac) {
            collectKeyUsage(mac.getMessage(), usage, keyKinds);

        } else if (node instanceof HashExprNode h) {
            collectKeyUsage(h.getInner(), usage, keyKinds);

        } else if (node instanceof ConcatNode cat) {
            collectKeyUsage(cat.getLeft(), usage, keyKinds);
            collectKeyUsage(cat.getRight(), usage, keyKinds);
        }
    }


    private static void markKeyUses(
        SyntaxNode node,
        Map<String, EnumSet<KeyUse>> uses,
        Map<String, KeyKind> keyKinds
    ) {
        if (node instanceof AssignNode a) {
            markKeyUses(a.getValue(), uses, keyKinds);
            return;
        }

        if (node instanceof EncryptExprNode enc) {
            String keyName = enc.getKey().getName();
            if (keyKinds.get(keyName) == KeyKind.PUBLIC) {
                uses.get(keyName).add(KeyUse.PK_ENCRYPT);
            }
            markKeyUses(enc.getMessage(), uses, keyKinds);
            return;
        }

        if (node instanceof SignExprNode s) {
            String sk = s.getSigningKey().getName();
            if (uses.containsKey(sk)) uses.get(sk).add(KeyUse.SIGN);
            markKeyUses(s.getMessage(), uses, keyKinds);
            return;
        }

        if (node instanceof VerifyExprNode v) {
            String pk = v.getPublicKey().getName();
            if (uses.containsKey(pk)) uses.get(pk).add(KeyUse.VERIFY);
            markKeyUses(v.getMessage(), uses, keyKinds);
            markKeyUses(v.getSignature(), uses, keyKinds);
            return;
        }

        if (node instanceof MacExprNode m) {
            markKeyUses(m.getMessage(), uses, keyKinds);
            return;
        }

        if (node instanceof HashExprNode h) {
            markKeyUses(h.getInner(), uses, keyKinds);
            return;
        }

        if (node instanceof ConcatNode c) {
            markKeyUses(c.getLeft(), uses, keyKinds);
            markKeyUses(c.getRight(), uses, keyKinds);
        }
    }


    // ----------------- Helper: collect plaintext symbols -----------------

    private static void collectMessageSymbols(SyntaxNode node,
                                              Set<String> usedIds,
                                              Set<String> assignedIds) {
        if (node instanceof AssignNode a) {
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
    }

    // ----------------- Helper: expression generation -----------------

    private static String generateExpr(SyntaxNode node, Map<String, KeyKind> keyKinds) {

        if (node instanceof IdentifierNode id) {
            return id.getName();
        }

        if (node instanceof ConcatNode cat) {
            String left  = generateExpr(cat.getLeft(), keyKinds);
            String right = generateExpr(cat.getRight(), keyKinds);
            return "CryptoUtil.concat(" + left + ", " + right + ")";
        }

        if (node instanceof EncryptExprNode enc) {
            String keyName = enc.getKey().getName();
            String msgExpr = generateExpr(enc.getMessage(), keyKinds);
            KeyKind kind   = keyKinds.getOrDefault(keyName, KeyKind.SHARED);

            if (kind == KeyKind.PUBLIC) {
                return "CryptoUtil.elGamalEncrypt(" + keyName + ", " + msgExpr + ")";
            }
            return "CryptoUtil.encryptAESGCM(" + keyName + ", " + msgExpr + ")";
        }

        if (node instanceof MacExprNode mac) {
            String keyName = mac.getKey().getName();
            String msgExpr = generateExpr(mac.getMessage(), keyKinds);
            return "CryptoUtil.hmacSha256(" + keyName + ".getEncoded(), " + msgExpr + ")";
        }

        if (node instanceof HashExprNode h) {
            String inner = generateExpr(h.getInner(), keyKinds);
            return "CryptoUtil.sha256(" + inner + ")";
        }

        if (node instanceof SignExprNode s) {
            String skName  = s.getSigningKey().getName();
            String msgExpr = generateExpr(s.getMessage(), keyKinds);
            return "CryptoUtil.sign(" + skName + ", " + msgExpr + ")";
        }

        if (node instanceof VerifyExprNode v) {
            String pkName   = v.getPublicKey().getName();
            String msgExpr  = generateExpr(v.getMessage(), keyKinds);
            String sigExpr  = generateExpr(v.getSignature(), keyKinds);
            return "CryptoUtil.verify(" + pkName + ", " + msgExpr + ", " + sigExpr + ")";
        }

        return ("\"" + node.label() + "\".getBytes()");
    }
}
