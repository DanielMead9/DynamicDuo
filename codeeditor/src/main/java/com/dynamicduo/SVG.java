package com.dynamicduo;

import java.io.File;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Label;
import  guru.nidi.graphviz.engine.*;
import  guru.nidi.graphviz.model.*;
import static guru.nidi.graphviz.model.Factory.*;


public class SVG {
    MutableGraph g;
    Graph g2;
    
    public SVG(){
                // Build graph structure
        /*g = mutGraph("example").setDirected(true)
            .add(mutNode("Start").addLink(mutNode("Process")))
            .add(mutNode("Process").addLink(mutNode("End")));*/
        /* 
            MutableNode A1 = mutNode("A1").add(Label.of("A1"));
            MutableNode A2 = mutNode("A1").add(Label.of("A1"));
            MutableNode B1 = mutNode("A1").add(Label.of("A1"));
            MutableNode B2 = mutNode("A1").add(Label.of("A1"));

            g = mutGraph("two_columns").setDirected(true)
            .graphAttr().with("rankdir", "LR")
            .with(
                A1.addLink(B1),
                B2.addLink(A2)
            );

            */

            Node A1 = node("A1").with(Color.rgb("ADD8E6").fill());
            Node A2 = node("A2").with(Color.rgb("ADD8E6").fill());
            Node A3 = node("A3").with(Color.rgb("ADD8E6").fill());

            // Right column nodes
            Node B1 = node("B1").with(Color.rgb("90EE90").fill());
            Node B2 = node("B2").with(Color.rgb("90EE90").fill());
            Node B3 = node("B3").with(Color.rgb("90EE90").fill());

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
                        A1.link(to(B1).with(Label.of("step 1"))),
                        B1.link(to(A2).with(Label.of("step 2"))),
                        A2.link(to(B2).with(Label.of("step 3"))),
                        B2.link(to(A3).with(Label.of("step 4"))),
                        A3.link(to(B3).with(Label.of("step 5")))
                );

    }

    public MutableGraph getG(){
        return g;
    }

    public graph getG2(){
        return g2;
    }
}