package com.byteforce.ui;

import com.byteforce.domain.Concept;
import com.byteforce.domain.LearningResource;
import com.byteforce.domain.ResourceType;
import com.byteforce.domain.Subject;
import com.byteforce.domain.Topic;
import com.byteforce.service.LearnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearnPanelTest {

    @Mock
    private LearnService mockLearnService;

    private AtomicBoolean backToDashboardInvoked;
    private LearnPanel learnPanel;

    private Subject sampleSubject;
    private Topic sampleTopic;
    private Concept sampleConcept;

    @BeforeEach
    void setUp() {
        backToDashboardInvoked = new AtomicBoolean(false);

        sampleTopic = new Topic(101L, "Arrays", "arrays", "Continuous memory elements", 1, Instant.now());
        sampleSubject = Subject.create("data-structures", "Data Structures", "Core CS memory structures", 1, List.of(sampleTopic));

        LearningResource resource = LearningResource.create("Array Resource", ResourceType.ARTICLE, "https://example.com/array", "Article notes");
        sampleConcept = Concept.create(
                1001L,
                101L,
                "Arrays",
                "Array Traversal",
                "Iterating through contiguous memory",
                List.of("O(1) index access", "Cache-friendly spatial locality"),
                "for (int x : arr) {}",
                List.of(resource)
        );
    }

    private void initializePanel() {
        learnPanel = new LearnPanel(mockLearnService, () -> backToDashboardInvoked.set(true));
    }

    private void waitForCondition(java.util.function.BooleanSupplier condition, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
    }

    @Test
    @DisplayName("LearnPanel can be constructed with default constructor and defaults to empty card")
    void shouldConstructWithDefaultConstructor() {
        LearnPanel panel = new LearnPanel();
        assertNotNull(panel);
        assertEquals(LearnPanel.CARD_EMPTY, panel.getCurrentCard());
        assertNotNull(panel.getBackToSubjectsFromEmptyButton());
    }

    @Test
    @DisplayName("Should load available subjects and render subject cards")
    void shouldLoadAvailableSubjects() throws Exception {
        when(mockLearnService.getAllSubjects()).thenReturn(List.of(sampleSubject));

        initializePanel();

        waitForCondition(() -> !learnPanel.getAvailableSubjects().isEmpty(), 2000);

        assertEquals(LearnPanel.CARD_SUBJECTS, learnPanel.getCurrentCard());
        assertEquals(1, learnPanel.getAvailableSubjects().size());
        assertEquals("Data Structures", learnPanel.getAvailableSubjects().get(0).getName());
        assertTrue(learnPanel.getSubjectsStatusLabel().getText().contains("1 core CS placement subject"));
    }

    @Test
    @DisplayName("Should show empty state when no subjects are available")
    void shouldShowEmptyStateWhenNoSubjectsAvailable() throws Exception {
        when(mockLearnService.getAllSubjects()).thenReturn(List.of());

        initializePanel();

        waitForCondition(() -> LearnPanel.CARD_EMPTY.equals(learnPanel.getCurrentCard()), 2000);

        assertEquals(LearnPanel.CARD_EMPTY, learnPanel.getCurrentCard());
        assertTrue(learnPanel.getEmptyTitleLabel().getText().contains("No Content Available"));
    }

    @Test
    @DisplayName("Selecting a topic should transition to Concepts view and load topic concepts")
    void shouldSelectTopicAndLoadConcepts() throws Exception {
        when(mockLearnService.getAllSubjects()).thenReturn(List.of(sampleSubject));
        when(mockLearnService.getConceptsForTopic(101L)).thenReturn(List.of(sampleConcept));

        initializePanel();

        waitForCondition(() -> !learnPanel.getAvailableSubjects().isEmpty(), 2000);

        learnPanel.selectTopic(sampleSubject, sampleTopic);

        assertEquals(LearnPanel.CARD_CONCEPTS, learnPanel.getCurrentCard());
        assertEquals("Arrays", learnPanel.getConceptsTopicTitleLabel().getText());
        assertTrue(learnPanel.getConceptsBreadcrumbLabel().getText().contains("Data Structures > Arrays"));

        waitForCondition(() -> !learnPanel.getActiveTopicConcepts().isEmpty(), 2000);

        assertEquals(1, learnPanel.getActiveTopicConcepts().size());
        assertEquals("Array Traversal", learnPanel.getActiveTopicConcepts().get(0).getTitle());
    }

    @Test
    @DisplayName("Selecting a concept should transition to Detail view and render explanation, key points, example, and resources")
    void shouldSelectConceptAndRenderDetail() throws Exception {
        lenient().when(mockLearnService.getAllSubjects()).thenReturn(List.of(sampleSubject));
        initializePanel();

        learnPanel.selectConceptForDetail(sampleConcept);

        assertEquals(LearnPanel.CARD_DETAIL, learnPanel.getCurrentCard());
        assertEquals("Array Traversal", learnPanel.getDetailConceptTitleLabel().getText());
        assertEquals("Iterating through contiguous memory", learnPanel.getDetailExplanationArea().getText());
        assertEquals("for (int x : arr) {}", learnPanel.getDetailExampleArea().getText());
    }

    @Test
    @DisplayName("Back navigation buttons should navigate back through Detail -> Concepts -> Subjects")
    void shouldNavigateBackwards() throws Exception {
        when(mockLearnService.getAllSubjects()).thenReturn(List.of(sampleSubject));
        when(mockLearnService.getConceptsForTopic(101L)).thenReturn(List.of(sampleConcept));

        initializePanel();
        learnPanel.selectTopic(sampleSubject, sampleTopic);
        learnPanel.selectConceptForDetail(sampleConcept);

        assertEquals(LearnPanel.CARD_DETAIL, learnPanel.getCurrentCard());

        // Detail -> Concepts
        learnPanel.getBackToConceptsFromDetailButton().doClick();
        assertEquals(LearnPanel.CARD_CONCEPTS, learnPanel.getCurrentCard());

        // Concepts -> Subjects
        learnPanel.getBackToSubjectsFromConceptsButton().doClick();
        assertEquals(LearnPanel.CARD_SUBJECTS, learnPanel.getCurrentCard());
    }

    @Test
    @DisplayName("Search field should filter concepts across topics")
    void shouldFilterConceptsViaSearch() throws Exception {
        when(mockLearnService.getAllSubjects()).thenReturn(List.of(sampleSubject));
        when(mockLearnService.searchConcepts("traversal")).thenReturn(List.of(sampleConcept));

        initializePanel();
        waitForCondition(() -> !learnPanel.getAvailableSubjects().isEmpty(), 2000);

        learnPanel.getSubjectsSearchField().setText("traversal");

        waitForCondition(() -> learnPanel.getSubjectsStatusLabel().getText().contains("Found 1 concept"), 2000);
        assertTrue(learnPanel.getSubjectsStatusLabel().getText().contains("Found 1 concept"));
    }

    @Test
    @DisplayName("Reset to home should clear search and reload subjects")
    void shouldResetToHome() throws Exception {
        lenient().when(mockLearnService.getAllSubjects()).thenReturn(List.of(sampleSubject));
        initializePanel();

        learnPanel.selectConceptForDetail(sampleConcept);
        assertEquals(LearnPanel.CARD_DETAIL, learnPanel.getCurrentCard());

        learnPanel.resetToHome();
        assertEquals(LearnPanel.CARD_SUBJECTS, learnPanel.getCurrentCard());
    }

    @Test
    @DisplayName("openResourceExternal should safely execute without crashing in any desktop environment")
    void shouldSafelyHandleExternalResourceUrl() {
        org.junit.jupiter.api.Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(), "Skipping desktop browse test in headless environment");
        LearnPanel panel = new LearnPanel();
        // Should execute cleanly without throwing exceptions
        panel.openResourceExternal("https://example.com/learn");
    }
}
