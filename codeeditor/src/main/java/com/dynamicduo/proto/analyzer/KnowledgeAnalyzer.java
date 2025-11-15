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
 * What we track:
 * - For each principal P:
 * knows[P] = set of atomic terms P knows (K_AB, N_A, M_1, c, ...)
 * encryptTerms[P] = set of ciphertext terms P has seen ("Enc(K_AB, M_1)", ...)
 *
 * Rules (Dolev–Yao style, simplified):
 * - When P sends or receives a message, P "sees" the terms that appear in the
 * clear.
 * - The Adversary is assumed to see ALL messages (passive eavesdropper).
 * - Ciphertexts are opaque: seeing Enc(k, m) does NOT reveal k or m.
 * - If P knows Enc(k, m) and also knows k (from somewhere else), then we add m
 * to P's knowledge.
 * - We repeat that inference until no new terms are learned.
 *
 * Simplifications:
 * - Identifiers (IdentifierNode) are treated as atomic symbols: "K_AB", "N_A",
 * "M_1", "c", etc.
 * - EncryptExprNode is treated as a term Enc(key, message).
 * - "Catastrophic" is: the adversary learns any K_* or M_* (keys or plaintext
 * messages),
 * purely as a demo signal.
 */
public final class KnowledgeAnalyzer {

    // Label for the implicit eavesdropping adversary.
    private static final String ADVERSARY = "Adversary";

    // Utility class; no one should instantiate this.
    private KnowledgeAnalyzer() {
    }

    /**
     * Public entry point.
     *
     * Call from Demo.java as:
     * KnowledgeAnalyzer.analyzeAndPrint(tree);
     *
     * It prints the knowledge summary to stdout.
     */
    public static void analyzeAndPrint(ProtocolNode proto) {
        // knows[P] = set of terms that principal P knows
        Map<String, Set<String>> knows = new LinkedHashMap<>();

        // 1) Initialize knowledge map for all declared roles in the protocol.
        for (IdentifierNode id : proto.getRoles().getRoles()) {
            // LinkedHashSet keeps insertion order so output is stable and readable.
            knows.put(id.getName(), new LinkedHashSet<>());
        }

        // Add the implicit passive adversary.
        knows.put(ADVERSARY, new LinkedHashSet<>());

        // encryptTerms[P] = set of ciphertext terms "Enc(k, m)" that P has seen
        Map<String, Set<String>> encryptTerms = new LinkedHashMap<>();
        for (String p : knows.keySet()) {
            encryptTerms.put(p, new LinkedHashSet<>());
        }

        // 2) Process each message in order.
        //
        // Each MessageSendNode represents something like:
        // Alice -> Bob : c = Enc(K_AB, M_1)
        //
        // Observers:
        // - sender (Alice)
        // - receiver (Bob)
        // - adversary (sees everything on the wire)
        for (MessageSendNode msg : proto.getMessages()) {
            String sender = msg.getSender().getName();
            String recv = msg.getReceiver().getName();

            List<String> observers = List.of(sender, recv, ADVERSARY);

            // Collect clear identifiers and opaque ciphertext terms from this message body.
            Set<String> ids = new LinkedHashSet<>();
            Set<String> encs = new LinkedHashSet<>();
            collectTerms(msg.getBody(), ids, encs);

            // Everything visible in this message is learned by each observer.
            for (String p : observers) {
                knows.get(p).addAll(ids);
                encryptTerms.get(p).addAll(encs);
            }
        }

        // 3) Apply the decryption inference rule until no new information appears.
        //
        // Rule:
        // If P knows Enc(k, m) in encryptTerms[P]
        // AND P knows k in knows[P]
        // THEN add m to knows[P].
        //
        // This may unlock new keys/messages, so we iterate until stable.
        boolean changed;
        do {
            changed = false;

            for (String p : knows.keySet()) {
                Set<String> terms = knows.get(p); // plain knowledge of P
                Set<String> encs = encryptTerms.get(p); // ciphertexts P has seen

                for (String enc : encs) {
                    // Expect format "Enc(k, m)" as constructed in collectTerms.
                    if (!enc.startsWith("Enc(") || !enc.endsWith(")")) {
                        continue; // defensive skip if malformed
                    }

                    String inside = enc.substring("Enc(".length(), enc.length() - 1);
                    // Split "k, m" into ["k", "m"]
                    String[] parts = inside.split(",", 2);
                    if (parts.length != 2)
                        continue;

                    String k = parts[0].trim(); // key identifier
                    String m = parts[1].trim(); // message identifier

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
        // or a plaintext message (M_*), we flag it.
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
     * - identifiers that appear in the clear (LHS of assignments, bare identifiers)
     * - ciphertext terms "Enc(k, m)" as opaque units
     *
     * IMPORTANT (correct Dolev–Yao behavior):
     * - When we see Enc(k, m), we record ONLY "Enc(k, m)" as a visible term.
     * - We DO NOT expose k or m from inside the ciphertext.
     * - This ensures the adversary does NOT automatically see plaintext or keys.
     */
    private static void collectTerms(SyntaxNode node,
            Set<String> identifiers,
            Set<String> encrypts) {

        // A bare identifier (not under encryption) appears in the clear.
        // Example: "K_AB" by itself, "c" as a variable name, etc.
        if (node instanceof IdentifierNode id) {
            identifiers.add(id.getName());
        }

        // An encryption expression Enc(key, msg):
        // - We treat this as an opaque term "Enc(key, msg)".
        // - We DO NOT add key or msg to identifiers (no peeking inside).
        else if (node instanceof EncryptExprNode enc) {
            String kName = enc.getKey().getName();
            String mName = enc.getMessage().getName();

            String term = "Enc(" + kName + ", " + mName + ")";
            encrypts.add(term);

            // DO NOT:
            // identifiers.add(kName);
            // identifiers.add(mName);
            // That would incorrectly give visibility into the ciphertext.
        }

        // Assignment exposes the variable on the left-hand side.
        // Example: c = Enc(K_AB, M_1)
        // - "c" appears in the clear and should be considered known to observers.
        // - The right-hand side may contain identifiers and ciphertexts, but
        // visibility is handled by recursive calls (with correct opacity rules).
        else if (node instanceof AssignNode a) {
            identifiers.add(a.getTarget().getName());
            collectTerms(a.getValue(), identifiers, encrypts);
        }

        // If we introduce more node types later (tuples, concatenations, explicit
        // nonces, etc.), extend this method with additional "else if" branches
        // to define how they contribute to visibility.
    }
}
