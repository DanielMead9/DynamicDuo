package com.dynamicduo;

import java.io.File;

import  guru.nidi.graphviz.engine.*;
import  guru.nidi.graphviz.model.*;
import static guru.nidi.graphviz.model.Factory.*;


public class SVG {
    MutableGraph g;
    
    public SVG(){
                // Build graph structure
        g = mutGraph("example").setDirected(true)
            .add(mutNode("Start").addLink(mutNode("Process")))
            .add(mutNode("Process").addLink(mutNode("End")));

    }

    public MutableGraph getG(){
        return g;
    }
}
