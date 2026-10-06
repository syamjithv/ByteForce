package com.byteforce.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserProgressTest {

    @Test
    @DisplayName("Should correctly calculate solve rate with non-zero attempts")
    void shouldCalculateSolveRateCorrectly() {
        UUID userId = UUID.randomUUID();
        UserProgress progress = UserProgress.calculate(userId, 20, 10);

        assertEquals(userId, progress.userId());
        assertEquals(20, progress.totalAttempts());
        assertEquals(10, progress.solvedQuestions());
        assertEquals(50.0, progress.solveRatePercentage(), 0.001);
        assertNotNull(progress.solvedByDifficulty());
        assertNotNull(progress.solvedByTopic());
    }

    @Test
    @DisplayName("Should return 0.0% solve rate when total attempts is zero")
    void shouldReturnZeroSolveRateWhenTotalAttemptsIsZero() {
        UUID userId = UUID.randomUUID();
        UserProgress progress = UserProgress.calculate(userId, 0, 0);

        assertEquals(0, progress.totalAttempts());
        assertEquals(0, progress.solvedQuestions());
        assertEquals(0.0, progress.solveRatePercentage(), 0.001);
    }

    @Test
    @DisplayName("Should support breakdown maps by difficulty and topic")
    void shouldSupportBreakdownMaps() {
        UUID userId = UUID.randomUUID();
        Map<Difficulty, Long> diffMap = Map.of(Difficulty.EASY, 5L, Difficulty.MEDIUM, 3L);
        Map<String, Long> topicMap = Map.of("Arrays", 4L, "Strings", 4L);

        UserProgress progress = UserProgress.calculate(userId, 15, 8, diffMap, topicMap);

        assertEquals(15, progress.totalAttempts());
        assertEquals(8, progress.solvedQuestions());
        assertEquals(53.33, progress.solveRatePercentage(), 0.01);
        assertEquals(5L, progress.solvedByDifficulty().get(Difficulty.EASY));
        assertEquals(3L, progress.solvedByDifficulty().get(Difficulty.MEDIUM));
        assertEquals(4L, progress.solvedByTopic().get("Arrays"));
        assertEquals(4L, progress.solvedByTopic().get("Strings"));
    }

    @Test
    @DisplayName("Should handle null maps gracefully by defaulting to empty maps")
    void shouldHandleNullMapsGracefully() {
        UUID userId = UUID.randomUUID();
        UserProgress progress = UserProgress.calculate(userId, 10, 5, null, null);

        assertNotNull(progress.solvedByDifficulty());
        assertNotNull(progress.solvedByTopic());
        assertTrue(progress.solvedByDifficulty().isEmpty());
        assertTrue(progress.solvedByTopic().isEmpty());
    }

    @Test
    @DisplayName("Should throw NullPointerException when userId is null")
    void shouldThrowWhenUserIdIsNull() {
        assertThrows(NullPointerException.class, () -> UserProgress.calculate(null, 10, 5));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when attempts are negative")
    void shouldThrowWhenAttemptsNegative() {
        UUID userId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> UserProgress.calculate(userId, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> UserProgress.calculate(userId, 5, -1));
    }

    @Test
    @DisplayName("Should handle 100% solve rate edge case")
    void shouldHandleHundredPercentSolveRate() {
        UUID userId = UUID.randomUUID();
        UserProgress progress = UserProgress.calculate(userId, 10, 10);
        assertEquals(100.0, progress.solveRatePercentage(), 0.001);
    }
}
