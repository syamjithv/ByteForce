package com.byteforce.learn;

import com.byteforce.ByteForceWebApplication;
import com.byteforce.domain.Concept;
import com.byteforce.domain.LearningResource;
import com.byteforce.domain.ResourceType;
import com.byteforce.domain.Subject;
import com.byteforce.domain.Topic;
import com.byteforce.repository.ConceptRepository;
import com.byteforce.repository.LearningResourceRepository;
import com.byteforce.repository.SubjectRepository;
import com.byteforce.repository.TopicRepository;
import com.byteforce.service.LearnService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(classes = ByteForceWebApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LearnDatabaseFoundationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private ConceptRepository conceptRepository;

    @Autowired
    private LearningResourceRepository learningResourceRepository;

    @Autowired
    private LearnService learnService;

    @Test
    @DisplayName("1. Subject persistence: Subjects are database-backed and retrievable")
    void testSubjectPersistence() {
        assertTrue(subjectRepository.existsById("data-structures"));
        assertTrue(subjectRepository.existsById("dbms"));
        assertTrue(subjectRepository.existsById("operating-systems"));

        Subject ds = subjectRepository.findById("data-structures").orElseThrow();
        assertEquals("Data Structures & Algorithms", ds.getName());
        assertTrue(ds.getDisplayOrder() > 0);
    }

    @Test
    @DisplayName("2. Topic -> Subject relationship: Topics belong to subjects")
    void testTopicSubjectRelationship() {
        List<Topic> dsTopics = topicRepository.findBySubjectId("data-structures");
        assertFalse(dsTopics.isEmpty(), "Data Structures subject should have attached topics");

        for (Topic t : dsTopics) {
            assertEquals("data-structures", t.getSubjectId());
        }

        List<Topic> dbmsTopics = topicRepository.findBySubjectId("dbms");
        assertFalse(dbmsTopics.isEmpty(), "DBMS subject should have attached topics");
        for (Topic t : dbmsTopics) {
            assertEquals("dbms", t.getSubjectId());
        }
    }

    @Test
    @DisplayName("3. Concept -> Topic relationship: Concepts belong to topics")
    void testConceptTopicRelationship() {
        Optional<Topic> arrayTopicOpt = topicRepository.findBySlug("arrays-two-pointers");
        if (arrayTopicOpt.isEmpty()) {
            arrayTopicOpt = topicRepository.findBySlug("arrays");
        }
        assertTrue(arrayTopicOpt.isPresent(), "Arrays topic should exist");
        Topic arrayTopic = arrayTopicOpt.get();

        List<Concept> concepts = conceptRepository.findByTopicId(arrayTopic.getId());
        assertFalse(concepts.isEmpty(), "Arrays topic should have database concepts");

        Concept traversal = concepts.stream()
                .filter(c -> c.getTitle().contains("Array Traversal"))
                .findFirst()
                .orElseThrow();

        assertEquals(arrayTopic.getId(), traversal.getTopicId());
        assertFalse(traversal.getKeyPoints().isEmpty());
        assertNotNull(traversal.getExample());
    }

    @Test
    @DisplayName("4. LearningResource persistence: Resources belong to concepts and persist")
    void testLearningResourcePersistence() {
        List<Concept> allConcepts = conceptRepository.findAll();
        assertFalse(allConcepts.isEmpty());

        Concept conceptWithResources = allConcepts.stream()
                .filter(c -> !c.getResources().isEmpty())
                .findFirst()
                .orElseThrow();

        List<LearningResource> resources = learningResourceRepository.findByConceptId(conceptWithResources.getId());
        assertFalse(resources.isEmpty());
        LearningResource res = resources.get(0);
        assertNotNull(res.getTitle());
        assertNotNull(res.getResourceType());
        assertNotNull(res.getUrl());
    }

    @Test
    @DisplayName("5. Learn service retrieval: Hierarchical curriculum retrieval works")
    void testLearnServiceRetrieval() {
        List<Subject> subjects = learnService.getAllSubjects();
        assertFalse(subjects.isEmpty());

        Subject first = subjects.get(0);
        assertNotNull(first.getTopics());

        List<Topic> topics = learnService.getTopicsForSubject(first.getId());
        assertNotNull(topics);

        List<Concept> searchResults = learnService.searchConcepts("Array");
        assertFalse(searchResults.isEmpty());
    }

    @Test
    @DisplayName("6. Learn page rendering: /learn renders database-backed subjects")
    void testLearnIndexPageRendering() throws Exception {
        mockMvc.perform(get("/learn"))
                .andExpect(status().isOk())
                .andExpect(view().name("learn/index"))
                .andExpect(model().attributeExists("subjects"))
                .andExpect(content().string(containsString("Data Structures")))
                .andExpect(content().string(containsString("Database Management Systems")))
                .andExpect(content().string(containsString("Operating Systems")));
    }

    @Test
    @DisplayName("7. Existing concept page rendering: /learn/concept/{id} renders concept and resources")
    void testConceptPageRendering() throws Exception {
        mockMvc.perform(get("/learn/concept/1001"))
                .andExpect(status().isOk())
                .andExpect(view().name("learn/concept"))
                .andExpect(model().attributeExists("concept"))
                .andExpect(model().attributeExists("resources"))
                .andExpect(content().string(containsString("Array Traversal")))
                .andExpect(content().string(containsString("Key Placement Takeaways")))
                .andExpect(content().string(containsString("Code & Implementation Example")));
    }

    @Test
    @DisplayName("8. Invalid concept still returns the existing friendly 404 behavior")
    void testInvalidConceptNotFound() throws Exception {
        mockMvc.perform(get("/learn/concept/999999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(content().string(containsString("Concept not found with ID: 999999")));
    }
}
