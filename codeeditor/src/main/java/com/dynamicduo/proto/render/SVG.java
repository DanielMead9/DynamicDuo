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

package com.dynamicduo.proto.render;

import guru.nidi.graphviz.model.*;
import static guru.nidi.graphviz.model.Factory.*;
import guru.nidi.graphviz.attribute.*;

public class SVG {
        private Graph g;

        public SVG(int numNodes, String p1, String p2, String[] messages, String[] passer) {

                // Arrays to contain node groups
                Node[] nodesA = new Node[numNodes];
                Node[] nodesB = new Node[numNodes];

                // Set up base node one
                Node A1 = node(p1)
                                .with(Attributes.attr("width", "1"))
                                .with(Attributes.attr("height", "1"))
                                .with(Shape.UNDERLINE)
                                .with(Attributes.attr("fontsize", "20"));
                nodesA[0] = A1;

                // Set up base node two
                Node B1 = node(p2).with(Attributes.attr("width", "1"))
                                .with(Attributes.attr("height", "1"))
                                .with(Shape.UNDERLINE)
                                .with(Attributes.attr("fontsize", "20"));
                nodesB[0] = B1;

                // set up all other nodes
                for (int i = 1; i < numNodes; i++) {
                        nodesA[i] = node("A" + (i + 1))
                                        .with(Style.INVIS, Label.of(""), Shape.POINT);
                        nodesB[i] = node("B" + (i + 1))
                                        .with(Style.INVIS, Label.of(""), Shape.POINT);
                }

                // Create the left column of nodes
                Graph leftColumn = graph("left")
                                .with(nodesA)
                                .graphAttr().with("rank", "same");

                // Create the right column of nodes
                Graph rightColumn = graph("right")
                                .with(nodesB)
                                .graphAttr().with("rank", "same");

                // Array to store all link connections
                LinkSource[] links = new LinkSource[numNodes * 3 - 2];

                // Create all links other than Header link
                for (int i = 0; i < nodesA.length - 1; i++) {
                        // Create left dotted line
                        links[i] = nodesA[i].link(to(nodesA[i + 1]).with(Style.DOTTED, Arrow.NONE));

                        // Create right dotted line
                        links[i + numNodes - 1] = nodesB[i].link(to(nodesB[i + 1]).with(Style.DOTTED, Arrow.NONE));

                        // Create links across graph dependent on which principle is passing the message
                        if (passer[i].equals(p1))
                                links[i + numNodes * 2 - 1] = nodesA[i + 1]
                                                .link(to(nodesB[i + 1]).with(Label.of(wrapLabel(messages[i]))));
                        else
                                links[i + numNodes * 2 - 1] = nodesB[i + 1]
                                                .link(to(nodesA[i + 1]).with(Label.of(wrapLabel(messages[i]))));
                }

                // Create invisible header link to ensure that the columns stay where they
                // should
                links[numNodes * 2 - 2] = nodesA[0].link(to(nodesB[0]).with("style", "invis"));

                // Create Directed graph
                g = graph("twoColumnBackAndForth").directed()
                                .graphAttr().with(Attributes.attr("rankdir", "LR"), // makes the graph go left to right
                                                Attributes.attr("nodesep", ".5"), // spacing between nodes in same rank
                                                Attributes.attr("ranksep", "1.0") // spacing between ranks
                                )
                                .with(leftColumn, rightColumn) // adds the node columns with ranks
                                .with(links); // adds all of the links

        }

        public Graph getGraph() {
                return g;
        }

        // This code is based on code from ChatGPT
        // This method is used to make the text stack if it exceeds a certain length
        public static String wrapLabel(String text) {
                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (String word : text.split(" ")) {
                        if (count + word.length() > 75) {
                                sb.append("\\n");
                                count = 0;
                        }
                        sb.append(word).append(" ");
                        count += word.length() + 1;
                }
                return sb.toString().trim();
        }
}