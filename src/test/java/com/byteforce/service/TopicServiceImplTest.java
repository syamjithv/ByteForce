package com.byteforce.service;

import com.byteforce.domain.Topic;
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
class TopicServiceImplTest {

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private QuestionRepository questionRepository;

    private TopicServiceImpl topicService;

    private Topic sampleTopic;

    @BeforeEach
    void setUp() {
        topicService = new TopicServiceImpl(topicRepository, questionRepository);
        sampleTopic = Topic.create("Data Structures", "data-structures", "DS concepts", 1).withId(1L);
    }

    @Test
    @DisplayName("createTopic should succeed with valid inputs")
    void createTopicShouldSucceed() {
        when(topicRepository.existsByName("Algorithms")).thenReturn(false);
        when(topicRepository.existsBySlug("algorithms")).thenReturn(false);
        when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> {
            Topic t = inv.getArgument(0);
            return t.withId(2L);
        });

        Topic created = topicService.createTopic("Algorithms", "algorithms", "Algo desc", 2);

        assertNotNull(created);
        assertEquals("Algorithms", created.getName());
        assertEquals("algorithms", created.getSlug());
        assertEquals(2, created.getDisplayOrder());
        verify(topicRepository).save(any(Topic.class));
    }

    @Test
    @DisplayName("createTopic should validate blank name or slug")
    void createTopicShouldValidateBlankInputs() {
        assertThrows(ValidationException.class, () ->
                topicService.createTopic("  ", "slug", "desc", 1));
        assertThrows(ValidationException.class, () ->
                topicService.createTopic("Name", "   ", "desc", 1));
        assertThrows(ValidationException.class, () ->
                topicService.createTopic(null, "slug", "desc", 1));
    }

    @Test
    @DisplayName("createTopic should reject duplicate name or slug")
    void createTopicShouldRejectDuplicates() {
        when(topicRepository.existsByName("Data Structures")).thenReturn(true);
        assertThrows(ValidationException.class, () ->
                topicService.createTopic("Data Structures", "new-slug", "desc", 1));

        when(topicRepository.existsByName("Unique Name")).thenReturn(false);
        when(topicRepository.existsBySlug("existing-slug")).thenReturn(true);
        assertThrows(ValidationException.class, () ->
                topicService.createTopic("Unique Name", "existing-slug", "desc", 1));
    }

    @Test
    @DisplayName("updateTopic should succeed when topic exists and fields are valid")
    void updateTopicShouldSucceed() {
        when(topicRepository.findById(1L)).thenReturn(Optional.of(sampleTopic));
        when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> inv.getArgument(0));

        Topic updated = topicService.updateTopic(1L, "Advanced Data Structures", "adv-ds", "Updated desc", 5);

        assertEquals("Advanced Data Structures", updated.getName());
        assertEquals("adv-ds", updated.getSlug());
        assertEquals(5, updated.getDisplayOrder());
    }

    @Test
    @DisplayName("updateTopic should throw ResourceNotFoundException when topic not found")
    void updateTopicShouldThrowWhenNotFound() {
        when(topicRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                topicService.updateTopic(999L, "Name", "slug", "desc", 1));
    }

    @Test
    @DisplayName("updateTopic should throw ValidationException when updating to an existing name")
    void updateTopicShouldThrowOnDuplicateName() {
        when(topicRepository.findById(1L)).thenReturn(Optional.of(sampleTopic));
        when(topicRepository.existsByName("Other Name")).thenReturn(true);

        assertThrows(ValidationException.class, () ->
                topicService.updateTopic(1L, "Other Name", "data-structures", "desc", 1));
    }

    @Test
    @DisplayName("getTopicById should return topic when found")
    void getTopicByIdShouldReturnTopic() {
        when(topicRepository.findById(1L)).thenReturn(Optional.of(sampleTopic));

        Optional<Topic> result = topicService.getTopicById(1L);

        assertTrue(result.isPresent());
        assertEquals(sampleTopic.getName(), result.get().getName());
    }

    @Test
    @DisplayName("getTopicBySlug should return topic when found")
    void getTopicBySlugShouldReturnTopic() {
        when(topicRepository.findBySlug("data-structures")).thenReturn(Optional.of(sampleTopic));

        Optional<Topic> result = topicService.getTopicBySlug("data-structures");

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    @DisplayName("getAllTopics should return all topics from repository")
    void getAllTopicsShouldReturnList() {
        when(topicRepository.findAll()).thenReturn(List.of(sampleTopic));

        List<Topic> result = topicService.getAllTopics();

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("deleteTopic should succeed when no questions are associated")
    void deleteTopicShouldSucceedWhenNoQuestions() {
        when(topicRepository.findById(1L)).thenReturn(Optional.of(sampleTopic));
        when(questionRepository.countByTopicId(1L)).thenReturn(0L);

        topicService.deleteTopic(1L);

        verify(topicRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteTopic should throw ValidationException when topic has associated questions")
    void deleteTopicShouldFailWhenHasQuestions() {
        when(topicRepository.findById(1L)).thenReturn(Optional.of(sampleTopic));
        when(questionRepository.countByTopicId(1L)).thenReturn(3L);

        assertThrows(ValidationException.class, () -> topicService.deleteTopic(1L));
        verify(topicRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("deleteTopic should throw ResourceNotFoundException when topic not found")
    void deleteTopicShouldThrowWhenNotFound() {
        when(topicRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> topicService.deleteTopic(999L));
    }
}
