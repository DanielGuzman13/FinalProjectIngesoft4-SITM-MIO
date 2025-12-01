package org.mio.processing.master;

import org.mio.graph.Graph;
import org.mio.graph.GraphBuilder;
import org.mio.model.*;
import org.mio.processing.config.MasterConfig;
import org.mio.processing.master.WorkerConnection;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class MasterNodeService {
    
    public static void main(String[] args) {
        try {
            // Parámetros: [archivo_csv] [datagramas_a_procesar]
            if (args.length < 2) {
                System.out.println("Uso: java -jar sitm-master-1.0.jar <archivo_csv> <datagramas_a_procesar>");
                System.out.println("Ejemplo: java -jar sitm-master-1.0.jar datagrams.csv 1000000");
                System.out.println("Opciones de datagramas: 1000, 10000, 100000, 1000000");
                System.out.println();
                System.out.println("EJEMPLOS CON RUTA COMPLETA:");
                System.out.println("java -jar sitm-master-1.0.jar \"C:\\ruta\\datagrams.csv\" 1000000");
                System.out.println("run-master.bat \"C:\\ruta\\datagrams.csv\" 1000000");
                return;
            }
            
            String csvFilePath = args[0];
            
            // Validar y convertir datagramCount con mejor manejo de errores
            int datagramCount;
            try {
                // Limpiar el string de posibles espacios y caracteres extraños
                String datagramStr = args[1].trim();
                datagramCount = Integer.parseInt(datagramStr);
            } catch (NumberFormatException e) {
                System.err.println("Error: '" + args[1] + "' no es un número válido de datagramas");
                System.err.println("Usa números como: 1000, 10000, 100000, 1000000");
                System.out.println("Uso: java -jar sitm-master-1.0.jar <archivo_csv> <datagramas_a_procesar>");
                return;
            }
            
            // Validar cantidad de datagramas
            if (datagramCount <= 0) {
                System.err.println("Error: La cantidad de datagramas debe ser mayor que 0");
                System.err.println("Usa números como: 1000, 10000, 100000, 1000000");
                return;
            }
            
            // Validar que el archivo exista
            File csvFile = new File(csvFilePath);
            if (!csvFile.exists()) {
                System.err.println("Error: No se encuentra el archivo CSV: " + csvFilePath);
                System.err.println("Verifica que la ruta sea correcta y el archivo exista.");
                System.err.println("Ejemplo con ruta completa:");
                System.err.println("java -jar sitm-master-1.0.jar \"C:\\Users\\usuario\\Desktop\\dataset\\datagrams4history.csv\" 1000000");
                return;
            }
            
            System.out.println("=== CONFIGURACIÓN MASTER NODE ===");
            System.out.println("Archivo CSV: " + csvFilePath);
            System.out.println("Datagramas a procesar: " + datagramCount);
            System.out.println("Archivo encontrado: " + csvFile.exists() + " (tamaño: " + csvFile.length() + " bytes)");
            System.out.println();
            
            // Cargar grafo
            GraphBuilder builder = new GraphBuilder();
            Graph graph = builder.build();
            
            // Iniciar Master Node Service
            MasterNodeService masterService = new MasterNodeService(graph, 8080);
            masterService.start(csvFilePath, datagramCount);
            
        } catch (Exception e) {
            System.err.println("Error iniciando Master Node: " + e.getMessage());
            System.err.println("Causa: " + e.getCause());
            e.printStackTrace();
        }
    }
    
    // ... (rest of the code remains the same)
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

    public void start(String csvFilePath, int datagramCount) throws IOException {
        // Mostrar banner del Master
        System.out.println(MasterConfig.MASTER_BANNER);
        System.out.println(MasterConfig.MASTER_ROLE);
        System.out.println("Puerto: " + masterPort);
        System.out.println("Datagramas a procesar: " + datagramCount);
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
        processDatagrams(csvFilePath, datagramCount);
    }

    private void processDatagrams(String csvFilePath, int datagramCount) {
        // Limpiar la ruta: remover comillas si existen
        csvFilePath = csvFilePath.replace("\"", "").trim();
        final String finalCsvPath = csvFilePath; // Para lambda
        
        System.out.println(MasterConfig.PROCESSING_BANNER);
        System.out.println(MasterConfig.LOADING_GRAPH);
        
        System.out.println("Master: Workers activos: " + workers.size());
        System.out.println("Master: Archivo de datos: " + csvFilePath);
        System.out.println("Master: Arcos en grafo: " + graph.getArcs().size());
        System.out.println("Master: Datagramas a procesar: " + datagramCount);
        System.out.println(MasterConfig.LOADING_DATAGRAMS);
        
        long startTime = System.currentTimeMillis();
        AtomicLong totalProcessed = new AtomicLong(0);

        // Cargar datagramas y distribuir a workers usando procesamiento por lotes
        Thread producerThread = new Thread(() -> {
            try {
                loadDatagramsInBatches(finalCsvPath, datagramCount); // Usar datagramCount dinámico
                System.out.println("Master: ✓ Procesamiento por lotes completado");
            } catch (Exception e) {
                System.err.println("Master: Error cargando datagramas: " + e.getMessage());
                e.printStackTrace();
            }
        });

        producerThread.start();

        // Esperar a que el thread de procesamiento termine
        try {
            producerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
            
        System.out.println(MasterConfig.DISTRIBUTION_COMPLETE);
        System.out.println(MasterConfig.WAITING_RESULTS);
        
        // Esperar a que todos los workers terminen
        try {
            while (workers.stream().anyMatch(w -> w.isProcessing())) {
                Thread.sleep(1000);
                System.out.println("Master: Workers procesando... (" + 
                                 workers.stream().mapToInt(w -> w.getResults().size()).sum() + " arcos calculados hasta ahora)");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println(MasterConfig.SENDING_STOP_SIGNAL);
        for (WorkerConnection worker : workers) {
            worker.sendStop();
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println(MasterConfig.PROCESSING_COMPLETE);
        System.out.println(MasterConfig.AGGREGATING_RESULTS);
        
        aggregateResults();

        long endTime = System.currentTimeMillis();
        double processingTime = (endTime - startTime) / 1000.0;
        
        System.out.println(MasterConfig.RESULTS_BANNER);
        System.out.println(String.format("Resumen: %d workers, %.0f segundos, %.1f datagramas/segundo, %d arcos con velocidad", 
            workers.size(), processingTime, 
            totalProcessed.get() / processingTime, aggregatedResults.size()));

        printSpeedResults();
        shutdown();
    }

    private void loadDatagramsInBatches(String filePath, int targetDatagrams) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        AtomicLong totalProcessed = new AtomicLong(0);
        AtomicBoolean stopProcessing = new AtomicBoolean(false);
        
        // Definir tamaño de lote manejable para evitar OutOfMemoryError
        int batchSize = Math.min(10000, targetDatagrams); // Máximo 10,000 por lote
        
        System.out.println("Master: Iniciando procesamiento por lotes de " + batchSize + " datagramas...");
        System.out.println("Master: Objetivo total: " + targetDatagrams + " datagramas");
        
        try (Scanner scanner = new Scanner(new File(filePath), "UTF-8")) {
            // Saltar header
            if (scanner.hasNextLine()) {
                scanner.nextLine();
            }
            
            List<String[]> batch = new ArrayList<>();
            int batchCount = 0;
            
            while (scanner.hasNextLine() && !stopProcessing.get() && totalProcessed.get() < targetDatagrams) {
                String line = scanner.nextLine();
                String[] row = line.split(",");
                batch.add(row);
                
                // Procesar lote cuando alcanza el tamaño o cuando tenemos suficientes para alcanzar el objetivo
                if (batch.size() >= batchSize || 
                    (totalProcessed.get() + batch.size() >= targetDatagrams && batch.size() > 0)) {
                    
                    processBatch(batch, formatter, totalProcessed, stopProcessing, targetDatagrams);
                    batch.clear();
                    batchCount++;
                    
                    // Mostrar progreso
                    System.out.println("Master: Progreso - Lote " + batchCount + 
                                     " completado (" + totalProcessed.get() + "/" + targetDatagrams + ")");
                    
                    // Pequeña pausa para permitir GC
                    if (batchCount % 10 == 0) {
                        System.gc();
                        Thread.sleep(100);
                    }
                }
            }
            
            // Procesar último lote si tiene datos y no hemos alcanzado el objetivo
            if (!batch.isEmpty() && !stopProcessing.get() && totalProcessed.get() < targetDatagrams) {
                processBatch(batch, formatter, totalProcessed, stopProcessing, targetDatagrams);
                batchCount++;
            }
            
        } catch (Exception e) {
            System.err.println("Master: Error leyendo archivo: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("Master: ✓ Procesamiento por lotes completado - Total procesado: " + totalProcessed.get());
        System.out.println("Master: ✓ Objetivo alcanzado: " + (totalProcessed.get() >= targetDatagrams ? "SI" : "NO"));
    }
    
    private void processBatch(List<String[]> batch, DateTimeFormatter formatter, 
                            AtomicLong totalProcessed, AtomicBoolean stopProcessing, int maxDatagrams) {
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
        
        // Detenerse solo si encontramos datagramas válidos y procesamos suficientes
        if (datagrams.size() > 0 && totalProcessed.get() >= maxDatagrams) {
            System.out.println("Master: ✓ Lote con datos encontrado - Enviando señal de parada a workers");
            sendStopToAllWorkers();
            stopProcessing.set(true);
        }
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
        System.out.println("\n=== VELOCIDADES PROMEDIO POR ARCO (SITM-MIO) ===\n");
        
        if (aggregatedResults.isEmpty()) {
            System.out.println("No se encontraron arcos con velocidad calculada.");
            return;
        }
        
        // Mostrar todos los arcos con velocidad, ordenados por velocidad descendente
        System.out.println("ID_ARCO                              | LINEA | ORIGEN      -> DESTINO     | VELOCIDAD | MUESTRAS | RANGO (km/h)");
        System.out.println("-------------------------------------|-------|-------------|-------------|-----------|----------|---------------");
        
        aggregatedResults.values().stream()
            .filter(arcSpeed -> arcSpeed.getSampleCount() >= 1) // Mostrar todos con al menos 1 muestra
            .sorted(Comparator.comparingDouble(ArcSpeed::getAverageSpeed).reversed())
            .forEach(arcSpeed -> {
                Arc arc = arcSpeed.getArc();
                String arcId = arc.getFrom().getStopId() + "-" + arc.getTo().getStopId() + "-" + arc.getLineId();
                String fromName = arc.getFrom().getShortName() != null ? arc.getFrom().getShortName() : arc.getFrom().getStopId() + "";
                String toName = arc.getTo().getShortName() != null ? arc.getTo().getShortName() : arc.getTo().getStopId() + "";
                
                // Truncar nombres si son muy largos
                if (fromName.length() > 11) fromName = fromName.substring(0, 11);
                if (toName.length() > 11) toName = toName.substring(0, 11);
                
                System.out.println(String.format("%-36s | %5d | %-11s -> %-11s | %8.2f | %8d | %5.1f - %5.1f",
                    arcId,
                    arc.getLineId(),
                    fromName,
                    toName,
                    arcSpeed.getAverageSpeed(),
                    arcSpeed.getSampleCount(),
                    arcSpeed.getMinSpeed(),
                    arcSpeed.getMaxSpeed()
                ));
            });
            
        System.out.println("\n=== RESUMEN ESTADISTICO ===");
        System.out.println("Total arcos con velocidad: " + aggregatedResults.size());
        System.out.println("Velocidad promedio general: " + 
            String.format("%.2f km/h", 
                aggregatedResults.values().stream()
                    .mapToDouble(ArcSpeed::getAverageSpeed)
                    .average()
                    .orElse(0.0)));
        System.out.println("Velocidad maxima: " + 
            String.format("%.2f km/h", 
                aggregatedResults.values().stream()
                    .mapToDouble(ArcSpeed::getAverageSpeed)
                    .max()
                    .orElse(0.0)));
        System.out.println("Velocidad minima: " + 
            String.format("%.2f km/h", 
                aggregatedResults.values().stream()
                    .mapToDouble(ArcSpeed::getAverageSpeed)
                    .min()
                    .orElse(0.0)));
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
}
