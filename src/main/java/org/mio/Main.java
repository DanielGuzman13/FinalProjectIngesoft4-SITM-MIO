package org.mio;

import org.mio.graph.Graph;
import org.mio.graph.GraphBuilder;

public class Main {
    public static void main(String[] args) {

        GraphBuilder builder = new GraphBuilder();
        Graph graph = builder.build();

        System.out.println("\n=== ARCOS GENERADOS ===\n");
        graph.getArcs().forEach(System.out::println);
    }
}
