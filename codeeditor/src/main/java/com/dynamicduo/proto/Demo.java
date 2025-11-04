package com.dynamicduo.proto;

import com.dynamicduo.proto.lexer.*;
import com.dynamicduo.proto.parser.*;
import com.dynamicduo.proto.ast.*;

/**
 * Tiny CLI to prove the pipeline works for your report-out.
 * Run from Maven/VSCode:
 *   - VSCode: Run > Run Java on this file
 *   - Maven:  mvn -q -Dexec.mainClass=com.dynamicduo.proto.Demo exec:java
 */
public class Demo {
    public static void main(String[] args) {
        // Change this sample as you wish for the demo
        String input = """
            roles: Alice, Bob
            Alice -> Bob : c = Enc(k, m)
            Alice -> Bob : Enc(k, m)
        """;

        // 1) Lex
        Lexer lexer = new Lexer(input);

        // 2) Parse
        ProtocolParser parser = new ProtocolParser(lexer);

        try {
            ProtocolNode tree = parser.parse();

            // 3) Pretty-print AST tree for your slides
            System.out.println("=== AST ===");
            System.out.println(tree.pretty());

        } catch (ParseException e) {
            System.err.println("Parse error: " + e.getMessage());
            System.err.println("Line: " + e.getLine());
        }
    }
}
