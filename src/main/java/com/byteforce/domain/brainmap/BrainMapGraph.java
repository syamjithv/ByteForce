package com.byteforce.domain.brainmap;

import com.byteforce.domain.Concept;
import com.byteforce.domain.Subject;
import com.byteforce.domain.Topic;

import java.util.List;
import java.util.Objects;

/**
 * Encapsulates the visual concept map graph structure, layout bounds, nodes, and edges.
 */
public final class BrainMapGraph {

    private final Concept centerConcept;
    private final Topic topic;
    private final Subject subject;
    private final List<BrainMapNode> nodes;
    private final List<BrainMapEdge> edges;
    private final double minX;
    private final double minY;
    private final double viewBoxWidth;
    private final double viewBoxHeight;

    public BrainMapGraph(Concept centerConcept,
                         Topic topic,
                         Subject subject,
                         List<BrainMapNode> nodes,
                         List<BrainMapEdge> edges,
                         double minX,
                         double minY,
                         double viewBoxWidth,
                         double viewBoxHeight) {
        this.centerConcept = Objects.requireNonNull(centerConcept, "centerConcept must not be null");
        this.topic = topic;
        this.subject = subject;
        this.nodes = nodes != null ? List.copyOf(nodes) : List.of();
        this.edges = edges != null ? List.copyOf(edges) : List.of();
        this.minX = minX;
        this.minY = minY;
        this.viewBoxWidth = viewBoxWidth > 0 ? viewBoxWidth : 960.0;
        this.viewBoxHeight = viewBoxHeight > 0 ? viewBoxHeight : 640.0;
    }

    public BrainMapGraph(Concept centerConcept,
                         Topic topic,
                         Subject subject,
                         List<BrainMapNode> nodes,
                         List<BrainMapEdge> edges,
                         double viewBoxWidth,
                         double viewBoxHeight) {
        this(centerConcept, topic, subject, nodes, edges, 0.0, 0.0, viewBoxWidth, viewBoxHeight);
    }

    public Concept getCenterConcept() {
        return centerConcept;
    }

    public Topic getTopic() {
        return topic;
    }

    public Subject getSubject() {
        return subject;
    }

    public List<BrainMapNode> getNodes() {
        return nodes;
    }

    public List<BrainMapEdge> getEdges() {
        return edges;
    }

    public double getMinX() {
        return minX;
    }

    public double getMinY() {
        return minY;
    }

    public double getViewBoxWidth() {
        return viewBoxWidth;
    }

    public double getViewBoxHeight() {
        return viewBoxHeight;
    }

    public String getViewBox() {
        return String.format(java.util.Locale.US, "%.0f %.0f %.0f %.0f", minX, minY, viewBoxWidth, viewBoxHeight);
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }
}
