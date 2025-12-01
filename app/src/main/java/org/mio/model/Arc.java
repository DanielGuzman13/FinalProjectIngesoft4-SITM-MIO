package org.mio.model;

import java.io.Serializable;

public class Arc implements Serializable {
    private static final long serialVersionUID = 1L;
    
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

    public Stop getFrom() {
        return from;
    }

    public Stop getTo() {
        return to;
    }

    public int getLineId() {
        return lineId;
    }

    public int getOrientation() {
        return orientation;
    }

    @Override
    public String toString() {
        return "[" + lineId + " | " + orientation + "] "
                + from + " → " + to;
    }
}
