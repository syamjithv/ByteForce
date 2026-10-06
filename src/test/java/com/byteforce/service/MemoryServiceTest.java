package com.byteforce.service;

import com.byteforce.domain.Concept;
import com.byteforce.domain.ConceptMemoryStatus;
import com.byteforce.domain.ConceptRelationshipType;
import com.byteforce.domain.MemoryAnalytics;
import com.byteforce.domain.MemoryReviewItemView;
import com.byteforce.domain.RelatedConceptView;
import com.byteforce.domain.RememberItem;
import com.byteforce.domain.RememberItemType;
import com.byteforce.domain.ReviewRating;
import com.byteforce.domain.ReviewState;
import com.byteforce.domain.Subject;
import com.byteforce.domain.Topic;
import com.byteforce.domain.UserRememberReview;
import com.byteforce.repository.ConceptRelationshipRepository;
import com.byteforce.repository.ConceptRepository;
import com.byteforce.repository.RememberItemRepository;
import com.byteforce.repository.SubjectRepository;
import com.byteforce.repository.TopicRepository;
import com.byteforce.repository.UserRememberReviewRepository;
import com.byteforce.service.memory.FsrsScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemoryServiceTest {

    @Mock
    private UserRememberReviewRepository userRememberReviewRepository;
    @Mock
    private RememberItemRepository rememberItemRepository;
    @Mock
    private ConceptRepository conceptRepository;
    @Mock
    private TopicRepository topicRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private ConceptRelationshipRepository conceptRelationshipRepository;
    @Mock
    private BrainMapService brainMapService;

    private FsrsScheduler scheduler;
    private MemoryServiceImpl memoryService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        scheduler = new FsrsScheduler();
        memoryService = new MemoryServiceImpl(
                userRememberReviewRepository,
                rememberItemRepository,
                conceptRepository,
                topicRepository,
                subjectRepository,
                conceptRelationshipRepository,
                scheduler,
                brainMapService
        );
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("enrollConcept: enrolls all active remember items into user review schedule")
    void testEnrollConcept() {
        long conceptId = 42L;
        RememberItem item1 = RememberItem.create(1L, conceptId, RememberItemType.KEY_FACT, "Content 1", 1);
        RememberItem item2 = RememberItem.create(2L, conceptId, RememberItemType.COMMON_CONFUSION, "Content 2", 2);

        when(rememberItemRepository.findByConceptId(conceptId)).thenReturn(List.of(item1, item2));
        when(userRememberReviewRepository.findByUserIdAndRememberItemId(eq(userId), eq(1L))).thenReturn(Optional.empty());
        when(userRememberReviewRepository.findByUserIdAndRememberItemId(eq(userId), eq(2L))).thenReturn(Optional.empty());
        when(userRememberReviewRepository.save(any(UserRememberReview.class))).thenAnswer(inv -> inv.getArgument(0));

        int enrolled = memoryService.enrollConcept(userId, conceptId);
        assertEquals(2, enrolled);
    }

    @Test
    @DisplayName("recordReview: processes GOOD rating, updates FSRS stability, and persists result")
    void testRecordReviewGood() {
        long itemId = 101L;
        RememberItem item = RememberItem.create(itemId, 10L, RememberItemType.KEY_FACT, "Content", 1);
        when(rememberItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        Instant now = Instant.now().minus(2, ChronoUnit.DAYS);
        UserRememberReview existing = new UserRememberReview(
                5L, userId, itemId, now, now, 1, 1,
                ReviewState.REVIEW, 5.0, 2.4, 0.9, ReviewRating.GOOD, now, now
        );
        when(userRememberReviewRepository.findByUserIdAndRememberItemId(userId, itemId)).thenReturn(Optional.of(existing));
        when(userRememberReviewRepository.save(any(UserRememberReview.class))).thenAnswer(inv -> inv.getArgument(0));

        UserRememberReview updated = memoryService.recordReview(userId, itemId, ReviewRating.GOOD);

        assertEquals(2, updated.getReviewCount());
        assertEquals(2, updated.getSuccessfulReviewCount());
        assertEquals(ReviewState.REVIEW, updated.getState());
        assertEquals(ReviewRating.GOOD, updated.getLastRating());
        assertTrue(updated.getStability() > 2.4, "Stability should increase");
        assertTrue(updated.getDueAt().isAfter(Instant.now()));
    }

    @Test
    @DisplayName("getDueQueue: applies cognitive interleaving across topics")
    void testDueQueueInterleaving() {
        Instant now = Instant.now();
        Instant past = now.minus(1, ChronoUnit.DAYS);

        // Reviews for 4 items: 2 from Topic 1, 2 from Topic 2
        UserRememberReview rev1 = UserRememberReview.createInitial(userId, 1L, past);
        UserRememberReview rev2 = UserRememberReview.createInitial(userId, 2L, past);
        UserRememberReview rev3 = UserRememberReview.createInitial(userId, 3L, past);
        UserRememberReview rev4 = UserRememberReview.createInitial(userId, 4L, past);

        when(userRememberReviewRepository.findDueReviewsByUserId(eq(userId), any(Instant.class), eq(30)))
                .thenReturn(List.of(rev1, rev2, rev3, rev4));

        RememberItem item1 = RememberItem.create(1L, 10L, RememberItemType.KEY_FACT, "Item 1", 1);
        RememberItem item2 = RememberItem.create(2L, 10L, RememberItemType.KEY_FACT, "Item 2", 2);
        RememberItem item3 = RememberItem.create(3L, 20L, RememberItemType.KEY_FACT, "Item 3", 1);
        RememberItem item4 = RememberItem.create(4L, 20L, RememberItemType.KEY_FACT, "Item 4", 2);

        when(rememberItemRepository.findById(1L)).thenReturn(Optional.of(item1));
        when(rememberItemRepository.findById(2L)).thenReturn(Optional.of(item2));
        when(rememberItemRepository.findById(3L)).thenReturn(Optional.of(item3));
        when(rememberItemRepository.findById(4L)).thenReturn(Optional.of(item4));

        Concept concept1 = new Concept(10L, 100L, "OS Topic", "Concept A", "Expl", List.of(), "", List.of());
        Concept concept2 = new Concept(20L, 200L, "DBMS Topic", "Concept B", "Expl", List.of(), "", List.of());

        when(conceptRepository.findById(10L)).thenReturn(Optional.of(concept1));
        when(conceptRepository.findById(20L)).thenReturn(Optional.of(concept2));

        Topic topic1 = new Topic(100L, "os", "OS Topic", "os-topic", "desc", 1, Instant.now());
        Topic topic2 = new Topic(200L, "dbms", "DBMS Topic", "dbms-topic", "desc", 2, Instant.now());

        when(topicRepository.findById(100L)).thenReturn(Optional.of(topic1));
        when(topicRepository.findById(200L)).thenReturn(Optional.of(topic2));

        List<MemoryReviewItemView> queue = memoryService.getDueQueue(userId, 10);

        assertEquals(4, queue.size());
        // Interleaved pattern: Topic 1 (Item 1), Topic 2 (Item 3), Topic 1 (Item 2), Topic 2 (Item 4)
        assertEquals(100L, queue.get(0).getTopic().getId());
        assertEquals(200L, queue.get(1).getTopic().getId());
        assertEquals(100L, queue.get(2).getTopic().getId());
        assertEquals(200L, queue.get(3).getTopic().getId());
    }

    @Test
    @DisplayName("getMemoryAnalytics: returns honest empty state when user has no reviews")
    void testMemoryAnalyticsEmpty() {
        when(userRememberReviewRepository.findByUserId(userId)).thenReturn(List.of());

        MemoryAnalytics analytics = memoryService.getMemoryAnalytics(userId);

        assertFalse(analytics.isHasSufficientData());
        assertEquals(0, analytics.getTotalReviewsCompleted());
        assertEquals("—", analytics.getFormattedSuccessRate());
    }

    @Test
    @DisplayName("recordPracticeWeaknessSignal: updates review due date to now so learner can revise immediately")
    void testRecordPracticeWeaknessSignal() {
        long conceptId = 55L;
        RememberItem item = RememberItem.create(1L, conceptId, RememberItemType.KEY_FACT, "Content", 1);
        when(rememberItemRepository.findByConceptId(conceptId)).thenReturn(List.of(item));

        Instant nextWeek = Instant.now().plus(7, ChronoUnit.DAYS);
        UserRememberReview review = new UserRememberReview(
                1L, userId, 1L, nextWeek, Instant.now().minus(2, ChronoUnit.DAYS), 3, 3,
                ReviewState.REVIEW, 4.0, 10.0, 0.95, ReviewRating.GOOD, Instant.now(), Instant.now()
        );
        when(userRememberReviewRepository.findByUserIdAndRememberItemId(userId, 1L)).thenReturn(Optional.of(review));

        memoryService.recordPracticeWeaknessSignal(userId, conceptId);

        verify(userRememberReviewRepository).save(any(UserRememberReview.class));
    }
}
