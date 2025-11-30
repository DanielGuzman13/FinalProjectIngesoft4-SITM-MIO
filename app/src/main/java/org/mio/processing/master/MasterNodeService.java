package org.mio.processing.master;

import org.mio.graph.Graph;
import org.mio.graph.GraphBuilder;
import org.mio.model.ArcSpeed;
import org.mio.model.Datagram;
import org.mio.util.BatchCsvReader;
import org.mio.processing.config.MasterConfig;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.net.*;
import java.io.*;

public class MasterNodeService {
    
    private final Graph graph;
    private final int masterPort;
    private final List<WorkerConnection> workers;
    private final Map<String, ArcSpeed> aggregatedResults;
    private final ExecutorService executor;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public MasterNodeService(Graph graph, int masterPort) {
        this.graph = graph;
        this.masterPort = masterPort;
        this.workers = new ArrayList<>();
        this.aggregatedResults = new ConcurrentHashMap<>();
        this.executor = Executors.newFixedThreadPool(10);
        this.running = true;
    }

    public void start(String csvFilePath) throws IOException {
        // Mostrar banner del Master
        System.out.println(MasterConfig.MASTER_BANNER);
        System.out.println(MasterConfig.MASTER_ROLE);
        System.out.println("Puerto: " + masterPort);
        System.out.println(MasterConfig.SERVER_STARTING);
        
        serverSocket = new ServerSocket(masterPort);
        
        // Thread para aceptar conexiones de workers
        Thread acceptThread = new Thread(() -> {
            while (running) {
                try {
                    Socket workerSocket = serverSocket.accept();
                    WorkerConnection worker = new WorkerConnection(workerSocket, this);
                    workers.add(worker);
                    executor.submit(worker);
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error aceptando conexión: " + e.getMessage());
                    }
                }
            }
        });
        
        acceptThread.start();
        
        System.out.println(MasterConfig.SERVER_STARTED);
        System.out.println(MasterConfig.WAITING_WORKERS);
        System.out.println("Master: Workers conectados: " + workers.size() + "/" + MasterConfig.EXPECTED_WORKERS);
        
        // Esperar a que todos los workers se conecten
        while (workers.size() < MasterConfig.EXPECTED_WORKERS) {
            try {
                Thread.sleep(1000);
                System.out.println("Master: Workers conectados: " + workers.size() + "/" + MasterConfig.EXPECTED_WORKERS + " (esperando...)");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println(MasterConfig.ALL_WORKERS_CONNECTED);
        System.out.println(MasterConfig.START_PROCESSING);
        
        // Esperar a que el usuario presione Enter
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        scanner.close();
        
        // Iniciar procesamiento
        processDatagrams(csvFilePath);
    }

    private void processDatagrams(String csvFilePath) {
        // Limpiar la ruta: remover comillas si existen
        csvFilePath = csvFilePath.replace("\"", "").trim();
        final String finalCsvPath = csvFilePath; // Para lambda
        
        System.out.println(MasterConfig.PROCESSING_BANNER);
        System.out.println(MasterConfig.LOADING_GRAPH);
        
        System.out.println("Master: Workers activos: " + workers.size());
        System.out.println("Master: Archivo de datos: " + csvFilePath);
        System.out.println("Master: Arcos en grafo: " + graph.getArcs().size());
        System.out.println(MasterConfig.LOADING_DATAGRAMS);
        
        long startTime = System.currentTimeMillis();
        AtomicLong totalDatagrams = new AtomicLong(0);
        AtomicLong distributedCount = new AtomicLong(0);

        // Cargar datagramas y distribuir a workers usando procesamiento por lotes
        Thread producerThread = new Thread(() -> {
            try {
                loadDatagramsInBatches(finalCsvPath, 1000); // Empezar con 1000 datagramas
                System.out.println("Master: ✓ Procesamiento por lotes completado");
            } catch (Exception e) {
                System.err.println("Master: Error cargando datagramas: " + e.getMessage());
                e.printStackTrace();
            }
        });

        producerThread.start();

        // Monitorear distribución en tiempo real
        Thread monitorThread = new Thread(() -> {
            while (distributedCount.get() < totalDatagrams.get() || totalDatagrams.get() == 0) {
                try {
                    Thread.sleep(MasterConfig.MONITOR_INTERVAL_MS);
                    if (totalDatagrams.get() > 0) {
                        double percentage = (distributedCount.get() * 100.0) / totalDatagrams.get();
                        System.out.println(String.format(MasterConfig.DISTRIBUTION_PROGRESS, 
                            distributedCount.get(), totalDatagrams.get(), percentage));
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        
        monitorThread.start();

        try {
            producerThread.join();
            monitorThread.interrupt();
            
            System.out.println(MasterConfig.DISTRIBUTION_COMPLETE);
            System.out.println(MasterConfig.WAITING_RESULTS);
            
            // Esperar a que todos los workers terminen
            while (workers.stream().anyMatch(w -> w.isProcessing())) {
                Thread.sleep(1000);
                System.out.println("Master: Workers procesando... (" + 
                                 workers.stream().mapToInt(w -> w.getResults().size()).sum() + " arcos calculados hasta ahora)");
            }

            System.out.println(MasterConfig.SENDING_STOP_SIGNAL);
            for (WorkerConnection worker : workers) {
                worker.sendStop();
            }

            Thread.sleep(2000);
            System.out.println(MasterConfig.PROCESSING_COMPLETE);
            System.out.println(MasterConfig.AGGREGATING_RESULTS);
            
            aggregateResults();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.currentTimeMillis();
        double processingTime = (endTime - startTime) / 1000.0;
        
        System.out.println(MasterConfig.RESULTS_BANNER);
        System.out.println(String.format(MasterConfig.FINAL_SUMMARY, 
            workers.size(), totalDatagrams.get(), processingTime, 
            totalDatagrams.get() / processingTime, aggregatedResults.size()));

        printSpeedResults();
        shutdown();
    }

    private void loadDatagramsInBatches(String filePath, int batchSize) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        AtomicLong totalProcessed = new AtomicLong(0);
        AtomicBoolean stopProcessing = new AtomicBoolean(false);
        
        System.out.println("Master: Iniciando procesamiento por lotes de " + batchSize + " datagramas...");
        
        BatchCsvReader.readInBatches(filePath, batchSize, batch -> {
            // Detenerse después de procesar el primer lote con datos
            if (stopProcessing.get()) {
                return;
            }
            
            // Procesar y distribuir este lote
            List<Datagram> datagrams = new ArrayList<>();
            int validRows = 0;
            int errorRows = 0;
            
            for (String[] row : batch) {
                try {
                    // Formato real: eventType, registerdate, stopId, odometer, latitude, longitude, taskId, lineId, tripId, unknown1, datagramDate, busId
                    if (row.length < 12) {
                        errorRows++;
                        continue;
                    }
                    
                    // Extraer datos según el formato real
                    String eventType = row[0].replace("\"", "").trim();
                    String registerDate = row[1].replace("\"", "").trim();
                    String stopId = row[2].replace("\"", "").trim();
                    String odometer = row[3].replace("\"", "").trim();
                    String latitudeStr = row[4].replace("\"", "").trim();
                    String longitudeStr = row[5].replace("\"", "").trim();
                    String taskId = row[6].replace("\"", "").trim();
                    String lineIdStr = row[7].replace("\"", "").trim();
                    String tripId = row[8].replace("\"", "").trim();
                    String unknown1 = row[9].replace("\"", "").trim();
                    String datagramDate = row[10].replace("\"", "").trim();
                    String busId = row[11].replace("\"", "").trim();
                    
                    // Convertir coordenadas (viene en formato -764873683, dividir por 10000000)
                    double latitude = Double.parseDouble(latitudeStr) / 10000000.0;
                    double longitude = Double.parseDouble(longitudeStr) / 10000000.0;
                    
                    // Convertir fecha (formato: 2019-05-27 20:14:43)
                    LocalDateTime timestamp = LocalDateTime.parse(datagramDate, formatter);
                    
                    // Convertir lineId
                    int lineId = Integer.parseInt(lineIdStr);
                    
                    // Determinar orientación (basado en taskId o algún otro criterio)
                    int orientation = 1; // Por defecto
                    
                    Datagram datagram = new Datagram(busId, latitude, longitude, timestamp, lineId, orientation, eventType);
                    datagrams.add(datagram);
                    validRows++;
                    
                } catch (Exception e) {
                    errorRows++;
                    // Mostrar primer error para depuración
                    if (errorRows == 1) {
                        System.err.println("Master: Error parsing datagram: " + e.getMessage());
                        System.err.println("Master: Fila problemática: " + String.join(",", batch.get(0)));
                    }
                }
            }
            
            // Mostrar estadísticas del lote
            System.out.println("Master: Estadísticas lote - Filas totales: " + batch.size() + 
                             ", Válidas: " + validRows + ", Errores: " + errorRows + 
                             ", Datagramas: " + datagrams.size());
            
            // Distribuir este lote a workers
            distributeBatchToWorkers(datagrams);
            totalProcessed.addAndGet(datagrams.size());
            
            System.out.println("Master: Lote procesado - " + datagrams.size() + " datagramas distribuidos (Total: " + totalProcessed.get() + ")");
            
            // Detenerse solo si encontramos datagramas válidos en este lote
            if (datagrams.size() > 0) {
                System.out.println("Master: ✓ Lote con datos encontrado - Enviando señal de parada a workers");
                sendStopToAllWorkers();
                stopProcessing.set(true);
            }
        });
        
        System.out.println("Master: ✓ Procesamiento por lotes completado - Total procesado: " + totalProcessed.get());
    }
    
    private void distributeBatchToWorkers(List<Datagram> datagrams) {
        if (workers.isEmpty()) return;
        
        int workerIndex = 0;
        for (WorkerConnection worker : workers) {
            if (worker != null && worker.isProcessing()) {
                // Distribuir parte del lote a este worker
                int batchSize = datagrams.size() / workers.size();
                int startIndex = workerIndex * batchSize;
                int endIndex = (workerIndex == workers.size() - 1) ? datagrams.size() : startIndex + batchSize;
                
                for (int i = startIndex; i < endIndex; i++) {
                    worker.sendDatagram(datagrams.get(i));
                }
                
                workerIndex++;
            }
        }
    }
    
    private void sendStopToAllWorkers() {
        for (WorkerConnection worker : workers) {
            if (worker != null) {
                worker.sendStop();
            }
        }
    }

    private void aggregateResults() {
        for (WorkerConnection worker : workers) {
            Map<String, ArcSpeed> workerResults = worker.getResults();
            
            System.out.println(String.format(MasterConfig.WORKER_RESULTS, 
                worker.getWorkerId(), workerResults.size()));
            
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

    public void addWorkerResults(int workerId, Map<String, ArcSpeed> results) {
        System.out.println("Master: Recibidos resultados de Worker " + workerId + " (" + results.size() + " arcos)");
    }

    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Ignorar errores de cierre
        }
    }

    public static void main(String[] args) {
        try {
            System.out.println(MasterConfig.LOADING_GRAPH);
            GraphBuilder graphBuilder = new GraphBuilder();
            Graph graph = graphBuilder.build();
            
            System.out.println(String.format(MasterConfig.GRAPH_LOADED, graph.getArcs().size()));
            
            // Usar la ruta por defecto directamente
            String csvPath = MasterConfig.DEFAULT_CSV_PATH;
            
            MasterNodeService master = new MasterNodeService(graph, MasterConfig.MASTER_PORT);
            master.start(csvPath);
            
        } catch (Exception e) {
            System.err.println("Error en Master: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
