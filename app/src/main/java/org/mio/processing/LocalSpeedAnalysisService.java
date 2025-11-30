package org.mio.processing;

import org.mio.graph.Graph;
import org.mio.graph.GraphBuilder;
import org.mio.model.ArcSpeed;

import java.util.Map;
import java.util.Scanner;

public class LocalSpeedAnalysisService {

    public static void main(String[] args) {
        System.out.println("=== SITM-MIO - Sistema Local de Análisis de Velocidad Promedio ===\n");

        GraphBuilder graphBuilder = new GraphBuilder();
        Graph graph = graphBuilder.build();

        Scanner scanner = new Scanner(System.in);

        System.out.print("Ingrese la ruta del archivo de datagramas (ej: datagrams4history.csv): ");
        String csvPath = scanner.nextLine().trim();

        if (csvPath.isEmpty()) {
            csvPath = "C:\\Users\\default.LAPTOP-M81T5L1M\\Desktop\\ICESI2025II\\ingesoft 4\\dataset\\datagrams4history";
        }

        System.out.print("Ingrese el número de workers a utilizar (default 4): ");
        int numWorkers = 4;
        try {
            String workersInput = scanner.nextLine().trim();
            if (!workersInput.isEmpty()) {
                numWorkers = Integer.parseInt(workersInput);
            }
        } catch (NumberFormatException e) {
            System.out.println("Usando valor default: 4 workers");
        }

        scanner.close();

        System.out.println("\n=== CONFIGURACIÓN LOCAL ===");
        System.out.println("Master Node: localhost (este computador)");
        System.out.println("Workers: " + numWorkers + " threads en el mismo proceso");
        System.out.println("Arquitectura: ThreadPool local");
        System.out.println("Archivo: " + csvPath);
        System.out.println("Arcos en el grafo: " + graph.getArcs().size());
        System.out.println("Paradas en el sistema: " + graphBuilder.getStops().size());
        System.out.println("\n--- Iniciando procesamiento local ---\n");

    }
}
