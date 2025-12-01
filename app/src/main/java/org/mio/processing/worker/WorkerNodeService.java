package org.mio.processing.worker;

import org.mio.graph.Graph;
import org.mio.graph.GraphBuilder;
import org.mio.model.*;
import org.mio.processing.config.WorkerConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import java.net.*;

public class WorkerNodeService implements Runnable {
    
    private final int workerId;
    private final String masterIp;
    private final int masterPort;
    private final Graph graph;
    private final Map<String, ArcSpeed> results;
    private final Map<String, List<Datagram>> busHistory;
    private volatile boolean running;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private long processedCount;

    public WorkerNodeService(int workerId, String masterIp, int masterPort, Graph graph) {
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
        System.out.println(String.format(WorkerConfig.WORKER_BANNER, workerId));
        System.out.println(WorkerConfig.WORKER_ROLE);
        System.out.println(String.format(WorkerConfig.CONNECTING_TO_MASTER, workerId, masterIp, masterPort));
        
        try {
            // Conectar al Master
            socket = new Socket(masterIp, masterPort);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            
            // Enviar registro
            Map<String, Object> registration = new HashMap<>();
            registration.put("type", "REGISTER");
            registration.put("workerId", workerId);
            registration.put("address", "localhost:" + (WorkerConfig.DEFAULT_MASTER_PORT + workerId));
            out.writeObject(registration);
            out.flush();
            
            System.out.println(String.format(WorkerConfig.CONNECTED_TO_MASTER, workerId));
            System.out.println(String.format(WorkerConfig.WAITING_FOR_DATAGRAMS, workerId));
            
            processedCount = 0;
            long lastReportTime = System.currentTimeMillis();
            
            // Procesar datagramas del Master
            while (running) {
                try {
                    Object message = in.readObject();
                    if (message instanceof Datagram) {
                        processDatagram((Datagram) message);
                        processedCount++;
                        
                        // Reporte cada N datagramas o cada T segundos
                        long currentTime = System.currentTimeMillis();
                        if (processedCount % WorkerConfig.PROGRESS_REPORT_INTERVAL == 0 || 
                            (currentTime - lastReportTime) > WorkerConfig.PROGRESS_TIME_INTERVAL_MS) {
                            System.out.println(String.format(WorkerConfig.PROCESSING_PROGRESS, 
                                workerId, processedCount, results.size(), getAverageSpeed()));
                            lastReportTime = currentTime;
                        }
                        
                    } else if (message instanceof String && "STOP".equals(message)) {
                        System.out.println(String.format(WorkerConfig.STOP_SIGNAL_RECEIVED, workerId));
                        break;
                    }
                } catch (EOFException e) {
                    System.out.println(String.format(WorkerConfig.DISCONNECTED, workerId));
                    break;
                }
            }
            
            System.out.println(String.format(WorkerConfig.PROCESSING_FINISHED, workerId));
            System.out.println(String.format(WorkerConfig.FINAL_STATS, workerId, processedCount, 
                workerId, results.size(), workerId, getAverageSpeed()));
            
            // Enviar resultados al Master
            System.out.println(String.format(WorkerConfig.SENDING_RESULTS, workerId));
            Map<String, Object> resultsMessage = new HashMap<>();
            resultsMessage.put("type", "RESULTS");
            resultsMessage.put("workerId", workerId);
            resultsMessage.put("results", results);
            out.writeObject(resultsMessage);
            out.flush();
            
            System.out.println(String.format(WorkerConfig.RESULTS_SENT, workerId));
            
        } catch (Exception e) {
            System.err.println(String.format(WorkerConfig.CONNECTION_ERROR, workerId, e.getMessage()));
            e.printStackTrace();
        } finally {
            cleanup();
            System.out.println(String.format(WorkerConfig.WORKER_FINISHED, workerId));
        }
    }
    
    private double getAverageSpeed() {
        return results.values().stream()
            .filter(arcSpeed -> arcSpeed.getSampleCount() >= WorkerConfig.MIN_SAMPLES_FOR_AVERAGE)
            .mapToDouble(ArcSpeed::getAverageSpeed)
            .average()
            .orElse(0.0);
    }

    private void processDatagram(Datagram datagram) {
        // El eventType en el CSV es "0", no "GPS_POSITION"
        if (!"0".equals(datagram.getEventType())) {
            return;
        }

        String busKey = datagram.getBusId();
        busHistory.computeIfAbsent(busKey, k -> new ArrayList<>()).add(datagram);
        
        List<Datagram> history = busHistory.get(busKey);
        if (history.size() < 2) {
            return;
        }

        Datagram previous = history.get(history.size() - 2);
        double speed = calculateSpeedSimple(previous, datagram);
        
        // Logging para depuración
        if (processedCount % 50 == 0) {
            System.out.println(String.format("Worker %d: Procesados %d datagramas | Velocidad actual: %.2f km/h | Arcos con velocidad: %d", 
                workerId, processedCount, speed, results.size()));
        }
        
        if (speed > WorkerConfig.MIN_SPEED_THRESHOLD && speed < WorkerConfig.MAX_SPEED_THRESHOLD) {
            Arc arc = findArcForDatagramSimple(graph, datagram, history);
            if (arc != null) {
                String arcKey = arc.getFrom().getStopId() + "-" + arc.getTo().getStopId() + "-" + arc.getLineId();
                
                results.computeIfAbsent(arcKey, k -> new ArcSpeed(arc)).addSpeedSample(speed);
                
                // Mostrar cuando se encuentra un arco
                if (results.size() % 10 == 0) {
                    System.out.println(String.format("Worker %d: ¡Arco encontrado! %s - Velocidad: %.2f km/h", 
                        workerId, arcKey, speed));
                }
            } else {
                // Mostrar cuando no se encuentra arco (solo algunas veces para no saturar)
                if (processedCount % 100 == 0) {
                    System.out.println(String.format("Worker %d: Sin arco para datagrama del bus %s en (%.6f, %.6f)", 
                        workerId, busKey, datagram.getLatitude(), datagram.getLongitude()));
                }
            }
        }

        if (history.size() > WorkerConfig.MAX_HISTORY_SIZE) {
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
    
    public static void main(String[] args) {
        System.out.println(String.format(WorkerConfig.WORKER_BANNER, 
            args.length > 0 ? Integer.parseInt(args[0]) : 1));
        
        if (args.length < 1) {
            System.err.println("Uso: WorkerNodeService <workerId> [masterIp] [masterPort]");
            System.err.println("Ejemplo: WorkerNodeService 1 localhost 8080");
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
        
        String masterIp = args.length > 1 ? args[1] : WorkerConfig.DEFAULT_MASTER_IP;
        int masterPort = args.length > 2 ? Integer.parseInt(args[1]) : WorkerConfig.DEFAULT_MASTER_PORT;
        
        try {
            System.out.println(String.format(WorkerConfig.LOADING_GRAPH, workerId));
            GraphBuilder graphBuilder = new GraphBuilder();
            Graph graph = graphBuilder.build();
            
            System.out.println(String.format(WorkerConfig.GRAPH_LOADED, workerId, graph.getArcs().size()));
            System.out.println(String.format(WorkerConfig.READY_TO_PROCESS, workerId));
            System.out.println(WorkerConfig.WAITING_FOR_MASTER);
            System.out.println(String.format(WorkerConfig.PROCESSING_STARTED, workerId));
            
            WorkerNodeService worker = new WorkerNodeService(workerId, masterIp, masterPort, graph);
            Thread workerThread = new Thread(worker);
            workerThread.start();
            
            System.out.println("Worker " + workerId + ": Presiona Ctrl+C para detener el worker");
            
            workerThread.join();
            
        } catch (Exception e) {
            System.err.println(String.format(WorkerConfig.PROCESSING_ERROR, workerId, e.getMessage()));
            e.printStackTrace();
        }
    }
    
    /**
     * Calcula velocidad simplificada entre dos datagramas
     */
    private double calculateSpeedSimple(Datagram previous, Datagram current) {
        if (previous == null || current == null) {
            return 0.0;
        }
        
        // Calcular distancia simplificada
        double distance = calculateDistanceSimple(
            previous.getLatitude(), previous.getLongitude(),
            current.getLatitude(), current.getLongitude()
        );
        
        // Calcular tiempo en segundos (simplificado)
        double timeDiff = 1.0; // Asumir 1 segundo de diferencia
        
        if (timeDiff <= 0) {
            return 0.0;
        }
        
        // Velocidad en km/h
        double speedKmh = (distance / 1000.0) / (timeDiff / 3600.0);
        
        // Limitar a valores realistas
        return Math.max(0.0, Math.min(120.0, speedKmh));
    }
    
    /**
     * Encuentra arco simplificado para un datagrama
     */
    private Arc findArcForDatagramSimple(Graph graph, Datagram datagram, List<Datagram> history) {
        if (graph != null && graph.getArcs() != null) {
            for (Arc arc : graph.getArcs()) {
                if (isNearArcSimple(datagram, arc)) {
                    return arc;
                }
            }
        }
        return null;
    }
    
    /**
     * Calcula distancia simplificada
     */
    private double calculateDistanceSimple(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // Radio de la Tierra en metros
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    /**
     * Verifica si un datagrama está cerca de un arco
     */
    private boolean isNearArcSimple(Datagram datagram, Arc arc) {
        if (datagram == null || arc == null) {
            return false;
        }
        
        if (arc.getFrom() != null && arc.getTo() != null) {
            double distanceFromStart = calculateDistanceSimple(
                datagram.getLatitude(), datagram.getLongitude(),
                arc.getFrom().getLat(), arc.getFrom().getLon()
            );
            
            double distanceFromEnd = calculateDistanceSimple(
                datagram.getLatitude(), datagram.getLongitude(),
                arc.getTo().getLat(), arc.getTo().getLon()
            );
            
            return distanceFromStart < 100 || distanceFromEnd < 100;
        }
        
        return false;
    }
    
    /**
     * Calcula diferencia de tiempo entre timestamps en formato string
     */
    private double calculateTimeDifference(String timestamp1, String timestamp2) {
        try {
            // Simplificado: asumir diferencia de 1 segundo si no se puede parsear
            return 1.0;
        } catch (Exception e) {
            return 1.0;
        }
    }
}
