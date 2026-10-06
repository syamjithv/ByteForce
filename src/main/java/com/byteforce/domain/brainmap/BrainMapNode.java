package com.byteforce.domain.brainmap;

import java.util.Objects;

/**
 * Represents a visual concept node on the Brain Map canvas.
 */
public final class BrainMapNode {

    private final long id;
    private final String title;
    private final String shortExplanation;
    private final boolean center;
    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final String memoryStatus; // "DUE", "REVIEWED", "NEW", "NONE"
    private final String relationshipRole; // "CENTER", "PREREQUISITE", "PARENT", "CHILD", "RELATED", "COMMONLY_CONFUSED"
    private final long topicId;

    public BrainMapNode(long id,
                        String title,
                        String shortExplanation,
                        boolean center,
                        double x,
                        double y,
                        double width,
                        double height,
                        String memoryStatus,
                        String relationshipRole,
                        long topicId) {
        this.id = id;
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.shortExplanation = shortExplanation != null ? shortExplanation : "";
        this.center = center;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.memoryStatus = memoryStatus != null ? memoryStatus : "NONE";
        this.relationshipRole = relationshipRole != null ? relationshipRole : "RELATED";
        this.topicId = topicId;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getShortExplanation() {
        return shortExplanation;
    }

    public boolean isCenter() {
        return center;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public String getMemoryStatus() {
        return memoryStatus;
    }

    public String getRelationshipRole() {
        return relationshipRole;
    }

    public long getTopicId() {
        return topicId;
    }

    public double getCenterX() {
        return x + (width / 2.0);
    }

    public double getCenterY() {
        return y + (height / 2.0);
    }
}
