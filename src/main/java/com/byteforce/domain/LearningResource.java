package com.byteforce.domain;

import java.util.Objects;

/**
 * Immutable domain entity representing an external or reference learning resource
 * attached to a placement concept (e.g. PDF cheatsheet, video lecture, or article).
 */
public final class LearningResource {

    private final long id;
    private final long conceptId;
    private final String title;
    private final ResourceType resourceType;
    private final String url;
    private final String description;

    public LearningResource(long id, long conceptId, String title, ResourceType resourceType, String url, String description) {
        this.id = id;
        this.conceptId = conceptId;

        Objects.requireNonNull(title, "title must not be null");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        this.title = title.trim();

        this.resourceType = Objects.requireNonNull(resourceType, "resourceType must not be null");

        Objects.requireNonNull(url, "url must not be null");
        if (url.isBlank()) {
            throw new IllegalArgumentException("url must not be blank");
        }
        this.url = url.trim();

        this.description = description != null ? description.trim() : "";
    }

    public LearningResource(String title, ResourceType resourceType, String url, String description) {
        this(0, 0, title, resourceType, url, description);
    }

    public static LearningResource create(String title, ResourceType resourceType, String url, String description) {
        return new LearningResource(0, 0, title, resourceType, url, description);
    }

    public static LearningResource create(long id, long conceptId, String title, ResourceType resourceType, String url, String description) {
        return new LearningResource(id, conceptId, title, resourceType, url, description);
    }

    public static LearningResource of(String title, ResourceType resourceType, String url) {
        return new LearningResource(0, 0, title, resourceType, url, "");
    }

    public long getId() {
        return id;
    }

    public long getConceptId() {
        return conceptId;
    }

    public String getTitle() {
        return title;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public ResourceType getType() {
        return resourceType;
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }

    public LearningResource withId(long newId) {
        return new LearningResource(newId, this.conceptId, this.title, this.resourceType, this.url, this.description);
    }

    public LearningResource withConceptId(long newConceptId) {
        return new LearningResource(this.id, newConceptId, this.title, this.resourceType, this.url, this.description);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LearningResource that = (LearningResource) o;
        if (id > 0 && that.id > 0) {
            return id == that.id;
        }
        return title.equals(that.title) &&
                resourceType == that.resourceType &&
                url.equals(that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, resourceType, url);
    }

    @Override
    public String toString() {
        return "LearningResource{" +
                "id=" + id +
                ", conceptId=" + conceptId +
                ", title='" + title + '\'' +
                ", resourceType=" + resourceType +
                ", url='" + url + '\'' +
                '}';
    }
}
