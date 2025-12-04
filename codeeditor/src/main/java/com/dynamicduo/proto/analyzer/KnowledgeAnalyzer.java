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

package com.dynamicduo.proto.analyzer;
import com.dynamicduo.proto.ast.*;
import java.util.*;

/**
 * Simple Dolev–Yao style knowledge analysis.
 *
 * For each principal (including an implicit "Adversary"), we approximate
 * which terms they know after a single run of the protocol.
 */
public final class KnowledgeAnalyzer {

    private static final String ADVERSARY = "Adversary";

    private KnowledgeAnalyzer() {
        // utility class, not instantiated
    }

    /**
     * Convenience method for CLI: compute analysis and print it.
     */
    public static void analyzeAndPrint(ProtocolNode proto) {
        System.out.print(analyzeToString(proto));
    }

    /**
     * Main entry point for the GUI: compute the analysis as a String.
     */
    public static String analyzeToString(ProtocolNode proto) {
        StringBuilder out = new StringBuilder();

        // For each principal P:
        //   knows[P]        = plain terms (identifiers) P knows
        //   encryptTerms[P] = ciphertexts Enc(k, m) P has seen
        Map<String, Set<String>> knows = new LinkedHashMap<>();
        Map<String, Set<String>> encryptTerms = new LinkedHashMap<>();

        // Initialize with declared roles
        for (IdentifierNode id : proto.getRoles().getRoles()) {
            knows.put(id.getName(), new LinkedHashSet<>());
        }
        // Add implicit passive adversary
        knows.put(ADVERSARY, new LinkedHashSet<>());

        for (String p : knows.keySet()) {
            encryptTerms.put(p, new LinkedHashSet<>());
        }

        // Process each message
        for (MessageSendNode msg : proto.getMessages()) {
            String sender = msg.getSender().getName();
            String recv   = msg.getReceiver().getName();
            List<String> observers = List.of(sender, recv, ADVERSARY);

            Set<String> ids  = new LinkedHashSet<>();
            Set<String> encs = new LinkedHashSet<>();
            collectTerms(msg.getBody(), ids, encs);

            for (String p : observers) {
                knows.get(p).addAll(ids);
                encryptTerms.get(p).addAll(encs);
            }
        }

        // Decryption rule:
        // If P knows Enc(k, m) and P knows k, then P also knows m.
        boolean changed;
        do {
            changed = false;

            for (String p : knows.keySet()) {
                Set<String> terms = knows.get(p);
                Set<String> encs  = encryptTerms.get(p);

                for (String enc : encs) {
                    if (!enc.startsWith("Enc(") || !enc.endsWith(")")) {
                        continue;
                    }
                    String inside = enc.substring("Enc(".length(), enc.length() - 1);
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

        // Build summary text
        out.append("=== Knowledge Summary ===\n");
        for (Map.Entry<String, Set<String>> e : knows.entrySet()) {
            out.append(e.getKey())
               .append(" knows: ")
               .append(e.getValue())
               .append('\n');
        }

        // Flag catastrophic cases: adversary learns any K_* or M_*
        Set<String> advTerms = knows.get(ADVERSARY);
        Set<String> catastrophic = new LinkedHashSet<>();
        for (String t : advTerms) {
            if (t.startsWith("K_") || t.startsWith("M_")) {
                catastrophic.add(t);
            }
        }

        if (!catastrophic.isEmpty()) {
            out.append("*** Catastrophic: adversary learned ")
               .append(catastrophic)
               .append(" ***\n");
        } else {
            out.append("No catastrophic leaks under this simple model.\n");
        }

        return out.toString();
    }

    /**
     * Collect identifiers that appear in the clear and ciphertexts seen in the body.
     */
    private static void collectTerms(SyntaxNode node,
                                     Set<String> identifiers,
                                     Set<String> encrypts) {

        if (node instanceof IdentifierNode id) {
            // Bare identifier in the clear
            identifiers.add(id.getName());
        } else if (node instanceof EncryptExprNode enc) {
            // Ciphertext is treated as opaque Enc(k, m)
            String kName = enc.getKey().getName();
            String mName = enc.getMessage().getName();
            encrypts.add("Enc(" + kName + ", " + mName + ")");
        } else if (node instanceof AssignNode a) {
            // LHS is visible in the clear; RHS may contain identifiers or ciphertexts
            identifiers.add(a.getTarget().getName());
            collectTerms(a.getValue(), identifiers, encrypts);
        }
        // other node types can be handled here as needed
    }
}
