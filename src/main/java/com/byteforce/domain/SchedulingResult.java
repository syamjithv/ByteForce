package com.byteforce.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Result of a spaced-repetition scheduling calculation.
 */
public final class SchedulingResult {

    private final ReviewRating rating;
    private final ReviewState nextState;
    private final double difficulty;
    private final double stability;
    private final double retrievability;
    private final Instant nextDueAt;
    private final Duration interval;

    public SchedulingResult(ReviewRating rating,
                            ReviewState nextState,
                            double difficulty,
                            double stability,
                            double retrievability,
                            Instant nextDueAt,
                            Duration interval) {
        this.rating = Objects.requireNonNull(rating, "rating must not be null");
        this.nextState = Objects.requireNonNull(nextState, "nextState must not be null");
        this.difficulty = difficulty;
        this.stability = stability;
        this.retrievability = retrievability;
        this.nextDueAt = Objects.requireNonNull(nextDueAt, "nextDueAt must not be null");
        this.interval = Objects.requireNonNull(interval, "interval must not be null");
    }

    public ReviewRating getRating() {
        return rating;
    }

    public ReviewState getNextState() {
        return nextState;
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

    public Instant getNextDueAt() {
        return nextDueAt;
    }

    public Duration getInterval() {
        return interval;
    }

    /**
     * Human-friendly label for interval preview (e.g., "10m", "1d", "3d", "1.2mo").
     */
    public String getIntervalLabel() {
        long seconds = interval.getSeconds();
        if (seconds < 60) {
            return "<1m";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "m";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "h";
        }
        long days = hours / 24;
        if (days < 30) {
            return days + "d";
        }
        long months = days / 30;
        if (months < 12) {
            return months + "mo";
        }
        long years = days / 365;
        return years + "y";
    }

    @Override
    public String toString() {
        return "SchedulingResult{" +
                "rating=" + rating +
                ", nextState=" + nextState +
                ", difficulty=" + difficulty +
                ", stability=" + stability +
                ", interval=" + getIntervalLabel() +
                '}';
    }
}
