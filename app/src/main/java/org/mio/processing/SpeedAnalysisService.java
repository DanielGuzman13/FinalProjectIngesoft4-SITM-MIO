package org.mio.processing;

import org.mio.graph.Graph;
import org.mio.graph.GraphBuilder;
import org.mio.model.ArcSpeed;

import java.util.Map;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

public class SpeedAnalysisService {
    
    public static void main(String[] args) {
        System.out.println("=== SITM-MIO - Sistema de Análisis de Velocidad Promedio ===\n");
        
        GraphBuilder graphBuilder = new GraphBuilder();
        Graph graph = graphBuilder.build();
        
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("¿Modo de ejecución? (LOCAL/DISTRIBUIDO): ");
        String mode = scanner.nextLine().trim().toUpperCase();
        
        boolean distributedMode = "DISTRIBUIDO".equals(mode);
        
        System.out.print("Ingrese la ruta del archivo de datagramas (ej: datagrams4history.csv): ");
        String csvPath = scanner.nextLine().trim();
        
        if (csvPath.isEmpty()) {
            csvPath = "C:\\Users\\default.LAPTOP-M81T5L1M\\Desktop\\ICESI2025II\\ingesoft 4\\dataset\\datagrams4history";
        }
        
        MasterNode master;
        
        if (distributedMode) {
            System.out.println("\n=== CONFIGURACIÓN MODO DISTRIBUIDO ===");
            System.out.print("Puerto del Master Node (default 8080): ");
            int masterPort = 8080;
            try {
                String portInput = scanner.nextLine().trim();
                if (!portInput.isEmpty()) {
                    masterPort = Integer.parseInt(portInput);
                }
            } catch (NumberFormatException e) {
                System.out.println("Usando puerto default: 8080");
            }
            
            System.out.print("Número de workers remotos: ");
            int numWorkers = 0;
            try {
                numWorkers = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Número inválido. Saliendo...");
                scanner.close();
                return;
            }
            
            List<String> workerAddresses = new ArrayList<>();
            for (int i = 0; i < numWorkers; i++) {
                System.out.print("IP:Puerto del Worker " + (i + 1) + " (ej: 192.168.1.100:8081): ");
                String address = scanner.nextLine().trim();
                if (!address.isEmpty()) {
                    workerAddresses.add(address);
                }
            }
            
            master = new MasterNode(graph, numWorkers, true, masterPort, workerAddresses);
            
            System.out.println("\n=== INSTRUCCIONES PARA WORKERS REMOTOS ===");
            System.out.println("1. Copia el proyecto a cada computador worker");
            System.out.println("2. Ejecuta en cada worker:");
            System.out.println("   ./gradlew run --args=\"org.mio.processing.WorkerService " + masterPort + "\"");
            System.out.println("3. Los workers se conectarán automáticamente al Master");
            System.out.println("4. Presiona Enter para iniciar el procesamiento...");
            scanner.nextLine();
            
        } else {
            System.out.print("Ingrese el número de workers locales (default 4): ");
            int numWorkers = 4;
            try {
                String workersInput = scanner.nextLine().trim();
                if (!workersInput.isEmpty()) {
                    numWorkers = Integer.parseInt(workersInput);
                }
            } catch (NumberFormatException e) {
                System.out.println("Usando valor default: 4 workers");
            }
            
            master = new MasterNode(graph, numWorkers);
        }
        
        scanner.close();
        
        System.out.println("\n=== INICIANDO PROCESAMIENTO ===");
        System.out.println("Modo: " + (distributedMode ? "DISTRIBUIDO" : "LOCAL"));
        System.out.println("Archivo: " + csvPath);
        System.out.println("Arcos en el grafo: " + graph.getArcs().size());
        System.out.println("Paradas en el sistema: " + graphBuilder.getStops().size());
        System.out.println("\n--- Procesando ---\n");
        
        long startTime = System.currentTimeMillis();
        Map<String, ArcSpeed> results = master.processDatagrams(csvPath);
        long endTime = System.currentTimeMillis();
        
        System.out.println("\nAnálisis completado en " + ((endTime - startTime) / 1000.0) + " segundos");
        
        master.printSpeedResults();
        
        System.out.println("\n=== ESTADISTICAS FINALES ===");
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
        
        if (distributedMode) {
            System.out.println("\n=== CONFIGURACIÓN PARA PRÓXIMA EJECUCIÓN ===");
            System.out.println("Para configurar workers en otros computadores:");
            System.out.println("1. Asegura que los computadores estén en la misma red");
            System.out.println("2. Configura firewall para permitir conexiones TCP");
            System.out.println("3. Usa las IPs y puertos que configuraste arriba");
            System.out.println("4. Los workers deben ejecutarse antes que el Master");
        }
    }
}
