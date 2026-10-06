package com.byteforce.service;

import com.byteforce.domain.ConceptMemoryStatus;
import com.byteforce.domain.MemoryAnalytics;
import com.byteforce.domain.MemoryReviewItemView;
import com.byteforce.domain.ReviewRating;
import com.byteforce.domain.SchedulingResult;
import com.byteforce.domain.UserRememberReview;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for ByteForce Memory: personal spaced-repetition revision,
 * retrieval practice scheduling, interleaving, and analytics.
 */
public interface MemoryService {

    /**
     * Enrolls all active RememberItems of a concept into the learner's personal memory queue.
     * Returns the number of items enrolled.
     */
    int enrollConcept(UUID userId, long conceptId);

    /**
     * Retrieves the due review queue for a student, intelligently interleaved across
     * related concepts and topics to enhance retention.
     */
    List<MemoryReviewItemView> getDueQueue(UUID userId, int limit);

    /**
     * Records a review rating for a specific RememberItem by a learner.
     * Invokes the FSRS scheduler, updates the review state, and calculates next due interval.
     */
    UserRememberReview recordReview(UUID userId, long rememberItemId, ReviewRating rating);

    /**
     * Previews interval outcomes for all ratings for an item.
     */
    Map<ReviewRating, SchedulingResult> previewRatings(UUID userId, long rememberItemId);

    /**
     * Retrieves memory status for a specific concept for the given student.
     */
    ConceptMemoryStatus getConceptMemoryStatus(UUID userId, long conceptId);

    /**
     * Computes real database-backed memory analytics for the given student.
     */
    MemoryAnalytics getMemoryAnalytics(UUID userId);

    /**
     * Retrieves the next item in the user's due queue, or empty if queue is clear.
     */
    Optional<MemoryReviewItemView> getNextDueItem(UUID userId);

    /**
     * Retrieves a specific item for review by rememberItemId.
     */
    Optional<MemoryReviewItemView> getReviewItem(UUID userId, long rememberItemId);

    /**
     * Integration hook for Practice / Assessment weakness signals:
     * When a student fails questions related to a concept, signals the memory scheduler.
     */
    void recordPracticeWeaknessSignal(UUID userId, long conceptId);

    /**
     * Retrieves recently reviewed items.
     */
    List<MemoryReviewItemView> getRecentlyReviewed(UUID userId, int limit);

    /**
     * Retrieves weak/at-risk memory items.
     */
    List<MemoryReviewItemView> getWeakItems(UUID userId, int limit);
}
