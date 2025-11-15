package org.mio.visual;

import org.mio.model.Arc;
import org.mio.model.Stop;
import org.mio.graph.Graph;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;

public class GraphRenderer {

    public void render(Graph graph, String filename) {

        int width = 2000;
        int height = 2000;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // Fondo blanco
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // Suavizado
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // ================================
        // 1. Extraer nodos únicos del grafo
        // ================================
        Set<Stop> allStops = new HashSet<>();
        for (Arc arc : graph.getArcs()) {
            allStops.add(arc.getFrom());
            allStops.add(arc.getTo());
        }

        // =========================================
        // 2. Asignar posiciones aleatorias a cada nodo
        // =========================================
        Map<Integer, Point> positions = new HashMap<>();
        Random rnd = new Random();

        for (Stop s : allStops) {
            int x = 100 + rnd.nextInt(width - 200);
            int y = 100 + rnd.nextInt(height - 200);
            positions.put(s.getStopId(), new Point(x, y));
        }

        // ================================
        // 3. Dibujar arcos
        // ================================
        for (Arc arc : graph.getArcs()) {
            Point p1 = positions.get(arc.getFrom().getStopId());
            Point p2 = positions.get(arc.getTo().getStopId());

            // color por línea
            g.setColor(colorForLine(arc.getLineId()));
            g.setStroke(new BasicStroke(2));

            g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        // ================================
        // 4. Dibujar nodos (paradas)
        // ================================
        for (Stop s : allStops) {
            Point p = positions.get(s.getStopId());

            g.setColor(Color.BLACK);
            g.fillOval(p.x - 5, p.y - 5, 10, 10);

            g.drawString(s.getShortName(), p.x + 8, p.y + 8);
        }

        g.dispose();

        // ================================
        // 5. Guardar como JPG
        // ================================
        try {
            ImageIO.write(img, "jpg", new File(filename));
            System.out.println("Grafo exportado a: " + filename);
        } catch (Exception e) {
            throw new RuntimeException("Error guardando JPG", e);
        }
    }

    // ================================
    // Función para colorear cada línea
    // ================================
    private Color colorForLine(int lineId) {
        Random r = new Random(lineId); // determinístico
        return new Color(100 + r.nextInt(155), 100 + r.nextInt(155), 100 + r.nextInt(155));
    }
}
