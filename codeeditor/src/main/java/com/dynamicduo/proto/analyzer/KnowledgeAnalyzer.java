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

                    // If P knows the key and does not yet know the message, P learns the message.
                    if (terms.contains(k) && !terms.contains(m)) {
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
     * collectTerms
     *
     * Walks a SyntaxNode subtree and extracts:
     *   - identifiers that appear in the clear (LHS of assignments, bare identifiers)
     *   - opaque structured crypto terms (Enc, Mac, H, Sign, Verify, Concat, ...)
     *
     * Modeling decisions:
     *
     *   - When it see Enc(k, m) or Mac(k, m) or H(m) or Sign(sk, m),
     *     or Verify(pk, m, sig), or Concat(...),
     *     I record only an opaque string for that whole term in cryptoTerms.
     *
     *   - I do NOT automatically expose identifiers inside these structured
     *     terms to the adversary. Sending "m1 || 0" is treated as one opaque
     *     Concat(...) term, not as separate m1 and 0.
     *
     *   - Plain IdentifierNodes that are not under crypto are treated as visible
     *     atoms in the clear and are added to identifiers.
     */
    private static void collectTerms(
            SyntaxNode node,
            Set<String> identifiers,   // identifiers in the clear
            Set<String> cryptoTerms    // opaque structured terms
    ) {
        if (node instanceof IdentifierNode id) {
            // A bare identifier (not under crypto) appears in the clear.
            identifiers.add(id.getName());
        }

        else if (node instanceof EncryptExprNode enc) {
            String kName = enc.getKey().getName();
            // For the message part, I only record the label of the subtree.
            // If the message is a simple identifier, label() will return its name.
            // If it is something structured (like a concat), label() will be
            // something like "Concat" or another descriptive label.
            String mLabel = enc.getMessage().label();
            String term = "Enc(" + kName + ", " + mLabel + ")";
            cryptoTerms.add(term);
        }

        else if (node instanceof MacExprNode mac) {
            String kName = mac.getKey().getName();
            String mLabel = mac.getMessage().label();
            String term = "Mac(" + kName + ", " + mLabel + ")";
            cryptoTerms.add(term);
        }

        else if (node instanceof HashExprNode h) {
            String innerLabel = h.getInner().label();
            String term = "H(" + innerLabel + ")";
            cryptoTerms.add(term);
        }

        else if (node instanceof SignExprNode s) {
            String skName = s.getSk().getName();
            String mLabel = s.getMessage().label();
            String term = "Sign(" + skName + ", " + mLabel + ")";
            cryptoTerms.add(term);
        }

        else if (node instanceof VerifyExprNode v) {
            String pkName = v.getPk().getName();
            String term = "Verify(" + pkName + ", ...)";
            cryptoTerms.add(term);
        }

        else if (node instanceof ConcatNode c) {
            // In this model, I treat concatenation as an opaque bitstring.
            // I do not automatically reveal the inner identifiers; instead I
            // just record one structured "Concat(...)" term.
            String leftLabel  = c.getLeft().label();
            String rightLabel = c.getRight().label();
            String term = "Concat(" + leftLabel + ", " + rightLabel + ")";
            cryptoTerms.add(term);

            // If I ever want to switch back to a fully symbolic model where
            // concat in the clear exposes its components, I can replace this
            // block with:
            //
            //     collectTerms(c.getLeft(), identifiers, cryptoTerms);
            //     collectTerms(c.getRight(), identifiers, cryptoTerms);
            //
            // For now I keep concat opaque.
        }

        else if (node instanceof AssignNode a) {
            // Assignment exposes the variable on the left-hand side.
            // Example: c = Enc(K_AB, M_1 || 0)
            //
            // "c" appears in the clear and should be considered known to observers.
            identifiers.add(a.getTarget().getName());
            // I still recurse into the value so I can collect any opaque
            // crypto terms (Enc, Mac, Concat, etc.), but that recursion
            // does NOT leak identifiers from inside those structures.
            collectTerms(a.getValue(), identifiers, cryptoTerms);
        }

        // If I introduce more node types later, I can extend this method with
        // more "else if" branches to define how they contribute to visibility.
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

            Set<String> ids = new LinkedHashSet<>();
            Set<String> encs = new LinkedHashSet<>();
            collectTerms(msg.getBody(), ids, encs);

            for (String p : observers) {
                knows.get(p).addAll(ids);
                encryptTerms.get(p).addAll(encs);
            }
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

                    if (terms.contains(k) && !terms.contains(m)) {
                        terms.add(m);
                        changed = true;
                    }
                }
            }

        } while (changed);

        // Build output string
        StringBuilder sb = new StringBuilder();
        sb.append("=== Knowledge Summary ===\n");

        for (var e : knows.entrySet()) {
            sb.append(e.getKey()).append(" knows: ").append(e.getValue()).append("\n");
        }

        Set<String> adv = knows.get(ADVERSARY);
        Set<String> catastrophic = new LinkedHashSet<>();

        for (String t : adv) {
            if (t.startsWith("K_") || t.startsWith("M_"))
                catastrophic.add(t);
        }

        if (!catastrophic.isEmpty()) {
            sb.append("*** Catastrophic for protocol: adversary learned ")
            .append(catastrophic).append(" ***\n");
        } else {
            sb.append("No catastrophic leaks under this simple model.\n");
        }

        return sb.toString();
    }

}
