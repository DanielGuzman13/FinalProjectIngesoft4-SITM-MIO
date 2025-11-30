package org.mio.processing;

import org.mio.model.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import java.net.*;

public class LocalTcpWorkerNode implements Runnable {
    private final int workerId;
    private final String masterIp;
    private final int masterPort;
    private final org.mio.graph.Graph graph;
    private final Map<String, ArcSpeed> results;
    private final Map<String, List<Datagram>> busHistory;
    private volatile boolean running;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public LocalTcpWorkerNode(int workerId, String masterIp, int masterPort, org.mio.graph.Graph graph) {
        this.workerId = workerId;
        this.masterIp = masterIp;
        this.masterPort = masterPort;
        this.graph = graph;
        this.results = new ConcurrentHashMap<>();
        this.busHistory = new ConcurrentHashMap<>();
        this.running = true;
    }

    @Override
    public void run() {
        System.out.println("=== WORKER " + workerId + " - PROCESAMIENTO INICIADO ===");
        System.out.println("Worker " + workerId + ": Conectando al Master en " + masterIp + ":" + masterPort);
        
        try {
            // Conectar al Master
            socket = new Socket(masterIp, masterPort);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            
            // Enviar registro
            Map<String, Object> registration = new HashMap<>();
            registration.put("type", "REGISTER");
            registration.put("workerId", workerId);
            registration.put("address", "localhost:" + (8080 + workerId));
            out.writeObject(registration);
            out.flush();
            
            System.out.println("Worker " + workerId + ": ✓ CONECTADO AL MASTER");
            System.out.println("Worker " + workerId + ": Esperando datagramas para procesar...");
            
            long processedCount = 0;
            long lastReportTime = System.currentTimeMillis();
            
            // Procesar datagramas del Master
            while (running) {
                try {
                    Object message = in.readObject();
                    if (message instanceof Datagram) {
                        processDatagram((Datagram) message);
                        processedCount++;
                        
                        // Reporte cada 5 segundos o cada 1000 datagramas
                        long currentTime = System.currentTimeMillis();
                        if (processedCount % 1000 == 0 || (currentTime - lastReportTime) > 5000) {
                            System.out.println("Worker " + workerId + ": Procesados " + processedCount + 
                                             " datagramas | Arcos con velocidad: " + results.size() + 
                                             " | Velocidad promedio: " + String.format("%.2f", getAverageSpeed()) + " km/h");
                            lastReportTime = currentTime;
                        }
                        
                    } else if (message instanceof String && "STOP".equals(message)) {
                        System.out.println("Worker " + workerId + ": ✓ SEÑAL DE PARADA RECIBIDA");
                        break;
                    }
                } catch (EOFException e) {
                    System.out.println("Worker " + workerId + ": Master cerró conexión");
                    break;
                }
            }
            
            System.out.println("Worker " + workerId + ": ✓ PROCESAMIENTO FINALIZADO");
            System.out.println("Worker " + workerId + ": Total datagramas procesados: " + processedCount);
            System.out.println("Worker " + workerId + ": Total arcos con velocidad: " + results.size());
            System.out.println("Worker " + workerId + ": Velocidad promedio general: " + String.format("%.2f", getAverageSpeed()) + " km/h");
            
            // Enviar resultados al Master
            System.out.println("Worker " + workerId + ": Enviando resultados al Master...");
            Map<String, Object> resultsMessage = new HashMap<>();
            resultsMessage.put("type", "RESULTS");
            resultsMessage.put("workerId", workerId);
            resultsMessage.put("results", results);
            out.writeObject(resultsMessage);
            out.flush();
            
            System.out.println("Worker " + workerId + ": ✓ RESULTADOS ENVIADOS AL MASTER");
            
        } catch (Exception e) {
            System.err.println("Error en Worker " + workerId + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
            System.out.println("=== WORKER " + workerId + " - FINALIZADO ===");
        }
    }
    
    private double getAverageSpeed() {
        return results.values().stream()
            .filter(arcSpeed -> arcSpeed.getSampleCount() >= 5)
            .mapToDouble(ArcSpeed::getAverageSpeed)
            .average()
            .orElse(0.0);
    }

    private void processDatagram(Datagram datagram) {
        if (!"GPS_POSITION".equals(datagram.getEventType())) {
            return;
        }

        String busKey = datagram.getBusId();
        busHistory.computeIfAbsent(busKey, k -> new ArrayList<>()).add(datagram);
        
        List<Datagram> history = busHistory.get(busKey);
        if (history.size() < 2) {
            return;
        }

        Datagram previous = history.get(history.size() - 2);
        double speed = SpeedCalculator.calculateSpeed(previous, datagram);
        
        if (speed > 0 && speed < 100) {
            Arc arc = SpeedCalculator.findArcForDatagram(graph, datagram, history);
            if (arc != null) {
                String arcKey = arc.getFrom().getStopId() + "-" + arc.getTo().getStopId() + "-" + arc.getLineId();
                
                results.computeIfAbsent(arcKey, k -> new ArcSpeed(arc)).addSpeedSample(speed);
            }
        }

        if (history.size() > 100) {
            history.remove(0);
        }
    }

    public Map<String, ArcSpeed> getResults() {
        return new HashMap<>(results);
    }

    public void stop() {
        running = false;
        cleanup();
    }

    private void cleanup() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            // Ignorar errores de cierre
        }
    }

    public int getProcessedCount() {
        return results.size();
    }
}
