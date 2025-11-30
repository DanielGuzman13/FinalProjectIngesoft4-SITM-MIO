package org.mio.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Datagram implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String busId;
    private double latitude;
    private double longitude;
    private String timestamp; // String en formato "yyyy-MM-dd HH:mm:ss"
    private int lineId;
    private int orientation;
    private String eventType;

    public Datagram(String busId, double latitude, double longitude, 
                   LocalDateTime timestamp, int lineId, int orientation, String eventType) {
        this.busId = busId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp.toString(); // Convertir a String
        this.lineId = lineId;
        this.orientation = orientation;
        this.eventType = eventType;
    }

    public String getBusId() { return busId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public LocalDateTime getTimestamp() { return LocalDateTime.parse(timestamp); }
    public int getLineId() { return lineId; }
    public int getOrientation() { return orientation; }
    public String getEventType() { return eventType; }

    @Override
    public String toString() {
        return String.format("Datagram{bus=%s, lat=%.6f, lon=%.6f, time=%s, line=%d, orient=%d, type=%s}",
                busId, latitude, longitude, timestamp, lineId, orientation, eventType);
    }
}
