package org.mio;

import org.mio.graph.Graph;
import org.mio.graph.GraphBuilder;
import org.mio.ui.AppWindow;

import javax.swing.*;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        runConsoleMode();

        launchGuiMode();
    }


    private static void launchGuiMode() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("No fue posible aplicar el estilo del sistema.");
            }
            new AppWindow().setVisible(true);
        });
    }


    private static void runConsoleMode() {
        GraphBuilder builder = new GraphBuilder();
        Graph graph = builder.build();

        System.out.println("\n=== ARCOS GENERADOS (ORDENADOS POR RUTA Y SECUENCIA) ===\n");

        List<org.mio.model.Arc> sortedArcs = new java.util.ArrayList<>(graph.getArcs());
        sortedArcs.sort(java.util.Comparator
                .comparingInt(org.mio.model.Arc::getLineId)
                .thenComparingInt(org.mio.model.Arc::getOrientation));

        sortedArcs.forEach(System.out::println);
    }
}
