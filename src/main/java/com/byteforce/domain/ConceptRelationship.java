package com.byteforce.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable domain entity representing a relational connection between two concepts
 * within the Brain Map knowledge network.
 */
public final class ConceptRelationship {

    private final long id;
    private final long sourceConceptId;
    private final long targetConceptId;
    private final ConceptRelationshipType relationshipType;
    private final String description;
    private final int displayOrder;
    private final Instant createdAt;
    private final Instant updatedAt;

    public ConceptRelationship(long id,
                               long sourceConceptId,
                               long targetConceptId,
                               ConceptRelationshipType relationshipType,
                               String description,
                               int displayOrder,
                               Instant createdAt,
                               Instant updatedAt) {
        this.id = id;
        if (sourceConceptId <= 0) {
            throw new IllegalArgumentException("sourceConceptId must be positive");
        }
        if (targetConceptId <= 0) {
            throw new IllegalArgumentException("targetConceptId must be positive");
        }
        if (sourceConceptId == targetConceptId) {
            throw new IllegalArgumentException("Self-referencing concept relationship is not allowed");
        }
        this.sourceConceptId = sourceConceptId;
        this.targetConceptId = targetConceptId;
        this.relationshipType = Objects.requireNonNull(relationshipType, "relationshipType must not be null");
        this.description = description != null ? description.trim() : "";
        this.displayOrder = displayOrder;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static ConceptRelationship create(long sourceConceptId,
                                            long targetConceptId,
                                            ConceptRelationshipType relationshipType,
                                            String description,
                                            int displayOrder) {
        return new ConceptRelationship(0, sourceConceptId, targetConceptId, relationshipType, description, displayOrder, Instant.now(), Instant.now());
    }

    public static ConceptRelationship of(long sourceConceptId,
                                         long targetConceptId,
                                         ConceptRelationshipType relationshipType) {
        return create(sourceConceptId, targetConceptId, relationshipType, "", 0);
    }

    public long getId() {
        return id;
    }

    public long getSourceConceptId() {
        return sourceConceptId;
    }

    public long getTargetConceptId() {
        return targetConceptId;
    }

    public ConceptRelationshipType getRelationshipType() {
        return relationshipType;
    }

    public String getDescription() {
        return description;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public ConceptRelationship withId(long newId) {
        return new ConceptRelationship(newId, this.sourceConceptId, this.targetConceptId, this.relationshipType, this.description, this.displayOrder, this.createdAt, this.updatedAt);
    }

    public ConceptRelationship withSourceConceptId(long newSource) {
        return new ConceptRelationship(this.id, newSource, this.targetConceptId, this.relationshipType, this.description, this.displayOrder, this.createdAt, Instant.now());
    }

    public ConceptRelationship withTargetConceptId(long newTarget) {
        return new ConceptRelationship(this.id, this.sourceConceptId, newTarget, this.relationshipType, this.description, this.displayOrder, this.createdAt, Instant.now());
    }

    public ConceptRelationship withRelationshipType(ConceptRelationshipType newType) {
        return new ConceptRelationship(this.id, this.sourceConceptId, this.targetConceptId, newType, this.description, this.displayOrder, this.createdAt, Instant.now());
    }

    public ConceptRelationship withDescription(String newDescription) {
        return new ConceptRelationship(this.id, this.sourceConceptId, this.targetConceptId, this.relationshipType, newDescription, this.displayOrder, this.createdAt, Instant.now());
    }

    public ConceptRelationship withDisplayOrder(int newDisplayOrder) {
        return new ConceptRelationship(this.id, this.sourceConceptId, this.targetConceptId, this.relationshipType, this.description, newDisplayOrder, this.createdAt, Instant.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConceptRelationship that = (ConceptRelationship) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return "ConceptRelationship{" +
                "id=" + id +
                ", sourceConceptId=" + sourceConceptId +
                ", targetConceptId=" + targetConceptId +
                ", relationshipType=" + relationshipType +
                ", description='" + description + '\'' +
                '}';
    }
}
