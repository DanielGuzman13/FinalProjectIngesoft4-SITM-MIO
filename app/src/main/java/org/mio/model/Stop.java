package org.mio.model;

import java.io.Serializable;

public class Stop implements Serializable {
    private static final long serialVersionUID = 1L;
    
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

    public String getShortName() {
        return shortName;
    }

    public String getLongName() {
        return longName;
    }

    public int getStopId() {
        return stopId;
    }


    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    @Override
    public String toString() {
        return shortName + " (" + stopId + ")";
    }
}
