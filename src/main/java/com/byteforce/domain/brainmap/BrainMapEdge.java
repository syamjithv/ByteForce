package com.byteforce.domain.brainmap;

import com.byteforce.domain.ConceptRelationshipType;

import java.util.Objects;

/**
 * Represents a directional or associative relationship connection between two concepts on the Brain Map.
 */
public final class BrainMapEdge {

    private final long sourceId;
    private final long targetId;
    private final ConceptRelationshipType type;
    private final String description;
    private final double x1;
    private final double y1;
    private final double x2;
    private final double y2;
    private final boolean outgoingFromCenter;

    public BrainMapEdge(long sourceId,
                        long targetId,
                        ConceptRelationshipType type,
                        String description,
                        double x1,
                        double y1,
                        double x2,
                        double y2,
                        boolean outgoingFromCenter) {
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.description = description != null ? description : "";
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.outgoingFromCenter = outgoingFromCenter;
    }

    public long getSourceId() {
        return sourceId;
    }

    public long getTargetId() {
        return targetId;
    }

    public ConceptRelationshipType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public double getX1() {
        return x1;
    }

    public double getY1() {
        return y1;
    }

    public double getX2() {
        return x2;
    }

    public double getY2() {
        return y2;
    }

    public boolean isOutgoingFromCenter() {
        return outgoingFromCenter;
    }

    public double getMidX() {
        return (x1 + x2) / 2.0;
    }

    public double getMidY() {
        return (y1 + y2) / 2.0;
    }

    public String getStrokeStyle() {
        return switch (type) {
            case PREREQUISITE -> "stroke: #818cf8; stroke-width: 2.2; marker-end: url(#arrowhead-prereq);";
            case PARENT -> "stroke: #3b82f6; stroke-width: 2.0; stroke-dasharray: 6 3; marker-end: url(#arrowhead-parent);";
            case CHILD -> "stroke: #38bdf8; stroke-width: 2.0; marker-end: url(#arrowhead-child);";
            case COMMONLY_CONFUSED -> "stroke: #f59e0b; stroke-width: 2.0; stroke-dasharray: 5 4; marker-end: url(#arrowhead-confused);";
            case RELATED -> "stroke: #94a3b8; stroke-width: 1.8; stroke-dasharray: 4 4;";
        };
    }
}
