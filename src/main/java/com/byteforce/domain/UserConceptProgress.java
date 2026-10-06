package com.byteforce.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing an authenticated student's learning progress
 * and viewing state for a specific concept.
 */
public final class UserConceptProgress {

    private final long id;
    private final UUID userId;
    private final long conceptId;
    private final boolean completed;
    private final Instant completedAt;
    private final Instant lastViewedAt;

    public UserConceptProgress(long id,
                               UUID userId,
                               long conceptId,
                               boolean completed,
                               Instant completedAt,
                               Instant lastViewedAt) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.conceptId = conceptId;
        this.completed = completed;
        this.completedAt = completedAt;
        this.lastViewedAt = lastViewedAt != null ? lastViewedAt : Instant.now();
    }

    public static UserConceptProgress viewed(UUID userId, long conceptId) {
        return new UserConceptProgress(0, userId, conceptId, false, null, Instant.now());
    }

    public static UserConceptProgress completed(UUID userId, long conceptId) {
        Instant now = Instant.now();
        return new UserConceptProgress(0, userId, conceptId, true, now, now);
    }

    public long getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public long getConceptId() {
        return conceptId;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getLastViewedAt() {
        return lastViewedAt;
    }

    public UserConceptProgress withId(long newId) {
        return new UserConceptProgress(newId, userId, conceptId, completed, completedAt, lastViewedAt);
    }

    public UserConceptProgress markLearned(boolean isLearned) {
        Instant now = Instant.now();
        return new UserConceptProgress(
                id,
                userId,
                conceptId,
                isLearned,
                isLearned ? now : null,
                now
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserConceptProgress that = (UserConceptProgress) o;
        return conceptId == that.conceptId && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, conceptId);
    }

    @Override
    public String toString() {
        return "UserConceptProgress{" +
                "id=" + id +
                ", userId=" + userId +
                ", conceptId=" + conceptId +
                ", completed=" + completed +
                ", lastViewedAt=" + lastViewedAt +
                '}';
    }
}
