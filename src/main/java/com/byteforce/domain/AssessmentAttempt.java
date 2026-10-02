package com.byteforce.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable domain entity representing a student's attempt of an assessment.
 */
public final class AssessmentAttempt {

    private final long id;
    private final long assessmentId;
    private final UUID userId;
    private final int score;
    private final String status;
    private final Instant startedAt;
    private final Instant completedAt;

    public AssessmentAttempt(long id, long assessmentId, UUID userId, int score,
                             String status, Instant startedAt, Instant completedAt) {
        this.id = id;

        if (assessmentId <= 0) {
            throw new IllegalArgumentException("assessmentId must be positive");
        }
        this.assessmentId = assessmentId;

        this.userId = Objects.requireNonNull(userId, "userId must not be null");

        if (score < 0) {
            throw new IllegalArgumentException("score cannot be negative");
        }
        this.score = score;

        this.status = Objects.requireNonNull(status, "status must not be null");
        this.startedAt = startedAt != null ? startedAt : Instant.now();
        this.completedAt = completedAt;
    }

    public static AssessmentAttempt start(long assessmentId, UUID userId) {
        return new AssessmentAttempt(0, assessmentId, userId, 0, "IN_PROGRESS", Instant.now(), null);
    }

    public long getId() {
        return id;
    }

    public long getAssessmentId() {
        return assessmentId;
    }

    public UUID getUserId() {
        return userId;
    }

    public int getScore() {
        return score;
    }

    public String getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public boolean isCompleted() {
        return "COMPLETED".equalsIgnoreCase(status);
    }

    public AssessmentAttempt withId(long newId) {
        return new AssessmentAttempt(newId, this.assessmentId, this.userId, this.score,
                this.status, this.startedAt, this.completedAt);
    }

    public AssessmentAttempt complete(int finalScore) {
        return new AssessmentAttempt(this.id, this.assessmentId, this.userId, finalScore,
                "COMPLETED", this.startedAt, Instant.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssessmentAttempt that = (AssessmentAttempt) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return "AssessmentAttempt{" +
                "id=" + id +
                ", assessmentId=" + assessmentId +
                ", userId=" + userId +
                ", score=" + score +
                ", status='" + status + '\'' +
                ", startedAt=" + startedAt +
                ", completedAt=" + completedAt +
                '}';
    }
}
