package com.byteforce.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConceptTest {

    @Test
    @DisplayName("Concept should construct immutable entity with all required fields")
    void shouldConstructConceptSuccessfully() {
        LearningResource res = LearningResource.create("Article Guide", ResourceType.ARTICLE, "https://example.com", "Guide");
        Concept concept = Concept.create(
                10L, 101L, "Arrays", "Array Traversal",
                "Iterating through array elements",
                List.of("O(1) indexing", "O(n) traversal"),
                "for (int x : arr) {}",
                List.of(res)
        );

        assertNotNull(concept);
        assertEquals(10L, concept.getId());
        assertEquals(101L, concept.getTopicId());
        assertEquals("Arrays", concept.getTopicName());
        assertEquals("Array Traversal", concept.getTitle());
        assertEquals("Iterating through array elements", concept.getShortExplanation());
        assertEquals(2, concept.getKeyPoints().size());
        assertEquals("for (int x : arr) {}", concept.getExample());
        assertEquals(1, concept.getResources().size());

        assertThrows(UnsupportedOperationException.class, () -> concept.getKeyPoints().add("Extra point"));
        assertThrows(UnsupportedOperationException.class, () -> concept.getResources().add(res));
    }

    @Test
    @DisplayName("Concept should throw on blank or null title and explanation")
    void shouldValidateConceptInputs() {
        assertThrows(NullPointerException.class, () -> Concept.create(1L, 101L, "Topic", null, "Exp", List.of(), "", List.of()));
        assertThrows(IllegalArgumentException.class, () -> Concept.create(1L, 101L, "Topic", "  ", "Exp", List.of(), "", List.of()));
        assertThrows(NullPointerException.class, () -> Concept.create(1L, 101L, "Topic", "Title", null, List.of(), "", List.of()));
    }
}
