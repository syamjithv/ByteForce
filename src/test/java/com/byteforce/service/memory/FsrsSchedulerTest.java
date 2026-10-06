package com.byteforce.service.memory;

import com.byteforce.domain.ReviewRating;
import com.byteforce.domain.ReviewState;
import com.byteforce.domain.SchedulingResult;
import com.byteforce.domain.UserRememberReview;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FsrsSchedulerTest {

    private FsrsScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new FsrsScheduler();
    }

    @Test
    @DisplayName("Initial review: scheduling a new card produces appropriate states and stabilities")
    void testScheduleNewCard() {
        Instant now = Instant.parse("2026-10-03T10:00:00Z");

        // AGAIN
        SchedulingResult resAgain = scheduler.schedule(null, ReviewRating.AGAIN, now);
        assertEquals(ReviewState.LEARNING, resAgain.getNextState());
        assertTrue(resAgain.getDifficulty() > 5.0, "Difficulty should increase after AGAIN");
        assertTrue(resAgain.getNextDueAt().isAfter(now));

        // HARD
        SchedulingResult resHard = scheduler.schedule(null, ReviewRating.HARD, now);
        assertEquals(ReviewState.LEARNING, resHard.getNextState());
        assertTrue(resHard.getNextDueAt().isAfter(now));

        // GOOD
        SchedulingResult resGood = scheduler.schedule(null, ReviewRating.GOOD, now);
        assertEquals(ReviewState.REVIEW, resGood.getNextState());
        assertTrue(resGood.getStability() >= 2.0);
        assertTrue(resAgain.getDifficulty() > resGood.getDifficulty());

        // EASY
        SchedulingResult resEasy = scheduler.schedule(null, ReviewRating.EASY, now);
        assertEquals(ReviewState.REVIEW, resEasy.getNextState());
        assertTrue(resEasy.getStability() > resGood.getStability());
        assertTrue(resEasy.getDifficulty() < resGood.getDifficulty());
    }

    @Test
    @DisplayName("Subsequent review: GOOD recall increases stability and preserves REVIEW state")
    void testScheduleSubsequentGoodReview() {
        UUID userId = UUID.randomUUID();
        Instant t0 = Instant.parse("2026-10-01T10:00:00Z");
        Instant t1 = Instant.parse("2026-10-03T10:00:00Z"); // 2 days later

        UserRememberReview review = new UserRememberReview(
                1L, userId, 100L, t1, t0, 1, 1,
                ReviewState.REVIEW, 5.0, 2.4, 0.9, ReviewRating.GOOD, t0, t0
        );

        SchedulingResult result = scheduler.schedule(review, ReviewRating.GOOD, t1);

        assertEquals(ReviewState.REVIEW, result.getNextState());
        assertTrue(result.getStability() > review.getStability(), "Stability must increase after successful GOOD review");
        assertTrue(result.getNextDueAt().isAfter(t1));
    }

    @Test
    @DisplayName("Subsequent review: AGAIN lapse decreases stability and transitions to RELEARNING")
    void testScheduleSubsequentLapse() {
        UUID userId = UUID.randomUUID();
        Instant t0 = Instant.parse("2026-09-01T10:00:00Z");
        Instant t1 = Instant.parse("2026-10-01T10:00:00Z"); // 30 days later

        UserRememberReview review = new UserRememberReview(
                1L, userId, 100L, t1, t0, 5, 5,
                ReviewState.REVIEW, 4.0, 25.0, 0.85, ReviewRating.GOOD, t0, t0
        );

        SchedulingResult result = scheduler.schedule(review, ReviewRating.AGAIN, t1);

        assertEquals(ReviewState.RELEARNING, result.getNextState());
        assertTrue(result.getDifficulty() > review.getDifficulty(), "Difficulty should increase on lapse");
        assertTrue(result.getStability() < review.getStability(), "Stability should decrease on lapse");
    }

    @Test
    @DisplayName("Retrievability decay adheres to power law: R(S, S) equals 0.90")
    void testRetrievabilityTarget() {
        double stability = 10.0; // 10 days
        Instant lastReview = Instant.parse("2026-10-01T00:00:00Z");
        Instant tenDaysLater = lastReview.plus(Duration.ofDays(10));

        double r = scheduler.calculateRetrievability(stability, lastReview, tenDaysLater);
        assertEquals(0.90, r, 0.001, "At t = S, retrievability should be exactly 0.90 (90%)");
    }

    @Test
    @DisplayName("previewAllRatings provides predictions for all 4 ratings")
    void testPreviewAllRatings() {
        Instant now = Instant.now();
        Map<ReviewRating, SchedulingResult> previews = scheduler.previewAllRatings(null, now);

        assertEquals(4, previews.size());
        assertNotNull(previews.get(ReviewRating.AGAIN));
        assertNotNull(previews.get(ReviewRating.HARD));
        assertNotNull(previews.get(ReviewRating.GOOD));
        assertNotNull(previews.get(ReviewRating.EASY));
        assertNotNull(previews.get(ReviewRating.GOOD).getIntervalLabel());
    }
}
