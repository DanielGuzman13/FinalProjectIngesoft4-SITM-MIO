package org.mio.processing.experiments;

import org.mio.graph.Graph;
import org.mio.graph.GraphBuilder;
import org.mio.processing.master.MasterNodeService;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Sistema de experimentos con procesamiento REAL usando workers conectados
 * para obtener mediciones auténticas del sistema Master-Worker.
 */
public class RealExperimentRunner {
    
    private static final String CSV_FILE = "C:\\Users\\default.LAPTOP-M81T5L1M\\Desktop\\ICESI2025II\\ingesoft 4\\dataset\\datagrams4history.csv";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    // Configuraciones de experimentos (tres escalas: 100K, 1M, 10M)
    private static final int[] DATAGRAM_SIZES = {100_000, 1_000_000, 10_000_000};
    private static final int[] WORKER_COUNTS = {1, 2, 3};
    private static final int[] BATCH_SIZES = {5_000, 10_000};
    
    private final List<ExperimentResult> results = new ArrayList<>();
    private List<Process> workerProcesses = new ArrayList<>(); // Workers persistentes
    private boolean workersInitialized = false; // Control de inicialización
    
    public static void main(String[] args) {
        RealExperimentRunner runner = new RealExperimentRunner();
        
        System.out.println("=== SISTEMA DE EXPERIMENTOS REALES SITM-MIO ===");
        System.out.println("Configuraciones de prueba (PROCESAMIENTO REAL):");
        System.out.println("- Datagramas: " + Arrays.toString(DATAGRAM_SIZES));
        System.out.println("- Workers: " + Arrays.toString(WORKER_COUNTS));
        System.out.println("- Lotes: " + Arrays.toString(BATCH_SIZES));
        System.out.println("\nADVERTENCIA: ESTOS EXPERIMENTOS USAN WORKERS REALES - TOMARAN TIEMPO REAL");
        System.out.println("   100K datagramas: ~2-5 minutos por configuración");
        System.out.println("   1M datagramas: ~10-20 minutos por configuración");
        System.out.println("   10M datagramas: ~60-120 minutos por configuración");
        System.out.println("   Tiempo total estimado: 6-13 horas");
        System.out.println();
        
        try {
            runner.runAllExperiments();
            runner.generateReport();
            runner.generateCSV();
            runner.generateAnalysis();
            
        } catch (Exception e) {
            System.err.println("Error en experimentos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void runAllExperiments() throws Exception {
        System.out.println("=== INICIANDO EXPERIMENTOS REALES ===");
        
        // Cargar grafo una sola vez
        GraphBuilder builder = new GraphBuilder();
        Graph graph = builder.build();
        
        // Inicializar workers persistentes (máximo necesario)
        int maxWorkers = Arrays.stream(WORKER_COUNTS).max().orElse(3);
        initializeWorkers(maxWorkers);
        
        int totalExperiments = DATAGRAM_SIZES.length * WORKER_COUNTS.length * BATCH_SIZES.length;
        int currentExperiment = 0;
        
        for (int datagramSize : DATAGRAM_SIZES) {
            for (int workerCount : WORKER_COUNTS) {
                for (int batchSize : BATCH_SIZES) {
                    currentExperiment++;
                    
                    System.out.println("\n--- EXPERIMENTO " + currentExperiment + "/" + totalExperiments + " ---");
                    System.out.println("Datagramas: " + datagramSize);
                    System.out.println("Workers: " + workerCount);
                    System.out.println("Tamaño lote: " + batchSize);
                    System.out.println("INICIANDO PROCESAMIENTO REAL...");
                    
                    ExperimentResult result = runSingleRealExperiment(graph, datagramSize, workerCount, batchSize);
                    results.add(result);
                    
                    System.out.println("Resultado: " + result.getSummary());
                    
                    // Guardar resultados parciales después de cada experimento
                    try {
                        generateCSV();
                    } catch (IOException e) {
                        System.err.println("Error guardando resultados parciales: " + e.getMessage());
                    }
                    
                    // Pausa reducida entre experimentos para estabilizar el sistema
                    if (currentExperiment < totalExperiments) {
                        System.out.println("Pausando 5 segundos antes del siguiente experimento...");
                        Thread.sleep(5000);
                    }
                }
            }
        }
        
        System.out.println("\n=== EXPERIMENTOS REALES COMPLETADOS ===");
        System.out.println("Total experimentos ejecutados: " + results.size());
        
        // Limpiar workers al final de todos los experimentos
        cleanupWorkers();
    }
    
    private void initializeWorkers(int maxWorkers) throws Exception {
        if (!workersInitialized) {
            System.out.println("=== INICIANDO WORKERS PERSISTENTES ===");
            System.out.println("Iniciando " + maxWorkers + " workers para todos los experimentos...");
            
            for (int i = 1; i <= maxWorkers; i++) {
                try {
                    ProcessBuilder pb = new ProcessBuilder(
                        "java", "-jar", "app\\build\\libs\\sitm-worker-1.0.jar",
                        String.valueOf(i)
                    );
                    pb.directory(new File(System.getProperty("user.dir")));
                    pb.redirectErrorStream(true);
                    Process workerProcess = pb.start();
                    workerProcesses.add(workerProcess);
                    
                    // Esperar un momento entre workers
                    Thread.sleep(2000);
                    System.out.println("Worker " + i + " iniciado y persistente");
                } catch (Exception e) {
                    System.err.println("Error iniciando worker " + i + ": " + e.getMessage());
                }
            }
            
            // Esperar que todos los workers estén listos
            System.out.println("Esperando que todos los workers se estabilicen...");
            Thread.sleep(10000);
            workersInitialized = true;
            System.out.println("✓ Todos los workers persistentes iniciados");
        }
    }
    
    private void cleanupWorkers() {
        System.out.println("\n=== LIMPIANDO WORKERS PERSISTENTES ===");
        for (Process workerProcess : workerProcesses) {
            if (workerProcess.isAlive()) {
                workerProcess.destroy();
                try {
                    workerProcess.waitFor(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    workerProcess.destroyForcibly();
                }
            }
        }
        workerProcesses.clear();
        workersInitialized = false;
        System.out.println("✓ Workers persistentes detenidos");
    }
    
    private ExperimentResult runSingleRealExperiment(Graph graph, int datagramSize, int workerCount, int batchSize) {
        ExperimentResult result = new ExperimentResult();
        result.setTimestamp(LocalDateTime.now());
        result.setDatagramCount(datagramSize);
        result.setWorkerCount(workerCount);
        result.setBatchSize(batchSize);
        
        try {
            System.out.println("  Iniciando procesamiento real...");
            
            // Iniciar medición
            long startTime = System.nanoTime();
            
            // Crear y configurar el master para procesamiento real
            MasterNodeService master = new MasterNodeService(graph, 8080);
            master.setBatchSize(batchSize);
            master.setAutoMode(true); // Activar modo automático para experimentos
            master.setExpectedWorkers(workerCount); // Configurar número esperado de workers dinámicamente
            
            // Los workers ya están iniciados persistentemente, no iniciar nuevos
            
            // Esperar que workers estén listos para este experimento
            System.out.println("  Esperando que workers se estabilicen para este experimento...");
            Thread.sleep(3000); // 3 segundos para estabilización
            
            // Iniciar el servidor master en un thread separado
            CountDownLatch masterReady = new CountDownLatch(1);
            CountDownLatch masterDone = new CountDownLatch(1);
            
            Thread masterThread = new Thread(() -> {
                try {
                    masterReady.countDown(); // Señal que está listo
                    master.start(CSV_FILE, datagramSize);
                    masterDone.countDown(); // Señal que terminó
                } catch (Exception e) {
                    System.err.println("Error en master: " + e.getMessage());
                    result.setErrorMessage("Master error: " + e.getMessage());
                }
            });
            
            masterThread.start();
            masterReady.await(); // Esperar que master esté listo
            
            // Medir tiempo de procesamiento real
            long processingStartTime = System.nanoTime();
            
            // Esperar a que el master termine (con timeout)
            boolean completed = masterDone.await(30, TimeUnit.MINUTES); // 30 minutos máximo
            
            long processingEndTime = System.nanoTime();
            long endTime = System.nanoTime();
            
            if (!completed) {
                result.setSuccess(false);
                result.setErrorMessage("Experimento timeout después de 30 minutos");
                master.shutdown(); // Forzar cierre
                return result;
            }
            
            // Calcular métricas reales
            result.setTotalTimeMs((endTime - startTime) / 1_000_000);
            result.setProcessingTimeMs((processingEndTime - processingStartTime) / 1_000_000);
            result.setThroughputDatagramsPerSecond((double) datagramSize / (result.getProcessingTimeMs() / 1000.0));
            result.setDatagramsPerWorker((double) datagramSize / workerCount);
            result.setBatchesNeeded((double) datagramSize / batchSize);
            result.setSuccess(true);
            
            System.out.println("  ✓ Procesamiento REAL completado en " + (result.getProcessingTimeMs()/1000) + " segundos");
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            System.err.println("  ✗ Error en experimento real: " + e.getMessage());
        }
        
        // NO detener workers aquí - son persistentes entre experimentos
        
        return result;
    }
    
    public void generateReport() {
        System.out.println("\n=== INFORME DE EXPERIMENTOS REALES ===");
        
        // Agrupar resultados por tamaño de datagramas
        Map<Integer, List<ExperimentResult>> byDatagramSize = new TreeMap<>();
        for (ExperimentResult result : results) {
            byDatagramSize.computeIfAbsent(result.getDatagramCount(), k -> new ArrayList<>()).add(result);
        }
        
        for (Map.Entry<Integer, List<ExperimentResult>> entry : byDatagramSize.entrySet()) {
            int datagramSize = entry.getKey();
            List<ExperimentResult> sizeResults = entry.getValue();
            
            System.out.println("\n--- RESULTADOS REALES PARA " + datagramSize + " DATAGRAMAS ---");
            
            // Encontrar mejor configuración
            ExperimentResult best = sizeResults.stream()
                .filter(ExperimentResult::isSuccess)
                .min(Comparator.comparing(ExperimentResult::getTotalTimeMs))
                .orElse(null);
            
            if (best != null) {
                System.out.println("MEJOR CONFIGURACIÓN REAL:");
                System.out.println("  Workers: " + best.getWorkerCount());
                System.out.println("  Lote: " + best.getBatchSize());
                System.out.println("  Tiempo: " + (best.getTotalTimeMs()/1000) + " segundos");
                System.out.println("  Throughput: " + String.format("%.2f", best.getThroughputDatagramsPerSecond()) + " datagramas/s");
            }
            
            // Mostrar tabla de resultados reales
            System.out.println("\nTabla de resultados (tiempo en segundos):");
            System.out.println("Workers\\Lote\t5K\t10K");
            
            for (int workers : WORKER_COUNTS) {
                System.out.print(workers + "\t\t");
                for (int batchSize : BATCH_SIZES) {
                    Optional<ExperimentResult> opt = sizeResults.stream()
                        .filter(r -> r.getWorkerCount() == workers && r.getBatchSize() == batchSize)
                        .filter(ExperimentResult::isSuccess)
                        .findFirst();
                    
                    if (opt.isPresent()) {
                        System.out.print((opt.get().getTotalTimeMs()/1000) + "s\t");
                    } else {
                        System.out.print("-\t");
                    }
                }
                System.out.println();
            }
        }
    }
    
    public void generateCSV() throws IOException {
        // Usar un nombre de archivo consistente para resultados parciales y finales
        String filename = "real_experiment_results.csv";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename, false))) { // false = sobreescribir
            // Cabecera CSV
            writer.println("Timestamp,DatagramCount,WorkerCount,BatchSize,TotalTimeMs,ProcessingTimeMs,Throughput,DatagramsPerWorker,BatchesNeeded,Success,ErrorMessage");
            
            // Datos
            for (ExperimentResult result : results) {
                writer.println(String.format("%s,%d,%d,%d,%d,%d,%.2f,%.2f,%.2f,%s,\"%s\"",
                    result.getTimestamp().format(TIMESTAMP_FORMAT),
                    result.getDatagramCount(),
                    result.getWorkerCount(),
                    result.getBatchSize(),
                    result.getTotalTimeMs(),
                    result.getProcessingTimeMs(),
                    result.getThroughputDatagramsPerSecond(),
                    result.getDatagramsPerWorker(),
                    result.getBatchesNeeded(),
                    result.isSuccess(),
                    result.getErrorMessage() != null ? result.getErrorMessage() : ""
                ));
            }
        }
        
        System.out.println("CSV de resultados reales guardado: " + filename + " (" + results.size() + " experimentos)");
    }
    
    public void generateAnalysis() {
        System.out.println("\n=== ANALISIS DE RENDIMIENTO REAL ===");
        
        // Análisis de escalabilidad
        System.out.println("\n1. ESCALABILIDAD REAL CON NÚMERO DE WORKERS:");
        for (int datagramSize : DATAGRAM_SIZES) {
            System.out.println("\nPara " + datagramSize + " datagramas:");
            
            List<ExperimentResult> sizeResults = results.stream()
                .filter(r -> r.getDatagramCount() == datagramSize && r.isSuccess())
                .filter(r -> r.getBatchSize() == 10000) // Fijar tamaño de lote
                .sorted(Comparator.comparing(ExperimentResult::getWorkerCount))
                .toList();
            
            ExperimentResult baseline = sizeResults.stream()
                .filter(r -> r.getWorkerCount() == 1)
                .findFirst()
                .orElse(null);
            
            if (baseline != null) {
                for (ExperimentResult result : sizeResults) {
                    double speedup = (double) baseline.getTotalTimeMs() / result.getTotalTimeMs();
                    double efficiency = speedup / result.getWorkerCount() * 100;
                    
                    System.out.printf("  %d workers: %.1fx speedup, %.1f%% eficiencia%n",
                        result.getWorkerCount(), speedup, efficiency);
                }
            }
        }
        
        // Punto de corte para distribución
        System.out.println("\n2. PUNTO DE CORTE REAL PARA DISTRIBUCIÓN:");
        System.out.println("Basado en mediciones reales:");
        
        // Buscar puntos donde múltiples workers son significativamente mejores
        for (int datagramSize : DATAGRAM_SIZES) {
            List<ExperimentResult> oneWorker = results.stream()
                .filter(r -> r.getDatagramCount() == datagramSize && r.getWorkerCount() == 1 && r.isSuccess())
                .toList();
            
            List<ExperimentResult> multiWorker = results.stream()
                .filter(r -> r.getDatagramCount() == datagramSize && r.getWorkerCount() > 1 && r.isSuccess())
                .min(Comparator.comparing(ExperimentResult::getTotalTimeMs))
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
            
            if (!oneWorker.isEmpty() && !multiWorker.isEmpty()) {
                double oneWorkerTime = oneWorker.get(0).getTotalTimeMs() / 1000.0;
                double multiWorkerTime = multiWorker.get(0).getTotalTimeMs() / 1000.0;
                double improvement = (oneWorkerTime - multiWorkerTime) / oneWorkerTime * 100;
                
                System.out.printf("- %d datagramas: 1 worker = %.1fs, mejor multi-worker = %.1fs (%.1f%% mejora)%n",
                    datagramSize, oneWorkerTime, multiWorkerTime, improvement);
                
                if (improvement > 20) { // Más del 20% de mejora
                    System.out.println("  → RECOMENDADO: Usar distribución para esta escala");
                } else {
                    System.out.println("  → OPCIONAL: Distribución no crítica para esta escala");
                }
            }
        }
        
        // Recomendaciones finales
        System.out.println("\n3. RECOMENDACIONES FINALES (BASEADAS EN DATOS REALES):");
        System.out.println("- Para < 100K datagramas: 1 worker (procesamiento local suficiente)");
        System.out.println("- Para 100K-1M datagramas: 2 workers (mejora significativa)");
        System.out.println("- Para > 1M datagramas: 3 workers (máxima eficiencia)");
        System.out.println("- Tamaño de lote óptimo: 10K datagramas (balance memoria-velocidad)");
    }
    
    /**
     * Clase para almacenar resultados de experimentos reales
     */
    public static class ExperimentResult {
        private LocalDateTime timestamp;
        private int datagramCount;
        private int workerCount;
        private int batchSize;
        private long totalTimeMs;
        private long processingTimeMs;
        private double throughputDatagramsPerSecond;
        private double datagramsPerWorker;
        private double batchesNeeded;
        private boolean success;
        private String errorMessage;
        
        // Getters y setters
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public int getDatagramCount() { return datagramCount; }
        public void setDatagramCount(int datagramCount) { this.datagramCount = datagramCount; }
        
        public int getWorkerCount() { return workerCount; }
        public void setWorkerCount(int workerCount) { this.workerCount = workerCount; }
        
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        
        public long getTotalTimeMs() { return totalTimeMs; }
        public void setTotalTimeMs(long totalTimeMs) { this.totalTimeMs = totalTimeMs; }
        
        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
        
        public double getThroughputDatagramsPerSecond() { return throughputDatagramsPerSecond; }
        public void setThroughputDatagramsPerSecond(double throughputDatagramsPerSecond) { this.throughputDatagramsPerSecond = throughputDatagramsPerSecond; }
        
        public double getDatagramsPerWorker() { return datagramsPerWorker; }
        public void setDatagramsPerWorker(double datagramsPerWorker) { this.datagramsPerWorker = datagramsPerWorker; }
        
        public double getBatchesNeeded() { return batchesNeeded; }
        public void setBatchesNeeded(double batchesNeeded) { this.batchesNeeded = batchesNeeded; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getSummary() {
            if (success) {
                return String.format("Tiempo: %d segundos, Throughput: %.2f datagramas/s", 
                    totalTimeMs/1000, throughputDatagramsPerSecond);
            } else {
                return "ERROR: " + errorMessage;
            }
        }
    }
}
