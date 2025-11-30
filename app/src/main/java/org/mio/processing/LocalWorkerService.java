package org.mio.processing;

import org.mio.graph.Graph;
import org.mio.graph.GraphBuilder;

public class LocalWorkerService {
    
    public static void main(String[] args) {
        System.out.println("=== SITM-MIO - WORKER LOCAL ===\n");
        
        if (args.length < 1) {
            System.err.println("Uso: LocalWorkerService <workerId>");
            System.err.println("Ejemplo: LocalWorkerService 1");
            System.exit(1);
        }
        
        int workerId;
        try {
            workerId = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Worker ID inválido: " + args[0]);
            System.exit(1);
            return;
        }
        
        System.out.println("=== WORKER " + workerId + " INICIADO ===");
        System.out.println("Rol: PROCESADOR DE DATAGRAMAS");
        System.out.println("Conectando al Master en localhost:8080...");
        
        try {
            // Cargar solo el grafo necesario para procesamiento (sin interfaz)
            GraphBuilder graphBuilder = new GraphBuilder();
            Graph graph = graphBuilder.build();
            System.out.println("Worker " + workerId + ": Grafo cargado - " + graph.getArcs().size() + " arcos");
            
            // Crear worker TCP que procesará datos
            LocalTcpWorkerNode worker = new LocalTcpWorkerNode(workerId, "localhost", 8080, graph);
            
            System.out.println("Worker " + workerId + ": Listo para procesar datagramas");
            System.out.println("Estado: ESPERANDO CONEXIÓN CON MASTER...");
            
            // Ejecutar worker
            Thread workerThread = new Thread(worker);
            workerThread.start();
            
            System.out.println("Worker " + workerId + ": Conectado al Master");
            System.out.println("Estado: PROCESANDO DATAGRAMAS...");
            System.out.println("Presiona Ctrl+C para detener el worker");
            
            // Mantener el worker corriendo
            workerThread.join();
            
        } catch (Exception e) {
            System.err.println("Error en Worker " + workerId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
