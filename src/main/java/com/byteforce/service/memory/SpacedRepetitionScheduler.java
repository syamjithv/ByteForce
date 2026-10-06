package com.byteforce.service.memory;

import com.byteforce.domain.ReviewRating;
import com.byteforce.domain.SchedulingResult;
import com.byteforce.domain.UserRememberReview;

import java.time.Instant;
import java.util.Map;

/**
 * Interface defining the spaced repetition scheduling contract.
 */
public interface SpacedRepetitionScheduler {

    /**
     * Calculates the new memory state and next due timestamp based on the provided rating.
     */
    SchedulingResult schedule(UserRememberReview currentReview, ReviewRating rating, Instant reviewTime);

    /**
     * Previews the next interval and state for all possible ratings (AGAIN, HARD, GOOD, EASY)
     * at the current point in time.
     */
    Map<ReviewRating, SchedulingResult> previewAllRatings(UserRememberReview currentReview, Instant reviewTime);

    /**
     * Calculates current retrievability R(t, S) (probability of recall) given item stability and elapsed time.
     */
    double calculateRetrievability(double stability, Instant lastReviewedAt, Instant now);
}
