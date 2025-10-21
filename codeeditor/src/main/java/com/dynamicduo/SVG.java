package com.dynamicduo;

import java.io.File;

import  guru.nidi.graphviz.engine.*;
import  guru.nidi.graphviz.model.*;
import static guru.nidi.graphviz.model.Factory.*;
import guru.nidi.graphviz.attribute.*;


public class SVG {
    MutableGraph g;
    Graph g2;
    
    public SVG(){

            //Left column nodes
            Node A1 = node("Alice").with(Color.rgb("ADD8E6").fill());
            Node A2 = node("A2").with(Style.INVIS, Label.of(""), Shape.POINT);
            Node A3 = node("A3").with(Style.INVIS, Label.of(""), Shape.POINT);

            // Right column nodes
            Node B1 = node("B1").with(Color.rgb("90EE90").fill());
            Node B2 = node("B2").with(Style.INVIS, Label.of(""), Shape.POINT);
            Node B3 = node("B3").with(Style.INVIS, Label.of(""), Shape.POINT);

            Graph leftColumn = graph("left")
            .with(A1, A2, A3)
            .graphAttr().with("rank", "same"); // keep same column

            Graph rightColumn = graph("right")
            .with(B1, B2, B3)
            .graphAttr().with("rank", "same");

            g2 = graph("twoColumnBackAndForth").directed()
                .graphAttr().with("rankdir", "LR")
                .with(

                        leftColumn,
                        rightColumn,
                        A1.link(to(B1).with("style", "invis")),
                        B1.link(to(B2).with("style", "invis")),
                        A2.link(to(B2).with(Label.of("Message 1"))),
                        A2.link(to(A3).with("style", "invis")),
                        B3.link(to(A3).with(Label.of("Message 2")))
                );

    }

    public MutableGraph getG(){
        return g;
    }

    public graph getG2(){
        return g2;
    }
}