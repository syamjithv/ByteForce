package com.byteforce.repository;

import com.byteforce.domain.Concept;
import com.byteforce.domain.LearningResource;
import com.byteforce.domain.Subject;
import com.byteforce.domain.Topic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryLearnRepositoryTest {

    private InMemoryLearnRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryLearnRepository();
    }

    @Test
    @DisplayName("Should seed and return subjects sorted by display order")
    void shouldFindAllSubjects() {
        List<Subject> subjects = repository.findAllSubjects();
        assertNotNull(subjects);
        assertEquals(3, subjects.size());
        assertEquals("Data Structures", subjects.get(0).getName());
        assertEquals("Database Management Systems", subjects.get(1).getName());
        assertEquals("Operating Systems", subjects.get(2).getName());
    }

    @Test
    @DisplayName("Should find subject by ID case-insensitively")
    void shouldFindSubjectById() {
        Optional<Subject> ds = repository.findSubjectById("data-structures");
        assertTrue(ds.isPresent());
        assertEquals("Data Structures", ds.get().getName());

        Optional<Subject> dbms = repository.findSubjectById("DBMS");
        assertTrue(dbms.isPresent());
        assertEquals("Database Management Systems", dbms.get().getName());

        Optional<Subject> none = repository.findSubjectById("non-existent");
        assertFalse(none.isPresent());
    }

    @Test
    @DisplayName("Should find topics for a subject")
    void shouldFindTopicsBySubjectId() {
        List<Topic> dsTopics = repository.findTopicsBySubjectId("data-structures");
        assertEquals(2, dsTopics.size());
        assertEquals("Arrays", dsTopics.get(0).getName());
        assertEquals("Linked Lists", dsTopics.get(1).getName());

        List<Topic> dbmsTopics = repository.findTopicsBySubjectId("dbms");
        assertEquals(1, dbmsTopics.size());
        assertEquals("SQL", dbmsTopics.get(0).getName());
    }

    @Test
    @DisplayName("Should find concepts for a topic ID")
    void shouldFindConceptsByTopicId() {
        List<Concept> arrayConcepts = repository.findConceptsByTopicId(101L);
        assertEquals(2, arrayConcepts.size());
        assertEquals("Array Traversal", arrayConcepts.get(0).getTitle());
        assertEquals("Prefix Sum", arrayConcepts.get(1).getTitle());

        List<Concept> sqlConcepts = repository.findConceptsByTopicId(201L);
        assertEquals(3, sqlConcepts.size());
        assertEquals("SELECT Statement", sqlConcepts.get(0).getTitle());
        assertEquals("JOIN Operations", sqlConcepts.get(1).getTitle());
        assertEquals("GROUP BY Clause", sqlConcepts.get(2).getTitle());
    }

    @Test
    @DisplayName("Should retrieve resources attached to a concept")
    void shouldFindResourcesByConceptId() {
        List<LearningResource> resources = repository.findResourcesByConceptId(1001L);
        assertNotNull(resources);
        assertEquals(2, resources.size());
        assertTrue(resources.stream().anyMatch(r -> r.getTitle().contains("Array")));
    }

    @Test
    @DisplayName("Should search concepts across titles, explanations, and topics")
    void shouldSearchConcepts() {
        List<Concept> results = repository.searchConcepts("join");
        assertEquals(1, results.size());
        assertEquals("JOIN Operations", results.get(0).getTitle());

        List<Concept> processResults = repository.searchConcepts("context");
        assertEquals(1, processResults.size());
        assertEquals("Context Switching", processResults.get(0).getTitle());

        List<Concept> emptyQuery = repository.searchConcepts("");
        assertTrue(emptyQuery.size() >= 9);
    }
}
