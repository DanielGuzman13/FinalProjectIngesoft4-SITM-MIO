package org.mio.model;

public class LineStop {
    private int lineId;
    private int stopId;
    private int sequence;
    private int orientation;

    public LineStop(int lineId, int stopId, int sequence, int orientation) {
        this.lineId = lineId;
        this.stopId = stopId;
        this.sequence = sequence;
        this.orientation = orientation;
    }

    public int getLineId() { return lineId; }
    public int getStopId() { return stopId; }
    public int getSequence() { return sequence; }
    public int getOrientation() { return orientation; }
}
