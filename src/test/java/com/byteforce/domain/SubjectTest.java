package com.byteforce.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubjectTest {

    @Test
    @DisplayName("Subject should create entity with valid properties and immutable topics")
    void shouldCreateSubjectSuccessfully() {
        Topic arrays = new Topic(1L, "Arrays", "arrays", "Array topics", 1, Instant.now());
        Subject subject = Subject.create("ds", "Data Structures", "Data structure fundamentals", 1, List.of(arrays));

        assertNotNull(subject);
        assertEquals("ds", subject.getId());
        assertEquals("Data Structures", subject.getName());
        assertEquals("Data structure fundamentals", subject.getDescription());
        assertEquals(1, subject.getDisplayOrder());
        assertEquals(1, subject.getTopics().size());
        assertEquals("Arrays", subject.getTopics().get(0).getName());

        assertThrows(UnsupportedOperationException.class, () -> subject.getTopics().add(arrays));
    }

    @Test
    @DisplayName("Subject should throw exception on null or blank ID and Name")
    void shouldValidateSubjectInputs() {
        assertThrows(NullPointerException.class, () -> Subject.create(null, "Name", "Desc", 1, List.of()));
        assertThrows(IllegalArgumentException.class, () -> Subject.create("", "Name", "Desc", 1, List.of()));
        assertThrows(NullPointerException.class, () -> Subject.create("id", null, "Desc", 1, List.of()));
        assertThrows(IllegalArgumentException.class, () -> Subject.create("id", "  ", "Desc", 1, List.of()));
    }
}
