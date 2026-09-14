package com.byteforce.service;

import com.byteforce.domain.AttemptStatus;
import com.byteforce.domain.Difficulty;
import com.byteforce.domain.QuestionAttempt;
import com.byteforce.domain.UserProgress;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for QuestionAttempt business operations and progress analytics.
 */
public interface AttemptService {

    QuestionAttempt recordAttempt(UUID userId, long questionId, AttemptStatus status,
                                  String codeSnippet, Integer executionTimeMs);

    List<QuestionAttempt> getAttemptsForUser(UUID userId);

    List<QuestionAttempt> getAttemptsForQuestion(long questionId);

    List<QuestionAttempt> getAttemptsForUserAndQuestion(UUID userId, long questionId);

    Optional<QuestionAttempt> getLatestAttempt(UUID userId, long questionId);

    boolean hasUserSolved(UUID userId, long questionId);

    long getTotalAttemptsCount(UUID userId);

    long getSolvedCount(UUID userId);

    Map<Difficulty, Long> getSolvedCountsByDifficulty(UUID userId);

    Map<String, Long> getSolvedCountsByTopic(UUID userId);

    List<QuestionAttempt> getRecentAttempts(UUID userId, int limit);

    UserProgress getProgressSummary(UUID userId);
}
