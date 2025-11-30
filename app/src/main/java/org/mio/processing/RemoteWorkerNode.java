package org.mio.processing;

import org.mio.model.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import java.net.*;

public class RemoteWorkerNode implements Runnable {
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

    public RemoteWorkerNode(int workerId, String masterIp, int masterPort, org.mio.graph.Graph graph) {
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
        System.out.println("Worker " + workerId + " (remoto) conectándose a " + masterIp + ":" + masterPort);
        
        try {
            // Conectar al Master
            socket = new Socket(masterIp, masterPort);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            
            // Enviar registro
            Map<String, Object> registration = new HashMap<>();
            registration.put("type", "REGISTER");
            registration.put("workerId", workerId);
            registration.put("address", getLocalAddress());
            out.writeObject(registration);
            out.flush();
            
            System.out.println("Worker " + workerId + " conectado desde " + getLocalAddress());
            
            // Procesar datagramas del Master
            while (running) {
                try {
                    Object message = in.readObject();
                    if (message instanceof Datagram) {
                        processDatagram((Datagram) message);
                    } else if (message instanceof String && "STOP".equals(message)) {
                        break;
                    }
                } catch (EOFException e) {
                    break; // Master cerró conexión
                }
            }
            
            // Enviar resultados al Master
            Map<String, Object> resultsMessage = new HashMap<>();
            resultsMessage.put("type", "RESULTS");
            resultsMessage.put("workerId", workerId);
            resultsMessage.put("results", results);
            out.writeObject(resultsMessage);
            out.flush();
            
        } catch (Exception e) {
            System.err.println("Error en Worker " + workerId + ": " + e.getMessage());
        } finally {
            cleanup();
            System.out.println("Worker " + workerId + " finalizado. Procesó " + results.size() + " arcos");
        }
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

    private String getLocalAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress() + ":808" + (workerId + 1);
        } catch (Exception e) {
            return "unknown:" + (8080 + workerId + 1);
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
