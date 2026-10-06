package com.byteforce.domain;

import java.util.Objects;

/**
 * Value object representing practice problem-solving performance for a specific topic.
 */
public record TopicPerformance(
        long topicId,
        String topicName,
        long totalAttempts,
        long solvedCount,
        long failedCount,
        long skippedCount,
        double accuracyPercentage
) {

    public TopicPerformance {
        Objects.requireNonNull(topicName, "topicName must not be null");
        if (totalAttempts < 0) throw new IllegalArgumentException("totalAttempts cannot be negative");
        if (solvedCount < 0) throw new IllegalArgumentException("solvedCount cannot be negative");
        if (failedCount < 0) throw new IllegalArgumentException("failedCount cannot be negative");
        if (skippedCount < 0) throw new IllegalArgumentException("skippedCount cannot be negative");
    }

    public static TopicPerformance calculate(
            long topicId,
            String topicName,
            long totalAttempts,
            long solvedCount,
            long failedCount,
            long skippedCount) {

        long evaluated = solvedCount + failedCount;
        double accuracy = evaluated > 0
                ? ((double) solvedCount / evaluated) * 100.0
                : (totalAttempts > 0 ? ((double) solvedCount / totalAttempts) * 100.0 : 0.0);
        double roundedAccuracy = Math.round(accuracy * 10.0) / 10.0;

        return new TopicPerformance(
                topicId,
                topicName,
                totalAttempts,
                solvedCount,
                failedCount,
                skippedCount,
                roundedAccuracy
        );
    }
}
