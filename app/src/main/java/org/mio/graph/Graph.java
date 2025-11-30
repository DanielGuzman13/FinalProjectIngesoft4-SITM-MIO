package org.mio.graph;

import org.mio.model.Arc;

import java.util.ArrayList;
import java.util.List;

public class Graph {

    private List<Arc> arcs = new ArrayList<>();

    public void addArc(Arc arc) {
        arcs.add(arc);
    }

    public List<Arc> getArcs() {
        return arcs;
    }
}
