package com.byteforce.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Value object summarizing a student's completed assessment attempt for progress tracking.
 */
public record AssessmentAttemptSummary(
        long attemptId,
        long assessmentId,
        String assessmentTitle,
        int score,
        int totalMarks,
        double percentage,
        String status,
        Instant completedAt
) {

    public AssessmentAttemptSummary {
        Objects.requireNonNull(assessmentTitle, "assessmentTitle must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }

    public static AssessmentAttemptSummary of(
            long attemptId,
            long assessmentId,
            String assessmentTitle,
            int score,
            int totalMarks,
            String status,
            Instant completedAt) {

        double pct = totalMarks > 0 ? ((double) score / totalMarks) * 100.0 : 0.0;
        double roundedPct = Math.round(pct * 10.0) / 10.0;

        return new AssessmentAttemptSummary(
                attemptId,
                assessmentId,
                assessmentTitle,
                score,
                totalMarks,
                roundedPct,
                status,
                completedAt
        );
    }
}
