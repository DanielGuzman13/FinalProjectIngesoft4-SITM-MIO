package org.mio.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BatchCsvReader {

    public static void readInBatches(String filePath, int batchSize, Consumer<List<String[]>> batchProcessor) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean skipHeader = true;
            int lineNumber = 0;
            List<String[]> batch = new ArrayList<>(batchSize);
            
            System.out.println("Leyendo archivo en lotes de " + batchSize + " registros...");
            
            while ((line = br.readLine()) != null) {
                lineNumber++;
                
                if (skipHeader) { 
                    skipHeader = false; 
                    continue;
                }
                
                try {
                    String[] fields = line.split(",");
                    batch.add(fields);
                    
                    // Procesar lote cuando alcanza el tamaño
                    if (batch.size() >= batchSize) {
                        batchProcessor.accept(new ArrayList<>(batch));
                        batch.clear();
                        
                        // Mostrar progreso
                        if (lineNumber % 100000 == 0) {
                            System.out.println("Procesadas " + lineNumber + " líneas...");
                        }
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error procesando línea " + lineNumber + ": " + line.substring(0, Math.min(100, line.length())));
                    // Continuar con la siguiente línea
                }
            }
            
            // Procesar el último lote si hay datos restantes
            if (!batch.isEmpty()) {
                batchProcessor.accept(batch);
            }
            
            System.out.println("✓ Lectura completada: " + lineNumber + " líneas totales");
            
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo archivo CSV: " + filePath, e);
        }
    }
}
