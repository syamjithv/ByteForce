package com.byteforce.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable domain entity representing a user's attempt at solving a question.
 */
public final class QuestionAttempt {

    private final long id;
    private final UUID userId;
    private final long questionId;
    private final AttemptStatus status;
    private final String codeSnippet;
    private final Integer executionTimeMs;
    private final Instant attemptedAt;

    public QuestionAttempt(long id, UUID userId, long questionId, AttemptStatus status,
                           String codeSnippet, Integer executionTimeMs, Instant attemptedAt) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        if (questionId <= 0) {
            throw new IllegalArgumentException("questionId must be positive");
        }
        this.questionId = questionId;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.codeSnippet = codeSnippet;
        if (executionTimeMs != null && executionTimeMs < 0) {
            throw new IllegalArgumentException("executionTimeMs must not be negative");
        }
        this.executionTimeMs = executionTimeMs;
        this.attemptedAt = Objects.requireNonNull(attemptedAt, "attemptedAt must not be null");
    }

    /**
     * Factory for creating a new question attempt (id 0 indicates unsaved).
     */
    public static QuestionAttempt create(UUID userId, long questionId, AttemptStatus status,
                                         String codeSnippet, Integer executionTimeMs) {
        return new QuestionAttempt(0, userId, questionId, status, codeSnippet, executionTimeMs, Instant.now());
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

    public AttemptStatus getStatus() {
        return status;
    }

    public String getCodeSnippet() {
        return codeSnippet;
    }

    public Integer getExecutionTimeMs() {
        return executionTimeMs;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }

    /**
     * Checks if this attempt represents a successfully solved solution.
     */
    public boolean isSolved() {
        return status == AttemptStatus.SOLVED;
    }

    /**
     * Returns a copy of this attempt with the database-assigned ID.
     */
    public QuestionAttempt withId(long newId) {
        return new QuestionAttempt(newId, this.userId, this.questionId, this.status,
                this.codeSnippet, this.executionTimeMs, this.attemptedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuestionAttempt that = (QuestionAttempt) o;
        if (id != 0 && that.id != 0) {
            return id == that.id;
        }
        return questionId == that.questionId
                && Objects.equals(userId, that.userId)
                && status == that.status
                && Objects.equals(attemptedAt, that.attemptedAt);
    }

    @Override
    public int hashCode() {
        return id != 0 ? Long.hashCode(id) : Objects.hash(userId, questionId, status, attemptedAt);
    }

    @Override
    public String toString() {
        String snippetPreview = codeSnippet == null ? "null"
                : (codeSnippet.length() <= 30 ? codeSnippet : codeSnippet.substring(0, 27) + "...");
        return "QuestionAttempt{" +
                "id=" + id +
                ", userId=" + userId +
                ", questionId=" + questionId +
                ", status=" + status +
                ", executionTimeMs=" + executionTimeMs +
                ", codeSnippet='" + snippetPreview + '\'' +
                ", attemptedAt=" + attemptedAt +
                '}';
    }
}
