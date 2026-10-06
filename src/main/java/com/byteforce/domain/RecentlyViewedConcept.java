package com.byteforce.domain;

import java.time.Instant;

/**
 * Presentation-friendly view model for recently viewed concepts.
 */
public record RecentlyViewedConcept(
        long conceptId,
        String conceptTitle,
        long topicId,
        String topicName,
        String subjectId,
        String subjectName,
        boolean completed,
        Instant lastViewedAt
) {}
