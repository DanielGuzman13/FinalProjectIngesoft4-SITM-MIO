package org.mio.model;

public class Arc {
    private Stop from;
    private Stop to;
    private int lineId;
    private int orientation;

    public Arc(Stop from, Stop to, int lineId, int orientation) {
        this.from = from;
        this.to = to;
        this.lineId = lineId;
        this.orientation = orientation;
    }

    @Override
    public String toString() {
        return "[" + lineId + " | " + orientation + "] "
                + from + " → " + to;
    }
}
