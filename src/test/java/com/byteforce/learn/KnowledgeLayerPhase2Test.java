package com.byteforce.learn;

import com.byteforce.ByteForceWebApplication;
import com.byteforce.domain.Concept;
import com.byteforce.domain.ConceptRelationship;
import com.byteforce.domain.ConceptRelationshipType;
import com.byteforce.domain.RelatedConceptView;
import com.byteforce.domain.RememberItem;
import com.byteforce.domain.RememberItemType;
import com.byteforce.exception.ResourceNotFoundException;
import com.byteforce.exception.ValidationException;
import com.byteforce.repository.ConceptRelationshipRepository;
import com.byteforce.repository.ConceptRepository;
import com.byteforce.repository.RememberItemRepository;
import com.byteforce.service.BrainMapService;
import com.byteforce.service.RememberService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Comprehensive verification of Phase 2 — Knowledge Layer:
 * 1. Remember Quick Revision persistence, concept -> multi-item relationship, service methods, and UI.
 * 2. Brain Maps concept relationship network, relational queries, bidirectional views, and UI.
 * 3. /remember and /brain-maps endpoints and filtering.
 */
@SpringBootTest(classes = ByteForceWebApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KnowledgeLayerPhase2Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RememberItemRepository rememberItemRepository;

    @Autowired
    private ConceptRelationshipRepository conceptRelationshipRepository;

    @Autowired
    private ConceptRepository conceptRepository;

    @Autowired
    private RememberService rememberService;

    @Autowired
    private BrainMapService brainMapService;

    // ==========================================
    // REMEMBER FEATURE TESTS
    // ==========================================

    @Test
    @DisplayName("Remember 1: RememberItem persistence and CRUD works via repository")
    void testRememberItemPersistence() {
        assertTrue(conceptRepository.existsById(1001L), "Concept 1001 must exist");

        RememberItem item = RememberItem.create(1001L, RememberItemType.FORMULA, "Test formula for testing: sum = n*(n+1)/2", 99);
        RememberItem saved = rememberItemRepository.save(item);

        assertTrue(saved.getId() > 0);
        assertEquals(1001L, saved.getConceptId());
        assertEquals(RememberItemType.FORMULA, saved.getType());
        assertEquals("Test formula for testing: sum = n*(n+1)/2", saved.getContent());

        Optional<RememberItem> found = rememberItemRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Test formula for testing: sum = n*(n+1)/2", found.get().getContent());

        // Cleanup
        rememberItemRepository.deleteById(saved.getId());
        assertFalse(rememberItemRepository.existsById(saved.getId()));
    }

    @Test
    @DisplayName("Remember 2: One Concept has multiple RememberItems")
    void testOneConceptHasManyRememberItems() {
        List<RememberItem> arrayItems = rememberItemRepository.findByConceptId(1001L);
        assertNotNull(arrayItems);
        assertTrue(arrayItems.size() >= 3, "Array Traversal should have at least 3 seeded remember items");

        // Verify different types coexist for the same concept
        boolean hasKeyFact = arrayItems.stream().anyMatch(i -> i.getType() == RememberItemType.KEY_FACT);
        boolean hasConfusion = arrayItems.stream().anyMatch(i -> i.getType() == RememberItemType.COMMON_CONFUSION);
        assertTrue(hasKeyFact, "Should have KEY_FACT");
        assertTrue(hasConfusion, "Should have COMMON_CONFUSION");
    }

    @Test
    @DisplayName("Remember 3: RememberService business logic and validation")
    void testRememberServiceBehavior() {
        // Concept validation
        assertThrows(ResourceNotFoundException.class, () ->
                rememberService.createItem(999999L, RememberItemType.KEY_FACT, "Content", 1)
        );

        // Blank content validation
        assertThrows(ValidationException.class, () ->
                rememberService.createItem(1001L, RememberItemType.KEY_FACT, "   ", 1)
        );

        // Null type validation
        assertThrows(ValidationException.class, () ->
                rememberService.createItem(1001L, null, "Valid content", 1)
        );

        // Valid creation & retrieval
        RememberItem created = rememberService.createItem(1001L, RememberItemType.QUICK_EXAMPLE, "Service test example", 50);
        assertNotNull(created);
        assertEquals(RememberItemType.QUICK_EXAMPLE, created.getType());

        // Get by type
        List<RememberItem> quickExamples = rememberService.getItemsByType(RememberItemType.QUICK_EXAMPLE);
        assertFalse(quickExamples.isEmpty());

        // Update item
        RememberItem updated = rememberService.updateItem(created.getId(), RememberItemType.MEMORY_TRICK, "Updated memory trick", 51, true);
        assertEquals(RememberItemType.MEMORY_TRICK, updated.getType());
        assertEquals("Updated memory trick", updated.getContent());

        // Delete item
        rememberService.deleteItem(created.getId());
        assertTrue(rememberService.getItemById(created.getId()).isEmpty());
    }

    @Test
    @DisplayName("Remember 4: /learn/concept/{id} renders Remember quick revision section")
    void testConceptPageRendersRememberSection() throws Exception {
        mockMvc.perform(get("/learn/concept/1001"))
                .andExpect(status().isOk())
                .andExpect(view().name("learn/concept"))
                .andExpect(model().attributeExists("rememberItems"))
                .andExpect(content().string(containsString("Remember — Quick Revision")))
                .andExpect(content().string(containsString("Key Fact")))
                .andExpect(content().string(containsString("Array index access is O(1)")));
    }

    @Test
    @DisplayName("Remember 5: Dedicated /remember page renders items and supports filtering")
    void testRememberPageBrowsingAndFiltering() throws Exception {
        // 1. Browsing default all
        mockMvc.perform(get("/remember"))
                .andExpect(status().isOk())
                .andExpect(view().name("remember/index"))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("subjects"))
                .andExpect(model().attributeExists("types"))
                .andExpect(content().string(containsString("Remember — Quick Revision")))
                .andExpect(content().string(containsString("Filter by Subject")))
                .andExpect(content().string(containsString("Filter by Revision Type")));

        // 2. Filter by subject
        mockMvc.perform(get("/remember").param("subject", "data-structures"))
                .andExpect(status().isOk())
                .andExpect(view().name("remember/index"))
                .andExpect(model().attribute("selectedSubject", "data-structures"))
                .andExpect(content().string(containsString("Array Traversal")));

        // 3. Filter by type
        mockMvc.perform(get("/remember").param("type", "KEY_FACT"))
                .andExpect(status().isOk())
                .andExpect(view().name("remember/index"))
                .andExpect(model().attribute("selectedType", "KEY_FACT"));
    }

    // ==========================================
    // BRAIN MAP FEATURE TESTS
    // ==========================================

    @Test
    @DisplayName("Brain Map 1: Concept relationship persistence and relational query")
    void testConceptRelationshipPersistence() {
        assertTrue(conceptRepository.existsById(1001L));
        assertTrue(conceptRepository.existsById(1004L));

        // Create test relationship
        ConceptRelationship rel = ConceptRelationship.create(1001L, 1004L, ConceptRelationshipType.RELATED, "Test relationship link", 99);
        ConceptRelationship saved = conceptRelationshipRepository.save(rel);
        assertTrue(saved.getId() > 0);

        // Find by source
        List<ConceptRelationship> fromSource = conceptRelationshipRepository.findBySourceConceptId(1001L);
        assertTrue(fromSource.stream().anyMatch(r -> r.getTargetConceptId() == 1004L));

        // Find by target
        List<ConceptRelationship> toTarget = conceptRelationshipRepository.findByTargetConceptId(1004L);
        assertTrue(toTarget.stream().anyMatch(r -> r.getSourceConceptId() == 1001L));

        // Exists check
        assertTrue(conceptRelationshipRepository.exists(1001L, 1004L, ConceptRelationshipType.RELATED));

        // Cleanup
        conceptRelationshipRepository.deleteById(saved.getId());
        assertFalse(conceptRelationshipRepository.exists(1001L, 1004L, ConceptRelationshipType.RELATED));
    }

    @Test
    @DisplayName("Brain Map 2: BrainMapService prevents self-referencing and non-existent concepts")
    void testBrainMapServiceValidation() {
        // Self-referencing error
        assertThrows(ValidationException.class, () ->
                brainMapService.createRelationship(1001L, 1001L, ConceptRelationshipType.RELATED, "Self", 1)
        );

        // Non-existent source concept
        assertThrows(ResourceNotFoundException.class, () ->
                brainMapService.createRelationship(999999L, 1001L, ConceptRelationshipType.RELATED, "Invalid source", 1)
        );

        // Non-existent target concept
        assertThrows(ResourceNotFoundException.class, () ->
                brainMapService.createRelationship(1001L, 999999L, ConceptRelationshipType.RELATED, "Invalid target", 1)
        );
    }

    @Test
    @DisplayName("Brain Map 3: Bidirectional concept relationship resolution via RelatedConceptView")
    void testRelatedConceptViewResolution() {
        // 1001 (Array Traversal) -> 1002 (Prefix Sum) as PREREQUISITE
        List<RelatedConceptView> viewsFor1001 = brainMapService.getRelatedConceptsForConcept(1001L);
        assertFalse(viewsFor1001.isEmpty());

        // From 1001 perspective, 1002 is outgoing
        boolean found1002AsOutgoing = viewsFor1001.stream()
                .anyMatch(v -> v.getConnectedConcept().getId() == 1002L && v.isOutgoing());
        assertTrue(found1002AsOutgoing, "1002 should be outgoing from 1001");

        // From 1002 perspective, 1001 is incoming
        List<RelatedConceptView> viewsFor1002 = brainMapService.getRelatedConceptsForConcept(1002L);
        boolean found1001AsIncoming = viewsFor1002.stream()
                .anyMatch(v -> v.getConnectedConcept().getId() == 1001L && !v.isOutgoing());
        assertTrue(found1001AsIncoming, "1001 should be incoming to 1002");
    }

    @Test
    @DisplayName("Brain Map 4: /learn/concept/{id} renders Brain Map connected concepts with links")
    void testConceptPageRendersBrainMapSection() throws Exception {
        mockMvc.perform(get("/learn/concept/1001"))
                .andExpect(status().isOk())
                .andExpect(view().name("learn/concept"))
                .andExpect(model().attributeExists("relatedConcepts"))
                .andExpect(content().string(containsString("Brain Map — Connected Concepts")))
                .andExpect(content().string(containsString("Prefix Sum")))
                .andExpect(content().string(containsString("/learn/concept/1002")));
    }

    @Test
    @DisplayName("Brain Map 5: Dedicated /brain-maps explorer page renders navigation tree and node details")
    void testBrainMapsExplorerPage() throws Exception {
        mockMvc.perform(get("/brain-maps"))
                .andExpect(status().isOk())
                .andExpect(view().name("brain-maps/index"))
                .andExpect(model().attributeExists("subjects"))
                .andExpect(model().attributeExists("curriculumHierarchy"))
                .andExpect(model().attributeExists("selectedConcept"))
                .andExpect(model().attributeExists("relatedConcepts"))
                .andExpect(content().string(containsString("Brain Maps — Knowledge Network")))
                .andExpect(content().string(containsString("Curriculum Nodes")));

        // Specific concept exploration
        mockMvc.perform(get("/brain-maps").param("conceptId", "1001"))
                .andExpect(status().isOk())
                .andExpect(view().name("brain-maps/index"))
                .andExpect(model().attribute("selectedConcept", conceptRepository.findById(1001L).orElseThrow()))
                .andExpect(content().string(containsString("Array Traversal")))
                .andExpect(content().string(containsString("Prefix Sum")));
    }
}
