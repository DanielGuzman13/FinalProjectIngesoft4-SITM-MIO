package org.mio.processing;

import org.mio.graph.Graph;
import org.mio.graph.GraphBuilder;
import org.mio.model.ArcSpeed;

import java.util.Map;
import java.util.Scanner;

public class SpeedAnalysisService {
    
    public static void main(String[] args) {
        System.out.println("=== SITM-MIO - Sistema de Análisis de Velocidad Promedio ===\n");
        
        GraphBuilder graphBuilder = new GraphBuilder();
        Graph graph = graphBuilder.build();
        
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Ingrese la ruta del archivo de datagramas (default: C:\\Users\\default.LAPTOP-M81T5L1M\\Desktop\\ICESI2025II\\ingesoft 4\\dataset\\datagrams4history): ");
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
        
        MasterNode master = new MasterNode(graph, numWorkers);
        
        System.out.println("\nIniciando análisis con " + numWorkers + " workers...");
        System.out.println("Archivo: " + csvPath);
        System.out.println("Arcos en el grafo: " + graph.getArcs().size());
        System.out.println("Paradas en el sistema: " + graphBuilder.getStops().size());
        System.out.println("\n--- Procesando ---\n");
        
        long startTime = System.currentTimeMillis();
        Map<String, ArcSpeed> results = master.processDatagrams(csvPath);
        long endTime = System.currentTimeMillis();
        
        System.out.println("\nAnálisis completado en " + ((endTime - startTime) / 1000.0) + " segundos");
        
        master.printSpeedResults();
        
        System.out.println("\n=== ESTADÍSTICAS FINALES ===");
        System.out.println("Total arcos analizados: " + results.size());
        
        double avgSpeed = results.values().stream()
            .filter(arcSpeed -> arcSpeed.getSampleCount() >= 5)
            .mapToDouble(ArcSpeed::getAverageSpeed)
            .average()
            .orElse(0.0);
        
        System.out.println("Velocidad promedio general: " + String.format("%.2f", avgSpeed) + " km/h");
        
        long totalSamples = results.values().stream()
            .mapToLong(ArcSpeed::getSampleCount)
            .sum();
        
        System.out.println("Total muestras procesadas: " + totalSamples);
    }
}
