package com.byteforce.service;

import com.byteforce.domain.Activity;
import com.byteforce.domain.ActivityType;
import com.byteforce.domain.Assessment;
import com.byteforce.domain.AssessmentAttempt;
import com.byteforce.domain.AssessmentAttemptSummary;
import com.byteforce.domain.AttemptStatus;
import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;
import com.byteforce.domain.QuestionAttempt;
import com.byteforce.domain.QuestionType;
import com.byteforce.domain.StudentProgressSummary;
import com.byteforce.domain.Topic;
import com.byteforce.domain.TopicPerformance;
import com.byteforce.repository.QuestionRepository;
import com.byteforce.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackServiceImplTest {

    @Mock
    private AttemptService mockAttemptService;

    @Mock
    private AssessmentService mockAssessmentService;

    @Mock
    private ActivityService mockActivityService;

    @Mock
    private BookmarkService mockBookmarkService;

    @Mock
    private QuestionRepository mockQuestionRepository;

    @Mock
    private TopicRepository mockTopicRepository;

    private TrackServiceImpl trackService;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        trackService = new TrackServiceImpl(
                mockAttemptService,
                mockAssessmentService,
                mockActivityService,
                mockBookmarkService,
                mockQuestionRepository,
                mockTopicRepository
        );
        testUserId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should return empty progress summary when user has zero activity")
    void shouldReturnEmptyProgressSummaryWhenNoActivity() {
        when(mockAttemptService.getAttemptsForUser(testUserId)).thenReturn(List.of());
        when(mockAssessmentService.getAttemptsForUser(testUserId)).thenReturn(List.of());
        when(mockBookmarkService.getBookmarkCount(testUserId)).thenReturn(0L);

        StudentProgressSummary summary = trackService.getOverallProgress(testUserId);

        assertNotNull(summary);
        assertEquals(0, summary.totalAttempts());
        assertEquals(0, summary.solvedCount());
        assertEquals(0, summary.failedCount());
        assertEquals(0, summary.skippedCount());
        assertEquals(0.0, summary.accuracyPercentage());
        assertEquals(0, summary.assessmentsCompleted());
        assertEquals(0, summary.bookmarkedCount());
    }

    @Test
    @DisplayName("Should calculate correct overall progress metrics with real data")
    void shouldCalculateOverallProgressMetrics() {
        QuestionAttempt a1 = new QuestionAttempt(1L, testUserId, 10L, AttemptStatus.SOLVED, "code1", 120, Instant.now());
        QuestionAttempt a2 = new QuestionAttempt(2L, testUserId, 11L, AttemptStatus.SOLVED, "code2", 90, Instant.now());
        QuestionAttempt a3 = new QuestionAttempt(3L, testUserId, 12L, AttemptStatus.FAILED, "code3", 150, Instant.now());
        QuestionAttempt a4 = new QuestionAttempt(4L, testUserId, 13L, AttemptStatus.SKIPPED, null, 0, Instant.now());

        when(mockAttemptService.getAttemptsForUser(testUserId)).thenReturn(List.of(a1, a2, a3, a4));

        AssessmentAttempt compAttempt = new AssessmentAttempt(1L, 100L, testUserId, 18, "COMPLETED", Instant.now(), Instant.now());
        AssessmentAttempt inProgAttempt = new AssessmentAttempt(2L, 101L, testUserId, 0, "IN_PROGRESS", Instant.now(), null);
        when(mockAssessmentService.getAttemptsForUser(testUserId)).thenReturn(List.of(compAttempt, inProgAttempt));

        when(mockBookmarkService.getBookmarkCount(testUserId)).thenReturn(5L);

        StudentProgressSummary summary = trackService.getOverallProgress(testUserId);

        assertNotNull(summary);
        assertEquals(4, summary.totalAttempts());
        assertEquals(2, summary.solvedCount());
        assertEquals(1, summary.failedCount());
        assertEquals(1, summary.skippedCount());
        // evaluated = 2 solved + 1 failed = 3. Accuracy = 2/3 = 66.7%
        assertEquals(66.7, summary.accuracyPercentage());
        assertEquals(1, summary.assessmentsCompleted());
        assertEquals(5, summary.bookmarkedCount());
    }

    @Test
    @DisplayName("Should handle null userId safely across all methods")
    void shouldHandleNullUserIdSafely() {
        StudentProgressSummary summary = trackService.getOverallProgress(null);
        assertNotNull(summary);
        assertEquals(0, summary.totalAttempts());

        List<TopicPerformance> topics = trackService.getTopicPerformances(null);
        assertTrue(topics.isEmpty());

        List<TopicPerformance> weak = trackService.getWeakAreas(null);
        assertTrue(weak.isEmpty());

        List<AssessmentAttemptSummary> assessments = trackService.getAssessmentHistory(null);
        assertTrue(assessments.isEmpty());

        List<Activity> activities = trackService.getRecentActivities(null, 10);
        assertTrue(activities.isEmpty());
    }

    @Test
    @DisplayName("Should return topic performances for practiced topics only")
    void shouldReturnTopicPerformancesForPracticedTopics() {
        Topic arraysTopic = new Topic(1L, "Arrays", "arrays", "Continuous memory", 1, Instant.now());
        Topic treesTopic = new Topic(2L, "Trees", "trees", "Hierarchical data", 2, Instant.now());

        Question q1 = new Question(10L, 1L, "Two Sum", "two-sum", "Description", Difficulty.EASY, QuestionType.CODING, null, Instant.now(), Instant.now());
        Question q2 = new Question(11L, 1L, "3Sum", "3sum", "Description", Difficulty.MEDIUM, QuestionType.CODING, null, Instant.now(), Instant.now());
        Question q3 = new Question(20L, 2L, "Inorder Traversal", "inorder-traversal", "Description", Difficulty.EASY, QuestionType.CODING, null, Instant.now(), Instant.now());

        QuestionAttempt a1 = new QuestionAttempt(1L, testUserId, 10L, AttemptStatus.SOLVED, "code", 50, Instant.now());
        QuestionAttempt a2 = new QuestionAttempt(2L, testUserId, 11L, AttemptStatus.FAILED, "code", 70, Instant.now());
        QuestionAttempt a3 = new QuestionAttempt(3L, testUserId, 20L, AttemptStatus.SOLVED, "code", 60, Instant.now());

        when(mockAttemptService.getAttemptsForUser(testUserId)).thenReturn(List.of(a1, a2, a3));

        when(mockQuestionRepository.findById(10L)).thenReturn(Optional.of(q1));
        when(mockQuestionRepository.findById(11L)).thenReturn(Optional.of(q2));
        when(mockQuestionRepository.findById(20L)).thenReturn(Optional.of(q3));

        when(mockTopicRepository.findById(1L)).thenReturn(Optional.of(arraysTopic));
        when(mockTopicRepository.findById(2L)).thenReturn(Optional.of(treesTopic));

        List<TopicPerformance> performances = trackService.getTopicPerformances(testUserId);

        assertEquals(2, performances.size());

        // Sorted by name: Arrays then Trees
        TopicPerformance arraysPerf = performances.get(0);
        assertEquals("Arrays", arraysPerf.topicName());
        assertEquals(2, arraysPerf.totalAttempts());
        assertEquals(1, arraysPerf.solvedCount());
        assertEquals(1, arraysPerf.failedCount());
        assertEquals(50.0, arraysPerf.accuracyPercentage());

        TopicPerformance treesPerf = performances.get(1);
        assertEquals("Trees", treesPerf.topicName());
        assertEquals(1, treesPerf.totalAttempts());
        assertEquals(1, treesPerf.solvedCount());
        assertEquals(0, treesPerf.failedCount());
        assertEquals(100.0, treesPerf.accuracyPercentage());
    }

    @Test
    @DisplayName("Should identify weak areas when topic has >= 2 attempts and accuracy < 70%")
    void shouldIdentifyWeakAreasAccurately() {
        Topic arraysTopic = new Topic(1L, "Arrays", "arrays", "Continuous memory", 1, Instant.now());
        Topic treesTopic = new Topic(2L, "Trees", "trees", "Hierarchical data", 2, Instant.now());

        Question q1 = new Question(10L, 1L, "Two Sum", "two-sum", "Description", Difficulty.EASY, QuestionType.CODING, null, Instant.now(), Instant.now());
        Question q2 = new Question(11L, 1L, "3Sum", "3sum", "Description", Difficulty.MEDIUM, QuestionType.CODING, null, Instant.now(), Instant.now());
        Question q3 = new Question(20L, 2L, "Inorder", "inorder", "Description", Difficulty.EASY, QuestionType.CODING, null, Instant.now(), Instant.now());

        // Arrays: 1 solved, 1 failed (50% accuracy, 2 attempts) -> WEAK
        QuestionAttempt a1 = new QuestionAttempt(1L, testUserId, 10L, AttemptStatus.SOLVED, "code", 50, Instant.now());
        QuestionAttempt a2 = new QuestionAttempt(2L, testUserId, 11L, AttemptStatus.FAILED, "code", 70, Instant.now());
        // Trees: 1 solved (100% accuracy, 1 attempt) -> NOT WEAK
        QuestionAttempt a3 = new QuestionAttempt(3L, testUserId, 20L, AttemptStatus.SOLVED, "code", 60, Instant.now());

        when(mockAttemptService.getAttemptsForUser(testUserId)).thenReturn(List.of(a1, a2, a3));
        when(mockQuestionRepository.findById(10L)).thenReturn(Optional.of(q1));
        when(mockQuestionRepository.findById(11L)).thenReturn(Optional.of(q2));
        when(mockQuestionRepository.findById(20L)).thenReturn(Optional.of(q3));
        when(mockTopicRepository.findById(1L)).thenReturn(Optional.of(arraysTopic));
        when(mockTopicRepository.findById(2L)).thenReturn(Optional.of(treesTopic));

        List<TopicPerformance> weakAreas = trackService.getWeakAreas(testUserId);

        assertEquals(1, weakAreas.size());
        assertEquals("Arrays", weakAreas.get(0).topicName());
        assertEquals(50.0, weakAreas.get(0).accuracyPercentage());
    }

    @Test
    @DisplayName("Should return empty weak areas when total attempts are fewer than threshold")
    void shouldReturnEmptyWeakAreasWhenInsufficientAttempts() {
        Topic arraysTopic = new Topic(1L, "Arrays", "arrays", "Continuous memory", 1, Instant.now());
        Question q1 = new Question(10L, 1L, "Two Sum", "two-sum", "Description", Difficulty.EASY, QuestionType.CODING, null, Instant.now(), Instant.now());

        // Only 1 attempt total in entire history (< 3 threshold)
        QuestionAttempt a1 = new QuestionAttempt(1L, testUserId, 10L, AttemptStatus.FAILED, "code", 50, Instant.now());

        when(mockAttemptService.getAttemptsForUser(testUserId)).thenReturn(List.of(a1));
        when(mockQuestionRepository.findById(10L)).thenReturn(Optional.of(q1));
        when(mockTopicRepository.findById(1L)).thenReturn(Optional.of(arraysTopic));

        List<TopicPerformance> weakAreas = trackService.getWeakAreas(testUserId);
        assertTrue(weakAreas.isEmpty(), "Should return empty when practice attempts are insufficient");
    }

    @Test
    @DisplayName("Should return assessment history for completed tests sorted by date")
    void shouldReturnAssessmentHistoryForCompletedTests() {
        Instant t1 = Instant.parse("2026-09-28T10:00:00Z");
        Instant t2 = Instant.parse("2026-09-29T10:00:00Z");

        AssessmentAttempt comp1 = new AssessmentAttempt(1L, 100L, testUserId, 15, "COMPLETED", t1.minusSeconds(3600), t1);
        AssessmentAttempt comp2 = new AssessmentAttempt(2L, 101L, testUserId, 19, "COMPLETED", t2.minusSeconds(3600), t2);
        AssessmentAttempt inProg = new AssessmentAttempt(3L, 102L, testUserId, 0, "IN_PROGRESS", Instant.now(), null);

        when(mockAssessmentService.getAttemptsForUser(testUserId)).thenReturn(List.of(comp1, comp2, inProg));

        Assessment ass1 = Assessment.create("Java Basics Mock", "Description", 30, 20, Difficulty.EASY, null);
        Assessment ass2 = Assessment.create("DSA Placement Assessment", "Description", 45, 20, Difficulty.HARD, null);

        when(mockAssessmentService.getAssessmentById(100L)).thenReturn(Optional.of(ass1));
        when(mockAssessmentService.getAssessmentById(101L)).thenReturn(Optional.of(ass2));

        List<AssessmentAttemptSummary> history = trackService.getAssessmentHistory(testUserId);

        assertEquals(2, history.size(), "Only completed assessments should appear in history");

        // Most recent first: comp2 (t2) then comp1 (t1)
        assertEquals("DSA Placement Assessment", history.get(0).assessmentTitle());
        assertEquals(19, history.get(0).score());
        assertEquals(20, history.get(0).totalMarks());
        assertEquals(95.0, history.get(0).percentage());

        assertEquals("Java Basics Mock", history.get(1).assessmentTitle());
        assertEquals(15, history.get(1).score());
        assertEquals(20, history.get(1).totalMarks());
        assertEquals(75.0, history.get(1).percentage());
    }

    @Test
    @DisplayName("Should return recent activities from ActivityService")
    void shouldReturnRecentActivities() {
        Activity act1 = new Activity(1L, testUserId, ActivityType.SOLVED_QUESTION, "Solved Two Sum", Instant.now());
        Activity act2 = new Activity(2L, testUserId, ActivityType.LOGIN, "User login", Instant.now());

        when(mockActivityService.getRecentActivities(testUserId, 5)).thenReturn(List.of(act1, act2));

        List<Activity> activities = trackService.getRecentActivities(testUserId, 5);

        assertEquals(2, activities.size());
        assertEquals(ActivityType.SOLVED_QUESTION, activities.get(0).getActivityType());
        verify(mockActivityService).getRecentActivities(testUserId, 5);
    }
}
