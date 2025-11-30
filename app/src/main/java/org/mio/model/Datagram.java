package org.mio.model;

import java.time.LocalDateTime;

public class Datagram {
    private String busId;
    private double latitude;
    private double longitude;
    private LocalDateTime timestamp;
    private int lineId;
    private int orientation;
    private String eventType;

    public Datagram(String busId, double latitude, double longitude, 
                   LocalDateTime timestamp, int lineId, int orientation, String eventType) {
        this.busId = busId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.lineId = lineId;
        this.orientation = orientation;
        this.eventType = eventType;
    }

    public String getBusId() { return busId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public int getLineId() { return lineId; }
    public int getOrientation() { return orientation; }
    public String getEventType() { return eventType; }

    @Override
    public String toString() {
        return String.format("Datagram{bus=%s, lat=%.6f, lon=%.6f, time=%s, line=%d, orient=%d, type=%s}",
                busId, latitude, longitude, timestamp, lineId, orientation, eventType);
    }
}
