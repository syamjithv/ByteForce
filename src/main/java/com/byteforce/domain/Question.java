package com.byteforce.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable domain entity representing a coding question.
 */
public final class Question {

    private final long id;
    private final long topicId;
    private final String title;
    private final String slug;
    private final String description;
    private final Difficulty difficulty;
    private final String solution;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Question(long id, long topicId, String title, String slug, String description,
                    Difficulty difficulty, String solution, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.topicId = topicId;

        Objects.requireNonNull(title, "title must not be null");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        this.title = title.trim();

        Objects.requireNonNull(slug, "slug must not be null");
        if (slug.isBlank()) {
            throw new IllegalArgumentException("slug must not be blank");
        }
        this.slug = slug.trim().toLowerCase();

        Objects.requireNonNull(description, "description must not be null");
        if (description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        this.description = description.trim();

        this.difficulty = Objects.requireNonNull(difficulty, "difficulty must not be null");
        this.solution = solution;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    /**
     * Factory for creating a new question (id 0 indicates unsaved).
     */
    public static Question create(long topicId, String title, String slug, String description,
                                  Difficulty difficulty, String solution) {
        Instant now = Instant.now();
        return new Question(0, topicId, title, slug, description, difficulty, solution, now, now);
    }

    public long getId() {
        return id;
    }

    public long getTopicId() {
        return topicId;
    }

    public String getTitle() {
        return title;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public String getSolution() {
        return solution;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Returns a copy with the database-assigned id.
     */
    public Question withId(long newId) {
        return new Question(newId, this.topicId, this.title, this.slug, this.description,
                this.difficulty, this.solution, this.createdAt, this.updatedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Question question = (Question) o;
        return id == question.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return "Question{" +
                "id=" + id +
                ", topicId=" + topicId +
                ", title='" + title + '\'' +
                ", difficulty=" + difficulty +
                '}';
    }
}
