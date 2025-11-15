package org.mio.model;

public class Line {
    private int lineId;
    private int planVersionId;
    private String shortName;
    private String description;

    public Line(int lineId, int planVersionId, String shortName, String description) {
        this.lineId = lineId;
        this.planVersionId = planVersionId;
        this.shortName = shortName;
        this.description = description;
    }

    public int getLineId() {
        return lineId;
    }

    @Override
    public String toString() {
        return shortName + " (" + lineId + ")";
    }
}
