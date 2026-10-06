package com.byteforce.domain;

/**
 * Presentation-friendly view model for the "Continue Learning" dashboard card:
 * Displays the student's most recently accessed unfinished learning concept.
 * E.g.:
 * Continue Learning
 * Computer Networks (subjectName)
 * TCP Three-Way Handshake (conceptTitle)
 * 3 of 8 concepts (conceptIndex of totalConceptsInTopic)
 */
public record ContinueLearningView(
        long conceptId,
        String conceptTitle,
        long topicId,
        String topicName,
        String subjectId,
        String subjectName,
        int conceptIndex,
        int totalConceptsInTopic
) {}
