package org.mio.graph;

import org.mio.model.*;
import org.mio.util.CsvReader;

import java.util.*;

public class GraphBuilder {

    private Map<Integer, Stop> stops;

    public Graph build() {

        // ------ Load stops ------
        stops = new HashMap<>();
        for (String[] row : CsvReader.read("stops-241.csv")) {
            int stopId = Integer.parseInt(row[0].replace("\"", ""));
            String shortName = row[2].replace("\"", "");
            String longName = row[3].replace("\"", "");
            double lon = Double.parseDouble(row[6]);
            double lat = Double.parseDouble(row[7]);

            stops.put(stopId, new Stop(stopId, shortName, longName, lat, lon));
        }

        // ------ Load LineStops ------
        List<LineStop> lineStops = new ArrayList<>();
        for (String[] row : CsvReader.read("linestops-241.csv")) {
            int sequence = Integer.parseInt(row[1]);
            int orientation = Integer.parseInt(row[2]);
            int lineId = Integer.parseInt(row[3]);
            int stopId = Integer.parseInt(row[4]);

            lineStops.add(new LineStop(lineId, stopId, sequence, orientation));
        }

        // Group by line + orientation
        Map<String, List<LineStop>> grouped = new HashMap<>();
        for (LineStop ls : lineStops) {
            String key = ls.getLineId() + "-" + ls.getOrientation();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(ls);
        }

        Graph graph = new Graph();

        // Build arcs
        for (List<LineStop> group : grouped.values()) {
            group.sort(Comparator.comparingInt(LineStop::getSequence));

            for (int i = 0; i < group.size() - 1; i++) {
                Stop from = stops.get(group.get(i).getStopId());
                Stop to = stops.get(group.get(i + 1).getStopId());
                graph.addArc(new Arc(from, to, group.get(i).getLineId(), group.get(i).getOrientation()));
            }
        }

        return graph;
    }

    public Collection<Stop> getStops() {
        return stops.values();
    }
}
