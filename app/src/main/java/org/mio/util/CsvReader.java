package org.mio.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CsvReader {

    public static List<String[]> read(String filename) {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                CsvReader.class.getClassLoader().getResourceAsStream(filename)))) {

            String line;
            boolean skipHeader = true;

            while ((line = br.readLine()) != null) {
                if (skipHeader) { skipHeader = false; continue; }

                rows.add(line.split(","));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error leyendo CSV: " + filename, e);
        }
        return rows;
    }
}
