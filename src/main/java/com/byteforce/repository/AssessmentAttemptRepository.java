package com.byteforce.repository;

import com.byteforce.domain.AssessmentAttempt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for AssessmentAttempt domain entities.
 */
public interface AssessmentAttemptRepository {

    AssessmentAttempt save(AssessmentAttempt attempt);

    Optional<AssessmentAttempt> findById(long id);

    List<AssessmentAttempt> findByUserId(UUID userId);

    List<AssessmentAttempt> findByAssessmentId(long assessmentId);

    boolean updateScoreAndStatus(long attemptId, int score, String status, Instant completedAt);
}
