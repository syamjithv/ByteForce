package com.byteforce.domain;

import java.util.List;

/**
 * Honest, database-backed memory metrics and analytics.
 */
public final class MemoryAnalytics {

    private final long totalEnrolledItems;
    private final long dueCount;
    private final long totalReviewsCompleted;
    private final long successfulReviews;
    private final double successRate;
    private final long stableCount;
    private final long learningCount;
    private final long relearningCount;
    private final int currentStreakDays;
    private final List<MemoryReviewItemView> weakItems;
    private final boolean hasSufficientData;

    public MemoryAnalytics(long totalEnrolledItems,
                           long dueCount,
                           long totalReviewsCompleted,
                           long successfulReviews,
                           double successRate,
                           long stableCount,
                           long learningCount,
                           long relearningCount,
                           int currentStreakDays,
                           List<MemoryReviewItemView> weakItems,
                           boolean hasSufficientData) {
        this.totalEnrolledItems = totalEnrolledItems;
        this.dueCount = dueCount;
        this.totalReviewsCompleted = totalReviewsCompleted;
        this.successfulReviews = successfulReviews;
        this.successRate = successRate;
        this.stableCount = stableCount;
        this.learningCount = learningCount;
        this.relearningCount = relearningCount;
        this.currentStreakDays = currentStreakDays;
        this.weakItems = weakItems != null ? weakItems : List.of();
        this.hasSufficientData = hasSufficientData;
    }

    public static MemoryAnalytics empty() {
        return new MemoryAnalytics(0, 0, 0, 0, 0.0, 0, 0, 0, 0, List.of(), false);
    }

    public long getTotalEnrolledItems() {
        return totalEnrolledItems;
    }

    public long getDueCount() {
        return dueCount;
    }

    public long getTotalReviewsCompleted() {
        return totalReviewsCompleted;
    }

    public long getSuccessfulReviews() {
        return successfulReviews;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public String getFormattedSuccessRate() {
        if (!hasSufficientData) {
            return "—";
        }
        return Math.round(successRate) + "%";
    }

    public long getStableCount() {
        return stableCount;
    }

    public long getLearningCount() {
        return learningCount;
    }

    public long getRelearningCount() {
        return relearningCount;
    }

    public int getCurrentStreakDays() {
        return currentStreakDays;
    }

    public List<MemoryReviewItemView> getWeakItems() {
        return weakItems;
    }

    public boolean isHasSufficientData() {
        return hasSufficientData;
    }
}
