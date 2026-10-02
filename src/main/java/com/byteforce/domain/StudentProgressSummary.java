package com.byteforce.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing comprehensive student progress metrics
 * across practice questions, assessments, and bookmarks.
 */
public record StudentProgressSummary(
        UUID userId,
        long totalAttempts,
        long solvedCount,
        long failedCount,
        long skippedCount,
        double accuracyPercentage,
        long assessmentsCompleted,
        long bookmarkedCount
) {

    public StudentProgressSummary {
        Objects.requireNonNull(userId, "userId must not be null");
        if (totalAttempts < 0) throw new IllegalArgumentException("totalAttempts cannot be negative");
        if (solvedCount < 0) throw new IllegalArgumentException("solvedCount cannot be negative");
        if (failedCount < 0) throw new IllegalArgumentException("failedCount cannot be negative");
        if (skippedCount < 0) throw new IllegalArgumentException("skippedCount cannot be negative");
        if (assessmentsCompleted < 0) throw new IllegalArgumentException("assessmentsCompleted cannot be negative");
        if (bookmarkedCount < 0) throw new IllegalArgumentException("bookmarkedCount cannot be negative");
    }

    public static StudentProgressSummary empty(UUID userId) {
        return new StudentProgressSummary(userId, 0, 0, 0, 0, 0.0, 0, 0);
    }

    public static StudentProgressSummary calculate(
            UUID userId,
            long totalAttempts,
            long solvedCount,
            long failedCount,
            long skippedCount,
            long assessmentsCompleted,
            long bookmarkedCount) {

        long evaluatedAttempts = solvedCount + failedCount;
        double accuracy = evaluatedAttempts > 0
                ? ((double) solvedCount / evaluatedAttempts) * 100.0
                : (totalAttempts > 0 ? ((double) solvedCount / totalAttempts) * 100.0 : 0.0);
        double roundedAccuracy = Math.round(accuracy * 10.0) / 10.0;

        return new StudentProgressSummary(
                userId,
                totalAttempts,
                solvedCount,
                failedCount,
                skippedCount,
                roundedAccuracy,
                assessmentsCompleted,
                bookmarkedCount
        );
    }
}
