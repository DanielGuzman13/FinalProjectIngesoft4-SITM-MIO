package org.mio.processing;

import org.mio.graph.Graph;
import org.mio.graph.GraphBuilder;
import org.mio.model.ArcSpeed;
import org.mio.model.Datagram;
import org.mio.util.CsvReader;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.net.*;
import java.io.*;

public class LocalMasterService {
    
    private final Graph graph;
    private final int masterPort;
    private final List<WorkerConnection> workers;
    private final Map<String, ArcSpeed> aggregatedResults;
    private final BlockingQueue<Datagram> workQueue;
    private final ExecutorService executor;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public LocalMasterService(Graph graph, int masterPort) {
        this.graph = graph;
        this.masterPort = masterPort;
        this.workers = new ArrayList<>();
        this.aggregatedResults = new ConcurrentHashMap<>();
        this.workQueue = new LinkedBlockingQueue<>();
        this.executor = Executors.newFixedThreadPool(10);
        this.running = true;
    }

    public void start(String csvFilePath) throws IOException {
        System.out.println("==========================================");
        System.out.println("===     MASTER NODE - SITM-MIO        ===");
        System.out.println("==========================================");
        System.out.println("Rol: COORDINADOR DISTRIBUIDO");
        System.out.println("Puerto: " + masterPort);
        System.out.println("Estado: INICIANDO SERVIDOR TCP...");
        
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
        
        System.out.println("Master: ✓ SERVIDOR TCP INICIADO");
        System.out.println("Master: Esperando conexión de workers...");
        System.out.println("Master: Workers conectados: " + workers.size() + "/3");
        
        // Esperar a que todos los workers se conecten
        while (workers.size() < 3) {
            try {
                Thread.sleep(1000);
                System.out.println("Master: Workers conectados: " + workers.size() + "/3 (esperando...)");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println("Master: ✓ TODOS LOS WORKERS CONECTADOS");
        System.out.println("Master: Presiona Enter para iniciar el procesamiento distribuido...");
        
        // Esperar a que el usuario presione Enter
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        scanner.close();
        
        // Iniciar procesamiento
        processDatagrams(csvFilePath);
    }

    private void processDatagrams(String csvFilePath) {
        System.out.println("\n==========================================");
        System.out.println("===  INICIANDO PROCESAMIENTO DISTRIBUIDO ===");
        System.out.println("==========================================");
        System.out.println("Master: Workers activos: " + workers.size());
        System.out.println("Master: Archivo de datos: " + csvFilePath);
        System.out.println("Master: Arcos en grafo: " + graph.getArcs().size());
        System.out.println("Master: Iniciando carga y distribución de datagramas...");
        
        long startTime = System.currentTimeMillis();
        AtomicLong totalDatagrams = new AtomicLong(0);
        AtomicLong distributedCount = new AtomicLong(0);

        // Cargar datagramas y distribuir a workers
        Thread producerThread = new Thread(() -> {
            try {
                int processedCount = loadDatagramsFromCSV(csvFilePath);
                totalDatagrams.set(processedCount);
                System.out.println("Master: ✓ CARGA COMPLETADA - " + processedCount + " datagramas");
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
                    Thread.sleep(2000);
                    if (totalDatagrams.get() > 0) {
                        double percentage = (distributedCount.get() * 100.0) / totalDatagrams.get();
                        System.out.println("Master: Progreso distribución: " + distributedCount.get() + 
                                         "/" + totalDatagrams.get() + " (" + String.format("%.1f", percentage) + "%)");
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
            
            System.out.println("Master: ✓ DISTRIBUCIÓN FINALIZADA");
            System.out.println("Master: Esperando resultados de workers...");
            
            // Esperar a que todos los workers terminen
            while (!workQueue.isEmpty() || workers.stream().anyMatch(w -> w.isProcessing())) {
                Thread.sleep(1000);
                System.out.println("Master: Workers procesando... (" + 
                                 workers.stream().mapToInt(w -> w.getResults().size()).sum() + " arcos calculados hasta ahora)");
            }

            // Enviar señal de parada a todos los workers
            System.out.println("Master: Enviando señal de parada a workers...");
            for (WorkerConnection worker : workers) {
                worker.sendStop();
            }

            // Esperar resultados finales
            Thread.sleep(2000);
            
            System.out.println("Master: ✓ PROCESAMIENTO DISTRIBUIDO FINALIZADO");
            System.out.println("Master: Agregando resultados de todos los workers...");
            
            // Agregar resultados de todos los workers
            aggregateResults();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.currentTimeMillis();
        double processingTime = (endTime - startTime) / 1000.0;
        
        System.out.println("\n==========================================");
        System.out.println("===     RESULTADOS DISTRIBUIDOS        ===");
        System.out.println("==========================================");
        System.out.println("Master: Workers activos: " + workers.size() + " (TCP distribuido)");
        System.out.println("Master: Datagramas procesados: " + totalDatagrams.get());
        System.out.println("Master: Tiempo total: " + String.format("%.2f", processingTime) + " segundos");
        System.out.println("Master: Rendimiento: " + String.format("%.0f", totalDatagrams.get() / processingTime) + " datagramas/segundo");
        System.out.println("Master: Arcos con velocidad calculada: " + aggregatedResults.size());

        printSpeedResults();
        
        shutdown();
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
                    
                    // Distribuir datagrama a workers en round-robin
                    if (!workers.isEmpty()) {
                        WorkerConnection worker = workers.get(count % workers.size());
                        worker.sendDatagram(datagram);
                    }
                    
                    count++;
                    
                    if (count % 10000 == 0) {
                        System.out.println("Master: Cargados y distribuidos " + count + " datagramas");
                    }
                    
                } catch (Exception e) {
                    System.err.println("Master: Error procesando línea " + count + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Master: Error leyendo archivo CSV: " + e.getMessage());
            throw e;
        }
        
        return count;
    }

    private void aggregateResults() {
        for (WorkerConnection worker : workers) {
            Map<String, ArcSpeed> workerResults = worker.getResults();
            
            System.out.println("Worker " + worker.getWorkerId() + ": " + workerResults.size() + " arcos procesados");
            
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
        // Este método es llamado por WorkerConnection cuando recibe resultados
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
            GraphBuilder graphBuilder = new GraphBuilder();
            Graph graph = graphBuilder.build();
            
            System.out.println("Master: ✓ Grafo cargado - " + graph.getArcs().size() + " arcos");
            
            LocalMasterService master = new LocalMasterService(graph, 8080);
            
            System.out.print("Ingrese la ruta del archivo de datagramas (ej: datagrams4history): ");
            String csvPath = new Scanner(System.in).nextLine().trim();
            
            if (csvPath.isEmpty()) {
                csvPath = "C:\\Users\\default.LAPTOP-M81T5L1M\\Desktop\\ICESI2025II\\ingesoft 4\\dataset\\datagrams4history";
            }
            
            master.start(csvPath);
            
        } catch (Exception e) {
            System.err.println("Error en Master: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
