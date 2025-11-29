package org.mio.geo;

import org.mio.model.Stop;

import java.util.List;

public class CoordinateMapper {

    private final double minLat;
    private final double maxLat;
    private final double minLon;
    private final double maxLon;
    private final int mapWidth;
    private final int mapHeight;


    public CoordinateMapper(List<Stop> stops, int mapWidth, int mapHeight) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;

        if (stops == null || stops.isEmpty()) {
            this.minLat = 0;
            this.maxLat = 0;
            this.minLon = 0;
            this.maxLon = 0;
            return;
        }

        this.minLat = stops.stream().mapToDouble(Stop::getLat).min().orElse(0);
        this.maxLat = stops.stream().mapToDouble(Stop::getLat).max().orElse(0);
        this.minLon = stops.stream().mapToDouble(Stop::getLon).min().orElse(0);
        this.maxLon = stops.stream().mapToDouble(Stop::getLon).max().orElse(0);
    }


    public int toX(double lon) {
        if (maxLon == minLon) {
            return mapWidth / 2;
        }
        return (int) (mapWidth * (lon - minLon) / (maxLon - minLon));
    }


    public int toY(double lat) {
        if (maxLat == minLat) {
            return mapHeight / 2;
        }
        // Invert y-axis for screen coordinates (higher lat means lower y)
        return (int) (mapHeight * (maxLat - lat) / (maxLat - minLat));
    }
}
