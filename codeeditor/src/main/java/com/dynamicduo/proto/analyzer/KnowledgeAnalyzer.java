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
 * I should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.dynamicduo.proto.analyzer;

import com.dynamicduo.proto.ast.*;

import java.util.*;

/**
 * KnowledgeAnalyzer
 *
 * Goal:
 * Given a protocol AST (ProtocolNode), estimate what each principal
 * (Alice, Bob, etc.) and an implicit eavesdropper ("Adversary")
 * knows after a single run of the protocol.
 *
 * What this track:
 *   - For each principal P:
 *       knows[P]       = set of atomic terms P knows (K_AB, N_A, M_1, c, ...)
 *       cryptoTerms[P] = set of opaque structured terms P has seen
 *                        ("Enc(K_AB, M_1)", "Mac(K_AB, c)", "Concatenation(m1, 0)", ...)
 *
 * Semantic model (more “computational” than pure symbolic):
 *
 *   - When P sends or receives a message, P "sees":
 *       * identifiers that appear in the clear (not under crypto),
 *       * opaque crypto terms (Enc, Mac, H, Sign, Verify, Concat, ...).
 *
 *   - The adversary is assumed to see ALL messages (passive eavesdropper).
 *
 *   - Ciphertexts, MACs, hashes, signatures, and concatenations are opaque:
 *       * Seeing Enc(k, m) does not automatically reveal k or m.
 *       * Seeing m1 || 0 in the clear is treated as a single opaque Concat(...)
 *         term, not as separate identifiers m1 and 0.
 *
 *   - Decryption rule:
 *       If P knows Enc(k, m) (as an opaque term) and also knows k (from
 *       somewhere else as an identifier), then P learns m as a symbolic
 *       identifier.
 *
 *   - The analyzer does NOT currently “open up” concatenations. Even if P learns a
 *     Concat(m1, 0) term, that stays as one opaque blob from the point of
 *     view of this analyzer. This matches the idea that seeing a bitstring
 *     does not automatically tell me which variable it came from.
 *
 * Simplifications:
 *   - Identifiers (IdentifierNode) are treated as atomic symbols: "K_AB",
 *     "N_A", "M_1", "c", etc.
 *   - EncryptExprNode / MacExprNode / HashExprNode / SignExprNode /
 *     VerifyExprNode / ConcatNode are all treated as opaque structured
 *     terms. I encode them as strings like "Enc(K_AB, M_1)" for now.
 *   - "Catastrophic" is: the adversary learns any identifier that looks like
 *     a key (K_*) or plaintext message (M_*). I just use this as a rough
 *     signal for now.
 */
public final class KnowledgeAnalyzer {

    // Label for the implicit eavesdropping adversary.
    private static final String ADVERSARY = "Adversary";

    private KnowledgeAnalyzer() {
        // Utility class
    }

    /**
     * Public entry point.
     *
     * Typical usage from a demo:
     *     KnowledgeAnalyzer.analyzeAndPrint(tree);
     *
     * This prints the knowledge summary to stdout.
     * Later I can route this into the GUI's Analysis tab.
     */
    public static void analyzeAndPrint(ProtocolNode proto) {
        // knows[P] = set of atomic terms that principal P knows
        Map<String, Set<String>> knows = new LinkedHashMap<>();

        // 1) Initialize knowledge map for all declared roles in the protocol.
        for (IdentifierNode id : proto.getRoles().getRoles()) {
            // LinkedHashSet keeps insertion order so output is stable and readable.
            knows.put(id.getName(), new LinkedHashSet<>());
        }

        // Add the implicit passive adversary.
        knows.put(ADVERSARY, new LinkedHashSet<>());

        // cryptoTerms[P] = set of opaque structured terms P has seen
        // e.g., "Enc(K_AB, M_1)", "Mac(K_AB, c)", "Concat(m1, 0)", ...
        Map<String, Set<String>> cryptoTerms = new LinkedHashMap<>();
        for (String p : knows.keySet()) {
            cryptoTerms.put(p, new LinkedHashSet<>());
        }

        // 2) Process each message in order.
        //
        // Each MessageSendNode represents:
        //   sender -> receiver : <body>
        //
        // Observers:
        //   - sender
        //   - receiver
        //   - adversary (sees everything on the wire)
        for (MessageSendNode msg : proto.getMessages()) {
            String sender = msg.getSender().getName();
            String recv   = msg.getReceiver().getName();

            List<String> observers = List.of(sender, recv, ADVERSARY);

            // Collect identifiers in the clear and opaque crypto terms
            // from the message body.
            Set<String> ids  = new LinkedHashSet<>();
            Set<String> ops  = new LinkedHashSet<>();
            collectTerms(msg.getBody(), ids, ops);

            // Everything visible in this message is learned by each observer.
            for (String p : observers) {
                knows.get(p).addAll(ids);
                cryptoTerms.get(p).addAll(ops);
            }
        }

        // 3) Apply the decryption inference rule until no new information appears.
        //
        // Rule:
        //   If P has an opaque term "Enc(k, m)" in cryptoTerms[P]
        //   AND P knows k in knows[P]
        //   THEN add m to knows[P] as a new atomic term.
        //
        // I do NOT open concatenations or other structures here; I just
        // propagate symbolic identifiers.
        boolean changed;
        do {
            changed = false;

            for (String p : knows.keySet()) {
                Set<String> terms = knows.get(p);        // plain knowledge of P
                Set<String> ops   = cryptoTerms.get(p);  // opaque structured terms P has seen

                for (String op : ops) {
                    // Expect encryption terms in the form "Enc(k, m)".
                    if (!op.startsWith("Enc(") || !op.endsWith(")")) {
                        continue; // skip non-encryption structured terms here
                    }

                    String inside = op.substring("Enc(".length(), op.length() - 1);
                    // Split "k, m" into ["k", "m"]
                    String[] parts = inside.split(",", 2);
                    if (parts.length != 2)
                        continue;

                    String k = parts[0].trim(); // key identifier
                    String m = parts[1].trim(); // message identifier (symbolic name)

                    // If P knows the key and the plaintext is a bare identifier, P learns it.
                    if (terms.contains(k) && !terms.contains(m) && isBareIdentifier(m)) {
                        terms.add(m);
                        changed = true;
                    }
                }
            }
        } while (changed); // keep looping while new info is being added

        // 4) Print knowledge summary.
        System.out.println("=== Knowledge Summary ===");
        for (Map.Entry<String, Set<String>> e : knows.entrySet()) {
            System.out.println(e.getKey() + " knows: " + e.getValue());
        }

        // 5) Identify "catastrophic" leaks:
        //
        // If the adversary learns any term that looks like a key (K_*)
        // or a plaintext message (M_*), I flag it as catastrophic.
        Set<String> advTerms = knows.get(ADVERSARY);
        Set<String> catastrophic = new LinkedHashSet<>();
        for (String t : advTerms) {
            if (t.startsWith("K_") || t.startsWith("M_")) {
                catastrophic.add(t);
            }
        }

        if (!catastrophic.isEmpty()) {
            System.out.println("*** Catastrophic for protocol: adversary learned "
                    + catastrophic + " ***");
        } else {
            System.out.println("No catastrophic leaks under this simple model.");
        }
    }

    /**
     * Walk a SyntaxNode subtree and extract:
     *  - identifiers that appear in the clear (lhs of assignments, bare ids, and
     *    any opaque crypto/concat terms we want to list in the summary)
     *  - ciphertext terms "Enc(keySym, msgSym)" as opaque units in encrypts
     *
     * Visibility rules:
     *  - Bare identifiers: visible in the clear.
     *  - Assignment "x = expr": the variable x is visible; visibility of expr
     *    depends on Recursion + node-specific rules.
     *  - Enc(key, msgExpr): we record only the ciphertext symbol
     *      Enc(keyName, msgLabel)
     *    in encrypts; no visibility of msgExpr’s internals.
     *  - Concat(left, right): treated as a single symbol "(leftLabel || rightLabel)"
     *    with no automatic visibility of left/right.
     *  - Mac, Hash, Sign, Verify: treated as opaque symbols, added to identifiers
     *    so they appear in the knowledge summary but do not enable inference.
     */
    private static void collectTerms(
            SyntaxNode node,
            Set<String> identifiers,
            Set<String> encrypts) {

        // Bare identifier in the clear.
        if (node instanceof IdentifierNode id) {
            identifiers.add(id.getName());
        }

        // Encryption: Enc(keyExpr, msgExpr)
        else if (node instanceof EncryptExprNode enc) {
            String kName = enc.getKey().getName();
            // The message is a general expression, so we use its label as a symbol.
            String mSym  = enc.getMessage().label();

            String term = "Enc(" + kName + ", " + mSym + ")";
            encrypts.add(term);

            // Do NOT recurse into enc.getMessage() for visibility.
            // Ciphertext is opaque unless the key is known and the decryption
            // inference rule fires later.
        }

        // Concatenation: left || right
        else if (node instanceof ConcatNode cat) {
            String leftLabel  = cat.getLeft().label();
            String rightLabel = cat.getRight().label();
            String sym = "(" + leftLabel + " || " + rightLabel + ")";

            // Treat the entire concatenation as an opaque symbol.
            identifiers.add(sym);

            // Do NOT recurse into left/right; that matches the "opaque blob" model.
        }

        // MAC: Mac(keyId, msgExpr)
        else if (node instanceof MacExprNode mac) {
            String kName   = mac.getKey().getName();
            String msgSym  = mac.getMessage().label();
            String sym = "Mac(" + kName + ", " + msgSym + ")";

            // Include the MAC tag as a visible symbol, but no inference is built on it.
            identifiers.add(sym);
            // No recursion into msgExpr for visibility.
        }

        // Hash: Hash(expr)
        else if (node instanceof HashExprNode h) {
            String inner = h.getInner().label();
            String sym = "H(" + inner + ")";

            identifiers.add(sym);
            // Do not recurse into inner for visibility.
        }

        // Signature: Sign(sk, msgExpr)
        else if (node instanceof SignExprNode s) {
            String skName  = s.getSigningKey().getName();
            String msgSym  = s.getMessage().label();
            String sym = "Sign(" + skName + ", " + msgSym + ")";

            identifiers.add(sym);
        }

        // Verify: Verify(pk, msgExpr, sigExpr)
        else if (node instanceof VerifyExprNode v) {
            String pkName   = v.getPublicKey().getName();
            String msgSym   = v.getMessage().label();
            String sigSym   = v.getSignature().label();
            String sym = "Verify(" + pkName + ", " + msgSym + ", " + sigSym + ")";

            identifiers.add(sym);
        }

        // Assignment: exposes the LHS variable in the clear,
        // then applies the visibility rules recursively to the RHS.
        else if (node instanceof AssignNode a) {
            identifiers.add(a.getTarget().getName());
            collectTerms(a.getValue(), identifiers, encrypts);
        }
    }


        public static String analyzeToString(ProtocolNode proto) {
        Map<String, Set<String>> knows = new LinkedHashMap<>();
        Map<String, Set<String>> encryptTerms = new LinkedHashMap<>();

        // Initialization (same logic as analyzeAndPrint)
        for (IdentifierNode id : proto.getRoles().getRoles()) {
            knows.put(id.getName(), new LinkedHashSet<>());
        }
        knows.put(ADVERSARY, new LinkedHashSet<>());

        for (String p : knows.keySet()) {
            encryptTerms.put(p, new LinkedHashSet<>());
        }

        // Collect terms from messages
        for (MessageSendNode msg : proto.getMessages()) {
            String sender = msg.getSender().getName();
            String recv   = msg.getReceiver().getName();
            List<String> observers = List.of(sender, recv, ADVERSARY);

            // 1) What is visible on the wire (your existing visibility rules)
            Set<String> visibleIds = new LinkedHashSet<>();
            Set<String> encs       = new LinkedHashSet<>();
            collectTerms(msg.getBody(), visibleIds, encs);

            // 2) What the sender must know to BUILD this message (keys, nonces, etc.)
            Set<String> builtIds = new LinkedHashSet<>();
            collectAuthorIds(msg.getBody(), builtIds);

            // Observers learn only what is visible on the wire
            for (String p : observers) {
                knows.get(p).addAll(visibleIds);
                encryptTerms.get(p).addAll(encs);
            }

            // Sender also knows all identifiers used to construct the message
            knows.get(sender).addAll(builtIds);
        }

        // Apply decryption rule
        boolean changed;
        do {
            changed = false;

            for (String p : knows.keySet()) {
                Set<String> terms = knows.get(p);
                Set<String> encs  = encryptTerms.get(p);

                for (String enc : encs) {
                    if (!enc.startsWith("Enc(") || !enc.endsWith(")"))
                        continue;

                    String inside = enc.substring(4, enc.length() - 1);
                    String[] parts = inside.split(",", 2);
                    if (parts.length != 2) continue;

                    String k = parts[0].trim();
                    String m = parts[1].trim();

                    if (terms.contains(k) && !terms.contains(m) && isBareIdentifier(m)) {
                        terms.add(m);
                        changed = true;
                    }
                }
            }

        } while (changed);

        // Build pretty, categorized output
        StringBuilder sb = new StringBuilder();
        sb.append("=== Knowledge Summary ===\n\n");

        for (String principal : knows.keySet()) {
            sb.append(principal).append("\n");

            Set<String> atomic     = new LinkedHashSet<>();
            Set<String> structured = new LinkedHashSet<>();

            // Split into atomic vs structured
            for (String term : knows.get(principal)) {
                if (isStructuredTerm(term)) {
                    structured.add(term);
                } else {
                    atomic.add(term);
                }
            }

            // Enc(...) terms are only in encryptTerms, so include them as structured too
            structured.addAll(encryptTerms.get(principal));

            sb.append("  Atomic terms:\n");
            if (atomic.isEmpty()) {
                sb.append("    (none)\n");
            } else {
                for (String t : atomic) {
                    sb.append("    - ").append(t).append("\n");
                }
            }

            sb.append("  Structured terms:\n");
            if (structured.isEmpty()) {
                sb.append("    (none)\n");
            } else {
                for (String t : structured) {
                    sb.append("    - ").append(t).append("\n");
                }
            }

            sb.append("\n");
        }

        // Catastrophic leak summary
        Set<String> adv = knows.get(ADVERSARY);
        Set<String> catastrophic = new LinkedHashSet<>();

        for (String t : adv) {
            if (t.startsWith("K_") || t.startsWith("M_")) {
                catastrophic.add(t);
            }
        }

        if (!catastrophic.isEmpty()) {
            sb.append("*** Potentially catastrophic: adversary learned the following key/plaintext symbols:\n");
            for (String t : catastrophic) {
                sb.append("  - ").append(t).append("\n");
            }
        } else {
            sb.append("No catastrophic leaks under this simple model.\n");
        }

        return sb.toString();
    }

    /**
     * Heuristic: treat anything with parentheses or "||" as a structured term
     * (ciphertext, MAC, signature, hash, concat, etc.), and bare symbols
     * like K_AB, M_1, N_A as atomic.
     */
    private static boolean isStructuredTerm(String term) {
        return term.contains("(") || term.contains("||");
    }

    /**
     * Collect identifiers that a sender must know in order to BUILD the message.
     * Unlike collectTerms, this ignores visibility rules and recurses into
     * the internals of crypto/concat expressions.
     */
    private static void collectAuthorIds(SyntaxNode node, Set<String> out) {
        if (node instanceof IdentifierNode id) {
            out.add(id.getName());
        } else if (node instanceof EncryptExprNode enc) {
            collectAuthorIds(enc.getKey(), out);
            collectAuthorIds(enc.getMessage(), out);
        } else if (node instanceof ConcatNode cat) {
            collectAuthorIds(cat.getLeft(), out);
            collectAuthorIds(cat.getRight(), out);
        } else if (node instanceof MacExprNode mac) {
            collectAuthorIds(mac.getKey(), out);
            collectAuthorIds(mac.getMessage(), out);
        } else if (node instanceof HashExprNode h) {
            collectAuthorIds(h.getInner(), out);
        } else if (node instanceof SignExprNode s) {
            collectAuthorIds(s.getSigningKey(), out);
            collectAuthorIds(s.getMessage(), out);
        } else if (node instanceof VerifyExprNode v) {
            collectAuthorIds(v.getPublicKey(), out);
            collectAuthorIds(v.getMessage(), out);
            collectAuthorIds(v.getSignature(), out);
        } else if (node instanceof AssignNode a) {
            // The sender obviously knows the variable they assign to and
            // all identifiers used in the assigned expression.
            collectAuthorIds(a.getTarget(), out);
            collectAuthorIds(a.getValue(), out);
        }
        // For other node types, nothing to do
    }

        /**
     * Treat as "bare identifier" only simple symbols like K_AB, M_1, N_A, c, ack.
     * We exclude anything with non [A-Za-z0-9_] characters, and also the internal
     * "Concat" label which comes from expression labels, not from user-level ids.
     */
    private static boolean isBareIdentifier(String term) {
        if ("Concat".equals(term)) {
            return false; // internal label for concatenations; not a user-visible atom
        }
        if (term.isEmpty()) {
            return false;
        }
        for (int i = 0; i < term.length(); i++) {
            char c = term.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_')) {
                return false;
            }
        }
        return true;
    }

}
