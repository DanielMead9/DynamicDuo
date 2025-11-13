package com.dynamicduo.proto;

import com.dynamicduo.proto.lexer.*;
import com.dynamicduo.proto.parser.*;
import com.dynamicduo.proto.ast.*;

// ADD:
import com.dynamicduo.proto.render.DotBuilder;
import com.dynamicduo.proto.render.SvgRenderer;

public class Demo {
    public static void main(String[] args) {
        String input = """
            roles: Alice, Bob, Server

            Alice -> Server : req = Enc(K_AS, N_A)
            Server -> Bob   : ticket = Enc(K_BS, K_AB)
            Bob -> Server   : confirm = Enc(K_BS, N_B)
            Alice -> Bob    : data1 = Enc(K_AB, M_1)
            Bob   -> Alice  : data2 = Enc(K_AB, M_2)
        """;

        Lexer lexer = new Lexer(input);
        ProtocolParser parser = new ProtocolParser(lexer);

        try {
            ProtocolNode tree = parser.parse();

            System.out.println("=== AST ===");
            System.out.println(tree.pretty());

            // Build DOT and render SVG
            var dotFile = DotBuilder.buildAndWrite(tree, "protocol.dot"); // AST → .dot
            var svgFile = java.nio.file.Path.of("protocol.svg");
            SvgRenderer.render(dotFile, svgFile);                         // .dot → .svg

        } catch (ParseException e) {
            System.err.println("Parse error: " + e.getMessage());
            System.err.println("Line: " + e.getLine());
        } catch (Exception e) {
            System.err.println("Graph generation failed: " + e.getMessage());
        }
    }
}
