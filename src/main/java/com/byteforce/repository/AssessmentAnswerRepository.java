package com.byteforce.repository;

import com.byteforce.domain.AssessmentAnswer;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AssessmentAnswer domain entities.
 */
public interface AssessmentAnswerRepository {

    AssessmentAnswer save(AssessmentAnswer answer);

    Optional<AssessmentAnswer> findById(long id);

    Optional<AssessmentAnswer> findByAttemptAndQuestion(long attemptId, long questionId);

    List<AssessmentAnswer> findByAttemptId(long attemptId);

    boolean deleteById(long id);

    long countByAttemptId(long attemptId);
}
