package com.byteforce.service;

import com.byteforce.domain.ContinueLearningView;
import com.byteforce.domain.RecentlyViewedConcept;
import com.byteforce.domain.UserConceptProgress;
import com.byteforce.repository.UserConceptProgressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Production implementation of {@link ConceptProgressService}.
 */
public class ConceptProgressServiceImpl implements ConceptProgressService {

    private static final Logger log = LoggerFactory.getLogger(ConceptProgressServiceImpl.class);

    private final UserConceptProgressRepository progressRepository;

    public ConceptProgressServiceImpl(UserConceptProgressRepository progressRepository) {
        this.progressRepository = Objects.requireNonNull(progressRepository, "progressRepository must not be null");
    }

    @Override
    public void recordConceptView(UUID userId, long conceptId) {
        if (userId == null || conceptId <= 0) return;
        try {
            progressRepository.recordView(userId, conceptId);
        } catch (Exception e) {
            log.warn("Failed to record concept view for user {} concept {}: {}", userId, conceptId, e.getMessage());
        }
    }

    @Override
    public boolean markConceptLearned(UUID userId, long conceptId, boolean learned) {
        if (userId == null || conceptId <= 0) return false;
        log.info("Setting concept learned state: user={}, conceptId={}, learned={}", userId, conceptId, learned);
        return progressRepository.setCompleted(userId, conceptId, learned);
    }

    @Override
    public boolean isConceptLearned(UUID userId, long conceptId) {
        if (userId == null || conceptId <= 0) return false;
        return progressRepository.findByUserAndConcept(userId, conceptId)
                .map(UserConceptProgress::isCompleted)
                .orElse(false);
    }

    @Override
    public Optional<ContinueLearningView> getContinueLearning(UUID userId) {
        if (userId == null) return Optional.empty();
        return progressRepository.findContinueLearning(userId);
    }

    @Override
    public List<RecentlyViewedConcept> getRecentlyViewed(UUID userId, int limit) {
        if (userId == null) return Collections.emptyList();
        return progressRepository.findRecentlyViewed(userId, limit);
    }

    @Override
    public long getCompletedCountForTopic(UUID userId, long topicId) {
        if (userId == null || topicId <= 0) return 0;
        return progressRepository.countCompletedByTopic(userId, topicId);
    }

    @Override
    public long getCompletedCountForSubject(UUID userId, String subjectId) {
        if (userId == null || subjectId == null || subjectId.isBlank()) return 0;
        return progressRepository.countCompletedBySubject(userId, subjectId);
    }

    @Override
    public Map<Long, Boolean> getCompletionMapForTopic(UUID userId, long topicId) {
        if (userId == null || topicId <= 0) return Collections.emptyMap();
        return progressRepository.getCompletionMapForTopic(userId, topicId);
    }
}
