package com.byteforce.service.memory;

import com.byteforce.domain.ReviewRating;
import com.byteforce.domain.ReviewState;
import com.byteforce.domain.SchedulingResult;
import com.byteforce.domain.UserRememberReview;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Free Spaced Repetition Scheduler (FSRS) implementation.
 *
 * Based on the DSR (Difficulty, Stability, Retrievability) model of human memory.
 * - Stability (S): Duration (in days) over which retrievability stays >= target retention (default 90%).
 * - Difficulty (D): Inherent difficulty scale [1.0, 10.0].
 * - Retrievability (R): Probability of recalling the item at time t after previous review.
 *
 * Implements authoritative mathematical formulas for:
 * 1. Initial Stability & Difficulty S_0(G), D_0(G)
 * 2. Retrievability decay R(t, S) = (1 + t / (9 * S))^-1
 * 3. Difficulty updating D'(D, G) with mean reversion
 * 4. Stability updating on recall S_r(D, S, R, G) and on lapse S_f(D, S, R)
 * 5. Target-retention based interval derivation
 */
public class FsrsScheduler implements SpacedRepetitionScheduler {

    /**
     * Target retention rate (0.90 = 90% probability of successful recall when due).
     */
    public static final double TARGET_RETENTION = 0.90;

    // FSRS initial stabilities by rating G (1: Again, 2: Hard, 3: Good, 4: Easy)
    private static final double W0_AGAIN_STABILITY = 0.4;  // ~9.6 hours
    private static final double W1_HARD_STABILITY = 0.9;   // ~21.6 hours
    private static final double W2_GOOD_STABILITY = 2.4;   // 2.4 days
    private static final double W3_EASY_STABILITY = 5.8;   // 5.8 days

    // Initial difficulty baseline and rating step
    private static final double BASE_DIFFICULTY = 5.0;
    private static final double DIFFICULTY_STEP = 1.2;

    // Stability update parameters
    private static final double W8 = 1.49;
    private static final double W9 = 0.14;
    private static final double W10 = 0.94;
    private static final double HARD_PENALTY = 0.65;
    private static final double EASY_BONUS = 1.35;

    // Lapse parameters (AGAIN)
    private static final double W11 = 0.40;
    private static final double W12 = 0.25;
    private static final double W13 = 0.20;
    private static final double W14 = 0.30;

    // Minimum and maximum bounds
    private static final double MIN_DIFFICULTY = 1.0;
    private static final double MAX_DIFFICULTY = 10.0;
    private static final double MIN_STABILITY = 0.1; // ~2.4 hours minimum

    @Override
    public SchedulingResult schedule(UserRememberReview currentReview, ReviewRating rating, Instant reviewTime) {
        Objects.requireNonNull(rating, "rating must not be null");
        Instant now = reviewTime != null ? reviewTime : Instant.now();

        if (currentReview == null || currentReview.getState() == ReviewState.NEW || currentReview.getReviewCount() == 0) {
            return scheduleNew(rating, now);
        } else {
            return scheduleExisting(currentReview, rating, now);
        }
    }

    private SchedulingResult scheduleNew(ReviewRating rating, Instant now) {
        double stability;
        double difficulty;
        ReviewState nextState;

        switch (rating) {
            case AGAIN -> {
                stability = W0_AGAIN_STABILITY;
                difficulty = clampDifficulty(BASE_DIFFICULTY + (2 * DIFFICULTY_STEP)); // ~7.4
                nextState = ReviewState.LEARNING;
            }
            case HARD -> {
                stability = W1_HARD_STABILITY;
                difficulty = clampDifficulty(BASE_DIFFICULTY + DIFFICULTY_STEP);       // ~6.2
                nextState = ReviewState.LEARNING;
            }
            case GOOD -> {
                stability = W2_GOOD_STABILITY;
                difficulty = clampDifficulty(BASE_DIFFICULTY);                        // ~5.0
                nextState = ReviewState.REVIEW;
            }
            case EASY -> {
                stability = W3_EASY_STABILITY;
                difficulty = clampDifficulty(BASE_DIFFICULTY - DIFFICULTY_STEP);       // ~3.8
                nextState = ReviewState.REVIEW;
            }
            default -> throw new IllegalArgumentException("Unsupported rating: " + rating);
        }

        double retrievability = 1.0; // Immediately after first encounter
        Duration interval = calculateInterval(stability, rating);
        Instant nextDueAt = now.plus(interval);

        return new SchedulingResult(rating, nextState, difficulty, stability, retrievability, nextDueAt, interval);
    }

    private SchedulingResult scheduleExisting(UserRememberReview current, ReviewRating rating, Instant now) {
        double currentStability = Math.max(MIN_STABILITY, current.getStability());
        double currentDifficulty = clampDifficulty(current.getDifficulty());

        // Elapsed time t in fractional days
        Instant lastReviewed = current.getLastReviewedAt() != null ? current.getLastReviewedAt() : current.getCreatedAt();
        long elapsedSeconds = Math.max(0, Duration.between(lastReviewed, now).getSeconds());
        double elapsedDays = elapsedSeconds / 86400.0;

        // Current retrievability before the review
        double retrievability = calculateRetrievability(currentStability, elapsedDays);

        // Next difficulty with mean reversion towards 5.0
        int grade = rating.getValue(); // 1..4
        double rawD = currentDifficulty - 0.8 * (grade - 3);
        double nextDifficulty = clampDifficulty(0.1 * BASE_DIFFICULTY + 0.9 * rawD);

        double nextStability;
        ReviewState nextState;

        if (rating == ReviewRating.AGAIN) {
            // Lapse formula S_f
            nextStability = W11 * Math.pow(nextDifficulty, -W12)
                    * (Math.pow(currentStability + 1.0, W13) - 1.0)
                    * Math.exp((1.0 - retrievability) * W14);
            nextStability = Math.max(MIN_STABILITY, nextStability);
            nextState = ReviewState.RELEARNING;
        } else {
            // Successful recall S_r
            double h = (rating == ReviewRating.HARD) ? HARD_PENALTY : 1.0;
            double b = (rating == ReviewRating.EASY) ? EASY_BONUS : 1.0;

            double expTerm = Math.exp((1.0 - retrievability) * W10) - 1.0;
            double sFactor = 1.0 + Math.exp(W8) * (11.0 - nextDifficulty) * Math.pow(currentStability, -W9) * expTerm * h * b;

            nextStability = currentStability * Math.max(1.05, sFactor);
            nextState = ReviewState.REVIEW;
        }

        Duration interval = calculateInterval(nextStability, rating);
        Instant nextDueAt = now.plus(interval);

        return new SchedulingResult(rating, nextState, nextDifficulty, nextStability, retrievability, nextDueAt, interval);
    }

    @Override
    public Map<ReviewRating, SchedulingResult> previewAllRatings(UserRememberReview currentReview, Instant reviewTime) {
        Instant now = reviewTime != null ? reviewTime : Instant.now();
        Map<ReviewRating, SchedulingResult> previews = new EnumMap<>(ReviewRating.class);
        for (ReviewRating r : ReviewRating.values()) {
            previews.put(r, schedule(currentReview, r, now));
        }
        return previews;
    }

    @Override
    public double calculateRetrievability(double stability, Instant lastReviewedAt, Instant now) {
        if (stability <= 0.0) {
            return 0.0;
        }
        if (lastReviewedAt == null || now == null) {
            return 1.0;
        }
        long elapsedSeconds = Math.max(0, Duration.between(lastReviewedAt, now).getSeconds());
        double elapsedDays = elapsedSeconds / 86400.0;
        return calculateRetrievability(stability, elapsedDays);
    }

    private double calculateRetrievability(double stability, double elapsedDays) {
        if (elapsedDays <= 0.0) {
            return 1.0;
        }
        if (stability <= 0.0) {
            return 0.0;
        }
        // Power-law retrievability decay: R = (1 + t / (9 * S))^-1
        // When t == S, R = (1 + 1/9)^-1 = 0.90 (matching TARGET_RETENTION)
        return 1.0 / (1.0 + (elapsedDays / (9.0 * stability)));
    }

    private Duration calculateInterval(double stability, ReviewRating rating) {
        // Derive interval from stability: at target retention 90%, interval in days = stability
        double days = stability;
        long seconds = Math.round(days * 86400.0);

        // Safety clamps based on rating semantics
        if (rating == ReviewRating.AGAIN) {
            // Relearning minimum interval: 10 minutes (600s), up to max of 12 hours
            seconds = Math.max(600, Math.min(seconds, 43200));
        } else if (rating == ReviewRating.HARD) {
            // Hard minimum interval: 1 day (86400s)
            seconds = Math.max(86400, seconds);
        } else if (rating == ReviewRating.GOOD) {
            // Good minimum interval: 1 day (86400s)
            seconds = Math.max(86400, seconds);
        } else if (rating == ReviewRating.EASY) {
            // Easy minimum interval: 2 days (172800s)
            seconds = Math.max(172800, seconds);
        }

        return Duration.ofSeconds(seconds);
    }

    private double clampDifficulty(double d) {
        return Math.max(MIN_DIFFICULTY, Math.min(MAX_DIFFICULTY, Math.round(d * 100.0) / 100.0));
    }
}
