package com.byteforce.domain;

import java.util.Objects;

/**
 * Presentation helper wrapping a {@link ConceptRelationship} together with
 * the resolved connected {@link Concept} and directional context for Brain Map rendering.
 */
public final class RelatedConceptView {

    private final ConceptRelationship relationship;
    private final Concept connectedConcept;
    private final boolean outgoing;

    public RelatedConceptView(ConceptRelationship relationship, Concept connectedConcept, boolean outgoing) {
        this.relationship = Objects.requireNonNull(relationship, "relationship must not be null");
        this.connectedConcept = Objects.requireNonNull(connectedConcept, "connectedConcept must not be null");
        this.outgoing = outgoing;
    }

    public ConceptRelationship getRelationship() {
        return relationship;
    }

    public Concept getConnectedConcept() {
        return connectedConcept;
    }

    public boolean isOutgoing() {
        return outgoing;
    }

    public ConceptRelationshipType getType() {
        return relationship.getRelationshipType();
    }

    public String getDescription() {
        return relationship.getDescription();
    }
}
