package com.byteforce.service;

import com.byteforce.domain.Concept;
import com.byteforce.domain.ConceptMemoryStatus;
import com.byteforce.domain.ConceptRelationship;
import com.byteforce.domain.MemoryAnalytics;
import com.byteforce.domain.MemoryReviewItemView;
import com.byteforce.domain.RelatedConceptView;
import com.byteforce.domain.RememberItem;
import com.byteforce.domain.ReviewRating;
import com.byteforce.domain.ReviewState;
import com.byteforce.domain.SchedulingResult;
import com.byteforce.domain.Subject;
import com.byteforce.domain.Topic;
import com.byteforce.domain.UserRememberReview;
import com.byteforce.exception.ResourceNotFoundException;
import com.byteforce.repository.ConceptRelationshipRepository;
import com.byteforce.repository.ConceptRepository;
import com.byteforce.repository.RememberItemRepository;
import com.byteforce.repository.SubjectRepository;
import com.byteforce.repository.TopicRepository;
import com.byteforce.repository.UserRememberReviewRepository;
import com.byteforce.service.memory.SpacedRepetitionScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Production implementation of {@link MemoryService}.
 */
public class MemoryServiceImpl implements MemoryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryServiceImpl.class);

    private final UserRememberReviewRepository userRememberReviewRepository;
    private final RememberItemRepository rememberItemRepository;
    private final ConceptRepository conceptRepository;
    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final ConceptRelationshipRepository conceptRelationshipRepository;
    private final SpacedRepetitionScheduler scheduler;
    private final BrainMapService brainMapService;

    public MemoryServiceImpl(UserRememberReviewRepository userRememberReviewRepository,
                             RememberItemRepository rememberItemRepository,
                             ConceptRepository conceptRepository,
                             TopicRepository topicRepository,
                             SubjectRepository subjectRepository,
                             ConceptRelationshipRepository conceptRelationshipRepository,
                             SpacedRepetitionScheduler scheduler,
                             BrainMapService brainMapService) {
        this.userRememberReviewRepository = Objects.requireNonNull(userRememberReviewRepository, "userRememberReviewRepository must not be null");
        this.rememberItemRepository = Objects.requireNonNull(rememberItemRepository, "rememberItemRepository must not be null");
        this.conceptRepository = Objects.requireNonNull(conceptRepository, "conceptRepository must not be null");
        this.topicRepository = Objects.requireNonNull(topicRepository, "topicRepository must not be null");
        this.subjectRepository = Objects.requireNonNull(subjectRepository, "subjectRepository must not be null");
        this.conceptRelationshipRepository = Objects.requireNonNull(conceptRelationshipRepository, "conceptRelationshipRepository must not be null");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler must not be null");
        this.brainMapService = Objects.requireNonNull(brainMapService, "brainMapService must not be null");
    }

    @Override
    public int enrollConcept(UUID userId, long conceptId) {
        Objects.requireNonNull(userId, "userId must not be null");
        if (conceptId <= 0) {
            return 0;
        }

        List<RememberItem> items = rememberItemRepository.findByConceptId(conceptId)
                .stream()
                .filter(RememberItem::isActive)
                .toList();

        if (items.isEmpty()) {
            return 0;
        }

        int enrolled = 0;
        Instant now = Instant.now();
        for (RememberItem item : items) {
            Optional<UserRememberReview> existing = userRememberReviewRepository.findByUserIdAndRememberItemId(userId, item.getId());
            if (existing.isEmpty()) {
                UserRememberReview initial = UserRememberReview.createInitial(userId, item.getId(), now);
                userRememberReviewRepository.save(initial);
                enrolled++;
            }
        }
        log.info("Enrolled {} remember items for concept ID {} and user {}", enrolled, conceptId, userId);
        return enrolled;
    }

    @Override
    public List<MemoryReviewItemView> getDueQueue(UUID userId, int limit) {
        Objects.requireNonNull(userId, "userId must not be null");
        int safeLimit = Math.max(1, Math.min(limit, 50));
        Instant now = Instant.now();

        List<UserRememberReview> dueReviews = userRememberReviewRepository.findDueReviewsByUserId(userId, now, safeLimit * 3);
        if (dueReviews.isEmpty()) {
            return List.of();
        }

        // Cache concepts, topics, subjects to avoid N+1 queries
        Map<Long, RememberItem> itemMap = new HashMap<>();
        Map<Long, Concept> conceptMap = new HashMap<>();
        Map<Long, Topic> topicMap = new HashMap<>();
        Map<String, Subject> subjectMap = new HashMap<>();

        List<MemoryReviewItemView> rawViews = new ArrayList<>();
        for (UserRememberReview rev : dueReviews) {
            RememberItem item = itemMap.computeIfAbsent(rev.getRememberItemId(),
                    id -> rememberItemRepository.findById(id).orElse(null));
            if (item == null || !item.isActive()) {
                continue;
            }

            Concept concept = conceptMap.computeIfAbsent(item.getConceptId(),
                    id -> conceptRepository.findById(id).orElse(null));
            if (concept == null) {
                continue;
            }

            Topic topic = topicMap.computeIfAbsent(concept.getTopicId(),
                    id -> topicRepository.findById(id).orElse(null));

            String subjectId = topic != null ? topic.getSubjectId() : null;
            Subject subject = (subjectId != null)
                    ? subjectMap.computeIfAbsent(subjectId, id -> subjectRepository.findById(id).orElse(null))
                    : null;

            double retrievability = scheduler.calculateRetrievability(rev.getStability(), rev.getLastReviewedAt(), now);
            Map<ReviewRating, SchedulingResult> previews = scheduler.previewAllRatings(rev, now);

            rawViews.add(new MemoryReviewItemView(rev, item, concept, topic, subject, previews, retrievability));
        }

        // Apply intelligent cognitive interleaving
        return interleaveDueItems(rawViews, safeLimit);
    }

    /**
     * Interleaves review items so that questions from different topics and concepts
     * are alternated rather than presented in monotonous blocks.
     */
    private List<MemoryReviewItemView> interleaveDueItems(List<MemoryReviewItemView> items, int limit) {
        if (items.size() <= 2) {
            return items.stream().limit(limit).toList();
        }

        // Group items by topic ID (or concept ID if no topic)
        Map<Long, Queue<MemoryReviewItemView>> queuesByTopic = new LinkedHashMap<>();
        for (MemoryReviewItemView view : items) {
            long key = view.getTopic() != null ? view.getTopic().getId() : view.getConcept().getId();
            queuesByTopic.computeIfAbsent(key, k -> new LinkedList<>()).add(view);
        }

        List<MemoryReviewItemView> interleaved = new ArrayList<>(items.size());
        boolean hasItemsLeft = true;

        // Round-robin selection across distinct topics
        while (hasItemsLeft && interleaved.size() < limit) {
            hasItemsLeft = false;
            for (Queue<MemoryReviewItemView> queue : queuesByTopic.values()) {
                if (!queue.isEmpty()) {
                    interleaved.add(queue.poll());
                    hasItemsLeft = true;
                    if (interleaved.size() >= limit) {
                        break;
                    }
                }
            }
        }

        return interleaved;
    }

    @Override
    public UserRememberReview recordReview(UUID userId, long rememberItemId, ReviewRating rating) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(rating, "rating must not be null");
        if (rememberItemId <= 0) {
            throw new IllegalArgumentException("rememberItemId must be positive");
        }

        RememberItem item = rememberItemRepository.findById(rememberItemId)
                .orElseThrow(() -> new ResourceNotFoundException("RememberItem not found with ID: " + rememberItemId));

        Instant now = Instant.now();
        UserRememberReview review = userRememberReviewRepository.findByUserIdAndRememberItemId(userId, rememberItemId)
                .orElseGet(() -> UserRememberReview.createInitial(userId, rememberItemId, now));

        SchedulingResult result = scheduler.schedule(review, rating, now);

        UserRememberReview updated = review.withReviewResult(
                rating,
                result.getNextState(),
                result.getDifficulty(),
                result.getStability(),
                result.getRetrievability(),
                result.getNextDueAt(),
                now
        );

        UserRememberReview saved = userRememberReviewRepository.save(updated);
        log.info("Recorded review for user {} item {}: rating={}, nextState={}, interval={}",
                userId, rememberItemId, rating, result.getNextState(), result.getIntervalLabel());
        return saved;
    }

    @Override
    public Map<ReviewRating, SchedulingResult> previewRatings(UUID userId, long rememberItemId) {
        Objects.requireNonNull(userId, "userId must not be null");
        Instant now = Instant.now();
        UserRememberReview review = userRememberReviewRepository.findByUserIdAndRememberItemId(userId, rememberItemId)
                .orElseGet(() -> UserRememberReview.createInitial(userId, rememberItemId, now));
        return scheduler.previewAllRatings(review, now);
    }

    @Override
    public ConceptMemoryStatus getConceptMemoryStatus(UUID userId, long conceptId) {
        Objects.requireNonNull(userId, "userId must not be null");
        Concept concept = conceptRepository.findById(conceptId)
                .orElseThrow(() -> new ResourceNotFoundException("Concept not found with ID: " + conceptId));

        List<RememberItem> items = rememberItemRepository.findByConceptId(conceptId)
                .stream()
                .filter(RememberItem::isActive)
                .toList();

        List<RelatedConceptView> related = brainMapService.getRelatedConceptsForConcept(conceptId);

        if (items.isEmpty()) {
            return ConceptMemoryStatus.notEnrolled(conceptId, concept.getTitle(), 0, related);
        }

        List<UserRememberReview> reviews = userRememberReviewRepository.findByUserIdAndConceptId(userId, conceptId);
        if (reviews.isEmpty()) {
            return ConceptMemoryStatus.notEnrolled(conceptId, concept.getTitle(), items.size(), related);
        }

        Instant now = Instant.now();
        int dueCount = 0;
        int totalReviews = 0;
        int successfulReviews = 0;
        double sumStability = 0.0;
        double sumDifficulty = 0.0;
        Instant earliestDue = null;
        Instant latestReviewed = null;

        Map<ReviewState, Integer> stateCounts = new HashMap<>();

        for (UserRememberReview r : reviews) {
            if (r.isDue(now)) {
                dueCount++;
            }
            if (earliestDue == null || r.getDueAt().isBefore(earliestDue)) {
                earliestDue = r.getDueAt();
            }
            if (r.getLastReviewedAt() != null) {
                if (latestReviewed == null || r.getLastReviewedAt().isAfter(latestReviewed)) {
                    latestReviewed = r.getLastReviewedAt();
                }
            }
            totalReviews += r.getReviewCount();
            successfulReviews += r.getSuccessfulReviewCount();
            sumStability += r.getStability();
            sumDifficulty += r.getDifficulty();
            stateCounts.put(r.getState(), stateCounts.getOrDefault(r.getState(), 0) + 1);
        }

        double avgStability = reviews.isEmpty() ? 0.0 : Math.round((sumStability / reviews.size()) * 10.0) / 10.0;
        double avgDifficulty = reviews.isEmpty() ? 0.0 : Math.round((sumDifficulty / reviews.size()) * 10.0) / 10.0;

        ReviewState dominantState = stateCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(ReviewState.NEW);

        return new ConceptMemoryStatus(
                conceptId,
                concept.getTitle(),
                true,
                items.size(),
                reviews.size(),
                dueCount,
                earliestDue,
                latestReviewed,
                totalReviews,
                successfulReviews,
                avgStability,
                avgDifficulty,
                dominantState,
                related
        );
    }

    @Override
    public MemoryAnalytics getMemoryAnalytics(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        List<UserRememberReview> all = userRememberReviewRepository.findByUserId(userId);

        if (all.isEmpty()) {
            return MemoryAnalytics.empty();
        }

        Instant now = Instant.now();
        long dueCount = 0;
        long totalReviews = 0;
        long successfulReviews = 0;
        long stableCount = 0;
        long learningCount = 0;
        long relearningCount = 0;

        Set<LocalDate> reviewDates = new HashSet<>();
        ZoneId zone = ZoneId.of("UTC");

        for (UserRememberReview r : all) {
            if (r.isDue(now)) {
                dueCount++;
            }
            totalReviews += r.getReviewCount();
            successfulReviews += r.getSuccessfulReviewCount();

            if (r.getState() == ReviewState.REVIEW && r.getStability() >= 7.0) {
                stableCount++;
            } else if (r.getState() == ReviewState.RELEARNING) {
                relearningCount++;
            } else {
                learningCount++;
            }

            if (r.getLastReviewedAt() != null) {
                reviewDates.add(r.getLastReviewedAt().atZone(zone).toLocalDate());
            }
        }

        double successRate = totalReviews > 0 ? ((double) successfulReviews * 100.0 / totalReviews) : 0.0;
        int streak = calculateStreak(reviewDates);
        List<MemoryReviewItemView> weakItems = getWeakItems(userId, 5);

        boolean hasSufficientData = totalReviews > 0;

        return new MemoryAnalytics(
                all.size(),
                dueCount,
                totalReviews,
                successfulReviews,
                successRate,
                stableCount,
                learningCount,
                relearningCount,
                streak,
                weakItems,
                hasSufficientData
        );
    }

    private int calculateStreak(Set<LocalDate> dates) {
        if (dates.isEmpty()) {
            return 0;
        }
        LocalDate current = LocalDate.now(ZoneId.of("UTC"));
        int streak = 0;
        // Count consecutive days backward starting today or yesterday
        if (!dates.contains(current)) {
            current = current.minusDays(1);
        }
        while (dates.contains(current)) {
            streak++;
            current = current.minusDays(1);
        }
        return streak;
    }

    @Override
    public Optional<MemoryReviewItemView> getNextDueItem(UUID userId) {
        List<MemoryReviewItemView> queue = getDueQueue(userId, 1);
        return queue.isEmpty() ? Optional.empty() : Optional.of(queue.getFirst());
    }

    @Override
    public Optional<MemoryReviewItemView> getReviewItem(UUID userId, long rememberItemId) {
        Objects.requireNonNull(userId, "userId must not be null");
        RememberItem item = rememberItemRepository.findById(rememberItemId).orElse(null);
        if (item == null || !item.isActive()) {
            return Optional.empty();
        }

        Concept concept = conceptRepository.findById(item.getConceptId()).orElse(null);
        if (concept == null) {
            return Optional.empty();
        }

        Topic topic = topicRepository.findById(concept.getTopicId()).orElse(null);
        String subjectId = topic != null ? topic.getSubjectId() : null;
        Subject subject = subjectId != null ? subjectRepository.findById(subjectId).orElse(null) : null;

        Instant now = Instant.now();
        UserRememberReview review = userRememberReviewRepository.findByUserIdAndRememberItemId(userId, rememberItemId)
                .orElseGet(() -> UserRememberReview.createInitial(userId, rememberItemId, now));

        double retrievability = scheduler.calculateRetrievability(review.getStability(), review.getLastReviewedAt(), now);
        Map<ReviewRating, SchedulingResult> previews = scheduler.previewAllRatings(review, now);

        return Optional.of(new MemoryReviewItemView(review, item, concept, topic, subject, previews, retrievability));
    }

    @Override
    public void recordPracticeWeaknessSignal(UUID userId, long conceptId) {
        Objects.requireNonNull(userId, "userId must not be null");
        if (conceptId <= 0) {
            return;
        }

        List<RememberItem> items = rememberItemRepository.findByConceptId(conceptId);
        if (items.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        for (RememberItem item : items) {
            Optional<UserRememberReview> reviewOpt = userRememberReviewRepository.findByUserIdAndRememberItemId(userId, item.getId());
            if (reviewOpt.isPresent()) {
                UserRememberReview rev = reviewOpt.get();
                // Lower stability slightly and make item immediately due for retrieval practice
                double adjustedStability = Math.max(0.2, rev.getStability() * 0.7);
                double adjustedDifficulty = Math.min(10.0, rev.getDifficulty() + 0.5);
                UserRememberReview adjusted = new UserRememberReview(
                        rev.getId(),
                        rev.getUserId(),
                        rev.getRememberItemId(),
                        now, // due immediately
                        rev.getLastReviewedAt(),
                        rev.getReviewCount(),
                        rev.getSuccessfulReviewCount(),
                        rev.getState() == ReviewState.NEW ? ReviewState.NEW : ReviewState.RELEARNING,
                        adjustedDifficulty,
                        adjustedStability,
                        rev.getRetrievability(),
                        rev.getLastRating(),
                        rev.getCreatedAt(),
                        now
                );
                userRememberReviewRepository.save(adjusted);
            } else {
                // If not enrolled yet, enroll it and schedule due immediately
                UserRememberReview initial = UserRememberReview.createInitial(userId, item.getId(), now);
                userRememberReviewRepository.save(initial);
            }
        }
        log.info("Recorded practice weakness signal for concept ID {} and user {}", conceptId, userId);
    }

    @Override
    public List<MemoryReviewItemView> getRecentlyReviewed(UUID userId, int limit) {
        Objects.requireNonNull(userId, "userId must not be null");
        List<UserRememberReview> list = userRememberReviewRepository.findRecentlyReviewedByUserId(userId, limit);
        return populateViews(list, userId);
    }

    @Override
    public List<MemoryReviewItemView> getWeakItems(UUID userId, int limit) {
        Objects.requireNonNull(userId, "userId must not be null");
        List<UserRememberReview> list = userRememberReviewRepository.findWeakReviewsByUserId(userId, limit);
        return populateViews(list, userId);
    }

    private List<MemoryReviewItemView> populateViews(List<UserRememberReview> reviews, UUID userId) {
        if (reviews.isEmpty()) {
            return List.of();
        }
        Instant now = Instant.now();
        Map<Long, RememberItem> itemMap = new HashMap<>();
        Map<Long, Concept> conceptMap = new HashMap<>();
        Map<Long, Topic> topicMap = new HashMap<>();
        Map<String, Subject> subjectMap = new HashMap<>();

        List<MemoryReviewItemView> views = new ArrayList<>();
        for (UserRememberReview rev : reviews) {
            RememberItem item = itemMap.computeIfAbsent(rev.getRememberItemId(),
                    id -> rememberItemRepository.findById(id).orElse(null));
            if (item == null) {
                continue;
            }

            Concept concept = conceptMap.computeIfAbsent(item.getConceptId(),
                    id -> conceptRepository.findById(id).orElse(null));
            if (concept == null) {
                continue;
            }

            Topic topic = topicMap.computeIfAbsent(concept.getTopicId(),
                    id -> topicRepository.findById(id).orElse(null));
            String subjectId = topic != null ? topic.getSubjectId() : null;
            Subject subject = subjectId != null
                    ? subjectMap.computeIfAbsent(subjectId, id -> subjectRepository.findById(id).orElse(null))
                    : null;

            double retrievability = scheduler.calculateRetrievability(rev.getStability(), rev.getLastReviewedAt(), now);
            Map<ReviewRating, SchedulingResult> previews = scheduler.previewAllRatings(rev, now);

            views.add(new MemoryReviewItemView(rev, item, concept, topic, subject, previews, retrievability));
        }
        return views;
    }
}
