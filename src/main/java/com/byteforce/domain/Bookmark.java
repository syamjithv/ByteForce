package com.byteforce.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable domain entity representing a user's bookmark on a question.
 */
public final class Bookmark {

    private final long id;
    private final UUID userId;
    private final long questionId;
    private final String notes;
    private final Instant createdAt;

    public Bookmark(long id, UUID userId, long questionId, String notes, Instant createdAt) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.questionId = questionId;
        this.notes = notes;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    /**
     * Factory for creating a new bookmark (id 0 indicates unsaved).
     */
    public static Bookmark create(UUID userId, long questionId, String notes) {
        return new Bookmark(0, userId, questionId, notes, Instant.now());
    }

    public long getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public long getQuestionId() {
        return questionId;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Bookmark withId(long newId) {
        return new Bookmark(newId, this.userId, this.questionId, this.notes, this.createdAt);
    }

    public Bookmark withNotes(String newNotes) {
        return new Bookmark(this.id, this.userId, this.questionId, newNotes, this.createdAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bookmark bookmark = (Bookmark) o;
        return id == bookmark.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return "Bookmark{" +
                "id=" + id +
                ", userId=" + userId +
                ", questionId=" + questionId +
                '}';
    }
}
