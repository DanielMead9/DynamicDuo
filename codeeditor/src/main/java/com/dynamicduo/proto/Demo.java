package com.dynamicduo.proto;

import com.dynamicduo.proto.lexer.Lexer;
import com.dynamicduo.proto.parser.ProtocolParser;
import com.dynamicduo.proto.parser.ParseException;
import com.dynamicduo.proto.ast.ProtocolNode;
import com.dynamicduo.proto.render.SequenceDiagramFromAst;

public class Demo {
    public static void main(String[] args) {
        // Simple two-party protocol that will render like your screenshot
        String input = """
            roles: Alice, Bob

            Alice -> Bob   : Message1 = Enc(K_AB, N_A)
            Bob   -> Alice : Message2 = Enc(K_AB, N_B)
            Alice -> Bob   : Message3 = Enc(K_AB, M_1)
            Alice -> Bob   : Message4 = Enc(K_AB, M_2)
            Bob   -> Alice : Message5 = Enc(K_AB, M_3)
            Alice -> Bob   : Message6 = Enc(K_AB, M_4)
        """;

        Lexer lexer = new Lexer(input);
        ProtocolParser parser = new ProtocolParser(lexer);

        try {
            ProtocolNode tree = parser.parse();

            System.out.println("=== AST ===");
            System.out.println(tree.pretty());

            // Use our adapter to create a nice sequence diagram SVG
            SequenceDiagramFromAst.renderTwoParty(tree, "pretty_protocol.svg");

        } catch (ParseException e) {
            System.err.println("Parse error: " + e.getMessage());
            System.err.println("Line: " + e.getLine());
        } catch (Exception e) {
            System.err.println("Render failed: " + e.getMessage());
        }
    }
}
