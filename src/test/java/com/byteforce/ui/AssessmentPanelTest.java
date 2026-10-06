package com.byteforce.ui;

import com.byteforce.domain.Assessment;
import com.byteforce.domain.AssessmentAnswer;
import com.byteforce.domain.AssessmentAttempt;
import com.byteforce.domain.AttemptStatus;
import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;
import com.byteforce.domain.QuestionType;
import com.byteforce.domain.Role;
import com.byteforce.domain.User;
import com.byteforce.service.AssessmentService;
import com.byteforce.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.swing.JRadioButton;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentPanelTest {

    @Mock
    private AssessmentService mockAssessmentService;
    @Mock
    private AuthService mockAuthService;

    private User student;
    private AtomicBoolean backToDashboardInvoked;
    private AssessmentPanel assessmentPanel;

    private Assessment sampleAssessment;
    private Question mcqQuestion;
    private Question conceptualQuestion;

    @BeforeEach
    void setUp() {
        student = User.create("student@byteforce.com", "hash", "Alice Developer", Role.STUDENT);
        backToDashboardInvoked = new AtomicBoolean(false);

        sampleAssessment = new Assessment(
                1L,
                "Java Placement Mock",
                "Core Java, OOP, Collections",
                45,
                100,
                true,
                Difficulty.MEDIUM,
                null,
                Instant.now()
        );

        mcqQuestion = new Question(
                10L, 1L, "Java Polymorphism MCQ", "java-poly-mcq",
                "Which concept is related to late binding?\nA) Method Overloading\nB) Method Overriding\nC) Private Methods\nD) Final Methods",
                Difficulty.MEDIUM, QuestionType.MCQ, "B) Method Overriding",
                Instant.now(), Instant.now()
        );

        conceptualQuestion = new Question(
                20L, 1L, "Explain Immutability", "explain-immutability",
                "Explain the requirements to create an immutable class in Java.",
                Difficulty.HARD, QuestionType.CONCEPTUAL, "Make class final, fields private final, no setters",
                Instant.now(), Instant.now()
        );
    }

    private void initializePanel() {
        assessmentPanel = new AssessmentPanel(
                mockAssessmentService,
                mockAuthService,
                () -> backToDashboardInvoked.set(true)
        );
    }

    private void waitForCondition(java.util.function.BooleanSupplier condition, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
    }

    @Test
    @DisplayName("AssessmentPanel can be constructed with default constructor")
    void shouldConstructWithDefaultConstructor() {
        AssessmentPanel panel = new AssessmentPanel();
        assertNotNull(panel);
        assertEquals(AssessmentPanel.CARD_EMPTY, panel.getCurrentCard());
        assertNotNull(panel.getStartTestButton());
        assertNotNull(panel.getSubmitTestButton());
        assertNotNull(panel.getPrevQuestionButton());
        assertNotNull(panel.getNextQuestionButton());
    }

    @Test
    @DisplayName("Should load available assessments and populate assessment list container")
    void shouldLoadAvailableAssessments() throws Exception {
        when(mockAssessmentService.getAvailableAssessments()).thenReturn(List.of(sampleAssessment));

        initializePanel();

        waitForCondition(() -> !assessmentPanel.getAvailableAssessments().isEmpty(), 2000);

        assertEquals(AssessmentPanel.CARD_LIST, assessmentPanel.getCurrentCard());
        assertEquals(1, assessmentPanel.getAvailableAssessments().size());
        assertEquals("Java Placement Mock", assessmentPanel.getAvailableAssessments().get(0).getTitle());
    }

    @Test
    @DisplayName("Should display empty state when no active assessments are available")
    void shouldDisplayEmptyStateWhenNoAssessmentsAvailable() throws Exception {
        when(mockAssessmentService.getAvailableAssessments()).thenReturn(List.of());

        initializePanel();

        waitForCondition(() -> AssessmentPanel.CARD_EMPTY.equals(assessmentPanel.getCurrentCard()), 2000);

        assertEquals(AssessmentPanel.CARD_EMPTY, assessmentPanel.getCurrentCard());
        assertNotNull(assessmentPanel.getEmptyTitleLabel());
        assertTrue(assessmentPanel.getEmptyTitleLabel().getText().contains("No Assessments Available"));
    }

    @Test
    @DisplayName("Should select assessment and display briefing start card")
    void shouldSelectAssessmentAndDisplayBriefing() throws Exception {
        when(mockAssessmentService.getAvailableAssessments()).thenReturn(List.of(sampleAssessment));
        when(mockAssessmentService.getQuestionsForAssessment(1L)).thenReturn(List.of(mcqQuestion, conceptualQuestion));

        initializePanel();

        waitForCondition(() -> !assessmentPanel.getAvailableAssessments().isEmpty(), 2000);

        assessmentPanel.selectAssessmentForBriefing(sampleAssessment);

        assertEquals(AssessmentPanel.CARD_START, assessmentPanel.getCurrentCard());
        assertEquals("Java Placement Mock", assessmentPanel.getStartTitleLabel().getText());
        assertTrue(assessmentPanel.getStartDurationLabel().getText().contains("45 mins"));
        assertTrue(assessmentPanel.getStartTotalMarksLabel().getText().contains("100 Marks"));

        waitForCondition(() -> assessmentPanel.getStartQuestionsCountLabel().getText().contains("2"), 2000);
        assertTrue(assessmentPanel.getStartQuestionsCountLabel().getText().contains("2"));
    }

    @Test
    @DisplayName("Should start test execution, show question, and initialize timer countdown")
    void shouldStartTestExecutionAndInitializeTimer() throws Exception {
        when(mockAssessmentService.getAvailableAssessments()).thenReturn(List.of(sampleAssessment));
        when(mockAssessmentService.getQuestionsForAssessment(1L)).thenReturn(List.of(mcqQuestion));
        when(mockAssessmentService.getQuestionMarks(1L)).thenReturn(Map.of(10L, 50));
        when(mockAuthService.getCurrentUser()).thenReturn(Optional.of(student));

        AssessmentAttempt attempt = AssessmentAttempt.start(1L, student.getId()).withId(99L);
        when(mockAssessmentService.startAssessment(eq(student.getId()), eq(1L))).thenReturn(attempt);

        initializePanel();
        assessmentPanel.selectAssessmentForBriefing(sampleAssessment);

        assessmentPanel.beginTestExecution();

        waitForCondition(() -> AssessmentPanel.CARD_TEST.equals(assessmentPanel.getCurrentCard()), 2000);

        assertEquals(AssessmentPanel.CARD_TEST, assessmentPanel.getCurrentCard());
        assertNotNull(assessmentPanel.getActiveAttempt());
        assertEquals(99L, assessmentPanel.getActiveAttempt().getId());

        // Verify timer is running and initialized
        assertNotNull(assessmentPanel.getCountdownTimer());
        assertTrue(assessmentPanel.getCountdownTimer().isRunning());
        assertTrue(assessmentPanel.getTimerLabel().getText().contains("Time Remaining"));

        // Verify MCQ question rendering
        assertEquals(4, assessmentPanel.getTestMcqRadios().size());
        assertEquals("A) Method Overloading", assessmentPanel.getTestMcqRadios().get(0).getText());
        assertEquals("B) Method Overriding", assessmentPanel.getTestMcqRadios().get(1).getText());
    }

    @Test
    @DisplayName("Should preserve entered answers when navigating between questions")
    void shouldPreserveAnswersWhenNavigatingBetweenQuestions() throws Exception {
        when(mockAssessmentService.getAvailableAssessments()).thenReturn(List.of(sampleAssessment));
        when(mockAssessmentService.getQuestionsForAssessment(1L)).thenReturn(List.of(mcqQuestion, conceptualQuestion));
        when(mockAssessmentService.getQuestionMarks(1L)).thenReturn(Map.of(10L, 50, 20L, 50));
        when(mockAuthService.getCurrentUser()).thenReturn(Optional.of(student));

        AssessmentAttempt attempt = AssessmentAttempt.start(1L, student.getId()).withId(99L);
        when(mockAssessmentService.startAssessment(eq(student.getId()), eq(1L))).thenReturn(attempt);

        initializePanel();
        assessmentPanel.selectAssessmentForBriefing(sampleAssessment);
        assessmentPanel.beginTestExecution();

        waitForCondition(() -> AssessmentPanel.CARD_TEST.equals(assessmentPanel.getCurrentCard()), 2000);

        // Q1: Select radio option B
        JRadioButton radioB = assessmentPanel.getTestMcqRadios().get(1);
        radioB.setSelected(true);

        // Navigate Next to Q2
        assessmentPanel.navigateQuestion(1);
        assertEquals(1, assessmentPanel.getCurrentQuestionIndex());

        // Q2 is Conceptual: enter text
        assertNotNull(assessmentPanel.getTestCodeOrTextArea());
        assessmentPanel.getTestCodeOrTextArea().setText("Custom immutability rules");

        // Navigate Prev back to Q1
        assessmentPanel.navigateQuestion(-1);
        assertEquals(0, assessmentPanel.getCurrentQuestionIndex());

        // Verify Q1 still has radio B selected
        assertTrue(assessmentPanel.getTestMcqRadios().get(1).isSelected(), "Option B must remain selected after navigating back");

        // Navigate Next back to Q2
        assessmentPanel.navigateQuestion(1);
        assertEquals(1, assessmentPanel.getCurrentQuestionIndex());

        // Verify Q2 still has entered text
        assertEquals("Custom immutability rules", assessmentPanel.getTestCodeOrTextArea().getText(), "Conceptual answer must be preserved");
    }

    @Test
    @DisplayName("Should submit assessment, calculate score, and render result screen")
    void shouldSubmitAssessmentAndRenderResultScreen() throws Exception {
        when(mockAssessmentService.getAvailableAssessments()).thenReturn(List.of(sampleAssessment));
        when(mockAssessmentService.getQuestionsForAssessment(1L)).thenReturn(List.of(mcqQuestion, conceptualQuestion));
        when(mockAssessmentService.getQuestionMarks(1L)).thenReturn(Map.of(10L, 50, 20L, 50));
        when(mockAuthService.getCurrentUser()).thenReturn(Optional.of(student));

        AssessmentAttempt attempt = AssessmentAttempt.start(1L, student.getId()).withId(99L);
        when(mockAssessmentService.startAssessment(eq(student.getId()), eq(1L))).thenReturn(attempt);

        AssessmentAttempt completedAttempt = attempt.complete(100);
        when(mockAssessmentService.submitAssessment(eq(99L), any())).thenReturn(completedAttempt);

        List<AssessmentAnswer> answers = List.of(
                new AssessmentAnswer(1L, 99L, 10L, "B) Method Overriding", 50, AttemptStatus.SOLVED, Instant.now()),
                new AssessmentAnswer(2L, 99L, 20L, "Make class final", 50, AttemptStatus.SOLVED, Instant.now())
        );
        when(mockAssessmentService.getAnswersForAttempt(99L)).thenReturn(answers);

        initializePanel();
        assessmentPanel.selectAssessmentForBriefing(sampleAssessment);
        assessmentPanel.beginTestExecution();

        waitForCondition(() -> AssessmentPanel.CARD_TEST.equals(assessmentPanel.getCurrentCard()), 2000);

        // Submit assessment
        assessmentPanel.submitAssessmentAttempt(false);

        waitForCondition(() -> AssessmentPanel.CARD_RESULT.equals(assessmentPanel.getCurrentCard()), 2000);

        assertEquals(AssessmentPanel.CARD_RESULT, assessmentPanel.getCurrentCard());
        assertTrue(assessmentPanel.getResultStatusBannerLabel().getText().contains("Completed Successfully"));
        assertTrue(assessmentPanel.getResultScoreLabel().getText().contains("100 / 100 Marks (100.0%)"));
        assertTrue(assessmentPanel.getResultMetricsLabel().getText().contains("Correct: 2"));
    }

    @Test
    @DisplayName("Should auto-submit assessment when timer timeout occurs")
    void shouldAutoSubmitOnTimeout() throws Exception {
        when(mockAssessmentService.getAvailableAssessments()).thenReturn(List.of(sampleAssessment));
        when(mockAssessmentService.getQuestionsForAssessment(1L)).thenReturn(List.of(mcqQuestion));
        when(mockAssessmentService.getQuestionMarks(1L)).thenReturn(Map.of(10L, 100));
        when(mockAuthService.getCurrentUser()).thenReturn(Optional.of(student));

        AssessmentAttempt attempt = AssessmentAttempt.start(1L, student.getId()).withId(99L);
        when(mockAssessmentService.startAssessment(eq(student.getId()), eq(1L))).thenReturn(attempt);

        AssessmentAttempt timeoutAttempt = attempt.complete(0);
        when(mockAssessmentService.submitAssessment(eq(99L), any())).thenReturn(timeoutAttempt);

        List<AssessmentAnswer> answers = List.of(
                new AssessmentAnswer(1L, 99L, 10L, null, 0, AttemptStatus.SKIPPED, null)
        );
        when(mockAssessmentService.getAnswersForAttempt(99L)).thenReturn(answers);

        initializePanel();
        assessmentPanel.selectAssessmentForBriefing(sampleAssessment);
        assessmentPanel.beginTestExecution();

        waitForCondition(() -> AssessmentPanel.CARD_TEST.equals(assessmentPanel.getCurrentCard()), 2000);

        // Trigger timeout submission
        assessmentPanel.submitAssessmentAttempt(true);

        waitForCondition(() -> AssessmentPanel.CARD_RESULT.equals(assessmentPanel.getCurrentCard()), 2000);

        assertEquals(AssessmentPanel.CARD_RESULT, assessmentPanel.getCurrentCard());
        assertTrue(assessmentPanel.getResultStatusBannerLabel().getText().contains("Time Expired"));
        assertTrue(assessmentPanel.getResultMetricsLabel().getText().contains("Skipped: 1"));
    }

    @Test
    @DisplayName("Should open review session and display question solutions")
    void shouldReviewSubmittedAnswers() throws Exception {
        when(mockAssessmentService.getAvailableAssessments()).thenReturn(List.of(sampleAssessment));
        when(mockAssessmentService.getQuestionsForAssessment(1L)).thenReturn(List.of(mcqQuestion));
        when(mockAssessmentService.getQuestionMarks(1L)).thenReturn(Map.of(10L, 50));
        when(mockAuthService.getCurrentUser()).thenReturn(Optional.of(student));

        AssessmentAttempt attempt = AssessmentAttempt.start(1L, student.getId()).withId(99L);
        when(mockAssessmentService.startAssessment(eq(student.getId()), eq(1L))).thenReturn(attempt);
        when(mockAssessmentService.submitAssessment(eq(99L), any())).thenReturn(attempt.complete(50));

        List<AssessmentAnswer> answers = List.of(
                new AssessmentAnswer(1L, 99L, 10L, "B) Method Overriding", 50, AttemptStatus.SOLVED, Instant.now())
        );
        when(mockAssessmentService.getAnswersForAttempt(99L)).thenReturn(answers);

        initializePanel();
        assessmentPanel.selectAssessmentForBriefing(sampleAssessment);
        assessmentPanel.beginTestExecution();

        waitForCondition(() -> AssessmentPanel.CARD_TEST.equals(assessmentPanel.getCurrentCard()), 2000);

        assessmentPanel.submitAssessmentAttempt(false);

        waitForCondition(() -> AssessmentPanel.CARD_RESULT.equals(assessmentPanel.getCurrentCard()), 2000);

        // Start review session
        assessmentPanel.startReviewSession();

        assertEquals(AssessmentPanel.CARD_REVIEW, assessmentPanel.getCurrentCard());
        assertEquals("Java Polymorphism MCQ", assessmentPanel.getReviewQuestionTitleLabel().getText());
        assertEquals("B) Method Overriding", assessmentPanel.getReviewSubmittedAnswerArea().getText());
        assertEquals("B) Method Overriding", assessmentPanel.getReviewSolutionArea().getText());
        assertTrue(assessmentPanel.getReviewVerdictBadge().getText().contains("CORRECT"));
    }

    @Test
    @DisplayName("Reset to home should reset assessment panel state and invoke dashboard callback on request")
    void shouldResetToHomeAndTriggerDashboardCallback() throws Exception {
        when(mockAssessmentService.getAvailableAssessments()).thenReturn(List.of(sampleAssessment));

        initializePanel();

        waitForCondition(() -> !assessmentPanel.getAvailableAssessments().isEmpty(), 2000);

        assessmentPanel.resetToHome();
        assertEquals(AssessmentPanel.CARD_LIST, assessmentPanel.getCurrentCard());

        assessmentPanel.getBackToDashboardFromResultButton().doClick();
        assertTrue(backToDashboardInvoked.get(), "Back to dashboard callback must be invoked");
    }
}
