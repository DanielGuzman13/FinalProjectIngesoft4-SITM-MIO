package org.mio.visual;

import org.mio.geo.CoordinateMapper;
import org.mio.graph.Graph;
import org.mio.model.Arc;
import org.mio.model.Stop;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class MapRenderer {

    private static final int STOP_DIAMETER = 8;
    private final Map<Integer, Color> lineColorCache = new ConcurrentHashMap<>();


    public void render(Graphics2D g, Graph graph, CoordinateMapper mapper,
                       Stop selectedStop, Integer filterLineId, Integer filterOrientation) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        List<Arc> visibleArcs = new ArrayList<>();
        for (Arc arc : graph.getArcs()) {
            boolean matchesLine = (filterLineId == null || arc.getLineId() == filterLineId);
            boolean matchesOrientation = (filterOrientation == null || arc.getOrientation() == filterOrientation);

            if (matchesLine && matchesOrientation) {
                visibleArcs.add(arc);
            }
        }

        Set<Stop> visibleStops = new HashSet<>();
        for (Arc arc : visibleArcs) {
            visibleStops.add(arc.getFrom());
            visibleStops.add(arc.getTo());
        }

        for (Arc arc : visibleArcs) {
            drawArc(g, arc, mapper);
        }

        for (Stop stop : visibleStops) {
            int x = mapper.toX(stop.getLon());
            int y = mapper.toY(stop.getLat());

            if (stop.equals(selectedStop)) {
                g.setColor(Color.YELLOW);
                g.fillOval(x - STOP_DIAMETER / 2, y - STOP_DIAMETER / 2, STOP_DIAMETER, STOP_DIAMETER);
            }

            g.setColor(Color.BLUE);
            g.fillOval(x - (STOP_DIAMETER - 2) / 2, y - (STOP_DIAMETER - 2) / 2, STOP_DIAMETER - 2, STOP_DIAMETER - 2);

            //nombres de los puntos
            /*g.setColor(Color.DARK_GRAY);
            g.setFont(new Font("SansSerif", Font.PLAIN, 9));
            g.drawString(stop.getShortName(), x + 5, y + 5);*/
        }
    }


    private void drawArc(Graphics2D g, Arc arc, CoordinateMapper mapper) {
        Stop from = arc.getFrom();
        Stop to = arc.getTo();

        int x1 = mapper.toX(from.getLon());
        int y1 = mapper.toY(from.getLat());
        int x2 = mapper.toX(to.getLon());
        int y2 = mapper.toY(to.getLat());

        g.setColor(getLineColor(arc.getLineId()));
        g.setStroke(new BasicStroke(2.0f));
        g.drawLine(x1, y1, x2, y2);
    }


    private Color getLineColor(int lineId) {
        return lineColorCache.computeIfAbsent(lineId, id -> {
            float hue = (float) (Integer.hashCode(id) % 360) / 360.0f;
            return Color.getHSBColor(hue, 0.8f, 0.9f);
        });
    }
}