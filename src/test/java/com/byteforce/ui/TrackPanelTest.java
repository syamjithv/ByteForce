package com.byteforce.ui;

import com.byteforce.domain.Activity;
import com.byteforce.domain.ActivityType;
import com.byteforce.domain.AssessmentAttemptSummary;
import com.byteforce.domain.Role;
import com.byteforce.domain.StudentProgressSummary;
import com.byteforce.domain.TopicPerformance;
import com.byteforce.domain.User;
import com.byteforce.service.AuthService;
import com.byteforce.service.TrackService;
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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackPanelTest {

    @Mock
    private TrackService mockTrackService;

    @Mock
    private AuthService mockAuthService;

    private AtomicBoolean backToDashboardInvoked;
    private TrackPanel trackPanel;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        backToDashboardInvoked = new AtomicBoolean(false);
        sampleUser = User.create("student@byteforce.com", "hash", "Bob Student", Role.STUDENT);
    }

    private void initializePanel() {
        trackPanel = new TrackPanel(mockTrackService, mockAuthService, () -> backToDashboardInvoked.set(true));
    }

    @Test
    @DisplayName("TrackPanel can be constructed with default constructor and renders empty state")
    void shouldConstructWithDefaultConstructor() {
        TrackPanel panel = new TrackPanel();
        assertNotNull(panel);
        assertNotNull(panel.getBackButton());
        assertNotNull(panel.getRefreshButton());
        assertEquals("0", panel.getAttemptedValueLabel().getText());
        assertEquals("0", panel.getSolvedValueLabel().getText());
        assertEquals("0", panel.getFailedValueLabel().getText());
        assertEquals("0", panel.getSkippedValueLabel().getText());
        assertEquals("0.0%", panel.getAccuracyValueLabel().getText());
        assertEquals("0", panel.getAssessmentsValueLabel().getText());
        assertEquals("0", panel.getBookmarksValueLabel().getText());
        assertEquals("Learning progress tracking will appear here as you study concepts.",
                panel.getLearnStatusLabel().getText());
        assertEquals("Complete more practice to identify your weak areas.",
                panel.getWeakAreasEmptyLabel().getText());
        assertEquals("No assessments completed yet.",
                panel.getEmptyAssessmentLabel().getText());
    }

    @Test
    @DisplayName("Should display empty states when student has zero activity")
    void shouldDisplayEmptyStatesWhenZeroActivity() {
        initializePanel();

        TrackPanel.TrackData emptyData = new TrackPanel.TrackData(
                StudentProgressSummary.empty(sampleUser.getId()),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        trackPanel.loadDataDirectly(emptyData);

        assertEquals("0", trackPanel.getAttemptedValueLabel().getText());
        assertEquals("0", trackPanel.getSolvedValueLabel().getText());
        assertEquals("0", trackPanel.getFailedValueLabel().getText());
        assertEquals("0", trackPanel.getSkippedValueLabel().getText());
        assertEquals("0.0%", trackPanel.getAccuracyValueLabel().getText());
        assertEquals("0", trackPanel.getAssessmentsValueLabel().getText());
        assertEquals("0", trackPanel.getBookmarksValueLabel().getText());

        assertTrue(trackPanel.getTopicCardsContainer().isAncestorOf(trackPanel.getEmptyTopicLabel()));
        assertTrue(trackPanel.getWeakAreasContainer().isAncestorOf(trackPanel.getWeakAreasEmptyLabel()));
        assertTrue(trackPanel.getAssessmentHistoryContainer().isAncestorOf(trackPanel.getEmptyAssessmentLabel()));
        assertTrue(trackPanel.getActivityHistoryContainer().isAncestorOf(trackPanel.getEmptyActivityLabel()));
    }

    @Test
    @DisplayName("Should render real student progress and topic breakdown accurately")
    void shouldRenderStudentProgressAndTopicBreakdown() {
        initializePanel();

        StudentProgressSummary summary = StudentProgressSummary.calculate(
                sampleUser.getId(), 10, 7, 2, 1, 2, 4);

        TopicPerformance arrays = TopicPerformance.calculate(1L, "Arrays", 6, 5, 1, 0);
        TopicPerformance sql = TopicPerformance.calculate(2L, "SQL", 4, 2, 1, 1);

        TopicPerformance weakTopic = TopicPerformance.calculate(2L, "SQL", 4, 2, 1, 1);

        AssessmentAttemptSummary assSummary = AssessmentAttemptSummary.of(
                1L, 101L, "Database Systems Test", 18, 20, "COMPLETED", Instant.now());

        Activity act = new Activity(1L, sampleUser.getId(), ActivityType.SOLVED_QUESTION, "Solved Two Sum", Instant.now());

        TrackPanel.TrackData fullData = new TrackPanel.TrackData(
                summary,
                List.of(arrays, sql),
                List.of(weakTopic),
                List.of(assSummary),
                List.of(act)
        );

        trackPanel.loadDataDirectly(fullData);

        assertEquals("10", trackPanel.getAttemptedValueLabel().getText());
        assertEquals("7", trackPanel.getSolvedValueLabel().getText());
        assertEquals("2", trackPanel.getFailedValueLabel().getText());
        assertEquals("1", trackPanel.getSkippedValueLabel().getText());
        assertEquals("77.8%", trackPanel.getAccuracyValueLabel().getText());
        assertEquals("2", trackPanel.getAssessmentsValueLabel().getText());
        assertEquals("4", trackPanel.getBookmarksValueLabel().getText());

        // Topics rendered (2 rows)
        assertEquals(2, trackPanel.getTopicCardsContainer().getComponentCount());

        // Weak areas rendered (card + spacing)
        assertTrue(trackPanel.getWeakAreasContainer().getComponentCount() >= 1);

        // Assessment history rendered (1 row)
        assertEquals(1, trackPanel.getAssessmentHistoryContainer().getComponentCount());

        // Activity history rendered (1 row)
        assertEquals(1, trackPanel.getActivityHistoryContainer().getComponentCount());
    }

    @Test
    @DisplayName("Back button should trigger navigation callback")
    void shouldTriggerBackToDashboardOnButtonClick() {
        initializePanel();
        trackPanel.getBackButton().doClick();
        assertTrue(backToDashboardInvoked.get(), "Back callback should be triggered");
    }

    @Test
    @DisplayName("Should fetch and refresh tracking data synchronously via refreshSync")
    void shouldRefreshDataSynchronously() {
        when(mockAuthService.isAuthenticated()).thenReturn(true);
        when(mockAuthService.getCurrentUser()).thenReturn(Optional.of(sampleUser));

        StudentProgressSummary summary = StudentProgressSummary.calculate(
                sampleUser.getId(), 5, 4, 1, 0, 1, 2);
        when(mockTrackService.getOverallProgress(sampleUser.getId())).thenReturn(summary);
        when(mockTrackService.getTopicPerformances(sampleUser.getId())).thenReturn(List.of());
        when(mockTrackService.getWeakAreas(sampleUser.getId())).thenReturn(List.of());
        when(mockTrackService.getAssessmentHistory(sampleUser.getId())).thenReturn(List.of());
        when(mockTrackService.getRecentActivities(sampleUser.getId(), 10)).thenReturn(List.of());

        initializePanel();
        trackPanel.refreshSync();

        assertEquals("5", trackPanel.getAttemptedValueLabel().getText());
        assertEquals("4", trackPanel.getSolvedValueLabel().getText());
        assertEquals("1", trackPanel.getFailedValueLabel().getText());
        assertEquals("0", trackPanel.getSkippedValueLabel().getText());
        assertEquals("80.0%", trackPanel.getAccuracyValueLabel().getText());
        assertEquals("1", trackPanel.getAssessmentsValueLabel().getText());
        assertEquals("2", trackPanel.getBookmarksValueLabel().getText());
    }
}
