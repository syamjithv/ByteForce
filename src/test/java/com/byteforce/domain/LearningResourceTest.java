package com.byteforce.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LearningResourceTest {

    @Test
    @DisplayName("LearningResource should create entity and validate fields")
    void shouldCreateLearningResource() {
        LearningResource res = LearningResource.create("Java Cheatsheet", ResourceType.PDF, "https://byteforce.dev/cs.pdf", "PDF Notes");
        assertNotNull(res);
        assertEquals("Java Cheatsheet", res.getTitle());
        assertEquals(ResourceType.PDF, res.getResourceType());
        assertEquals("https://byteforce.dev/cs.pdf", res.getUrl());
        assertEquals("PDF Notes", res.getDescription());

        LearningResource ofRes = LearningResource.of("Quick Article", ResourceType.ARTICLE, "https://example.com");
        assertEquals("", ofRes.getDescription());
    }

    @Test
    @DisplayName("LearningResource should throw on invalid arguments")
    void shouldValidateArguments() {
        assertThrows(NullPointerException.class, () -> LearningResource.create(null, ResourceType.PDF, "url", "desc"));
        assertThrows(IllegalArgumentException.class, () -> LearningResource.create("", ResourceType.PDF, "url", "desc"));
        assertThrows(NullPointerException.class, () -> LearningResource.create("Title", null, "url", "desc"));
        assertThrows(NullPointerException.class, () -> LearningResource.create("Title", ResourceType.PDF, null, "desc"));
        assertThrows(IllegalArgumentException.class, () -> LearningResource.create("Title", ResourceType.PDF, " ", "desc"));
    }
}
