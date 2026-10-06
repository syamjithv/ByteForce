package com.byteforce.ui;

import com.byteforce.domain.AttemptStatus;
import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;
import com.byteforce.domain.QuestionType;
import com.byteforce.domain.Role;
import com.byteforce.domain.Topic;
import com.byteforce.domain.User;
import com.byteforce.service.AttemptService;
import com.byteforce.service.AuthService;
import com.byteforce.service.BookmarkService;
import com.byteforce.service.QuestionService;
import com.byteforce.service.TopicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.swing.JRadioButton;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PracticePanelTest {

    @Mock
    private TopicService mockTopicService;
    @Mock
    private QuestionService mockQuestionService;
    @Mock
    private AttemptService mockAttemptService;
    @Mock
    private BookmarkService mockBookmarkService;
    @Mock
    private AuthService mockAuthService;

    private User student;
    private AtomicBoolean backToDashboardInvoked;
    private PracticePanel practicePanel;

    @BeforeEach
    void setUp() {
        student = User.create("student@byteforce.com", "hash", "Alice Developer", Role.STUDENT);
        backToDashboardInvoked = new AtomicBoolean(false);
    }

    private void initializePanel() {
        practicePanel = new PracticePanel(
                mockTopicService,
                mockQuestionService,
                mockAttemptService,
                mockBookmarkService,
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
    @DisplayName("PracticePanel can be constructed with default constructor")
    void shouldConstructWithDefaultConstructor() {
        PracticePanel panel = new PracticePanel();
        assertNotNull(panel);
        assertEquals(PracticePanel.CARD_FILTERS, panel.getCurrentCard());
        assertNotNull(panel.getTopicComboBox());
        assertNotNull(panel.getDifficultyComboBox());
        assertNotNull(panel.getStartPracticeButton());
        assertNotNull(panel.getSubmitButton());
        assertNotNull(panel.getSkipButton());
    }

    @Test
    @DisplayName("Should populate topic dropdown from TopicService")
    void shouldPopulateTopicsFromTopicService() throws Exception {
        Topic algoTopic = new Topic(1L, "Algorithms", "algorithms", "Algo desc", 1, Instant.now());
        when(mockTopicService.getAllTopics()).thenReturn(List.of(algoTopic));

        initializePanel();

        waitForCondition(() -> practicePanel.getTopicComboBox().getItemCount() >= 2, 2000);

        assertEquals(2, practicePanel.getTopicComboBox().getItemCount());
        assertEquals("All Topics", practicePanel.getTopicComboBox().getItemAt(0).getName());
        assertEquals("Algorithms", practicePanel.getTopicComboBox().getItemAt(1).getName());
    }

    @Test
    @DisplayName("Should display empty state when no questions match the filters")
    void shouldShowEmptyStateWhenNoQuestionsMatch() throws Exception {
        when(mockTopicService.getAllTopics()).thenReturn(List.of());
        when(mockQuestionService.getAllQuestions()).thenReturn(List.of());

        initializePanel();
        practicePanel.startPracticeSession();

        waitForCondition(() -> PracticePanel.CARD_EMPTY.equals(practicePanel.getCurrentCard()), 2000);

        assertEquals(PracticePanel.CARD_EMPTY, practicePanel.getCurrentCard());
    }

    @Test
    @DisplayName("Should load questions and render question screen with parsed MCQ options")
    void shouldLoadQuestionsAndRenderMcqOptions() throws Exception {
        when(mockTopicService.getAllTopics()).thenReturn(List.of());
        Question mcqQuestion = new Question(
                10L, 1L, "QuickSort Complexity", "quicksort-complexity",
                "What is the average time complexity of QuickSort?\nA) O(n)\nB) O(n log n)\nC) O(n^2)\nD) O(1)",
                Difficulty.MEDIUM, QuestionType.MCQ, "B) O(n log n)", Instant.now(), Instant.now()
        );
        when(mockQuestionService.getAllQuestions()).thenReturn(List.of(mcqQuestion));

        initializePanel();
        practicePanel.startPracticeSession();

        waitForCondition(() -> PracticePanel.CARD_QUESTION.equals(practicePanel.getCurrentCard()), 2000);

        assertEquals(PracticePanel.CARD_QUESTION, practicePanel.getCurrentCard());
        assertEquals("QuickSort Complexity", practicePanel.getQuestionTitleLabel().getText());
        assertEquals(4, practicePanel.getMcqRadioButtons().size());
        assertEquals("A) O(n)", practicePanel.getMcqRadioButtons().get(0).getText());
        assertEquals("B) O(n log n)", practicePanel.getMcqRadioButtons().get(1).getText());
    }

    @Test
    @DisplayName("Should evaluate correct MCQ answer, record SOLVED attempt, and display result")
    void shouldEvaluateCorrectMcqAnswerAndRecordSolved() throws Exception {
        when(mockTopicService.getAllTopics()).thenReturn(List.of());
        when(mockAuthService.getCurrentUser()).thenReturn(Optional.of(student));

        Question mcqQuestion = new Question(
                10L, 1L, "QuickSort Complexity", "quicksort-complexity",
                "What is the average time complexity of QuickSort?\nA) O(n)\nB) O(n log n)\nC) O(n^2)\nD) O(1)",
                Difficulty.MEDIUM, QuestionType.MCQ, "B) O(n log n)", Instant.now(), Instant.now()
        );
        when(mockQuestionService.getAllQuestions()).thenReturn(List.of(mcqQuestion));

        initializePanel();
        practicePanel.startPracticeSession();

        waitForCondition(() -> PracticePanel.CARD_QUESTION.equals(practicePanel.getCurrentCard()), 2000);

        // Select Option B (correct)
        JRadioButton optionB = practicePanel.getMcqRadioButtons().get(1);
        optionB.setSelected(true);

        practicePanel.performSubmit(false);

        waitForCondition(() -> PracticePanel.CARD_RESULT.equals(practicePanel.getCurrentCard()), 2000);

        verify(mockAttemptService).recordAttempt(eq(student.getId()), eq(10L), eq(AttemptStatus.SOLVED), eq("B) O(n log n)"), any());
        assertTrue(practicePanel.getResultBannerLabel().getText().contains("Correct"));
    }

    @Test
    @DisplayName("Should evaluate incorrect MCQ answer, record FAILED attempt, and display result")
    void shouldEvaluateIncorrectMcqAnswerAndRecordFailed() throws Exception {
        when(mockTopicService.getAllTopics()).thenReturn(List.of());
        when(mockAuthService.getCurrentUser()).thenReturn(Optional.of(student));

        Question mcqQuestion = new Question(
                10L, 1L, "QuickSort Complexity", "quicksort-complexity",
                "What is the average time complexity of QuickSort?\nA) O(n)\nB) O(n log n)\nC) O(n^2)\nD) O(1)",
                Difficulty.MEDIUM, QuestionType.MCQ, "B) O(n log n)", Instant.now(), Instant.now()
        );
        when(mockQuestionService.getAllQuestions()).thenReturn(List.of(mcqQuestion));

        initializePanel();
        practicePanel.startPracticeSession();

        waitForCondition(() -> PracticePanel.CARD_QUESTION.equals(practicePanel.getCurrentCard()), 2000);

        // Select Option A (incorrect)
        JRadioButton optionA = practicePanel.getMcqRadioButtons().get(0);
        optionA.setSelected(true);

        practicePanel.performSubmit(false);

        waitForCondition(() -> PracticePanel.CARD_RESULT.equals(practicePanel.getCurrentCard()), 2000);

        verify(mockAttemptService).recordAttempt(eq(student.getId()), eq(10L), eq(AttemptStatus.FAILED), eq("A) O(n)"), any());
        assertTrue(practicePanel.getResultBannerLabel().getText().contains("Incorrect"));
    }

    @Test
    @DisplayName("Should record SKIPPED attempt when user skips question")
    void shouldRecordSkippedAttempt() throws Exception {
        when(mockTopicService.getAllTopics()).thenReturn(List.of());
        when(mockAuthService.getCurrentUser()).thenReturn(Optional.of(student));

        Question mcqQuestion = new Question(
                10L, 1L, "QuickSort Complexity", "quicksort-complexity",
                "Prompt", Difficulty.EASY, QuestionType.CONCEPTUAL, "Answer", Instant.now(), Instant.now()
        );
        when(mockQuestionService.getAllQuestions()).thenReturn(List.of(mcqQuestion));

        initializePanel();
        practicePanel.startPracticeSession();

        waitForCondition(() -> PracticePanel.CARD_QUESTION.equals(practicePanel.getCurrentCard()), 2000);

        practicePanel.performSubmit(true);

        waitForCondition(() -> PracticePanel.CARD_RESULT.equals(practicePanel.getCurrentCard()), 2000);

        verify(mockAttemptService).recordAttempt(eq(student.getId()), eq(10L), eq(AttemptStatus.SKIPPED), eq(""), any());
        assertTrue(practicePanel.getResultBannerLabel().getText().contains("Skipped"));
    }

    @Test
    @DisplayName("Should submit conceptual answer and record attempt")
    void shouldSubmitConceptualAnswer() throws Exception {
        when(mockTopicService.getAllTopics()).thenReturn(List.of());
        when(mockAuthService.getCurrentUser()).thenReturn(Optional.of(student));

        Question conceptualQ = new Question(
                20L, 1L, "Polymorphism", "polymorphism",
                "Explain polymorphism in Java.", Difficulty.EASY, QuestionType.CONCEPTUAL, "Method overriding and overloading",
                Instant.now(), Instant.now()
        );
        when(mockQuestionService.getAllQuestions()).thenReturn(List.of(conceptualQ));

        initializePanel();
        practicePanel.startPracticeSession();

        waitForCondition(() -> PracticePanel.CARD_QUESTION.equals(practicePanel.getCurrentCard()), 2000);

        assertNotNull(practicePanel.getCodeOrConceptualArea());
        practicePanel.getCodeOrConceptualArea().setText("Ability of objects to take many forms");

        practicePanel.performSubmit(false);

        waitForCondition(() -> PracticePanel.CARD_RESULT.equals(practicePanel.getCurrentCard()), 2000);

        verify(mockAttemptService).recordAttempt(eq(student.getId()), eq(20L), eq(AttemptStatus.ATTEMPTED), eq("Ability of objects to take many forms"), any());
        assertEquals(PracticePanel.CARD_RESULT, practicePanel.getCurrentCard());
    }

    @Test
    @DisplayName("Should toggle bookmark via BookmarkService on bookmark button click")
    void shouldToggleBookmarkViaService() throws Exception {
        when(mockTopicService.getAllTopics()).thenReturn(List.of());
        when(mockAuthService.isAuthenticated()).thenReturn(true);
        when(mockAuthService.getCurrentUser()).thenReturn(Optional.of(student));

        Question q = new Question(
                30L, 1L, "Binary Tree Traversal", "tree-traversal",
                "Traverse tree in-order", Difficulty.HARD, QuestionType.CODING, "def inorder(): ...",
                Instant.now(), Instant.now()
        );
        when(mockQuestionService.getAllQuestions()).thenReturn(List.of(q));
        when(mockBookmarkService.isBookmarked(student.getId(), 30L)).thenReturn(false);
        when(mockBookmarkService.toggleBookmark(eq(student.getId()), eq(30L), anyString())).thenReturn(true);

        initializePanel();
        practicePanel.startPracticeSession();

        waitForCondition(() -> PracticePanel.CARD_QUESTION.equals(practicePanel.getCurrentCard()), 2000);

        practicePanel.getBookmarkButton().doClick();

        waitForCondition(() -> practicePanel.getBookmarkButton().getText().contains("Bookmarked"), 2000);

        verify(mockBookmarkService).toggleBookmark(eq(student.getId()), eq(30L), anyString());
        assertTrue(practicePanel.getBookmarkButton().getText().contains("Bookmarked"));
    }

    @Test
    @DisplayName("Back to Dashboard button should invoke navigation callback")
    void shouldInvokeBackToDashboardCallback() {
        when(mockTopicService.getAllTopics()).thenReturn(List.of());
        initializePanel();

        practicePanel.getBackToDashboardFromFiltersButton().doClick();

        assertTrue(backToDashboardInvoked.get(), "Back to dashboard callback should be invoked");
    }
}
