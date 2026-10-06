package com.byteforce.domain;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing progress and problem-solving statistics for a user,
 * including aggregate solve rates, difficulty breakdowns, and topic metrics.
 */
public record UserProgress(
        UUID userId,
        long totalAttempts,
        long solvedQuestions,
        double solveRatePercentage,
        Map<Difficulty, Long> solvedByDifficulty,
        Map<String, Long> solvedByTopic
) {

    public UserProgress {
        Objects.requireNonNull(userId, "userId must not be null");
        if (totalAttempts < 0) {
            throw new IllegalArgumentException("totalAttempts cannot be negative");
        }
        if (solvedQuestions < 0) {
            throw new IllegalArgumentException("solvedQuestions cannot be negative");
        }
        if (solveRatePercentage < 0.0 || solveRatePercentage > 100.0) {
            throw new IllegalArgumentException("solveRatePercentage must be between 0.0 and 100.0");
        }
        solvedByDifficulty = solvedByDifficulty != null ? Collections.unmodifiableMap(solvedByDifficulty) : Map.of();
        solvedByTopic = solvedByTopic != null ? Collections.unmodifiableMap(solvedByTopic) : Map.of();
    }

    public UserProgress(UUID userId, long totalAttempts, long solvedQuestions, double solveRatePercentage) {
        this(userId, totalAttempts, solvedQuestions, solveRatePercentage, Map.of(), Map.of());
    }

    public static UserProgress calculate(UUID userId, long totalAttempts, long solvedQuestions) {
        return calculate(userId, totalAttempts, solvedQuestions, Map.of(), Map.of());
    }

    public static UserProgress calculate(UUID userId, long totalAttempts, long solvedQuestions,
                                         Map<Difficulty, Long> solvedByDifficulty,
                                         Map<String, Long> solvedByTopic) {
        double rate = totalAttempts > 0 ? ((double) solvedQuestions / (double) totalAttempts) * 100.0 : 0.0;
        double roundedRate = Math.round(rate * 100.0) / 100.0;
        return new UserProgress(userId, totalAttempts, solvedQuestions, roundedRate, solvedByDifficulty, solvedByTopic);
    }
}
