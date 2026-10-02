package com.byteforce.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestionTypeTest {

    @Test
    @DisplayName("QuestionType enum should contain all 5 placement question categories")
    void shouldContainAllQuestionTypes() {
        QuestionType[] types = QuestionType.values();
        assertEquals(5, types.length);
        assertNotNull(QuestionType.valueOf("CODING"));
        assertNotNull(QuestionType.valueOf("MCQ"));
        assertNotNull(QuestionType.valueOf("SQL"));
        assertNotNull(QuestionType.valueOf("CONCEPTUAL"));
        assertNotNull(QuestionType.valueOf("APTITUDE"));
    }

    @Test
    @DisplayName("Question factory without questionType should default to CODING")
    void shouldDefaultToCodingQuestionType() {
        Question question = Question.create(1L, "Two Sum", "two-sum", "Find indices", Difficulty.EASY, "code");
        assertEquals(QuestionType.CODING, question.getQuestionType());
    }

    @ParameterizedTest
    @EnumSource(QuestionType.class)
    @DisplayName("Question should correctly store each supported QuestionType")
    void shouldSupportAllQuestionTypes(QuestionType questionType) {
        Question question = Question.create(1L, "Title", "slug-" + questionType.name().toLowerCase(),
                "Description", Difficulty.MEDIUM, questionType, "solution");
        assertEquals(questionType, question.getQuestionType());
    }

    @Test
    @DisplayName("Question constructor should throw NullPointerException when questionType is null")
    void shouldRejectNullQuestionType() {
        NullPointerException ex = assertThrows(NullPointerException.class, () ->
                new Question(1L, 1L, "Title", "slug", "Desc", Difficulty.HARD, null, "sol", Instant.now(), Instant.now()));
        assertEquals("questionType must not be null", ex.getMessage());
    }

    @Test
    @DisplayName("withQuestionType should return a copy with the updated QuestionType")
    void shouldUpdateQuestionTypeUsingWithMethod() {
        Question original = Question.create(1L, "SQL Joins", "sql-joins", "Query joins", Difficulty.MEDIUM, QuestionType.CODING, "SELECT");
        Question updated = original.withQuestionType(QuestionType.SQL);

        assertEquals(QuestionType.SQL, updated.getQuestionType());
        assertEquals(original.getTitle(), updated.getTitle());
        assertEquals(original.getSlug(), updated.getSlug());
    }
}
