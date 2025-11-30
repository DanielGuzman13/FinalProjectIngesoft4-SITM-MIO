package org.mio.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileCsvReader {

    public static List<String[]> read(String filePath) {
        List<String[]> rows = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean skipHeader = true;
            int lineNumber = 0;
            
            System.out.println("Leyendo archivo: " + filePath);
            
            while ((line = br.readLine()) != null) {
                lineNumber++;
                
                if (skipHeader) { 
                    skipHeader = false; 
                    continue;
                }
                
                try {
                    String[] fields = line.split(",");
                    rows.add(fields);
                    
                    // Mostrar progreso cada 10000 líneas
                    if (lineNumber % 10000 == 0) {
                        System.out.println("Procesadas " + lineNumber + " líneas...");
                    }
                } catch (Exception e) {
                    System.err.println("Error procesando línea " + lineNumber + ": " + line.substring(0, Math.min(100, line.length())));
                    // Continuar con la siguiente línea
                }
            }
            
            System.out.println("✓ Lectura completada: " + rows.size() + " registros válidos");
            
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo archivo CSV: " + filePath, e);
        }
        
        return rows;
    }
}
