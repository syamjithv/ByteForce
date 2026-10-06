package com.byteforce.service;

import com.byteforce.domain.ActivityType;
import com.byteforce.domain.AttemptStatus;
import com.byteforce.domain.Difficulty;
import com.byteforce.domain.QuestionAttempt;
import com.byteforce.domain.UserProgress;
import com.byteforce.exception.ValidationException;
import com.byteforce.repository.AttemptRepository;
import com.byteforce.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Production implementation of {@link AttemptService}.
 */
public class AttemptServiceImpl implements AttemptService {

    private static final Logger log = LoggerFactory.getLogger(AttemptServiceImpl.class);

    private final AttemptRepository attemptRepository;
    private final QuestionRepository questionRepository;
    private final ActivityService activityService;

    public AttemptServiceImpl(AttemptRepository attemptRepository, QuestionRepository questionRepository,
                              ActivityService activityService) {
        this.attemptRepository = Objects.requireNonNull(attemptRepository, "attemptRepository must not be null");
        this.questionRepository = Objects.requireNonNull(questionRepository, "questionRepository must not be null");
        this.activityService = activityService;
    }

    public AttemptServiceImpl(AttemptRepository attemptRepository, QuestionRepository questionRepository) {
        this(attemptRepository, questionRepository, null);
    }

    @Override
    public QuestionAttempt recordAttempt(UUID userId, long questionId, AttemptStatus status,
                                         String codeSnippet, Integer executionTimeMs) {
        if (userId == null) {
            throw new ValidationException("Cannot record attempt: User ID must not be null.");
        }
        if (questionId <= 0) {
            throw new ValidationException("Cannot record attempt: Question ID must be positive.");
        }
        if (status == null) {
            throw new ValidationException("Cannot record attempt: Attempt status must not be null.");
        }
        if (executionTimeMs != null && executionTimeMs < 0) {
            throw new ValidationException("Execution time cannot be negative.");
        }
        if (!questionRepository.existsById(questionId)) {
            throw new ValidationException("Cannot record attempt: Question not found with ID: " + questionId);
        }

        QuestionAttempt attempt = QuestionAttempt.create(userId, questionId, status, codeSnippet, executionTimeMs);
        QuestionAttempt saved = attemptRepository.save(attempt);
        log.info("Recorded attempt ID {} for user {} on question {} with status {}",
                saved.getId(), userId, questionId, status);

        if (activityService != null) {
            ActivityType actType = (status == AttemptStatus.SOLVED)
                    ? ActivityType.SOLVED_QUESTION
                    : ActivityType.ATTEMPTED_QUESTION;
            String desc = (status == AttemptStatus.SOLVED ? "Solved " : "Attempted ")
                    + "question #" + questionId;
            try {
                activityService.recordActivity(userId, actType, desc);
            } catch (Exception e) {
                log.warn("Failed to record activity for attempt ID {}", saved.getId(), e);
            }
        }

        return saved;
    }

    @Override
    public List<QuestionAttempt> getAttemptsForUser(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        return attemptRepository.findByUserId(userId);
    }

    @Override
    public List<QuestionAttempt> getAttemptsForQuestion(long questionId) {
        if (questionId <= 0) {
            return List.of();
        }
        return attemptRepository.findByQuestionId(questionId);
    }

    @Override
    public List<QuestionAttempt> getAttemptsForUserAndQuestion(UUID userId, long questionId) {
        if (userId == null || questionId <= 0) {
            return List.of();
        }
        return attemptRepository.findByUserIdAndQuestionId(userId, questionId);
    }

    @Override
    public Optional<QuestionAttempt> getLatestAttempt(UUID userId, long questionId) {
        if (userId == null || questionId <= 0) {
            return Optional.empty();
        }
        return attemptRepository.findLatestByUserIdAndQuestionId(userId, questionId);
    }

    @Override
    public boolean hasUserSolved(UUID userId, long questionId) {
        if (userId == null || questionId <= 0) {
            return false;
        }
        return attemptRepository.hasSolved(userId, questionId);
    }

    @Override
    public long getTotalAttemptsCount(UUID userId) {
        if (userId == null) {
            return 0;
        }
        return attemptRepository.countByUserId(userId);
    }

    @Override
    public long getSolvedCount(UUID userId) {
        if (userId == null) {
            return 0;
        }
        return attemptRepository.countSolvedByUserId(userId);
    }

    @Override
    public Map<Difficulty, Long> getSolvedCountsByDifficulty(UUID userId) {
        if (userId == null) {
            return Map.of();
        }
        return attemptRepository.countSolvedByDifficulty(userId);
    }

    @Override
    public Map<String, Long> getSolvedCountsByTopic(UUID userId) {
        if (userId == null) {
            return Map.of();
        }
        return attemptRepository.countSolvedByTopic(userId);
    }

    @Override
    public List<QuestionAttempt> getRecentAttempts(UUID userId, int limit) {
        if (userId == null || limit <= 0) {
            return List.of();
        }
        return attemptRepository.findRecentAttempts(userId, limit);
    }

    @Override
    public UserProgress getProgressSummary(UUID userId) {
        if (userId == null) {
            throw new ValidationException("Cannot calculate progress summary: User ID must not be null.");
        }
        long totalAttempts = attemptRepository.countByUserId(userId);
        long solvedQuestions = attemptRepository.countSolvedByUserId(userId);
        Map<Difficulty, Long> byDiff = attemptRepository.countSolvedByDifficulty(userId);
        Map<String, Long> byTopic = attemptRepository.countSolvedByTopic(userId);
        return UserProgress.calculate(userId, totalAttempts, solvedQuestions, byDiff, byTopic);
    }
}
