package com.byteforce.repository;

import com.byteforce.domain.UserRememberReview;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing student personal spaced repetition review state.
 */
public interface UserRememberReviewRepository {

    UserRememberReview save(UserRememberReview review);

    Optional<UserRememberReview> findById(long id);

    Optional<UserRememberReview> findByUserIdAndRememberItemId(UUID userId, long rememberItemId);

    List<UserRememberReview> findDueReviewsByUserId(UUID userId, Instant dueCutoff, int limit);

    List<UserRememberReview> findByUserId(UUID userId);

    List<UserRememberReview> findByUserIdAndConceptId(UUID userId, long conceptId);

    long countDueReviewsByUserId(UUID userId, Instant dueCutoff);

    long countReviewsByUserId(UUID userId);

    List<UserRememberReview> findRecentlyReviewedByUserId(UUID userId, int limit);

    List<UserRememberReview> findWeakReviewsByUserId(UUID userId, int limit);

    boolean deleteById(long id);

    boolean deleteByUserIdAndRememberItemId(UUID userId, long rememberItemId);
}
