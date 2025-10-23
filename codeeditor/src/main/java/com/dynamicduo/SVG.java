package com.dynamicduo;

import java.io.File;

import guru.nidi.graphviz.engine.*;
import guru.nidi.graphviz.model.*;
import static guru.nidi.graphviz.model.Factory.*;
import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.attribute.Rank.RankDir;

public class SVG {
        private Graph g;

        public SVG(int numNodes, String p1, String p2, String[] messages) {

                Node[] nodesA = new Node[numNodes];
                Node[] nodesB = new Node[numNodes];

                System.out.println(numNodes);
                System.out.println(p1);
                System.out.println(p2);
                System.out.println(messages.length);

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
                }

                for (int i = 1; i < numNodes; i++) {
                        nodesB[i] = node("B" + (i + 1))
                                        .with(Style.INVIS, Label.of(""), Shape.POINT);
                }

                // Left column nodes
                /*
                 * Node A2 = node("A2").with(Style.INVIS, Label.of(""), Shape.POINT);
                 * Node A3 = node("A3").with(Style.INVIS, Label.of(""), Shape.POINT);
                 */

                // Right column nodes

                /*
                 * Node B2 = node("B2").with(Style.INVIS, Label.of(""), Shape.POINT);
                 * Node B3 = node("B3").with(Style.INVIS, Label.of(""), Shape.POINT);
                 */

                Graph leftColumn = graph("left")
                                .with(nodesA)
                                .graphAttr().with("rank", "same"); // keep same column

                Graph rightColumn = graph("right")
                                .with(nodesB)
                                .graphAttr().with("rank", "same");

                LinkSource[] links = new LinkSource[numNodes * 3 - 2];

                for (int i = 0; i < nodesA.length - 1; i++) {
                        links[i] = nodesA[i].link(to(nodesA[i + 1]).with(Style.SOLID, Arrow.NONE));
                }
                for (int i = 0; i < nodesB.length - 1; i++) {
                        links[i + numNodes - 1] = nodesB[i].link(to(nodesB[i + 1]).with(Style.SOLID, Arrow.NONE));
                }

                // LinkSource[] links2 = new LinkSource[numNodes];

                // links2[0] = nodesA[0].link(to(nodesB[0]).with("style", "invis"));
                links[numNodes * 2 - 2] = nodesA[0].link(to(nodesB[0]).with("style", "invis"));

                System.out.println(nodesA.length);
                System.out.println(nodesB.length);
                // System.out.println(links2.length);

                for (int i = 1; i < nodesA.length; i++) {
                        links[i + numNodes * 2 - 2] = nodesA[i].link(to(nodesB[i]).with(Label.of(messages[i - 1])));
                        System.out.println(i);
                }

                /*
                 * for (int i = 1; i < nodesA.length; i++) {
                 * links2[i] = nodesA[i].link(to(nodesB[i]).with(Label.of(messages[i - 1])));
                 * System.out.println(i);
                 * }
                 */

                /*
                 * Link verticalLine = to(A2)
                 * .with(Style.SOLID, Arrow.NONE)
                 * .linkTo(A3)
                 * .with(Style.SOLID, Arrow.NONE);
                 */

                g = graph("twoColumnBackAndForth").directed()
                                .graphAttr().with(Attributes.attr("rankdir", "LR"), // use LR or TB for direction
                                                Attributes.attr("nodesep", ".5"), // spacing between nodes in same rank
                                                Attributes.attr("ranksep", "1.0") // spacing between ranks
                                )
                                .with(leftColumn, rightColumn)
                                .with(links);

                /*
                 * A1.link(to(B1).with("style", "invis")),
                 * B1.link(to(B2).with("style", "invis")),
                 * A2.link(to(B2).with(Label.of("Message 1"))),
                 * A2.link(to(A3).with("style", "invis")),
                 * B3.link(to(A3).with(Label.of("Message 2")))
                 */
                // A1.link(to(verticalLine))

        }

        public Graph getGraph() {
                return g;
        }
}