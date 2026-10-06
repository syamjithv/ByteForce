package com.byteforce.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable domain entity representing a student's personal spaced repetition
 * review state for a specific {@link RememberItem}.
 */
public final class UserRememberReview {

    private final long id;
    private final UUID userId;
    private final long rememberItemId;
    private final Instant dueAt;
    private final Instant lastReviewedAt;
    private final int reviewCount;
    private final int successfulReviewCount;
    private final ReviewState state;
    private final double difficulty;
    private final double stability;
    private final double retrievability;
    private final ReviewRating lastRating;
    private final Instant createdAt;
    private final Instant updatedAt;

    public UserRememberReview(long id,
                              UUID userId,
                              long rememberItemId,
                              Instant dueAt,
                              Instant lastReviewedAt,
                              int reviewCount,
                              int successfulReviewCount,
                              ReviewState state,
                              double difficulty,
                              double stability,
                              double retrievability,
                              ReviewRating lastRating,
                              Instant createdAt,
                              Instant updatedAt) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        if (rememberItemId <= 0) {
            throw new IllegalArgumentException("rememberItemId must be positive");
        }
        this.rememberItemId = rememberItemId;
        this.dueAt = Objects.requireNonNull(dueAt, "dueAt must not be null");
        this.lastReviewedAt = lastReviewedAt;
        this.reviewCount = Math.max(0, reviewCount);
        this.successfulReviewCount = Math.max(0, successfulReviewCount);
        this.state = state != null ? state : ReviewState.NEW;
        this.difficulty = difficulty;
        this.stability = stability;
        this.retrievability = retrievability;
        this.lastRating = lastRating;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static UserRememberReview createInitial(UUID userId, long rememberItemId, Instant dueAt) {
        Instant now = Instant.now();
        return new UserRememberReview(
                0,
                userId,
                rememberItemId,
                dueAt != null ? dueAt : now,
                null,
                0,
                0,
                ReviewState.NEW,
                0.0,
                0.0,
                0.0,
                null,
                now,
                now
        );
    }

    public long getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public long getRememberItemId() {
        return rememberItemId;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getLastReviewedAt() {
        return lastReviewedAt;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public int getSuccessfulReviewCount() {
        return successfulReviewCount;
    }

    public ReviewState getState() {
        return state;
    }

    public double getDifficulty() {
        return difficulty;
    }

    public double getStability() {
        return stability;
    }

    public double getRetrievability() {
        return retrievability;
    }

    public ReviewRating getLastRating() {
        return lastRating;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isDue(Instant now) {
        return !dueAt.isAfter(now);
    }

    public UserRememberReview withId(long newId) {
        return new UserRememberReview(
                newId,
                this.userId,
                this.rememberItemId,
                this.dueAt,
                this.lastReviewedAt,
                this.reviewCount,
                this.successfulReviewCount,
                this.state,
                this.difficulty,
                this.stability,
                this.retrievability,
                this.lastRating,
                this.createdAt,
                this.updatedAt
        );
    }

    public UserRememberReview withReviewResult(ReviewRating rating,
                                              ReviewState nextState,
                                              double nextDifficulty,
                                              double nextStability,
                                              double retrievability,
                                              Instant nextDueAt,
                                              Instant reviewTimestamp) {
        boolean successful = rating != ReviewRating.AGAIN;
        return new UserRememberReview(
                this.id,
                this.userId,
                this.rememberItemId,
                nextDueAt,
                reviewTimestamp,
                this.reviewCount + 1,
                this.successfulReviewCount + (successful ? 1 : 0),
                nextState,
                nextDifficulty,
                nextStability,
                retrievability,
                rating,
                this.createdAt,
                reviewTimestamp
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserRememberReview that = (UserRememberReview) o;
        return id == that.id && userId.equals(that.userId) && rememberItemId == that.rememberItemId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, rememberItemId);
    }

    @Override
    public String toString() {
        return "UserRememberReview{" +
                "id=" + id +
                ", userId=" + userId +
                ", rememberItemId=" + rememberItemId +
                ", dueAt=" + dueAt +
                ", state=" + state +
                ", reviewCount=" + reviewCount +
                ", difficulty=" + difficulty +
                ", stability=" + stability +
                '}';
    }
}
