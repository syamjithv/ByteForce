package com.byteforce.repository;

import com.byteforce.domain.Difficulty;
import com.byteforce.domain.QuestionAttempt;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for QuestionAttempt domain entities.
 */
public interface AttemptRepository {

    Optional<QuestionAttempt> findById(long id);

    QuestionAttempt save(QuestionAttempt attempt);

    List<QuestionAttempt> findByUserId(UUID userId);

    List<QuestionAttempt> findByQuestionId(long questionId);

    List<QuestionAttempt> findByUserIdAndQuestionId(UUID userId, long questionId);

    Optional<QuestionAttempt> findLatestByUserIdAndQuestionId(UUID userId, long questionId);

    long countByUserId(UUID userId);

    long countSolvedByUserId(UUID userId);

    Map<Difficulty, Long> countSolvedByDifficulty(UUID userId);

    Map<String, Long> countSolvedByTopic(UUID userId);

    boolean hasSolved(UUID userId, long questionId);

    List<QuestionAttempt> findRecentAttempts(UUID userId, int limit);

    boolean deleteById(long id);
}
