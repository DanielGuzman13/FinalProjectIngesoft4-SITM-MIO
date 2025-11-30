package org.mio.processing;

import org.mio.graph.Graph;
import org.mio.graph.GraphBuilder;

public class WorkerService {
    
    public static void main(String[] args) {
        System.out.println("=== SITM-MIO - Worker Service ===\n");
        
        if (args.length < 1) {
            System.err.println("Uso: WorkerService <masterPort>");
            System.err.println("Ejemplo: WorkerService 8080");
            System.exit(1);
        }
        
        int masterPort;
        try {
            masterPort = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Puerto inválido: " + args[0]);
            System.exit(1);
            return;
        }
        
        System.out.println("Iniciando Worker Service...");
        System.out.println("Conectando al Master en localhost:" + masterPort);
        
        try {
            // Cargar el grafo localmente
            GraphBuilder graphBuilder = new GraphBuilder();
            Graph graph = graphBuilder.build();
            
            // Crear y conectar worker remoto
            RemoteWorkerNode worker = new RemoteWorkerNode(0, "localhost", masterPort, graph);
            
            // Ejecutar worker en thread separado
            Thread workerThread = new Thread(worker);
            workerThread.start();
            
            System.out.println("Worker conectado y esperando trabajo del Master...");
            System.out.println("Presiona Ctrl+C para detener el worker");
            
            // Mantener el worker corriendo
            workerThread.join();
            
        } catch (Exception e) {
            System.err.println("Error en Worker Service: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
