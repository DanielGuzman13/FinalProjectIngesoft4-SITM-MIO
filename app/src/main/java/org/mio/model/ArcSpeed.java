package org.mio.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ArcSpeed implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Arc arc;
    private double averageSpeed;
    private int sampleCount;
    private String lastUpdated; // String para serialización
    private double totalSpeed;
    private double minSpeed;
    private double maxSpeed;

    public ArcSpeed(Arc arc) {
        this.arc = arc;
        this.averageSpeed = 0.0;
        this.sampleCount = 0;
        this.lastUpdated = LocalDateTime.now().toString(); // Convertir a String
        this.totalSpeed = 0.0;
        this.minSpeed = Double.MAX_VALUE;
        this.maxSpeed = 0.0;
    }

    public void addSpeedSample(double speed) {
        totalSpeed += speed;
        sampleCount++;
        averageSpeed = totalSpeed / sampleCount;
        lastUpdated = LocalDateTime.now().toString(); // Convertir a String
        
        if (speed < minSpeed) minSpeed = speed;
        if (speed > maxSpeed) maxSpeed = speed;
    }

    public Arc getArc() { return arc; }
    public double getAverageSpeed() { return averageSpeed; }
    public int getSampleCount() { return sampleCount; }
    public LocalDateTime getLastUpdated() { return LocalDateTime.parse(lastUpdated); }
    public double getMinSpeed() { return minSpeed == Double.MAX_VALUE ? 0.0 : minSpeed; }
    public double getMaxSpeed() { return maxSpeed; }

    @Override
    public String toString() {
        return String.format("ArcSpeed{arc=[%d|%d] %s→%s, avgSpeed=%.2f km/h, samples=%d, min=%.2f, max=%.2f}",
                arc.getLineId(), arc.getOrientation(), 
                arc.getFrom().getShortName(), arc.getTo().getShortName(),
                averageSpeed, sampleCount, getMinSpeed(), getMaxSpeed());
    }
}
