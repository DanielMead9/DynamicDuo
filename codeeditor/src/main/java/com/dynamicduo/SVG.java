package com.dynamicduo;

import java.io.File;

import guru.nidi.graphviz.engine.*;
import guru.nidi.graphviz.model.*;
import static guru.nidi.graphviz.model.Factory.*;
import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.attribute.Rank.RankDir;

public class SVG {
        private Graph g;

        public SVG() {

                // Left column nodes
                Node A1 = node("Alice")
                                .with(Attributes.attr("width", "1"))
                                .with(Attributes.attr("height", "1"))
                                .with(Shape.UNDERLINE)
                                .with(Attributes.attr("fontsize", "20"));

                Node A2 = node("A2").with(Style.INVIS, Label.of(""), Shape.POINT);
                Node A3 = node("A3").with(Style.INVIS, Label.of(""), Shape.POINT);

                // Right column nodes
                Node B1 = node("Bob").with(Attributes.attr("width", "1"))
                                .with(Attributes.attr("height", "1"))
                                .with(Shape.UNDERLINE)
                                .with(Attributes.attr("fontsize", "20"));

                Node B2 = node("B2").with(Style.INVIS, Label.of(""), Shape.POINT);
                Node B3 = node("B3").with(Style.INVIS, Label.of(""), Shape.POINT);

                Graph leftColumn = graph("left")
                                .with(A1, A2, A3)
                                .graphAttr().with("rank", "same"); // keep same column

                Graph rightColumn = graph("right")
                                .with(B1, B2, B3)
                                .graphAttr().with("rank", "same");

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
                                .with(

                                                leftColumn, rightColumn,
                                                A1.link(to(B1).with("style", "invis")),
                                                B1.link(to(B2).with("style", "invis")),
                                                A2.link(to(B2).with(Label.of("Message 1"))),
                                                A2.link(to(A3).with("style", "invis")),
                                                B3.link(to(A3).with(Label.of("Message 2")))
                                // A1.link(to(verticalLine))
                                );

        }

        public Graph getGraph() {
                return g;
        }
}