package com.byteforce.ui;

import com.byteforce.domain.Assessment;
import com.byteforce.domain.AssessmentAnswer;
import com.byteforce.domain.AssessmentAttempt;
import com.byteforce.domain.AttemptStatus;
import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;
import com.byteforce.domain.QuestionType;
import com.byteforce.domain.User;
import com.byteforce.service.AssessmentService;
import com.byteforce.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Assessment / Mock Test module view for ByteForce placement readiness platform.
 * Supports selecting assessments, reviewing briefings, timed test taking with countdown,
 * multi-question navigation with answer preservation, score calculation, result display,
 * and question-by-question answer review.
 *
 * Communicates strictly through AssessmentService and AuthService without direct JDBC access.
 */
public class AssessmentPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(AssessmentPanel.class);

    // Card identifiers
    public static final String CARD_LIST = "ASSESSMENT_LIST";
    public static final String CARD_START = "ASSESSMENT_START";
    public static final String CARD_TEST = "ASSESSMENT_TEST";
    public static final String CARD_RESULT = "ASSESSMENT_RESULT";
    public static final String CARD_REVIEW = "ASSESSMENT_REVIEW";
    public static final String CARD_EMPTY = "ASSESSMENT_EMPTY";

    // Palette matching ByteForce design system
    private static final Color BG_PAGE = new Color(248, 250, 252);          // Slate-50
    private static final Color BG_HEADER = new Color(15, 23, 42);           // Slate-900
    private static final Color BG_CARD = Color.WHITE;
    private static final Color BORDER_CARD = new Color(226, 232, 240);      // Slate-200
    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);      // Blue-600
    private static final Color COLOR_PRIMARY_HOVER = new Color(29, 78, 216);// Blue-700
    private static final Color COLOR_TEXT_MAIN = new Color(15, 23, 42);     // Slate-900
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139); // Slate-500
    private static final Color COLOR_SUCCESS = new Color(22, 163, 74);      // Green-600
    private static final Color COLOR_SUCCESS_BG = new Color(240, 253, 244); // Green-50
    private static final Color COLOR_DANGER = new Color(220, 38, 38);        // Red-600
    private static final Color COLOR_DANGER_BG = new Color(254, 242, 242);    // Red-50
    private static final Color COLOR_WARNING = new Color(202, 138, 4);      // Yellow-600
    private static final Color COLOR_WARNING_BG = new Color(254, 252, 232); // Yellow-50
    private static final Color COLOR_INPUT_BORDER = new Color(203, 213, 225);// Slate-300
    private static final Color COLOR_BADGE_BG = new Color(241, 245, 249);   // Slate-100

    private final AssessmentService assessmentService;
    private final AuthService authService;
    private final Runnable onBackToDashboard;

    private final CardLayout cardLayout;
    private final JPanel cardsContainer;
    private String currentCard;

    // Active state
    private final List<Assessment> availableAssessments = new ArrayList<>();
    private Assessment selectedAssessment;
    private AssessmentAttempt activeAttempt;
    private final List<Question> currentTestQuestions = new ArrayList<>();
    private final Map<Long, Integer> currentQuestionMarks = new HashMap<>();
    private final Map<Long, String> inProgressAnswers = new HashMap<>();
    private int currentQuestionIndex = 0;

    // Review state
    private List<AssessmentAnswer> completedAnswers = new ArrayList<>();
    private int currentReviewIndex = 0;

    // Timer
    private Timer countdownTimer;
    private int remainingSeconds = 0;

    // --- Components: LIST Screen ---
    private JPanel assessmentListContainer;
    private JLabel listStatusLabel;

    // --- Components: START Screen ---
    private JLabel startTitleLabel;
    private JLabel startDescriptionLabel;
    private JLabel startDurationLabel;
    private JLabel startTotalMarksLabel;
    private JLabel startQuestionsCountLabel;
    private JButton startTestButton;

    // --- Components: TEST Screen ---
    private JLabel testHeaderTitleLabel;
    private JLabel timerLabel;
    private JLabel testProgressLabel;
    private JLabel testDifficultyBadge;
    private JLabel testTypeBadge;
    private JLabel testQuestionTitleLabel;
    private JTextArea testQuestionDescriptionArea;
    private JPanel testAnswerContainer;
    private ButtonGroup testMcqGroup;
    private final List<JRadioButton> testMcqRadios = new ArrayList<>();
    private JTextArea testCodeOrTextArea;
    private JButton prevQuestionButton;
    private JButton nextQuestionButton;
    private JButton submitTestButton;
    private JLabel testStatusLabel;

    // --- Components: RESULT Screen ---
    private JLabel resultStatusBannerLabel;
    private JLabel resultAssessmentTitleLabel;
    private JLabel resultScoreLabel;
    private JLabel resultMetricsLabel;
    private JButton reviewAnswersButton;
    private JButton backToAssessmentsFromResultButton;
    private JButton backToDashboardFromResultButton;

    // --- Components: REVIEW Screen ---
    private JLabel reviewHeaderLabel;
    private JLabel reviewProgressLabel;
    private JLabel reviewVerdictBadge;
    private JLabel reviewMarksBadge;
    private JLabel reviewQuestionTitleLabel;
    private JTextArea reviewQuestionDescArea;
    private JTextArea reviewSubmittedAnswerArea;
    private JTextArea reviewSolutionArea;
    private JButton prevReviewButton;
    private JButton nextReviewButton;

    // --- Components: EMPTY Screen ---
    private JLabel emptyTitleLabel;
    private JLabel emptyMessageLabel;

    public AssessmentPanel() {
        this(null, null, null);
    }

    public AssessmentPanel(AssessmentService assessmentService,
                           AuthService authService,
                           Runnable onBackToDashboard) {
        super(new BorderLayout());
        this.assessmentService = assessmentService;
        this.authService = authService;
        this.onBackToDashboard = onBackToDashboard;

        this.cardLayout = new CardLayout();
        this.cardsContainer = new JPanel(cardLayout);

        cardsContainer.add(createListCard(), CARD_LIST);
        cardsContainer.add(createStartCard(), CARD_START);
        cardsContainer.add(createTestCard(), CARD_TEST);
        cardsContainer.add(createResultCard(), CARD_RESULT);
        cardsContainer.add(createReviewCard(), CARD_REVIEW);
        cardsContainer.add(createEmptyCard(), CARD_EMPTY);

        add(cardsContainer, BorderLayout.CENTER);

        showCard(CARD_LIST);
        loadAssessments();
    }

    // =========================================================================
    // Card Construction
    // =========================================================================

    private JPanel createListCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PAGE);

        JPanel topBar = createTopBar("ByteForce — Timed Assessments & Mock Tests", () -> {
            if (onBackToDashboard != null) onBackToDashboard.run();
        });
        panel.add(topBar, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG_PAGE);
        content.setBorder(BorderFactory.createEmptyBorder(28, 40, 28, 40));

        JLabel title = new JLabel("Available Assessments");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(COLOR_TEXT_MAIN);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Select an assessment to evaluate your technical and conceptual placement readiness.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(COLOR_TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        listStatusLabel = new JLabel(" ");
        listStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        listStatusLabel.setForeground(COLOR_DANGER);
        listStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        assessmentListContainer = new JPanel();
        assessmentListContainer.setLayout(new BoxLayout(assessmentListContainer, BoxLayout.Y_AXIS));
        assessmentListContainer.setBackground(BG_PAGE);
        assessmentListContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(title);
        content.add(Box.createRigidArea(new Dimension(0, 4)));
        content.add(subtitle);
        content.add(Box.createRigidArea(new Dimension(0, 10)));
        content.add(listStatusLabel);
        content.add(Box.createRigidArea(new Dimension(0, 16)));
        content.add(assessmentListContainer);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStartCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PAGE);

        JPanel topBar = createTopBar("ByteForce — Assessment Briefing", () -> showCard(CARD_LIST));
        panel.add(topBar, BorderLayout.NORTH);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(BG_PAGE);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(36, 40, 36, 40)
        ));
        card.setPreferredSize(new Dimension(580, 480));
        card.setMaximumSize(new Dimension(640, 520));

        startTitleLabel = new JLabel("Assessment Title");
        startTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        startTitleLabel.setForeground(COLOR_TEXT_MAIN);
        startTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        startDescriptionLabel = new JLabel("Assessment Description");
        startDescriptionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        startDescriptionLabel.setForeground(COLOR_TEXT_MUTED);
        startDescriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel metaBox = new JPanel(new GridLayout(1, 3, 16, 0));
        metaBox.setBackground(BG_CARD);
        metaBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        metaBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        startDurationLabel = new JLabel("⏱ Duration: 60 mins");
        startDurationLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        startDurationLabel.setForeground(COLOR_PRIMARY);

        startTotalMarksLabel = new JLabel("★ Total: 100 Marks");
        startTotalMarksLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        startTotalMarksLabel.setForeground(COLOR_SUCCESS);

        startQuestionsCountLabel = new JLabel("📝 Questions: -");
        startQuestionsCountLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        startQuestionsCountLabel.setForeground(COLOR_TEXT_MAIN);

        metaBox.add(startDurationLabel);
        metaBox.add(startTotalMarksLabel);
        metaBox.add(startQuestionsCountLabel);

        // Instructions
        JLabel rulesTitle = new JLabel("Test Guidelines");
        rulesTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        rulesTitle.setForeground(COLOR_TEXT_MAIN);
        rulesTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea rulesText = new JTextArea("""
                • The countdown timer begins as soon as you click Start Test.
                • You can freely navigate between questions using Previous and Next.
                • Your entered answers are preserved automatically when navigating.
                • Unanswered questions will be recorded as Skipped.
                • When the timer expires, your test will be submitted automatically.
                """);
        rulesText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rulesText.setForeground(COLOR_TEXT_MUTED);
        rulesText.setEditable(false);
        rulesText.setBackground(BG_CARD);
        rulesText.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btnRow.setBackground(BG_CARD);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        startTestButton = new JButton("Start Test (Begin Timer)");
        startTestButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        startTestButton.setForeground(Color.WHITE);
        startTestButton.setBackground(COLOR_PRIMARY);
        startTestButton.setOpaque(true);
        startTestButton.setBorderPainted(false);
        startTestButton.setFocusPainted(false);
        startTestButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        startTestButton.setPreferredSize(new Dimension(210, 42));
        startTestButton.addActionListener(e -> beginTestExecution());

        JButton backBtn = new JButton("Cancel");
        backBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        backBtn.setForeground(COLOR_TEXT_MAIN);
        backBtn.setBackground(BG_PAGE);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.setPreferredSize(new Dimension(100, 42));
        backBtn.addActionListener(e -> showCard(CARD_LIST));

        btnRow.add(startTestButton);
        btnRow.add(backBtn);

        card.add(startTitleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(startDescriptionLabel);
        card.add(Box.createRigidArea(new Dimension(0, 20)));
        card.add(metaBox);
        card.add(Box.createRigidArea(new Dimension(0, 20)));
        card.add(rulesTitle);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(rulesText);
        card.add(Box.createRigidArea(new Dimension(0, 20)));
        card.add(btnRow);

        centerWrapper.add(card);
        panel.add(centerWrapper, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createTestCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PAGE);

        // Top Timer Header
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_HEADER);
        topBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 58));
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));

        testHeaderTitleLabel = new JLabel("Assessment Test");
        testHeaderTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        testHeaderTitleLabel.setForeground(Color.WHITE);
        topBar.add(testHeaderTitleLabel, BorderLayout.WEST);

        timerLabel = new JLabel("⏱ Time Remaining: --:--");
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        timerLabel.setForeground(new Color(254, 240, 138)); // Amber text
        timerLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        topBar.add(timerLabel, BorderLayout.EAST);

        panel.add(topBar, BorderLayout.NORTH);

        // Content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG_PAGE);
        content.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        JPanel qCard = new JPanel();
        qCard.setLayout(new BoxLayout(qCard, BoxLayout.Y_AXIS));
        qCard.setBackground(BG_CARD);
        qCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(24, 28, 24, 28)
        ));
        qCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Badges
        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        badges.setBackground(BG_CARD);
        badges.setAlignmentX(Component.LEFT_ALIGNMENT);

        testProgressLabel = createBadge("Question 1 of 1", COLOR_TEXT_MAIN, COLOR_BADGE_BG);
        testDifficultyBadge = createBadge("MEDIUM", new Color(180, 83, 9), COLOR_WARNING_BG);
        testTypeBadge = createBadge("MCQ", new Color(109, 40, 217), new Color(245, 243, 255));

        badges.add(testProgressLabel);
        badges.add(testDifficultyBadge);
        badges.add(testTypeBadge);
        qCard.add(badges);
        qCard.add(Box.createRigidArea(new Dimension(0, 16)));

        testQuestionTitleLabel = new JLabel("Question Title");
        testQuestionTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        testQuestionTitleLabel.setForeground(COLOR_TEXT_MAIN);
        testQuestionTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        qCard.add(testQuestionTitleLabel);
        qCard.add(Box.createRigidArea(new Dimension(0, 10)));

        testQuestionDescriptionArea = new JTextArea();
        testQuestionDescriptionArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        testQuestionDescriptionArea.setForeground(COLOR_TEXT_MAIN);
        testQuestionDescriptionArea.setBackground(new Color(248, 250, 252));
        testQuestionDescriptionArea.setLineWrap(true);
        testQuestionDescriptionArea.setWrapStyleWord(true);
        testQuestionDescriptionArea.setEditable(false);
        testQuestionDescriptionArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_INPUT_BORDER, 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        testQuestionDescriptionArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        qCard.add(testQuestionDescriptionArea);
        qCard.add(Box.createRigidArea(new Dimension(0, 18)));

        // Answer area
        JLabel answerLbl = new JLabel("Your Answer");
        answerLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        answerLbl.setForeground(COLOR_TEXT_MAIN);
        answerLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        qCard.add(answerLbl);
        qCard.add(Box.createRigidArea(new Dimension(0, 6)));

        testAnswerContainer = new JPanel();
        testAnswerContainer.setLayout(new BoxLayout(testAnswerContainer, BoxLayout.Y_AXIS));
        testAnswerContainer.setBackground(BG_CARD);
        testAnswerContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        qCard.add(testAnswerContainer);
        qCard.add(Box.createRigidArea(new Dimension(0, 14)));

        testStatusLabel = new JLabel(" ");
        testStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        testStatusLabel.setForeground(COLOR_DANGER);
        testStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        qCard.add(testStatusLabel);
        qCard.add(Box.createRigidArea(new Dimension(0, 14)));

        // Navigation Row
        JPanel navRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        navRow.setBackground(BG_CARD);
        navRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        prevQuestionButton = new JButton("← Previous");
        prevQuestionButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        prevQuestionButton.setPreferredSize(new Dimension(120, 38));
        prevQuestionButton.setFocusPainted(false);
        prevQuestionButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        prevQuestionButton.addActionListener(e -> navigateQuestion(-1));

        nextQuestionButton = new JButton("Next →");
        nextQuestionButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        nextQuestionButton.setPreferredSize(new Dimension(120, 38));
        nextQuestionButton.setFocusPainted(false);
        nextQuestionButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        nextQuestionButton.addActionListener(e -> navigateQuestion(1));

        submitTestButton = new JButton("Submit Test");
        submitTestButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        submitTestButton.setForeground(Color.WHITE);
        submitTestButton.setBackground(COLOR_SUCCESS);
        submitTestButton.setOpaque(true);
        submitTestButton.setBorderPainted(false);
        submitTestButton.setFocusPainted(false);
        submitTestButton.setPreferredSize(new Dimension(140, 38));
        submitTestButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        submitTestButton.addActionListener(e -> confirmAndSubmitTest());

        navRow.add(prevQuestionButton);
        navRow.add(nextQuestionButton);
        navRow.add(submitTestButton);
        qCard.add(navRow);

        content.add(qCard);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createResultCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PAGE);

        JPanel topBar = createTopBar("ByteForce — Assessment Results", () -> showCard(CARD_LIST));
        panel.add(topBar, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG_PAGE);
        content.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(28, 32, 28, 32)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        resultStatusBannerLabel = new JLabel("✓ Assessment Completed Successfully");
        resultStatusBannerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        resultStatusBannerLabel.setForeground(COLOR_SUCCESS);
        card.add(resultStatusBannerLabel);
        card.add(Box.createRigidArea(new Dimension(0, 16)));

        resultAssessmentTitleLabel = new JLabel("Assessment Title");
        resultAssessmentTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        resultAssessmentTitleLabel.setForeground(COLOR_TEXT_MAIN);
        card.add(resultAssessmentTitleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 8)));

        resultScoreLabel = new JLabel("Score: 0 / 0 Marks (0.0%)");
        resultScoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        resultScoreLabel.setForeground(COLOR_PRIMARY);
        card.add(resultScoreLabel);
        card.add(Box.createRigidArea(new Dimension(0, 16)));

        resultMetricsLabel = new JLabel("Questions: - | Correct: - | Incorrect: - | Skipped: -");
        resultMetricsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        resultMetricsLabel.setForeground(COLOR_TEXT_MUTED);
        card.add(resultMetricsLabel);
        card.add(Box.createRigidArea(new Dimension(0, 24)));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btnRow.setBackground(BG_CARD);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        reviewAnswersButton = new JButton("Review Answers");
        reviewAnswersButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        reviewAnswersButton.setForeground(Color.WHITE);
        reviewAnswersButton.setBackground(COLOR_PRIMARY);
        reviewAnswersButton.setOpaque(true);
        reviewAnswersButton.setBorderPainted(false);
        reviewAnswersButton.setFocusPainted(false);
        reviewAnswersButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        reviewAnswersButton.setPreferredSize(new Dimension(160, 40));
        reviewAnswersButton.addActionListener(e -> startReviewSession());

        backToAssessmentsFromResultButton = new JButton("Back to Assessments");
        backToAssessmentsFromResultButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        backToAssessmentsFromResultButton.setPreferredSize(new Dimension(170, 40));
        backToAssessmentsFromResultButton.setFocusPainted(false);
        backToAssessmentsFromResultButton.addActionListener(e -> showCard(CARD_LIST));

        backToDashboardFromResultButton = new JButton("Back to Dashboard");
        backToDashboardFromResultButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        backToDashboardFromResultButton.setPreferredSize(new Dimension(160, 40));
        backToDashboardFromResultButton.setFocusPainted(false);
        backToDashboardFromResultButton.addActionListener(e -> {
            if (onBackToDashboard != null) onBackToDashboard.run();
        });

        btnRow.add(reviewAnswersButton);
        btnRow.add(backToAssessmentsFromResultButton);
        btnRow.add(backToDashboardFromResultButton);
        card.add(btnRow);

        content.add(card);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createReviewCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PAGE);

        JPanel topBar = createTopBar("ByteForce — Assessment Review", () -> showCard(CARD_RESULT));
        panel.add(topBar, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG_PAGE);
        content.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(24, 28, 24, 28)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        badges.setBackground(BG_CARD);
        badges.setAlignmentX(Component.LEFT_ALIGNMENT);

        reviewProgressLabel = createBadge("Review Question 1 of 1", COLOR_TEXT_MAIN, COLOR_BADGE_BG);
        reviewVerdictBadge = createBadge("CORRECT", COLOR_SUCCESS, COLOR_SUCCESS_BG);
        reviewMarksBadge = createBadge("Marks: 1 / 1", COLOR_PRIMARY, new Color(239, 246, 255));

        badges.add(reviewProgressLabel);
        badges.add(reviewVerdictBadge);
        badges.add(reviewMarksBadge);
        card.add(badges);
        card.add(Box.createRigidArea(new Dimension(0, 14)));

        reviewQuestionTitleLabel = new JLabel("Question Title");
        reviewQuestionTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        reviewQuestionTitleLabel.setForeground(COLOR_TEXT_MAIN);
        card.add(reviewQuestionTitleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 8)));

        reviewQuestionDescArea = new JTextArea();
        reviewQuestionDescArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        reviewQuestionDescArea.setBackground(new Color(248, 250, 252));
        reviewQuestionDescArea.setLineWrap(true);
        reviewQuestionDescArea.setWrapStyleWord(true);
        reviewQuestionDescArea.setEditable(false);
        reviewQuestionDescArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_INPUT_BORDER, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        card.add(reviewQuestionDescArea);
        card.add(Box.createRigidArea(new Dimension(0, 16)));

        JLabel ansLbl = new JLabel("Your Submitted Answer");
        ansLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        card.add(ansLbl);
        card.add(Box.createRigidArea(new Dimension(0, 4)));

        reviewSubmittedAnswerArea = new JTextArea(3, 40);
        reviewSubmittedAnswerArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        reviewSubmittedAnswerArea.setEditable(false);
        reviewSubmittedAnswerArea.setBackground(new Color(248, 250, 252));
        reviewSubmittedAnswerArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_INPUT_BORDER, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        card.add(reviewSubmittedAnswerArea);
        card.add(Box.createRigidArea(new Dimension(0, 16)));

        JLabel solLbl = new JLabel("Expected / Reference Solution");
        solLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        card.add(solLbl);
        card.add(Box.createRigidArea(new Dimension(0, 4)));

        reviewSolutionArea = new JTextArea(3, 40);
        reviewSolutionArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        reviewSolutionArea.setEditable(false);
        reviewSolutionArea.setBackground(new Color(248, 250, 252));
        reviewSolutionArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_INPUT_BORDER, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        card.add(reviewSolutionArea);
        card.add(Box.createRigidArea(new Dimension(0, 20)));

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        nav.setBackground(BG_CARD);
        nav.setAlignmentX(Component.LEFT_ALIGNMENT);

        prevReviewButton = new JButton("← Previous");
        prevReviewButton.setPreferredSize(new Dimension(120, 38));
        prevReviewButton.addActionListener(e -> navigateReview(-1));

        nextReviewButton = new JButton("Next →");
        nextReviewButton.setPreferredSize(new Dimension(120, 38));
        nextReviewButton.addActionListener(e -> navigateReview(1));

        JButton backToRes = new JButton("Return to Summary");
        backToRes.setPreferredSize(new Dimension(160, 38));
        backToRes.addActionListener(e -> showCard(CARD_RESULT));

        nav.add(prevReviewButton);
        nav.add(nextReviewButton);
        nav.add(backToRes);
        card.add(nav);

        content.add(card);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createEmptyCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PAGE);

        JPanel topBar = createTopBar("ByteForce — Assessments", () -> {
            if (onBackToDashboard != null) onBackToDashboard.run();
        });
        panel.add(topBar, BorderLayout.NORTH);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(BG_PAGE);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(40, 48, 40, 48)
        ));
        card.setPreferredSize(new Dimension(500, 260));

        emptyTitleLabel = new JLabel("No Assessments Available");
        emptyTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        emptyTitleLabel.setForeground(COLOR_TEXT_MAIN);
        emptyTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        emptyMessageLabel = new JLabel("There are currently no active assessments in the system.");
        emptyMessageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        emptyMessageLabel.setForeground(COLOR_TEXT_MUTED);
        emptyMessageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnRow.setBackground(BG_CARD);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setPreferredSize(new Dimension(120, 38));
        refreshBtn.addActionListener(e -> loadAssessments());

        JButton dashBtn = new JButton("Back to Dashboard");
        dashBtn.setPreferredSize(new Dimension(160, 38));
        dashBtn.addActionListener(e -> {
            if (onBackToDashboard != null) onBackToDashboard.run();
        });

        btnRow.add(refreshBtn);
        btnRow.add(dashBtn);

        card.add(emptyTitleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(emptyMessageLabel);
        card.add(Box.createRigidArea(new Dimension(0, 24)));
        card.add(btnRow);

        centerWrapper.add(card);
        panel.add(centerWrapper, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createTopBar(String title, Runnable onBack) {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_HEADER);
        topBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 56));
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        topBar.add(titleLabel, BorderLayout.WEST);

        if (onBack != null) {
            JButton backBtn = new JButton("Back to Dashboard");
            backBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            backBtn.setForeground(Color.WHITE);
            backBtn.setBackground(new Color(51, 65, 85));
            backBtn.setFocusPainted(false);
            backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            backBtn.addActionListener(e -> onBack.run());
            topBar.add(backBtn, BorderLayout.EAST);
        }

        return topBar;
    }

    private JLabel createBadge(String text, Color foreground, Color background) {
        JLabel badge = new JLabel(" " + text + " ");
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setForeground(foreground);
        badge.setBackground(background);
        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(background, 1, true),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        return badge;
    }

    // =========================================================================
    // Core Workflow Operations (EDT Safe with SwingWorker)
    // =========================================================================

    public void loadAssessments() {
        if (assessmentService == null) {
            showEmpty("Assessment service is currently unavailable.");
            return;
        }

        listStatusLabel.setText("Loading assessments...");
        listStatusLabel.setForeground(COLOR_PRIMARY);

        SwingWorker<List<Assessment>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Assessment> doInBackground() {
                return assessmentService.getAvailableAssessments();
            }

            @Override
            protected void done() {
                listStatusLabel.setText(" ");
                try {
                    List<Assessment> list = get();
                    availableAssessments.clear();
                    assessmentListContainer.removeAll();

                    if (list == null || list.isEmpty()) {
                        showEmpty("No active assessments found in database.");
                        return;
                    }

                    availableAssessments.addAll(list);
                    for (Assessment a : list) {
                        assessmentListContainer.add(createAssessmentCard(a));
                        assessmentListContainer.add(Box.createRigidArea(new Dimension(0, 16)));
                    }
                    assessmentListContainer.revalidate();
                    assessmentListContainer.repaint();
                    showCard(CARD_LIST);
                } catch (Exception e) {
                    log.error("Failed to load assessments", e);
                    showEmpty("Unable to load assessments from database.");
                }
            }
        };
        worker.execute();
    }

    private JPanel createAssessmentCard(Assessment a) {
        JPanel card = new JPanel(new BorderLayout(16, 12));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(BG_CARD);

        JLabel title = new JLabel(a.getTitle());
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(COLOR_TEXT_MAIN);

        JLabel desc = new JLabel(a.getDescription() != null ? a.getDescription() : "Placement Readiness Test");
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        desc.setForeground(COLOR_TEXT_MUTED);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        badges.setBackground(BG_CARD);
        badges.add(createBadge(a.getDifficulty().name(), new Color(180, 83, 9), COLOR_WARNING_BG));
        badges.add(createBadge("⏱ " + a.getDurationMinutes() + " mins", COLOR_PRIMARY, new Color(239, 246, 255)));
        badges.add(createBadge("★ " + a.getTotalMarks() + " marks", COLOR_SUCCESS, COLOR_SUCCESS_BG));

        left.add(title);
        left.add(Box.createRigidArea(new Dimension(0, 4)));
        left.add(desc);
        left.add(Box.createRigidArea(new Dimension(0, 10)));
        left.add(badges);

        card.add(left, BorderLayout.CENTER);

        JButton startBtn = new JButton("View Details →");
        startBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        startBtn.setForeground(Color.WHITE);
        startBtn.setBackground(COLOR_PRIMARY);
        startBtn.setOpaque(true);
        startBtn.setBorderPainted(false);
        startBtn.setFocusPainted(false);
        startBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        startBtn.setPreferredSize(new Dimension(140, 38));
        startBtn.addActionListener(e -> selectAssessmentForBriefing(a));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 20));
        right.setBackground(BG_CARD);
        right.add(startBtn);

        card.add(right, BorderLayout.EAST);
        return card;
    }

    public void selectAssessmentForBriefing(Assessment a) {
        this.selectedAssessment = a;
        startTitleLabel.setText(a.getTitle());
        startDescriptionLabel.setText(a.getDescription() != null ? a.getDescription() : "Comprehensive mock test.");
        startDurationLabel.setText("⏱ Duration: " + a.getDurationMinutes() + " mins");
        startTotalMarksLabel.setText("★ Total: " + a.getTotalMarks() + " Marks");
        startQuestionsCountLabel.setText("📝 Loading questions...");

        showCard(CARD_START);

        // Fetch question count in background
        SwingWorker<List<Question>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Question> doInBackground() {
                return assessmentService.getQuestionsForAssessment(a.getId());
            }

            @Override
            protected void done() {
                try {
                    List<Question> qs = get();
                    startQuestionsCountLabel.setText("📝 Questions: " + (qs != null ? qs.size() : 0));
                } catch (Exception e) {
                    startQuestionsCountLabel.setText("📝 Questions: -");
                }
            }
        };
        worker.execute();
    }

    public void beginTestExecution() {
        if (selectedAssessment == null || assessmentService == null) {
            return;
        }

        UUID userId = authService != null && authService.getCurrentUser().isPresent()
                ? authService.getCurrentUser().get().getId()
                : UUID.randomUUID();

        startTestButton.setEnabled(false);

        SwingWorker<AssessmentAttempt, Void> worker = new SwingWorker<>() {
            private List<Question> questions;
            private Map<Long, Integer> marksMap;

            @Override
            protected AssessmentAttempt doInBackground() {
                questions = assessmentService.getQuestionsForAssessment(selectedAssessment.getId());
                marksMap = assessmentService.getQuestionMarks(selectedAssessment.getId());
                return assessmentService.startAssessment(userId, selectedAssessment.getId());
            }

            @Override
            protected void done() {
                startTestButton.setEnabled(true);
                try {
                    activeAttempt = get();
                    currentTestQuestions.clear();
                    currentQuestionMarks.clear();
                    inProgressAnswers.clear();

                    if (questions == null || questions.isEmpty()) {
                        JOptionPane.showMessageDialog(AssessmentPanel.this,
                                "This assessment currently has no questions assigned.",
                                "No Questions Found",
                                JOptionPane.WARNING_MESSAGE);
                        showCard(CARD_LIST);
                        return;
                    }

                    currentTestQuestions.addAll(questions);
                    currentQuestionMarks.putAll(marksMap);
                    currentQuestionIndex = 0;

                    testHeaderTitleLabel.setText(selectedAssessment.getTitle());

                    // Start countdown timer safely on EDT
                    startTimer(selectedAssessment.getDurationMinutes());

                    renderTestQuestion();
                    showCard(CARD_TEST);
                } catch (Exception e) {
                    log.error("Unable to begin assessment", e);
                    JOptionPane.showMessageDialog(AssessmentPanel.this,
                            "Failed to start assessment: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()),
                            "Startup Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void startTimer(int durationMinutes) {
        stopTimer();
        this.remainingSeconds = durationMinutes * 60;
        updateTimerDisplay();

        countdownTimer = new Timer(1000, e -> {
            remainingSeconds--;
            updateTimerDisplay();
            if (remainingSeconds <= 0) {
                stopTimer();
                log.info("Assessment timer expired. Automatically submitting attempt {}",
                        activeAttempt != null ? activeAttempt.getId() : -1);
                submitAssessmentAttempt(true);
            }
        });
        countdownTimer.start();
    }

    private void stopTimer() {
        if (countdownTimer != null && countdownTimer.isRunning()) {
            countdownTimer.stop();
        }
        countdownTimer = null;
    }

    private void updateTimerDisplay() {
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        String formatted = String.format("%02d:%02d", minutes, seconds);
        timerLabel.setText("⏱ Time Remaining: " + formatted);

        if (remainingSeconds < 300) { // Under 5 minutes
            timerLabel.setForeground(COLOR_DANGER);
        } else {
            timerLabel.setForeground(new Color(254, 240, 138));
        }
    }

    private void renderTestQuestion() {
        if (currentTestQuestions.isEmpty() || currentQuestionIndex >= currentTestQuestions.size()) {
            return;
        }

        Question q = currentTestQuestions.get(currentQuestionIndex);

        testProgressLabel.setText(" Question " + (currentQuestionIndex + 1) + " of " + currentTestQuestions.size() + " ");
        testDifficultyBadge.setText(" " + q.getDifficulty().name() + " ");
        testTypeBadge.setText(" " + q.getQuestionType().name() + " ");

        testQuestionTitleLabel.setText(q.getTitle());
        testQuestionDescriptionArea.setText(q.getDescription());
        testStatusLabel.setText(" ");

        prevQuestionButton.setEnabled(currentQuestionIndex > 0);
        nextQuestionButton.setEnabled(currentQuestionIndex < currentTestQuestions.size() - 1);

        // Build Answer Controls
        testAnswerContainer.removeAll();
        testMcqGroup = new ButtonGroup();
        testMcqRadios.clear();
        testCodeOrTextArea = null;

        String savedAnswer = inProgressAnswers.get(q.getId());

        if (q.getQuestionType() == QuestionType.MCQ) {
            List<String> options = PracticePanel.parseMcqOptions(q.getDescription());
            if (!options.isEmpty()) {
                for (String opt : options) {
                    JRadioButton rb = new JRadioButton(opt);
                    rb.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    rb.setBackground(BG_CARD);
                    rb.setFocusPainted(false);
                    testMcqGroup.add(rb);
                    testMcqRadios.add(rb);
                    if (savedAnswer != null && savedAnswer.equalsIgnoreCase(opt)) {
                        rb.setSelected(true);
                    }
                    testAnswerContainer.add(rb);
                    testAnswerContainer.add(Box.createRigidArea(new Dimension(0, 6)));
                }
            } else {
                String[] letters = {"A", "B", "C", "D"};
                for (String letter : letters) {
                    JRadioButton rb = new JRadioButton("Option " + letter);
                    rb.setActionCommand(letter);
                    rb.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    rb.setBackground(BG_CARD);
                    rb.setFocusPainted(false);
                    testMcqGroup.add(rb);
                    testMcqRadios.add(rb);
                    if (savedAnswer != null && savedAnswer.equalsIgnoreCase(letter)) {
                        rb.setSelected(true);
                    }
                    testAnswerContainer.add(rb);
                    testAnswerContainer.add(Box.createRigidArea(new Dimension(0, 6)));
                }
            }
        } else {
            testCodeOrTextArea = new JTextArea(5, 40);
            testCodeOrTextArea.setFont(new Font(q.getQuestionType() == QuestionType.CODING ? "Consolas" : "Segoe UI", Font.PLAIN, 13));
            testCodeOrTextArea.setLineWrap(true);
            testCodeOrTextArea.setWrapStyleWord(true);
            testCodeOrTextArea.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_INPUT_BORDER, 1),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
            if (savedAnswer != null) {
                testCodeOrTextArea.setText(savedAnswer);
            }
            testAnswerContainer.add(testCodeOrTextArea);
        }

        testAnswerContainer.revalidate();
        testAnswerContainer.repaint();
    }

    private void saveCurrentAnswer() {
        if (currentTestQuestions.isEmpty() || currentQuestionIndex >= currentTestQuestions.size()) {
            return;
        }
        Question q = currentTestQuestions.get(currentQuestionIndex);
        String ans = "";

        if (q.getQuestionType() == QuestionType.MCQ) {
            for (JRadioButton rb : testMcqRadios) {
                if (rb.isSelected()) {
                    ans = rb.getActionCommand() != null && !rb.getActionCommand().isBlank()
                            ? rb.getActionCommand() : rb.getText();
                    break;
                }
            }
        } else if (testCodeOrTextArea != null) {
            ans = testCodeOrTextArea.getText() != null ? testCodeOrTextArea.getText().trim() : "";
        }

        if (!ans.isBlank()) {
            inProgressAnswers.put(q.getId(), ans);
        } else {
            inProgressAnswers.remove(q.getId());
        }
    }

    public void navigateQuestion(int direction) {
        saveCurrentAnswer();
        int newIndex = currentQuestionIndex + direction;
        if (newIndex >= 0 && newIndex < currentTestQuestions.size()) {
            currentQuestionIndex = newIndex;
            renderTestQuestion();
        }
    }

    private void confirmAndSubmitTest() {
        saveCurrentAnswer();
        int answeredCount = inProgressAnswers.size();
        int total = currentTestQuestions.size();

        int choice = JOptionPane.showConfirmDialog(this,
                "You have answered " + answeredCount + " of " + total + " questions.\n"
                        + "Are you sure you want to finish and submit your assessment?",
                "Submit Assessment Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            submitAssessmentAttempt(false);
        }
    }

    public void submitAssessmentAttempt(boolean isTimeout) {
        stopTimer();
        saveCurrentAnswer();

        if (activeAttempt == null || assessmentService == null) {
            return;
        }

        submitTestButton.setEnabled(false);
        prevQuestionButton.setEnabled(false);
        nextQuestionButton.setEnabled(false);
        testStatusLabel.setText("Submitting assessment and evaluating answers...");

        SwingWorker<AssessmentAttempt, Void> worker = new SwingWorker<>() {
            @Override
            protected AssessmentAttempt doInBackground() {
                return assessmentService.submitAssessment(activeAttempt.getId(), inProgressAnswers);
            }

            @Override
            protected void done() {
                submitTestButton.setEnabled(true);
                prevQuestionButton.setEnabled(true);
                nextQuestionButton.setEnabled(true);
                try {
                    activeAttempt = get();
                    loadResultScreen(isTimeout);
                } catch (Exception e) {
                    log.error("Failed to submit assessment attempt", e);
                    testStatusLabel.setText("Submission failed. Please try again.");
                }
            }
        };
        worker.execute();
    }

    private void loadResultScreen(boolean isTimeout) {
        if (activeAttempt == null || assessmentService == null) {
            return;
        }

        SwingWorker<List<AssessmentAnswer>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<AssessmentAnswer> doInBackground() {
                return assessmentService.getAnswersForAttempt(activeAttempt.getId());
            }

            @Override
            protected void done() {
                try {
                    completedAnswers = get();
                    renderResultCard(isTimeout);
                    showCard(CARD_RESULT);
                } catch (Exception e) {
                    log.error("Unable to load attempt answers for result", e);
                    showCard(CARD_LIST);
                }
            }
        };
        worker.execute();
    }

    private void renderResultCard(boolean isTimeout) {
        if (isTimeout) {
            resultStatusBannerLabel.setText("⏱ Time Expired — Assessment Submitted Automatically");
            resultStatusBannerLabel.setForeground(COLOR_DANGER);
        } else {
            resultStatusBannerLabel.setText("✓ Assessment Completed Successfully");
            resultStatusBannerLabel.setForeground(COLOR_SUCCESS);
        }

        resultAssessmentTitleLabel.setText(selectedAssessment != null ? selectedAssessment.getTitle() : "Assessment");

        int totalMarks = selectedAssessment != null ? selectedAssessment.getTotalMarks() : 100;
        int score = activeAttempt != null ? activeAttempt.getScore() : 0;
        double pct = totalMarks > 0 ? ((double) score / totalMarks) * 100.0 : 0.0;

        resultScoreLabel.setText(String.format("Score: %d / %d Marks (%.1f%%)", score, totalMarks, pct));

        long correctCount = completedAnswers.stream().filter(a -> a.getStatus() == AttemptStatus.SOLVED).count();
        long incorrectCount = completedAnswers.stream().filter(a -> a.getStatus() == AttemptStatus.FAILED).count();
        long skippedCount = completedAnswers.stream().filter(a -> a.getStatus() == AttemptStatus.SKIPPED).count();

        resultMetricsLabel.setText(String.format("Total Questions: %d | Correct: %d | Incorrect: %d | Skipped: %d",
                completedAnswers.size(), correctCount, incorrectCount, skippedCount));
    }

    public void startReviewSession() {
        if (completedAnswers.isEmpty() || currentTestQuestions.isEmpty()) {
            return;
        }
        currentReviewIndex = 0;
        renderReviewQuestion();
        showCard(CARD_REVIEW);
    }

    private void renderReviewQuestion() {
        if (currentReviewIndex >= currentTestQuestions.size() || currentReviewIndex >= completedAnswers.size()) {
            return;
        }

        Question q = currentTestQuestions.get(currentReviewIndex);
        AssessmentAnswer ans = completedAnswers.get(currentReviewIndex);

        reviewProgressLabel.setText(" Question " + (currentReviewIndex + 1) + " of " + currentTestQuestions.size() + " ");
        reviewQuestionTitleLabel.setText(q.getTitle());
        reviewQuestionDescArea.setText(q.getDescription());

        // Status Verdict
        AttemptStatus st = ans.getStatus();
        if (st == AttemptStatus.SOLVED) {
            reviewVerdictBadge.setText(" CORRECT ");
            reviewVerdictBadge.setForeground(COLOR_SUCCESS);
            reviewVerdictBadge.setBackground(COLOR_SUCCESS_BG);
        } else if (st == AttemptStatus.FAILED) {
            reviewVerdictBadge.setText(" INCORRECT ");
            reviewVerdictBadge.setForeground(COLOR_DANGER);
            reviewVerdictBadge.setBackground(COLOR_DANGER_BG);
        } else if (st == AttemptStatus.SKIPPED) {
            reviewVerdictBadge.setText(" SKIPPED ");
            reviewVerdictBadge.setForeground(COLOR_WARNING);
            reviewVerdictBadge.setBackground(COLOR_WARNING_BG);
        } else {
            reviewVerdictBadge.setText(" ATTEMPTED ");
            reviewVerdictBadge.setForeground(COLOR_PRIMARY);
            reviewVerdictBadge.setBackground(new Color(239, 246, 255));
        }

        int maxQMarks = currentQuestionMarks.getOrDefault(q.getId(), 1);
        reviewMarksBadge.setText(" Marks: " + ans.getMarksAwarded() + " / " + maxQMarks + " ");

        reviewSubmittedAnswerArea.setText(ans.getSubmittedAnswer() != null && !ans.getSubmittedAnswer().isBlank()
                ? ans.getSubmittedAnswer() : "(No answer submitted / Skipped)");

        reviewSolutionArea.setText(q.getSolution() != null && !q.getSolution().isBlank()
                ? q.getSolution() : "No reference solution available.");

        prevReviewButton.setEnabled(currentReviewIndex > 0);
        nextReviewButton.setEnabled(currentReviewIndex < currentTestQuestions.size() - 1);
    }

    public void navigateReview(int direction) {
        int newIdx = currentReviewIndex + direction;
        if (newIdx >= 0 && newIdx < currentTestQuestions.size()) {
            currentReviewIndex = newIdx;
            renderReviewQuestion();
        }
    }

    public void showEmpty(String message) {
        emptyMessageLabel.setText(message != null ? message : "No assessments available.");
        showCard(CARD_EMPTY);
    }

    public void resetToHome() {
        stopTimer();
        inProgressAnswers.clear();
        currentTestQuestions.clear();
        completedAnswers.clear();
        selectedAssessment = null;
        activeAttempt = null;
        showCard(CARD_LIST);
        loadAssessments();
    }

    public void showCard(String cardName) {
        cardLayout.show(cardsContainer, cardName);
        this.currentCard = cardName;
    }

    // =========================================================================
    // Component Getters for Unit Testing
    // =========================================================================

    public String getCurrentCard() {
        return currentCard;
    }

    public List<Assessment> getAvailableAssessments() {
        return availableAssessments;
    }

    public JPanel getAssessmentListContainer() {
        return assessmentListContainer;
    }

    public JButton getStartTestButton() {
        return startTestButton;
    }

    public JButton getSubmitTestButton() {
        return submitTestButton;
    }

    public JButton getPrevQuestionButton() {
        return prevQuestionButton;
    }

    public JButton getNextQuestionButton() {
        return nextQuestionButton;
    }

    public JButton getReviewAnswersButton() {
        return reviewAnswersButton;
    }

    public JLabel getTimerLabel() {
        return timerLabel;
    }

    public Timer getCountdownTimer() {
        return countdownTimer;
    }

    public Map<Long, String> getInProgressAnswers() {
        return inProgressAnswers;
    }

    public List<Question> getCurrentTestQuestions() {
        return currentTestQuestions;
    }

    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public AssessmentAttempt getActiveAttempt() {
        return activeAttempt;
    }

    public JLabel getResultScoreLabel() {
        return resultScoreLabel;
    }

    public List<JRadioButton> getTestMcqRadios() {
        return testMcqRadios;
    }

    public JTextArea getTestCodeOrTextArea() {
        return testCodeOrTextArea;
    }

    public JLabel getStartTitleLabel() {
        return startTitleLabel;
    }

    public JLabel getStartDescriptionLabel() {
        return startDescriptionLabel;
    }

    public JLabel getStartDurationLabel() {
        return startDurationLabel;
    }

    public JLabel getStartTotalMarksLabel() {
        return startTotalMarksLabel;
    }

    public JLabel getStartQuestionsCountLabel() {
        return startQuestionsCountLabel;
    }

    public JLabel getResultStatusBannerLabel() {
        return resultStatusBannerLabel;
    }

    public JLabel getResultMetricsLabel() {
        return resultMetricsLabel;
    }

    public JLabel getResultAssessmentTitleLabel() {
        return resultAssessmentTitleLabel;
    }

    public JLabel getReviewHeaderLabel() {
        return reviewHeaderLabel;
    }

    public JLabel getReviewVerdictBadge() {
        return reviewVerdictBadge;
    }

    public JLabel getReviewMarksBadge() {
        return reviewMarksBadge;
    }

    public JLabel getReviewQuestionTitleLabel() {
        return reviewQuestionTitleLabel;
    }

    public JTextArea getReviewSubmittedAnswerArea() {
        return reviewSubmittedAnswerArea;
    }

    public JTextArea getReviewSolutionArea() {
        return reviewSolutionArea;
    }

    public JButton getPrevReviewButton() {
        return prevReviewButton;
    }

    public JButton getNextReviewButton() {
        return nextReviewButton;
    }

    public JButton getBackToAssessmentsFromResultButton() {
        return backToAssessmentsFromResultButton;
    }

    public JButton getBackToDashboardFromResultButton() {
        return backToDashboardFromResultButton;
    }

    public JLabel getEmptyTitleLabel() {
        return emptyTitleLabel;
    }

    public JLabel getEmptyMessageLabel() {
        return emptyMessageLabel;
    }
}
