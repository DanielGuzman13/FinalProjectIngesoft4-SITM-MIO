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
    private final List<WorkerNode> workers;
    private final BlockingQueue<Datagram> workQueue;
    private final Map<String, ArcSpeed> aggregatedResults;

    public MasterNode(org.mio.graph.Graph graph, int numWorkers) {
        this.graph = graph;
        this.numWorkers = numWorkers;
        this.executor = Executors.newFixedThreadPool(numWorkers);
        this.workers = new ArrayList<>();
        this.workQueue = new LinkedBlockingQueue<>();
        this.aggregatedResults = new ConcurrentHashMap<>();
    }

    public Map<String, ArcSpeed> processDatagrams(String csvFilePath) {
        System.out.println("Iniciando procesamiento de datagramas desde: " + csvFilePath);
        
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
        System.out.println("Datagramas procesados: " + totalDatagrams.get());
        System.out.println("Workers utilizados: " + numWorkers);
        System.out.println("Tiempo total: " + String.format("%.2f", processingTime) + " segundos");
        System.out.println("Rendimiento: " + String.format("%.0f", totalDatagrams.get() / processingTime) + " datagramas/segundo");
        System.out.println("Arcos con velocidad calculada: " + aggregatedResults.size());

        return new HashMap<>(aggregatedResults);
    }

    private void createWorkers() {
        for (int i = 0; i < numWorkers; i++) {
            workers.add(new WorkerNode(i, workQueue, graph));
        }
    }

    private void startWorkers() {
        for (WorkerNode worker : workers) {
            executor.submit(worker);
        }
    }

    private void stopWorkers() {
        for (WorkerNode worker : workers) {
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
        
        for (WorkerNode worker : workers) {
            Map<String, ArcSpeed> workerResults = worker.getResults();
            
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
