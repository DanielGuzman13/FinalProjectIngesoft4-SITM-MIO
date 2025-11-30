package org.mio.processing;

import org.mio.model.*;
import org.mio.util.CsvReader;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class MasterNode {
    private final org.mio.graph.Graph graph;
    private final int numWorkers;
    private final ExecutorService executor;
    private final List<WorkerNode> localWorkers;
    private final List<RemoteWorkerNode> remoteWorkers;
    private final BlockingQueue<Datagram> workQueue;
    private final Map<String, ArcSpeed> aggregatedResults;
    
    // Configuración para modo distribuido
    private final boolean distributedMode;
    private final int masterPort;
    private final List<String> workerAddresses; // IP:Puerto de workers remotos

    public MasterNode(org.mio.graph.Graph graph, int numWorkers) {
        this(graph, numWorkers, false, 8080, Collections.emptyList());
    }
    
    public MasterNode(org.mio.graph.Graph graph, int numWorkers, boolean distributedMode, 
                     int masterPort, List<String> workerAddresses) {
        this.graph = graph;
        this.numWorkers = numWorkers;
        this.distributedMode = distributedMode;
        this.masterPort = masterPort;
        this.workerAddresses = workerAddresses;
        this.executor = Executors.newFixedThreadPool(numWorkers);
        this.localWorkers = new ArrayList<>();
        this.remoteWorkers = new ArrayList<>();
        this.workQueue = new LinkedBlockingQueue<>();
        this.aggregatedResults = new ConcurrentHashMap<>();
    }

    public Map<String, ArcSpeed> processDatagrams(String csvFilePath) {
        System.out.println("=== INICIANDO PROCESAMIENTO ===");
        if (distributedMode) {
            System.out.println("MODO: DISTRIBUIDO");
            System.out.println("Master Node: localhost:" + masterPort);
            System.out.println("Workers Remotos: " + workerAddresses.size());
            for (String addr : workerAddresses) {
                System.out.println("  - " + addr);
            }
        } else {
            System.out.println("MODO: LOCAL");
            System.out.println("Master Node: localhost (ThreadPool)");
            System.out.println("Workers: " + numWorkers + " threads locales");
        }
        System.out.println("Archivo: " + csvFilePath);
        System.out.println("Arcos en grafo: " + graph.getArcs().size());
        System.out.println();
        
        long startTime = System.currentTimeMillis();
        AtomicLong totalDatagrams = new AtomicLong(0);

        createWorkers();
        startWorkers();

        Thread producerThread = new Thread(() -> {
            try {
                int processedCount = loadDatagramsFromCSV(csvFilePath);
                totalDatagrams.set(processedCount);
                System.out.println("Carga completada: " + processedCount + " datagramas");
            } catch (Exception e) {
                System.err.println("Error cargando datagramas: " + e.getMessage());
                e.printStackTrace();
            }
        });

        producerThread.start();

        try {
            producerThread.join();
            
            while (!workQueue.isEmpty()) {
                Thread.sleep(100);
            }

            stopWorkers();
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        aggregateResults();
        
        long endTime = System.currentTimeMillis();
        double processingTime = (endTime - startTime) / 1000.0;
        
        System.out.println("\n=== RESUMEN DE PROCESAMIENTO ===");
        System.out.println("Modo: " + (distributedMode ? "DISTRIBUIDO" : "LOCAL"));
        System.out.println("Workers: " + (distributedMode ? workerAddresses.size() + " remotos" : numWorkers + " locales"));
        System.out.println("Datagramas procesados: " + totalDatagrams.get());
        System.out.println("Tiempo total: " + String.format("%.2f", processingTime) + " segundos");
        System.out.println("Rendimiento: " + String.format("%.0f", totalDatagrams.get() / processingTime) + " datagramas/segundo");
        System.out.println("Arcos con velocidad calculada: " + aggregatedResults.size());

        return new HashMap<>(aggregatedResults);
    }

    private void createWorkers() {
        if (distributedMode) {
            // En modo distribuido, crear workers remotos (TCP)
            for (int i = 0; i < workerAddresses.size(); i++) {
                String[] parts = workerAddresses.get(i).split(":");
                String ip = parts[0];
                int port = Integer.parseInt(parts[1]);
                remoteWorkers.add(new RemoteWorkerNode(i, ip, port, graph));
            }
        } else {
            // En modo local, crear workers con ThreadPool
            for (int i = 0; i < numWorkers; i++) {
                localWorkers.add(new WorkerNode(i, workQueue, graph));
            }
        }
    }

    private void startWorkers() {
        if (distributedMode) {
            // Iniciar workers remotos via TCP
            for (RemoteWorkerNode worker : remoteWorkers) {
                executor.submit(worker);
            }
        } else {
            // Iniciar workers locales
            for (WorkerNode worker : localWorkers) {
                executor.submit(worker);
            }
        }
    }

    private void stopWorkers() {
        for (WorkerNode worker : localWorkers) {
            worker.stop();
        }
        for (RemoteWorkerNode worker : remoteWorkers) {
            worker.stop();
        }
    }

    private int loadDatagramsFromCSV(String filePath) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        int count = 0;
        
        try {
            for (String[] row : CsvReader.read(filePath)) {
                try {
                    if (row.length < 12) continue;
                    
                    String busId = row[11].replace("\"", "").trim();
                    double latitude = Double.parseDouble(row[5].replace("\"", "").trim()) / 1000000.0;
                    double longitude = Double.parseDouble(row[6].replace("\"", "").trim()) / 1000000.0;
                    LocalDateTime timestamp = LocalDateTime.parse(row[12].replace("\"", "").trim(), formatter);
                    int lineId = Integer.parseInt(row[8].replace("\"", "").trim());
                    int orientation = row[7].equals("0") ? 1 : 2;
                    String eventType = "GPS_POSITION";
                    
                    Datagram datagram = new Datagram(busId, latitude, longitude, timestamp, lineId, orientation, eventType);
                    workQueue.offer(datagram);
                    
                    count++;
                    
                    if (count % 10000 == 0) {
                        System.out.println("Cargados " + count + " datagramas...");
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error procesando línea " + count + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error leyendo archivo CSV: " + e.getMessage());
            throw e;
        }
        
        return count;
    }

    private void aggregateResults() {
        aggregatedResults.clear();
        
        // Agregar resultados de workers locales
        for (WorkerNode worker : localWorkers) {
            Map<String, ArcSpeed> workerResults = worker.getResults();
            addWorkerResults(workerResults);
        }
        
        // Agregar resultados de workers remotos
        for (RemoteWorkerNode worker : remoteWorkers) {
            Map<String, ArcSpeed> workerResults = worker.getResults();
            addWorkerResults(workerResults);
        }
    }
    
    private void addWorkerResults(Map<String, ArcSpeed> workerResults) {
        for (Map.Entry<String, ArcSpeed> entry : workerResults.entrySet()) {
            String arcKey = entry.getKey();
            ArcSpeed workerSpeed = entry.getValue();
            
            aggregatedResults.computeIfAbsent(arcKey, k -> new ArcSpeed(workerSpeed.getArc()));
            
            ArcSpeed aggregated = aggregatedResults.get(arcKey);
            
            for (int i = 0; i < workerSpeed.getSampleCount(); i++) {
                aggregated.addSpeedSample(workerSpeed.getAverageSpeed());
            }
        }
    }

    public void printSpeedResults() {
        System.out.println("\n=== VELOCIDADES PROMEDIO POR ARCO ===\n");
        
        aggregatedResults.values().stream()
            .filter(arcSpeed -> arcSpeed.getSampleCount() >= 5)
            .sorted(Comparator.comparingDouble(ArcSpeed::getAverageSpeed).reversed())
            .limit(20)
            .forEach(arcSpeed -> {
                System.out.println(String.format("%-40s Vel: %6.2f km/h (muestras: %d, min: %5.2f, max: %5.2f)",
                    arcSpeed.getArc().toString(),
                    arcSpeed.getAverageSpeed(),
                    arcSpeed.getSampleCount(),
                    arcSpeed.getMinSpeed(),
                    arcSpeed.getMaxSpeed()
                ));
            });
    }
}
