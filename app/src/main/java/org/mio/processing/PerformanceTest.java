package org.mio.processing;

import org.mio.graph.Graph;
import org.mio.graph.GraphBuilder;
import org.mio.model.ArcSpeed;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class PerformanceTest {
    
    public static void main(String[] args) {
        System.out.println("=== PRUEBAS DE PERFORMANCE - ESCALAMIENTO LOGARÍTMICO ===\n");
        
        GraphBuilder graphBuilder = new GraphBuilder();
        Graph graph = graphBuilder.build();
        
        int[] testSizes = {1_000_000, 10_000_000, 100_000_000};
        int[] workerCounts = {2, 4, 8};
        
        List<TestResult> results = new ArrayList<>();
        
        for (int size : testSizes) {
            for (int workers : workerCounts) {
                System.out.println("Probando con " + formatNumber(size) + " datagramas y " + workers + " workers...");
                
                TestResult result = runPerformanceTest(graph, size, workers);
                results.add(result);
                
                System.out.println("  Tiempo: " + String.format("%.2f", result.getTimeSeconds()) + "s");
                System.out.println("  Rendimiento: " + String.format("%.0f", result.getThroughput()) + " datagramas/s");
                System.out.println("  Arcos procesados: " + result.getArcsProcessed());
                System.out.println();
            }
        }
        
        generatePerformanceReport(results);
        System.out.println("Reporte generado: performance_report.csv");
    }
    
    private static TestResult runPerformanceTest(Graph graph, int targetSize, int numWorkers) {
        String testFile = "C:\\Users\\default.LAPTOP-M81T5L1M\\Desktop\\ICESI2025II\\ingesoft 4\\dataset\\test_data_" + targetSize + ".csv";
        
        MasterNode master = new MasterNode(graph, numWorkers);
        
        long startTime = System.currentTimeMillis();
        Map<String, ArcSpeed> results = master.processDatagrams(testFile);
        long endTime = System.currentTimeMillis();
        
        double timeSeconds = (endTime - startTime) / 1000.0;
        double throughput = targetSize / timeSeconds;
        
        return new TestResult(targetSize, numWorkers, timeSeconds, throughput, results.size());
    }
    
    private static void generatePerformanceReport(List<TestResult> results) {
        try (FileWriter writer = new FileWriter("performance_report.csv")) {
            writer.write("Tamaño_Datagramas,Workers,Tiempo_Segundos,Rendimiento_Datagramas_Segundo,Arcos_Procesados,Eficiencia\n");
            
            for (TestResult result : results) {
                double efficiency = result.getThroughput() / result.getNumWorkers();
                
                writer.write(String.format("%d,%d,%.2f,%.0f,%d,%.0f\n",
                    result.getDataSize(),
                    result.getNumWorkers(),
                    result.getTimeSeconds(),
                    result.getThroughput(),
                    result.getArcsProcessed(),
                    efficiency
                ));
            }
        } catch (IOException e) {
            System.err.println("Error generando reporte: " + e.getMessage());
        }
    }
    
    private static String formatNumber(int number) {
        if (number >= 1_000_000) {
            return (number / 1_000_000) + "M";
        } else if (number >= 1_000) {
            return (number / 1_000) + "K";
        }
        return String.valueOf(number);
    }
    
    private static class TestResult {
        private final int dataSize;
        private final int numWorkers;
        private final double timeSeconds;
        private final double throughput;
        private final int arcsProcessed;
        
        public TestResult(int dataSize, int numWorkers, double timeSeconds, double throughput, int arcsProcessed) {
            this.dataSize = dataSize;
            this.numWorkers = numWorkers;
            this.timeSeconds = timeSeconds;
            this.throughput = throughput;
            this.arcsProcessed = arcsProcessed;
        }
        
        public int getDataSize() { return dataSize; }
        public int getNumWorkers() { return numWorkers; }
        public double getTimeSeconds() { return timeSeconds; }
        public double getThroughput() { return throughput; }
        public int getArcsProcessed() { return arcsProcessed; }
    }
}
