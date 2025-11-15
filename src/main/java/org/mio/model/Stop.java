package org.mio.model;

public class Stop {
    private int stopId;
    private String shortName;
    private String longName;
    private double lat;
    private double lon;

    public Stop(int stopId, String shortName, String longName, double lat, double lon) {
        this.stopId = stopId;
        this.shortName = shortName;
        this.longName = longName;
        this.lat = lat;
        this.lon = lon;
    }

    public int getStopId() {
        return stopId;
    }

    @Override
    public String toString() {
        return shortName + " (" + stopId + ")";
    }
}
