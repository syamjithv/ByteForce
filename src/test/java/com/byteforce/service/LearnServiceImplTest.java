package com.byteforce.service;

import com.byteforce.domain.Concept;
import com.byteforce.domain.LearningResource;
import com.byteforce.domain.ResourceType;
import com.byteforce.domain.Subject;
import com.byteforce.domain.Topic;
import com.byteforce.repository.LearnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearnServiceImplTest {

    @Mock
    private LearnRepository mockRepository;

    private LearnServiceImpl learnService;

    private Subject sampleSubject;
    private Topic sampleTopic;
    private Concept sampleConcept;

    @BeforeEach
    void setUp() {
        learnService = new LearnServiceImpl(mockRepository);

        sampleTopic = new Topic(1L, "Arrays", "arrays", "Array topic", 1, Instant.now());
        sampleSubject = Subject.create("ds", "Data Structures", "DS fundamentals", 1, List.of(sampleTopic));

        LearningResource resource = LearningResource.create("Array Resource", ResourceType.ARTICLE, "https://example.com", "Guide");
        sampleConcept = Concept.create(
                10L, 1L, "Arrays", "Array Traversal",
                "Explanation", List.of("Point 1"), "example()", List.of(resource)
        );
    }

    @Test
    @DisplayName("getAllSubjects should return subjects from repository")
    void shouldGetAllSubjects() {
        when(mockRepository.findAllSubjects()).thenReturn(List.of(sampleSubject));

        List<Subject> result = learnService.getAllSubjects();
        assertEquals(1, result.size());
        assertEquals("Data Structures", result.get(0).getName());
        verify(mockRepository).findAllSubjects();
    }

    @Test
    @DisplayName("getSubjectById should validate input and delegate to repository")
    void shouldGetSubjectById() {
        when(mockRepository.findSubjectById("ds")).thenReturn(Optional.of(sampleSubject));

        Optional<Subject> result = learnService.getSubjectById("ds");
        assertTrue(result.isPresent());
        assertEquals("Data Structures", result.get().getName());

        Optional<Subject> invalid = learnService.getSubjectById(" ");
        assertFalse(invalid.isPresent());
    }

    @Test
    @DisplayName("getTopicsForSubject should return topics for valid subject ID")
    void shouldGetTopicsForSubject() {
        when(mockRepository.findTopicsBySubjectId("ds")).thenReturn(List.of(sampleTopic));

        List<Topic> topics = learnService.getTopicsForSubject("ds");
        assertEquals(1, topics.size());
        assertEquals("Arrays", topics.get(0).getName());

        assertTrue(learnService.getTopicsForSubject(null).isEmpty());
    }

    @Test
    @DisplayName("getConceptsForTopic should return concepts and validate topic ID")
    void shouldGetConceptsForTopic() {
        when(mockRepository.findConceptsByTopicId(1L)).thenReturn(List.of(sampleConcept));

        List<Concept> concepts = learnService.getConceptsForTopic(1L);
        assertEquals(1, concepts.size());
        assertEquals("Array Traversal", concepts.get(0).getTitle());

        assertTrue(learnService.getConceptsForTopic(-1L).isEmpty());
    }

    @Test
    @DisplayName("getConceptById should return concept and validate ID")
    void shouldGetConceptById() {
        when(mockRepository.findConceptById(10L)).thenReturn(Optional.of(sampleConcept));

        Optional<Concept> concept = learnService.getConceptById(10L);
        assertTrue(concept.isPresent());
        assertEquals("Array Traversal", concept.get().getTitle());

        assertFalse(learnService.getConceptById(-5L).isPresent());
    }

    @Test
    @DisplayName("getResourcesForConcept should return learning resources")
    void shouldGetResourcesForConcept() {
        LearningResource res = sampleConcept.getResources().get(0);
        when(mockRepository.findResourcesByConceptId(10L)).thenReturn(List.of(res));

        List<LearningResource> resources = learnService.getResourcesForConcept(10L);
        assertEquals(1, resources.size());
        assertEquals(ResourceType.ARTICLE, resources.get(0).getResourceType());

        assertTrue(learnService.getResourcesForConcept(0).isEmpty());
    }

    @Test
    @DisplayName("searchConcepts should delegate to repository")
    void shouldSearchConcepts() {
        when(mockRepository.searchConcepts("array")).thenReturn(List.of(sampleConcept));

        List<Concept> results = learnService.searchConcepts("array");
        assertEquals(1, results.size());
        verify(mockRepository).searchConcepts("array");
    }
}
