package com.byteforce.service;

import com.byteforce.domain.ContinueLearningView;
import com.byteforce.domain.RecentlyViewedConcept;
import com.byteforce.domain.UserConceptProgress;
import com.byteforce.repository.UserConceptProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConceptProgressServiceImplTest {

    private UserConceptProgressRepository repository;
    private ConceptProgressService service;

    private final UUID userId = UUID.randomUUID();
    private final long conceptId = 42L;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(UserConceptProgressRepository.class);
        service = new ConceptProgressServiceImpl(repository);
    }

    @Test
    @DisplayName("recordConceptView delegates to repository")
    void testRecordConceptView() {
        service.recordConceptView(userId, conceptId);
        verify(repository).recordView(userId, conceptId);
    }

    @Test
    @DisplayName("markConceptLearned delegates with completed=true")
    void testMarkLearned() {
        when(repository.setCompleted(userId, conceptId, true)).thenReturn(true);
        boolean result = service.markConceptLearned(userId, conceptId, true);
        assertTrue(result);
        verify(repository).setCompleted(userId, conceptId, true);
    }

    @Test
    @DisplayName("markConceptLearned delegates with completed=false")
    void testUnmarkLearned() {
        when(repository.setCompleted(userId, conceptId, false)).thenReturn(true);
        boolean result = service.markConceptLearned(userId, conceptId, false);
        assertTrue(result);
        verify(repository).setCompleted(userId, conceptId, false);
    }

    @Test
    @DisplayName("isConceptLearned returns true when completed")
    void testIsLearned() {
        UserConceptProgress completed = new UserConceptProgress(1L, userId, conceptId, true, Instant.now(), Instant.now());
        when(repository.findByUserAndConcept(userId, conceptId)).thenReturn(Optional.of(completed));
        assertTrue(service.isConceptLearned(userId, conceptId));

        when(repository.findByUserAndConcept(userId, conceptId)).thenReturn(Optional.empty());
        assertFalse(service.isConceptLearned(userId, conceptId));
    }

    @Test
    @DisplayName("getRecentlyViewed handles null user or delegates to repo")
    void testGetRecentlyViewed() {
        assertEquals(Collections.emptyList(), service.getRecentlyViewed(null, 5));

        List<RecentlyViewedConcept> mockList = List.of(
                new RecentlyViewedConcept(1L, "Title", 10L, "Topic", "dsa", "Subject", false, Instant.now())
        );
        when(repository.findRecentlyViewed(userId, 5)).thenReturn(mockList);

        List<RecentlyViewedConcept> result = service.getRecentlyViewed(userId, 5);
        assertEquals(1, result.size());
        assertEquals("Title", result.get(0).conceptTitle());
    }

    @Test
    @DisplayName("getContinueLearning handles null user or delegates to repo")
    void testGetContinueLearning() {
        assertTrue(service.getContinueLearning(null).isEmpty());

        ContinueLearningView view = new ContinueLearningView(1L, "Title", 2L, "Topic", "dsa", "Subject", 1, 5);
        when(repository.findContinueLearning(userId)).thenReturn(Optional.of(view));

        Optional<ContinueLearningView> result = service.getContinueLearning(userId);
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().conceptId());
    }

    @Test
    @DisplayName("Topic and subject progress counts delegate properly")
    void testProgressCounts() {
        when(repository.countCompletedByTopic(userId, 10L)).thenReturn(4L);
        when(repository.countCompletedBySubject(userId, "dsa")).thenReturn(18L);
        when(repository.getCompletionMapForTopic(userId, 10L)).thenReturn(Map.of(1L, true, 2L, false));

        assertEquals(4L, service.getCompletedCountForTopic(userId, 10L));
        assertEquals(18L, service.getCompletedCountForSubject(userId, "dsa"));
        Map<Long, Boolean> map = service.getCompletionMapForTopic(userId, 10L);
        assertEquals(2, map.size());
        assertTrue(map.get(1L));
        assertFalse(map.get(2L));
    }
}
