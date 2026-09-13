package com.byteforce.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable domain entity representing a topic category for questions.
 */
public final class Topic {

    private final long id;
    private final String name;
    private final String slug;
    private final String description;
    private final int displayOrder;
    private final Instant createdAt;

    public Topic(long id, String name, String slug, String description, int displayOrder, Instant createdAt) {
        this.id = id;

        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.name = name.trim();

        Objects.requireNonNull(slug, "slug must not be null");
        if (slug.isBlank()) {
            throw new IllegalArgumentException("slug must not be blank");
        }
        this.slug = slug.trim().toLowerCase();

        this.description = description;
        this.displayOrder = displayOrder;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    /**
     * Factory for creating a new topic (id 0 indicates unsaved).
     */
    public static Topic create(String name, String slug, String description, int displayOrder) {
        return new Topic(0, name, slug, description, displayOrder, Instant.now());
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
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

    /**
     * Returns a copy with the database-assigned id.
     */
    public Topic withId(long newId) {
        return new Topic(newId, this.name, this.slug, this.description, this.displayOrder, this.createdAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Topic topic = (Topic) o;
        return id == topic.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return "Topic{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", slug='" + slug + '\'' +
                ", displayOrder=" + displayOrder +
                '}';
    }
}
