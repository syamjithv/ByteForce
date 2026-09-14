package com.byteforce.service;

import com.byteforce.domain.ActivityType;
import com.byteforce.domain.AttemptStatus;
import com.byteforce.domain.Difficulty;
import com.byteforce.domain.QuestionAttempt;
import com.byteforce.domain.UserProgress;
import com.byteforce.exception.ValidationException;
import com.byteforce.repository.AttemptRepository;
import com.byteforce.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttemptServiceImplTest {

    @Mock
    private AttemptRepository attemptRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private ActivityService activityService;

    private AttemptServiceImpl attemptService;

    private final UUID userId = UUID.randomUUID();
    private final long questionId = 42L;
    private QuestionAttempt sampleAttempt;

    @BeforeEach
    void setUp() {
        attemptService = new AttemptServiceImpl(attemptRepository, questionRepository, activityService);
        sampleAttempt = QuestionAttempt.create(userId, questionId, AttemptStatus.SOLVED, "code", 25).withId(1L);
    }

    @Test
    @DisplayName("recordAttempt should record solved attempt and log SOLVED_QUESTION activity")
    void recordAttemptSolved() {
        when(questionRepository.existsById(questionId)).thenReturn(true);
        when(attemptRepository.save(any(QuestionAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        QuestionAttempt result = attemptService.recordAttempt(userId, questionId, AttemptStatus.SOLVED, "code", 15);

        assertNotNull(result);
        assertEquals(AttemptStatus.SOLVED, result.getStatus());
        verify(activityService).recordActivity(eq(userId), eq(ActivityType.SOLVED_QUESTION), anyString());
    }

    @Test
    @DisplayName("recordAttempt should record failed attempt and log ATTEMPTED_QUESTION activity")
    void recordAttemptFailed() {
        when(questionRepository.existsById(questionId)).thenReturn(true);
        when(attemptRepository.save(any(QuestionAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        QuestionAttempt result = attemptService.recordAttempt(userId, questionId, AttemptStatus.FAILED, "code", 10);

        assertNotNull(result);
        assertEquals(AttemptStatus.FAILED, result.getStatus());
        verify(activityService).recordActivity(eq(userId), eq(ActivityType.ATTEMPTED_QUESTION), anyString());
    }

    @Test
    @DisplayName("recordAttempt should throw ValidationException on invalid arguments")
    void recordAttemptValidation() {
        assertThrows(ValidationException.class, () ->
                attemptService.recordAttempt(null, questionId, AttemptStatus.SOLVED, "code", 10));
        assertThrows(ValidationException.class, () ->
                attemptService.recordAttempt(userId, -1L, AttemptStatus.SOLVED, "code", 10));
        assertThrows(ValidationException.class, () ->
                attemptService.recordAttempt(userId, questionId, null, "code", 10));
        assertThrows(ValidationException.class, () ->
                attemptService.recordAttempt(userId, questionId, AttemptStatus.SOLVED, "code", -5));

        when(questionRepository.existsById(questionId)).thenReturn(false);
        assertThrows(ValidationException.class, () ->
                attemptService.recordAttempt(userId, questionId, AttemptStatus.SOLVED, "code", 10));
        verify(attemptRepository, never()).save(any());
    }

    @Test
    @DisplayName("query methods should delegate to repository")
    void queryMethodsShouldDelegate() {
        when(attemptRepository.findByUserId(userId)).thenReturn(List.of(sampleAttempt));
        when(attemptRepository.findByQuestionId(questionId)).thenReturn(List.of(sampleAttempt));
        when(attemptRepository.findByUserIdAndQuestionId(userId, questionId)).thenReturn(List.of(sampleAttempt));
        when(attemptRepository.findLatestByUserIdAndQuestionId(userId, questionId)).thenReturn(Optional.of(sampleAttempt));
        when(attemptRepository.hasSolved(userId, questionId)).thenReturn(true);
        when(attemptRepository.countByUserId(userId)).thenReturn(5L);
        when(attemptRepository.countSolvedByUserId(userId)).thenReturn(3L);
        when(attemptRepository.countSolvedByDifficulty(userId)).thenReturn(Map.of(Difficulty.EASY, 3L));
        when(attemptRepository.countSolvedByTopic(userId)).thenReturn(Map.of("Trees", 2L));
        when(attemptRepository.findRecentAttempts(userId, 10)).thenReturn(List.of(sampleAttempt));

        assertEquals(1, attemptService.getAttemptsForUser(userId).size());
        assertEquals(1, attemptService.getAttemptsForQuestion(questionId).size());
        assertEquals(1, attemptService.getAttemptsForUserAndQuestion(userId, questionId).size());
        assertTrue(attemptService.getLatestAttempt(userId, questionId).isPresent());
        assertTrue(attemptService.hasUserSolved(userId, questionId));
        assertEquals(5L, attemptService.getTotalAttemptsCount(userId));
        assertEquals(3L, attemptService.getSolvedCount(userId));
        assertEquals(1, attemptService.getSolvedCountsByDifficulty(userId).size());
        assertEquals(1, attemptService.getSolvedCountsByTopic(userId).size());
        assertEquals(1, attemptService.getRecentAttempts(userId, 10).size());
    }

    @Test
    @DisplayName("getProgressSummary should compute complete user progress with breakdowns")
    void getProgressSummaryShouldComputeMetrics() {
        when(attemptRepository.countByUserId(userId)).thenReturn(10L);
        when(attemptRepository.countSolvedByUserId(userId)).thenReturn(5L);
        when(attemptRepository.countSolvedByDifficulty(userId)).thenReturn(Map.of(Difficulty.EASY, 3L, Difficulty.MEDIUM, 2L));
        when(attemptRepository.countSolvedByTopic(userId)).thenReturn(Map.of("Arrays", 3L, "Graphs", 2L));

        UserProgress progress = attemptService.getProgressSummary(userId);

        assertNotNull(progress);
        assertEquals(userId, progress.userId());
        assertEquals(10L, progress.totalAttempts());
        assertEquals(5L, progress.solvedQuestions());
        assertEquals(50.0, progress.solveRatePercentage(), 0.001);
        assertEquals(3L, progress.solvedByDifficulty().get(Difficulty.EASY));
        assertEquals(2L, progress.solvedByDifficulty().get(Difficulty.MEDIUM));
        assertEquals(3L, progress.solvedByTopic().get("Arrays"));
        assertEquals(2L, progress.solvedByTopic().get("Graphs"));
    }

    @Test
    @DisplayName("getProgressSummary should throw ValidationException for null userId")
    void getProgressSummaryShouldValidateUserId() {
        assertThrows(ValidationException.class, () -> attemptService.getProgressSummary(null));
    }
}
