package org.mio.processing.master;

import org.mio.model.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import java.net.*;

public class WorkerConnection implements Runnable {
    private final Socket socket;
    private final MasterNodeService master;
    private final Map<String, ArcSpeed> results;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private volatile boolean processing;
    private int workerId;

    public WorkerConnection(Socket socket, MasterNodeService master) throws IOException {
        this.socket = socket;
        this.master = master;
        this.results = new ConcurrentHashMap<>();
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
        this.processing = true;
    }

    @Override
    public void run() {
        try {
            // Esperar mensaje de registro
            Object message = in.readObject();
            if (message instanceof Map) {
                Map<String, Object> registration = (Map<String, Object>) message;
                if ("REGISTER".equals(registration.get("type"))) {
                    this.workerId = (Integer) registration.get("workerId");
                    String address = (String) registration.get("address");
                    
                    System.out.println("Master: Worker " + workerId + " conectado desde " + address);
                    
                    // Enviar confirmación
                    Map<String, Object> confirmation = new HashMap<>();
                    confirmation.put("type", "CONNECTED");
                    confirmation.put("workerId", workerId);
                    out.writeObject(confirmation);
                    out.flush();
                    
                    // Procesar mensajes del worker
                    while (processing) {
                        try {
                            message = in.readObject();
                            if (message instanceof Map) {
                                Map<String, Object> msg = (Map<String, Object>) message;
                                if ("RESULTS".equals(msg.get("type"))) {
                                    this.results.putAll((Map<String, ArcSpeed>) msg.get("results"));
                                    master.addWorkerResults(workerId, this.results);
                                    processing = false;
                                }
                            }
                        } catch (EOFException e) {
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error en conexión con Worker " + workerId + ": " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignorar
            }
        }
    }

    public void sendDatagram(Datagram datagram) {
        try {
            out.writeObject(datagram);
            out.flush();
        } catch (IOException e) {
            System.err.println("Error enviando datagrama a Worker " + workerId + ": " + e.getMessage());
        }
    }

    public void sendStop() {
        try {
            out.writeObject("STOP");
            out.flush();
        } catch (IOException e) {
            System.err.println("Error enviando STOP a Worker " + workerId + ": " + e.getMessage());
        }
    }

    public Map<String, ArcSpeed> getResults() {
        return new HashMap<>(results);
    }

    public int getWorkerId() {
        return workerId;
    }

    public boolean isProcessing() {
        return processing;
    }
}
