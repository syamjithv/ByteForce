package com.byteforce.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable domain entity representing a student's answer to an individual question
 * within an assessment attempt.
 */
public final class AssessmentAnswer {

    private final long id;
    private final long assessmentAttemptId;
    private final long questionId;
    private final String submittedAnswer;
    private final int marksAwarded;
    private final AttemptStatus status;
    private final Instant answeredAt;

    public AssessmentAnswer(long id, long assessmentAttemptId, long questionId,
                            String submittedAnswer, int marksAwarded,
                            AttemptStatus status, Instant answeredAt) {
        this.id = id;
        if (assessmentAttemptId <= 0) {
            throw new IllegalArgumentException("assessmentAttemptId must be positive");
        }
        this.assessmentAttemptId = assessmentAttemptId;

        if (questionId <= 0) {
            throw new IllegalArgumentException("questionId must be positive");
        }
        this.questionId = questionId;

        this.submittedAnswer = submittedAnswer;

        if (marksAwarded < 0) {
            throw new IllegalArgumentException("marksAwarded must not be negative");
        }
        this.marksAwarded = marksAwarded;

        this.status = Objects.requireNonNull(status, "status must not be null");
        this.answeredAt = answeredAt;
    }

    /**
     * Factory for creating a new assessment answer (id 0 indicates unsaved).
     */
    public static AssessmentAnswer create(long assessmentAttemptId, long questionId,
                                          String submittedAnswer, int marksAwarded,
                                          AttemptStatus status) {
        Instant answeredAt = (status == AttemptStatus.SKIPPED && submittedAnswer == null) ? null : Instant.now();
        return new AssessmentAnswer(0, assessmentAttemptId, questionId, submittedAnswer, marksAwarded, status, answeredAt);
    }

    /**
     * Factory for recording a skipped/unanswered question.
     */
    public static AssessmentAnswer skipped(long assessmentAttemptId, long questionId) {
        return new AssessmentAnswer(0, assessmentAttemptId, questionId, null, 0, AttemptStatus.SKIPPED, null);
    }

    public long getId() {
        return id;
    }

    public long getAssessmentAttemptId() {
        return assessmentAttemptId;
    }

    public long getQuestionId() {
        return questionId;
    }

    public String getSubmittedAnswer() {
        return submittedAnswer;
    }

    public int getMarksAwarded() {
        return marksAwarded;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }

    public boolean isSkipped() {
        return status == AttemptStatus.SKIPPED;
    }

    public boolean isSolved() {
        return status == AttemptStatus.SOLVED;
    }

    /**
     * Returns a copy with the database-assigned ID.
     */
    public AssessmentAnswer withId(long newId) {
        return new AssessmentAnswer(newId, this.assessmentAttemptId, this.questionId,
                this.submittedAnswer, this.marksAwarded, this.status, this.answeredAt);
    }

    public AssessmentAnswer withAnswer(String newAnswer, int newMarks, AttemptStatus newStatus) {
        return new AssessmentAnswer(this.id, this.assessmentAttemptId, this.questionId,
                newAnswer, newMarks, newStatus, Instant.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssessmentAnswer that = (AssessmentAnswer) o;
        if (id != 0 && that.id != 0) {
            return id == that.id;
        }
        return assessmentAttemptId == that.assessmentAttemptId && questionId == that.questionId;
    }

    @Override
    public int hashCode() {
        return id != 0 ? Long.hashCode(id) : Objects.hash(assessmentAttemptId, questionId);
    }

    @Override
    public String toString() {
        String answerPreview = submittedAnswer == null ? "null"
                : (submittedAnswer.length() <= 30 ? submittedAnswer : submittedAnswer.substring(0, 27) + "...");
        return "AssessmentAnswer{" +
                "id=" + id +
                ", assessmentAttemptId=" + assessmentAttemptId +
                ", questionId=" + questionId +
                ", marksAwarded=" + marksAwarded +
                ", status=" + status +
                ", answer='" + answerPreview + '\'' +
                ", answeredAt=" + answeredAt +
                '}';
    }
}
