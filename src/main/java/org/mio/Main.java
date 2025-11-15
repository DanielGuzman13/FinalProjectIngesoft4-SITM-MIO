package org.mio;

import org.mio.graph.Graph;
import org.mio.graph.GraphBuilder;
import org.mio.visual.GraphRenderer;

public class Main {
    public static void main(String[] args) {

        GraphBuilder builder = new GraphBuilder();
        Graph graph = builder.build();

        System.out.println("\n=== ARCOS GENERADOS ===\n");
        graph.getArcs().forEach(System.out::println);

        // === Generar el gráfico ===
        GraphRenderer renderer = new GraphRenderer();
        renderer.render(graph, "grafo-mio.jpg");

        System.out.println("\nImagen generada: grafo-mio.jpg");
    }
}
