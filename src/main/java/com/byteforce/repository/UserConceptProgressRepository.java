package com.byteforce.repository;

import com.byteforce.domain.ContinueLearningView;
import com.byteforce.domain.RecentlyViewedConcept;
import com.byteforce.domain.UserConceptProgress;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Repository for student concept completion and viewing history.
 */
public interface UserConceptProgressRepository {

    Optional<UserConceptProgress> findByUserAndConcept(UUID userId, long conceptId);

    UserConceptProgress save(UserConceptProgress progress);

    void recordView(UUID userId, long conceptId);

    boolean setCompleted(UUID userId, long conceptId, boolean completed);

    List<RecentlyViewedConcept> findRecentlyViewed(UUID userId, int limit);

    Optional<ContinueLearningView> findContinueLearning(UUID userId);

    long countCompletedByTopic(UUID userId, long topicId);

    long countCompletedBySubject(UUID userId, String subjectId);

    Set<Long> findCompletedConceptIds(UUID userId, Collection<Long> conceptIds);

    Map<Long, Boolean> getCompletionMapForTopic(UUID userId, long topicId);
}
