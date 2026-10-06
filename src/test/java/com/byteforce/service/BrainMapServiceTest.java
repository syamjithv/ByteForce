package com.byteforce.service;

import com.byteforce.domain.Concept;
import com.byteforce.domain.ConceptRelationship;
import com.byteforce.domain.ConceptRelationshipType;
import com.byteforce.domain.Subject;
import com.byteforce.domain.Topic;
import com.byteforce.domain.brainmap.BrainMapGraph;
import com.byteforce.repository.ConceptRelationshipRepository;
import com.byteforce.repository.ConceptRepository;
import com.byteforce.repository.SubjectRepository;
import com.byteforce.repository.TopicRepository;
import com.byteforce.repository.UserRememberReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrainMapServiceTest {

    @Mock
    private ConceptRelationshipRepository relationshipRepository;
    @Mock
    private ConceptRepository conceptRepository;
    @Mock
    private TopicRepository topicRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private UserRememberReviewRepository userRememberReviewRepository;

    private BrainMapServiceImpl brainMapService;

    @BeforeEach
    void setUp() {
        brainMapService = new BrainMapServiceImpl(
                relationshipRepository,
                conceptRepository,
                topicRepository,
                subjectRepository,
                userRememberReviewRepository
        );
    }

    @Test
    @DisplayName("getBrainMapGraph: creates hierarchical graph with center node and differentiated relationships")
    void testGetBrainMapGraph() {
        long centerId = 1L;
        long prereqId = 2L;
        long succId = 3L;
        long confusedId = 4L;

        Concept center = new Concept(centerId, 10L, "OS", "Deadlock", "Explanation", List.of(), "", List.of());
        Concept prereq = new Concept(prereqId, 10L, "OS", "Process Synchronization", "Sync expl", List.of(), "", List.of());
        Concept succ = new Concept(succId, 10L, "OS", "Bankers Algorithm", "Bankers expl", List.of(), "", List.of());
        Concept confused = new Concept(confusedId, 10L, "OS", "Starvation", "Starvation expl", List.of(), "", List.of());

        Topic topic = new Topic(10L, "os-subject", "Operating Systems", "os", "desc", 1, Instant.now());
        Subject subject = new Subject("os-subject", "Computer Systems", "desc", 1, List.of(topic));

        when(conceptRepository.findById(centerId)).thenReturn(Optional.of(center));
        when(conceptRepository.findById(prereqId)).thenReturn(Optional.of(prereq));
        when(conceptRepository.findById(succId)).thenReturn(Optional.of(succ));
        when(conceptRepository.findById(confusedId)).thenReturn(Optional.of(confused));

        when(topicRepository.findById(10L)).thenReturn(Optional.of(topic));
        when(subjectRepository.findById("os-subject")).thenReturn(Optional.of(subject));

        // Outgoing: center -> Bankers (succ)
        ConceptRelationship rel1 = ConceptRelationship.create(centerId, succId, ConceptRelationshipType.CHILD, "Implements", 1);
        // Outgoing: center -> Starvation (confused)
        ConceptRelationship rel2 = ConceptRelationship.create(centerId, confusedId, ConceptRelationshipType.COMMONLY_CONFUSED, "Often confused", 2);
        // Outgoing: center -> Process Synchronization (prereq)
        ConceptRelationship rel3 = ConceptRelationship.create(centerId, prereqId, ConceptRelationshipType.PREREQUISITE, "Prerequisite", 3);
        when(relationshipRepository.findBySourceConceptId(centerId)).thenReturn(List.of(rel1, rel2, rel3));
        when(relationshipRepository.findByTargetConceptId(centerId)).thenReturn(List.of());

        BrainMapGraph graph = brainMapService.getBrainMapGraph(centerId, UUID.randomUUID());

        assertNotNull(graph);
        assertEquals(centerId, graph.getCenterConcept().getId());
        assertEquals(4, graph.getNodes().size(), "Must contain center + 3 related nodes");
        assertEquals(3, graph.getEdges().size(), "Must contain 3 connector edges");

        // Verify center node
        assertTrue(graph.getNodes().stream().anyMatch(n -> n.getId() == centerId && n.isCenter()));

        // Verify roles
        assertTrue(graph.getNodes().stream().anyMatch(n -> n.getId() == prereqId && "PREREQUISITE".equals(n.getRelationshipRole())));
        assertTrue(graph.getNodes().stream().anyMatch(n -> n.getId() == succId && "CHILD".equals(n.getRelationshipRole())));
        assertTrue(graph.getNodes().stream().anyMatch(n -> n.getId() == confusedId && "COMMONLY_CONFUSED".equals(n.getRelationshipRole())));
    }

    @Test
    @DisplayName("exportBrainMapPdf: generates a valid vector PDF document with PDF header")
    void testExportBrainMapPdf() {
        long centerId = 1L;
        Concept center = new Concept(centerId, 10L, "OS", "Deadlock", "Explanation", List.of(), "", List.of());
        when(conceptRepository.findById(centerId)).thenReturn(Optional.of(center));
        when(relationshipRepository.findBySourceConceptId(centerId)).thenReturn(List.of());
        when(relationshipRepository.findByTargetConceptId(centerId)).thenReturn(List.of());

        byte[] pdfBytes = brainMapService.exportBrainMapPdf(centerId);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF must not be empty");
        // PDF specification: begins with '%PDF-'
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }
}
