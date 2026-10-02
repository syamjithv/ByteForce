package com.byteforce.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable domain entity representing a concise quick-revision item
 * attached to a placement concept (Key Fact, Common Confusion, Formula, etc.).
 */
public final class RememberItem {

    private final long id;
    private final long conceptId;
    private final RememberItemType type;
    private final String content;
    private final int displayOrder;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    public RememberItem(long id,
                        long conceptId,
                        RememberItemType type,
                        String content,
                        int displayOrder,
                        boolean active,
                        Instant createdAt,
                        Instant updatedAt) {
        this.id = id;
        this.conceptId = conceptId;
        this.type = Objects.requireNonNull(type, "type must not be null");

        Objects.requireNonNull(content, "content must not be null");
        if (content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        this.content = content.trim();

        this.displayOrder = displayOrder;
        this.active = active;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static RememberItem create(long conceptId, RememberItemType type, String content, int displayOrder) {
        return new RememberItem(0, conceptId, type, content, displayOrder, true, Instant.now(), Instant.now());
    }

    public static RememberItem create(long id, long conceptId, RememberItemType type, String content, int displayOrder) {
        return new RememberItem(id, conceptId, type, content, displayOrder, true, Instant.now(), Instant.now());
    }

    public long getId() {
        return id;
    }

    public long getConceptId() {
        return conceptId;
    }

    public RememberItemType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public RememberItem withId(long newId) {
        return new RememberItem(newId, this.conceptId, this.type, this.content, this.displayOrder, this.active, this.createdAt, this.updatedAt);
    }

    public RememberItem withContent(String newContent) {
        return new RememberItem(this.id, this.conceptId, this.type, newContent, this.displayOrder, this.active, this.createdAt, Instant.now());
    }

    public RememberItem withType(RememberItemType newType) {
        return new RememberItem(this.id, this.conceptId, newType, this.content, this.displayOrder, this.active, this.createdAt, Instant.now());
    }

    public RememberItem withDisplayOrder(int newOrder) {
        return new RememberItem(this.id, this.conceptId, this.type, this.content, newOrder, this.active, this.createdAt, Instant.now());
    }

    public RememberItem withActive(boolean newActive) {
        return new RememberItem(this.id, this.conceptId, this.type, this.content, this.displayOrder, newActive, this.createdAt, Instant.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RememberItem that = (RememberItem) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return "RememberItem{" +
                "id=" + id +
                ", conceptId=" + conceptId +
                ", type=" + type +
                ", content='" + content + '\'' +
                ", displayOrder=" + displayOrder +
                ", active=" + active +
                '}';
    }
}
