package com.dynamicduo;

import guru.nidi.graphviz.model.*;
import static guru.nidi.graphviz.model.Factory.*;
import guru.nidi.graphviz.attribute.*;

public class SVG {
        private Graph g;

        public SVG(int numNodes, String p1, String p2, String[] messages, String[] passer) {

                Node[] nodesA = new Node[numNodes];
                Node[] nodesB = new Node[numNodes];

                Node A1 = node(p1)
                                .with(Attributes.attr("width", "1"))
                                .with(Attributes.attr("height", "1"))
                                .with(Shape.UNDERLINE)
                                .with(Attributes.attr("fontsize", "20"));
                nodesA[0] = A1;

                Node B1 = node(p2).with(Attributes.attr("width", "1"))
                                .with(Attributes.attr("height", "1"))
                                .with(Shape.UNDERLINE)
                                .with(Attributes.attr("fontsize", "20"));
                nodesB[0] = B1;

                for (int i = 1; i < numNodes; i++) {
                        nodesA[i] = node("A" + (i + 1))
                                        .with(Style.INVIS, Label.of(""), Shape.POINT);
                        nodesB[i] = node("B" + (i + 1))
                                        .with(Style.INVIS, Label.of(""), Shape.POINT);
                }

                Graph leftColumn = graph("left")
                                .with(nodesA)
                                .graphAttr().with("rank", "same"); // keep same column

                Graph rightColumn = graph("right")
                                .with(nodesB)
                                .graphAttr().with("rank", "same");

                LinkSource[] links = new LinkSource[numNodes * 3 - 2];

                for (int i = 0; i < nodesA.length - 1; i++) {
                        links[i] = nodesA[i].link(to(nodesA[i + 1]).with(Style.DOTTED, Arrow.NONE));
                        links[i + numNodes - 1] = nodesB[i].link(to(nodesB[i + 1]).with(Style.DOTTED, Arrow.NONE));
                        if (passer[i].equals(p1))
                                links[i + numNodes * 2 - 1] = nodesA[i + 1]
                                                .link(to(nodesB[i + 1]).with(Label.of(wrapLabel(messages[i]))));
                        else
                                links[i + numNodes * 2 - 1] = nodesB[i + 1]
                                                .link(to(nodesA[i + 1]).with(Label.of(wrapLabel(messages[i]))));
                }

                links[numNodes * 2 - 2] = nodesA[0].link(to(nodesB[0]).with("style", "invis"));

                g = graph("twoColumnBackAndForth").directed()
                                .graphAttr().with(Attributes.attr("rankdir", "LR"), // use LR or TB for direction
                                                Attributes.attr("nodesep", ".5"), // spacing between nodes in same rank
                                                Attributes.attr("ranksep", "1.0") // spacing between ranks
                                )
                                .with(leftColumn, rightColumn)
                                .with(links);

        }

        public Graph getGraph() {
                return g;
        }

        // This code is based on code from ChatGPT
        public static String wrapLabel(String text) {
                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (String word : text.split(" ")) {
                        if (count + word.length() > 100) {
                                sb.append("\\n"); // Graphviz newline
                                count = 0;
                        }
                        sb.append(word).append(" ");
                        count += word.length() + 1;
                }
                return sb.toString().trim();
        }
}