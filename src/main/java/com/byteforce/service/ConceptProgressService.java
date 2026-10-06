package com.byteforce.service;

import com.byteforce.domain.ContinueLearningView;
import com.byteforce.domain.RecentlyViewedConcept;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for tracking concept completion and learning history.
 */
public interface ConceptProgressService {

    void recordConceptView(UUID userId, long conceptId);

    boolean markConceptLearned(UUID userId, long conceptId, boolean learned);

    boolean isConceptLearned(UUID userId, long conceptId);

    Optional<ContinueLearningView> getContinueLearning(UUID userId);

    List<RecentlyViewedConcept> getRecentlyViewed(UUID userId, int limit);

    long getCompletedCountForTopic(UUID userId, long topicId);

    long getCompletedCountForSubject(UUID userId, String subjectId);

    Map<Long, Boolean> getCompletionMapForTopic(UUID userId, long topicId);
}
