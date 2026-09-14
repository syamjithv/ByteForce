package com.byteforce.service;

import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;
import com.byteforce.exception.ResourceNotFoundException;
import com.byteforce.exception.ValidationException;
import com.byteforce.repository.QuestionRepository;
import com.byteforce.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceImplTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private TopicRepository topicRepository;

    private QuestionServiceImpl questionService;

    private Question sampleQuestion;

    @BeforeEach
    void setUp() {
        questionService = new QuestionServiceImpl(questionRepository, topicRepository);
        sampleQuestion = Question.create(10L, "Two Sum", "two-sum",
                "Find two numbers that sum to target", Difficulty.EASY, "Use a HashMap").withId(1L);
    }

    @Test
    @DisplayName("createQuestion should succeed when topic exists and fields are valid")
    void createQuestionShouldSucceed() {
        when(topicRepository.existsById(10L)).thenReturn(true);
        when(questionRepository.existsBySlug("two-sum")).thenReturn(false);
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));

        Question created = questionService.createQuestion(
                10L, "Two Sum", "two-sum", "Description", Difficulty.EASY, "Solution");

        assertNotNull(created);
        assertEquals("Two Sum", created.getTitle());
        assertEquals("two-sum", created.getSlug());
        verify(questionRepository).save(any(Question.class));
    }

    @Test
    @DisplayName("createQuestion should throw ValidationException when topic does not exist")
    void createQuestionShouldThrowWhenTopicMissing() {
        when(topicRepository.existsById(99L)).thenReturn(false);

        assertThrows(ValidationException.class, () ->
                questionService.createQuestion(99L, "Title", "slug", "Desc", Difficulty.EASY, "Sol"));
        verify(questionRepository, never()).save(any());
    }

    @Test
    @DisplayName("createQuestion should validate empty title, slug, description, or difficulty")
    void createQuestionShouldValidateFields() {
        when(topicRepository.existsById(10L)).thenReturn(true);

        assertThrows(ValidationException.class, () ->
                questionService.createQuestion(10L, "  ", "slug", "Desc", Difficulty.EASY, "Sol"));
        assertThrows(ValidationException.class, () ->
                questionService.createQuestion(10L, "Title", "  ", "Desc", Difficulty.EASY, "Sol"));
        assertThrows(ValidationException.class, () ->
                questionService.createQuestion(10L, "Title", "slug", "  ", Difficulty.EASY, "Sol"));
        assertThrows(ValidationException.class, () ->
                questionService.createQuestion(10L, "Title", "slug", "Desc", null, "Sol"));
    }

    @Test
    @DisplayName("createQuestion should throw ValidationException on duplicate slug")
    void createQuestionShouldThrowOnDuplicateSlug() {
        when(topicRepository.existsById(10L)).thenReturn(true);
        when(questionRepository.existsBySlug("two-sum")).thenReturn(true);

        assertThrows(ValidationException.class, () ->
                questionService.createQuestion(10L, "Two Sum", "two-sum", "Desc", Difficulty.EASY, "Sol"));
    }

    @Test
    @DisplayName("updateQuestion should succeed when question and topic exist")
    void updateQuestionShouldSucceed() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(sampleQuestion));
        when(topicRepository.existsById(10L)).thenReturn(true);
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));

        Question updated = questionService.updateQuestion(
                1L, 10L, "Two Sum Updated", "two-sum", "New desc", Difficulty.MEDIUM, "New sol");

        assertEquals("Two Sum Updated", updated.getTitle());
        assertEquals(Difficulty.MEDIUM, updated.getDifficulty());
    }

    @Test
    @DisplayName("updateQuestion should throw ResourceNotFoundException when question does not exist")
    void updateQuestionShouldThrowWhenNotFound() {
        when(questionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                questionService.updateQuestion(999L, 10L, "Title", "slug", "Desc", Difficulty.EASY, "Sol"));
    }

    @Test
    @DisplayName("updateQuestion should throw ValidationException when topic not found")
    void updateQuestionShouldThrowWhenTopicNotFound() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(sampleQuestion));
        when(topicRepository.existsById(99L)).thenReturn(false);

        assertThrows(ValidationException.class, () ->
                questionService.updateQuestion(1L, 99L, "Title", "slug", "Desc", Difficulty.EASY, "Sol"));
    }

    @Test
    @DisplayName("getQuestionById and getQuestionBySlug should return question")
    void getQuestionByIdAndSlug() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(sampleQuestion));
        when(questionRepository.findBySlug("two-sum")).thenReturn(Optional.of(sampleQuestion));

        assertTrue(questionService.getQuestionById(1L).isPresent());
        assertTrue(questionService.getQuestionBySlug("two-sum").isPresent());
    }

    @Test
    @DisplayName("filter methods should delegate to repository")
    void filterMethodsShouldDelegate() {
        when(questionRepository.findAll()).thenReturn(List.of(sampleQuestion));
        when(questionRepository.findByTopicId(10L)).thenReturn(List.of(sampleQuestion));
        when(questionRepository.findByDifficulty(Difficulty.EASY)).thenReturn(List.of(sampleQuestion));
        when(questionRepository.findByTopicIdAndDifficulty(10L, Difficulty.EASY)).thenReturn(List.of(sampleQuestion));
        when(questionRepository.search("sum")).thenReturn(List.of(sampleQuestion));
        when(questionRepository.count()).thenReturn(1L);

        assertEquals(1, questionService.getAllQuestions().size());
        assertEquals(1, questionService.getQuestionsByTopic(10L).size());
        assertEquals(1, questionService.getQuestionsByDifficulty(Difficulty.EASY).size());
        assertEquals(1, questionService.getQuestionsByTopicAndDifficulty(10L, Difficulty.EASY).size());
        assertEquals(1, questionService.searchQuestions("sum").size());
        assertEquals(1L, questionService.getTotalQuestionCount());
    }

    @Test
    @DisplayName("deleteQuestion should delete when question exists")
    void deleteQuestionShouldSucceed() {
        when(questionRepository.existsById(1L)).thenReturn(true);

        questionService.deleteQuestion(1L);

        verify(questionRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteQuestion should throw ResourceNotFoundException when question does not exist")
    void deleteQuestionShouldThrowWhenNotFound() {
        when(questionRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> questionService.deleteQuestion(999L));
        verify(questionRepository, never()).deleteById(anyLong());
    }
}
