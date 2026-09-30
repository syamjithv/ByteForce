package com.byteforce.ui;

import com.byteforce.domain.AttemptStatus;
import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;
import com.byteforce.domain.QuestionType;
import com.byteforce.domain.Topic;
import com.byteforce.domain.User;
import com.byteforce.service.AttemptService;
import com.byteforce.service.AuthService;
import com.byteforce.service.BookmarkService;
import com.byteforce.service.QuestionService;
import com.byteforce.service.TopicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Practice module view for ByteForce placement readiness platform.
 * Allows students to filter by Topic and Difficulty, work through questions,
 * submit answers, record attempts, check solutions, and bookmark questions.
 *
 * Communicates strictly through services (TopicService, QuestionService,
 * AttemptService, BookmarkService, AuthService) without direct repository or DB access.
 */
public class PracticePanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(PracticePanel.class);

    // Card Names
    public static final String CARD_FILTERS = "FILTERS";
    public static final String CARD_QUESTION = "QUESTION";
    public static final String CARD_RESULT = "RESULT";
    public static final String CARD_EMPTY = "EMPTY";

    // Color Palette matching ByteForce design system
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
    private static final Color COLOR_BOOKMARK_ACTIVE = new Color(217, 119, 6);// Amber-600
    private static final Color COLOR_BOOKMARK_BG = new Color(254, 243, 199);// Amber-100

    // Services
    private final TopicService topicService;
    private final QuestionService questionService;
    private final AttemptService attemptService;
    private final BookmarkService bookmarkService;
    private final AuthService authService;
    private final Runnable onBackToDashboard;

    // Navigation and Cards
    private final CardLayout cardLayout;
    private final JPanel cardsContainer;
    private String currentCard;

    // Cached Topics Map (topicId -> Topic Name)
    private final Map<Long, String> topicNameMap = new HashMap<>();

    // In-memory Practice Session
    private final List<Question> activeQuestions = new ArrayList<>();
    private int currentQuestionIndex = 0;

    // --- Components: FILTERS Screen ---
    private JComboBox<TopicItem> topicComboBox;
    private JComboBox<DifficultyItem> difficultyComboBox;
    private JButton startPracticeButton;
    private JButton backToDashboardFromFiltersButton;
    private JLabel filtersStatusLabel;

    // --- Components: QUESTION Screen ---
    private JLabel progressLabel;
    private JLabel topicBadgeLabel;
    private JLabel difficultyBadgeLabel;
    private JLabel typeBadgeLabel;
    private JLabel questionTitleLabel;
    private JTextArea questionDescriptionArea;
    private JPanel answerContainer;
    private ButtonGroup mcqButtonGroup;
    private final List<JRadioButton> mcqRadioButtons = new ArrayList<>();
    private JTextField textAnswerField;
    private JTextArea codeOrConceptualArea;
    private JLabel noteLabel;
    private JButton submitButton;
    private JButton skipButton;
    private JButton bookmarkButton;
    private JButton backToFiltersFromQuestionButton;
    private JLabel questionStatusLabel;

    // --- Components: RESULT Screen ---
    private JPanel resultBannerPanel;
    private JLabel resultBannerLabel;
    private JLabel resultTitleLabel;
    private JLabel resultTopicDifficultyLabel;
    private JTextArea studentAnswerDisplayArea;
    private JTextArea solutionDisplayArea;
    private JButton nextQuestionButton;
    private JButton backToFiltersFromResultButton;
    private JButton backToDashboardFromResultButton;

    // --- Components: EMPTY State Screen ---
    private JLabel emptyStateTitleLabel;
    private JLabel emptyStateMessageLabel;
    private JButton changeFiltersFromEmptyButton;
    private JButton backToDashboardFromEmptyButton;

    public PracticePanel() {
        this(null, null, null, null, null, null);
    }

    public PracticePanel(TopicService topicService,
                         QuestionService questionService,
                         AttemptService attemptService,
                         BookmarkService bookmarkService,
                         AuthService authService,
                         Runnable onBackToDashboard) {
        super(new BorderLayout());
        this.topicService = topicService;
        this.questionService = questionService;
        this.attemptService = attemptService;
        this.bookmarkService = bookmarkService;
        this.authService = authService;
        this.onBackToDashboard = onBackToDashboard;

        this.cardLayout = new CardLayout();
        this.cardsContainer = new JPanel(cardLayout);

        cardsContainer.add(createFiltersCard(), CARD_FILTERS);
        cardsContainer.add(createQuestionCard(), CARD_QUESTION);
        cardsContainer.add(createResultCard(), CARD_RESULT);
        cardsContainer.add(createEmptyCard(), CARD_EMPTY);

        add(cardsContainer, BorderLayout.CENTER);

        showCard(CARD_FILTERS);
        refreshTopics();
    }

    // =========================================================================
    // Card Construction
    // =========================================================================

    private JPanel createFiltersCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PAGE);

        // Top Bar
        JPanel topBar = createTopBar("ByteForce — Practice & Problem Solving", () -> {
            if (onBackToDashboard != null) onBackToDashboard.run();
        });
        panel.add(topBar, BorderLayout.NORTH);

        // Center Filter Card
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(BG_PAGE);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(36, 40, 36, 40)
        ));
        card.setPreferredSize(new Dimension(540, 480));
        card.setMaximumSize(new Dimension(600, 520));

        JLabel title = new JLabel("Practice Questions");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(COLOR_TEXT_MAIN);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Filter placement preparation questions by topic and difficulty.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(COLOR_TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Topic Selector
        JLabel topicLabel = new JLabel("Topic");
        topicLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        topicLabel.setForeground(COLOR_TEXT_MAIN);
        topicLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        topicComboBox = new JComboBox<>();
        topicComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        topicComboBox.setPreferredSize(new Dimension(460, 38));
        topicComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        topicComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        topicComboBox.addItem(new TopicItem(null, "All Topics"));

        // Difficulty Selector
        JLabel difficultyLabel = new JLabel("Difficulty");
        difficultyLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        difficultyLabel.setForeground(COLOR_TEXT_MAIN);
        difficultyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        difficultyComboBox = new JComboBox<>();
        difficultyComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        difficultyComboBox.setPreferredSize(new Dimension(460, 38));
        difficultyComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        difficultyComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        difficultyComboBox.addItem(new DifficultyItem(null, "All Difficulties"));
        for (Difficulty diff : Difficulty.values()) {
            difficultyComboBox.addItem(new DifficultyItem(diff, formatDifficultyName(diff)));
        }

        // Status Label
        filtersStatusLabel = new JLabel(" ");
        filtersStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filtersStatusLabel.setForeground(COLOR_DANGER);
        filtersStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Buttons
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        buttonRow.setBackground(BG_CARD);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        startPracticeButton = new JButton("Start Practice");
        startPracticeButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        startPracticeButton.setForeground(Color.WHITE);
        startPracticeButton.setBackground(COLOR_PRIMARY);
        startPracticeButton.setOpaque(true);
        startPracticeButton.setBorderPainted(false);
        startPracticeButton.setFocusPainted(false);
        startPracticeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        startPracticeButton.setPreferredSize(new Dimension(170, 40));
        startPracticeButton.addActionListener(e -> startPracticeSession());

        backToDashboardFromFiltersButton = new JButton("Back to Dashboard");
        backToDashboardFromFiltersButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        backToDashboardFromFiltersButton.setForeground(COLOR_TEXT_MAIN);
        backToDashboardFromFiltersButton.setBackground(BG_PAGE);
        backToDashboardFromFiltersButton.setFocusPainted(false);
        backToDashboardFromFiltersButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backToDashboardFromFiltersButton.setPreferredSize(new Dimension(170, 40));
        backToDashboardFromFiltersButton.addActionListener(e -> {
            if (onBackToDashboard != null) onBackToDashboard.run();
        });

        buttonRow.add(startPracticeButton);
        buttonRow.add(backToDashboardFromFiltersButton);

        // Assembly
        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 4)));
        card.add(subtitle);
        card.add(Box.createRigidArea(new Dimension(0, 24)));
        card.add(topicLabel);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(topicComboBox);
        card.add(Box.createRigidArea(new Dimension(0, 16)));
        card.add(difficultyLabel);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(difficultyComboBox);
        card.add(Box.createRigidArea(new Dimension(0, 14)));
        card.add(filtersStatusLabel);
        card.add(Box.createRigidArea(new Dimension(0, 18)));
        card.add(buttonRow);

        centerWrapper.add(card);
        panel.add(centerWrapper, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createQuestionCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PAGE);

        // Top Navigation Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_HEADER);
        topBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 56));
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        backToFiltersFromQuestionButton = new JButton("← Change Filters");
        backToFiltersFromQuestionButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        backToFiltersFromQuestionButton.setForeground(Color.WHITE);
        backToFiltersFromQuestionButton.setBackground(new Color(51, 65, 85));
        backToFiltersFromQuestionButton.setFocusPainted(false);
        backToFiltersFromQuestionButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backToFiltersFromQuestionButton.addActionListener(e -> showCard(CARD_FILTERS));
        topBar.add(backToFiltersFromQuestionButton, BorderLayout.WEST);

        JLabel navTitle = new JLabel("ByteForce — Practice Mode");
        navTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        navTitle.setForeground(Color.WHITE);
        navTitle.setHorizontalAlignment(SwingConstants.CENTER);
        topBar.add(navTitle, BorderLayout.CENTER);

        JPanel rightTopActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightTopActions.setBackground(BG_HEADER);

        bookmarkButton = new JButton("☆ Bookmark");
        bookmarkButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        bookmarkButton.setForeground(Color.WHITE);
        bookmarkButton.setBackground(new Color(51, 65, 85));
        bookmarkButton.setFocusPainted(false);
        bookmarkButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bookmarkButton.addActionListener(e -> toggleCurrentQuestionBookmark());

        JButton exitToDashBtn = new JButton("Dashboard");
        exitToDashBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        exitToDashBtn.setForeground(Color.WHITE);
        exitToDashBtn.setBackground(new Color(51, 65, 85));
        exitToDashBtn.setFocusPainted(false);
        exitToDashBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exitToDashBtn.addActionListener(e -> {
            if (onBackToDashboard != null) onBackToDashboard.run();
        });

        rightTopActions.add(bookmarkButton);
        rightTopActions.add(exitToDashBtn);
        topBar.add(rightTopActions, BorderLayout.EAST);

        panel.add(topBar, BorderLayout.NORTH);

        // Scrollable Question Content
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(BG_PAGE);
        mainContent.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        // Question Details Card
        JPanel questionCard = new JPanel();
        questionCard.setLayout(new BoxLayout(questionCard, BoxLayout.Y_AXIS));
        questionCard.setBackground(BG_CARD);
        questionCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(24, 28, 24, 28)
        ));
        questionCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Header Metadata / Badges Row
        JPanel badgesRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        badgesRow.setBackground(BG_CARD);
        badgesRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        progressLabel = createBadge("Question 1 of 1", COLOR_TEXT_MAIN, COLOR_BADGE_BG);
        topicBadgeLabel = createBadge("Topic", COLOR_PRIMARY, new Color(239, 246, 255));
        difficultyBadgeLabel = createBadge("MEDIUM", new Color(180, 83, 9), COLOR_WARNING_BG);
        typeBadgeLabel = createBadge("MCQ", new Color(109, 40, 217), new Color(245, 243, 255));

        badgesRow.add(progressLabel);
        badgesRow.add(topicBadgeLabel);
        badgesRow.add(difficultyBadgeLabel);
        badgesRow.add(typeBadgeLabel);
        questionCard.add(badgesRow);
        questionCard.add(Box.createRigidArea(new Dimension(0, 16)));

        // Question Title
        questionTitleLabel = new JLabel("Question Title");
        questionTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 19));
        questionTitleLabel.setForeground(COLOR_TEXT_MAIN);
        questionTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        questionCard.add(questionTitleLabel);
        questionCard.add(Box.createRigidArea(new Dimension(0, 10)));

        // Question Description / Prompt
        questionDescriptionArea = new JTextArea();
        questionDescriptionArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        questionDescriptionArea.setForeground(COLOR_TEXT_MAIN);
        questionDescriptionArea.setBackground(new Color(248, 250, 252));
        questionDescriptionArea.setLineWrap(true);
        questionDescriptionArea.setWrapStyleWord(true);
        questionDescriptionArea.setEditable(false);
        questionDescriptionArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_INPUT_BORDER, 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        questionDescriptionArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        questionCard.add(questionDescriptionArea);
        questionCard.add(Box.createRigidArea(new Dimension(0, 20)));

        // Dynamic Answer Container
        JLabel answerSectionTitle = new JLabel("Your Answer");
        answerSectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        answerSectionTitle.setForeground(COLOR_TEXT_MAIN);
        answerSectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        questionCard.add(answerSectionTitle);
        questionCard.add(Box.createRigidArea(new Dimension(0, 8)));

        answerContainer = new JPanel();
        answerContainer.setLayout(new BoxLayout(answerContainer, BoxLayout.Y_AXIS));
        answerContainer.setBackground(BG_CARD);
        answerContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        questionCard.add(answerContainer);
        questionCard.add(Box.createRigidArea(new Dimension(0, 14)));

        // Question Status / Error Label
        questionStatusLabel = new JLabel(" ");
        questionStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        questionStatusLabel.setForeground(COLOR_DANGER);
        questionStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        questionCard.add(questionStatusLabel);
        questionCard.add(Box.createRigidArea(new Dimension(0, 14)));

        // Action Buttons Row
        JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        actionsRow.setBackground(BG_CARD);
        actionsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        submitButton = new JButton("Submit Answer");
        submitButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        submitButton.setForeground(Color.WHITE);
        submitButton.setBackground(COLOR_PRIMARY);
        submitButton.setOpaque(true);
        submitButton.setBorderPainted(false);
        submitButton.setFocusPainted(false);
        submitButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        submitButton.setPreferredSize(new Dimension(170, 40));
        submitButton.addActionListener(e -> performSubmit(false));

        skipButton = new JButton("Skip Question");
        skipButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        skipButton.setForeground(COLOR_TEXT_MUTED);
        skipButton.setBackground(BG_CARD);
        skipButton.setFocusPainted(false);
        skipButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_INPUT_BORDER, 1),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        skipButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        skipButton.setPreferredSize(new Dimension(140, 40));
        skipButton.addActionListener(e -> performSubmit(true));

        actionsRow.add(submitButton);
        actionsRow.add(skipButton);
        questionCard.add(actionsRow);

        mainContent.add(questionCard);

        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createResultCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PAGE);

        // Top Navigation Bar
        JPanel topBar = createTopBar("ByteForce — Question Result", () -> showCard(CARD_FILTERS));
        panel.add(topBar, BorderLayout.NORTH);

        // Scrollable Content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG_PAGE);
        content.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));

        // Result Card
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(28, 32, 28, 32)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Result Status Banner
        resultBannerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 12));
        resultBannerPanel.setBackground(COLOR_SUCCESS_BG);
        resultBannerPanel.setBorder(BorderFactory.createLineBorder(COLOR_SUCCESS, 1, true));
        resultBannerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultBannerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        resultBannerLabel = new JLabel("✓ Correct! Solution Verified");
        resultBannerLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        resultBannerLabel.setForeground(COLOR_SUCCESS);
        resultBannerPanel.add(resultBannerLabel);
        card.add(resultBannerPanel);
        card.add(Box.createRigidArea(new Dimension(0, 20)));

        // Question Summary
        resultTitleLabel = new JLabel("Question Title");
        resultTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        resultTitleLabel.setForeground(COLOR_TEXT_MAIN);
        resultTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(resultTitleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 4)));

        resultTopicDifficultyLabel = new JLabel("Topic: - | Difficulty: -");
        resultTopicDifficultyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        resultTopicDifficultyLabel.setForeground(COLOR_TEXT_MUTED);
        resultTopicDifficultyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(resultTopicDifficultyLabel);
        card.add(Box.createRigidArea(new Dimension(0, 20)));

        // Submitted Answer Display
        JLabel yourAnsLabel = new JLabel("Your Submitted Answer");
        yourAnsLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        yourAnsLabel.setForeground(COLOR_TEXT_MAIN);
        yourAnsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(yourAnsLabel);
        card.add(Box.createRigidArea(new Dimension(0, 6)));

        studentAnswerDisplayArea = new JTextArea(3, 40);
        studentAnswerDisplayArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        studentAnswerDisplayArea.setEditable(false);
        studentAnswerDisplayArea.setBackground(new Color(248, 250, 252));
        studentAnswerDisplayArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_INPUT_BORDER, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        studentAnswerDisplayArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(studentAnswerDisplayArea);
        card.add(Box.createRigidArea(new Dimension(0, 16)));

        // Solution Display
        JLabel solutionLabel = new JLabel("Expected Solution / Reference");
        solutionLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        solutionLabel.setForeground(COLOR_TEXT_MAIN);
        solutionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(solutionLabel);
        card.add(Box.createRigidArea(new Dimension(0, 6)));

        solutionDisplayArea = new JTextArea(4, 40);
        solutionDisplayArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        solutionDisplayArea.setEditable(false);
        solutionDisplayArea.setBackground(new Color(248, 250, 252));
        solutionDisplayArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_INPUT_BORDER, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        solutionDisplayArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(solutionDisplayArea);
        card.add(Box.createRigidArea(new Dimension(0, 24)));

        // Next / Navigation Actions
        JPanel resultActionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        resultActionsRow.setBackground(BG_CARD);
        resultActionsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        nextQuestionButton = new JButton("Next Question →");
        nextQuestionButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nextQuestionButton.setForeground(Color.WHITE);
        nextQuestionButton.setBackground(COLOR_PRIMARY);
        nextQuestionButton.setOpaque(true);
        nextQuestionButton.setBorderPainted(false);
        nextQuestionButton.setFocusPainted(false);
        nextQuestionButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        nextQuestionButton.setPreferredSize(new Dimension(170, 40));
        nextQuestionButton.addActionListener(e -> nextQuestion());

        backToFiltersFromResultButton = new JButton("Change Filters");
        backToFiltersFromResultButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        backToFiltersFromResultButton.setForeground(COLOR_TEXT_MAIN);
        backToFiltersFromResultButton.setBackground(BG_PAGE);
        backToFiltersFromResultButton.setFocusPainted(false);
        backToFiltersFromResultButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backToFiltersFromResultButton.setPreferredSize(new Dimension(150, 40));
        backToFiltersFromResultButton.addActionListener(e -> showCard(CARD_FILTERS));

        backToDashboardFromResultButton = new JButton("Back to Dashboard");
        backToDashboardFromResultButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        backToDashboardFromResultButton.setForeground(COLOR_TEXT_MUTED);
        backToDashboardFromResultButton.setBackground(BG_PAGE);
        backToDashboardFromResultButton.setFocusPainted(false);
        backToDashboardFromResultButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backToDashboardFromResultButton.setPreferredSize(new Dimension(160, 40));
        backToDashboardFromResultButton.addActionListener(e -> {
            if (onBackToDashboard != null) onBackToDashboard.run();
        });

        resultActionsRow.add(nextQuestionButton);
        resultActionsRow.add(backToFiltersFromResultButton);
        resultActionsRow.add(backToDashboardFromResultButton);
        card.add(resultActionsRow);

        content.add(card);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createEmptyCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PAGE);

        JPanel topBar = createTopBar("ByteForce — Practice Mode", () -> showCard(CARD_FILTERS));
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
        card.setPreferredSize(new Dimension(500, 280));
        card.setMaximumSize(new Dimension(540, 300));

        emptyStateTitleLabel = new JLabel("No Questions Found");
        emptyStateTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        emptyStateTitleLabel.setForeground(COLOR_TEXT_MAIN);
        emptyStateTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        emptyStateMessageLabel = new JLabel("No questions found for this combination of topic and difficulty.");
        emptyStateMessageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        emptyStateMessageLabel.setForeground(COLOR_TEXT_MUTED);
        emptyStateMessageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        btnRow.setBackground(BG_CARD);
        btnRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        changeFiltersFromEmptyButton = new JButton("Change Filters");
        changeFiltersFromEmptyButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        changeFiltersFromEmptyButton.setForeground(Color.WHITE);
        changeFiltersFromEmptyButton.setBackground(COLOR_PRIMARY);
        changeFiltersFromEmptyButton.setOpaque(true);
        changeFiltersFromEmptyButton.setBorderPainted(false);
        changeFiltersFromEmptyButton.setFocusPainted(false);
        changeFiltersFromEmptyButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        changeFiltersFromEmptyButton.setPreferredSize(new Dimension(150, 38));
        changeFiltersFromEmptyButton.addActionListener(e -> showCard(CARD_FILTERS));

        backToDashboardFromEmptyButton = new JButton("Back to Dashboard");
        backToDashboardFromEmptyButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        backToDashboardFromEmptyButton.setForeground(COLOR_TEXT_MAIN);
        backToDashboardFromEmptyButton.setBackground(BG_PAGE);
        backToDashboardFromEmptyButton.setFocusPainted(false);
        backToDashboardFromEmptyButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backToDashboardFromEmptyButton.setPreferredSize(new Dimension(160, 38));
        backToDashboardFromEmptyButton.addActionListener(e -> {
            if (onBackToDashboard != null) onBackToDashboard.run();
        });

        btnRow.add(changeFiltersFromEmptyButton);
        btnRow.add(backToDashboardFromEmptyButton);

        card.add(emptyStateTitleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(emptyStateMessageLabel);
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

    /**
     * Refreshes the topics dropdown using TopicService on a background thread.
     */
    public void refreshTopics() {
        if (topicService == null) {
            return;
        }

        SwingWorker<List<Topic>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Topic> doInBackground() {
                return topicService.getAllTopics();
            }

            @Override
            protected void done() {
                try {
                    List<Topic> topics = get();
                    topicNameMap.clear();
                    TopicItem selected = (TopicItem) topicComboBox.getSelectedItem();

                    topicComboBox.removeAllItems();
                    topicComboBox.addItem(new TopicItem(null, "All Topics"));

                    for (Topic t : topics) {
                        topicNameMap.put(t.getId(), t.getName());
                        topicComboBox.addItem(new TopicItem(t.getId(), t.getName()));
                    }

                    if (selected != null && selected.getId() != null) {
                        for (int i = 0; i < topicComboBox.getItemCount(); i++) {
                            TopicItem item = topicComboBox.getItemAt(i);
                            if (Objects.equals(item.getId(), selected.getId())) {
                                topicComboBox.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                    filtersStatusLabel.setText(" ");
                } catch (Exception e) {
                    log.error("Unable to load topics", e);
                    filtersStatusLabel.setText("Unable to load topics from database.");
                }
            }
        };
        worker.execute();
    }

    /**
     * Starts the practice session based on selected Topic and Difficulty filters.
     */
    public void startPracticeSession() {
        if (questionService == null) {
            showEmptyState("Question service is currently unavailable.");
            return;
        }

        TopicItem topicItem = (TopicItem) topicComboBox.getSelectedItem();
        DifficultyItem diffItem = (DifficultyItem) difficultyComboBox.getSelectedItem();

        final Long topicId = topicItem != null ? topicItem.getId() : null;
        final Difficulty difficulty = diffItem != null ? diffItem.getDifficulty() : null;

        startPracticeButton.setEnabled(false);
        filtersStatusLabel.setForeground(COLOR_PRIMARY);
        filtersStatusLabel.setText("Loading practice questions...");

        SwingWorker<List<Question>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Question> doInBackground() {
                if (topicId == null && difficulty == null) {
                    return questionService.getAllQuestions();
                } else if (topicId != null && difficulty == null) {
                    return questionService.getQuestionsByTopic(topicId);
                } else if (topicId == null && difficulty != null) {
                    return questionService.getQuestionsByDifficulty(difficulty);
                } else {
                    return questionService.getQuestionsByTopicAndDifficulty(topicId, difficulty);
                }
            }

            @Override
            protected void done() {
                startPracticeButton.setEnabled(true);
                filtersStatusLabel.setText(" ");
                try {
                    List<Question> questions = get();
                    activeQuestions.clear();
                    if (questions != null && !questions.isEmpty()) {
                        activeQuestions.addAll(questions);
                        currentQuestionIndex = 0;
                        renderCurrentQuestion();
                        showCard(CARD_QUESTION);
                    } else {
                        showEmptyState("No questions found for this combination.");
                    }
                } catch (Exception e) {
                    log.error("Unable to load questions for practice session", e);
                    filtersStatusLabel.setForeground(COLOR_DANGER);
                    filtersStatusLabel.setText("Unable to load questions. Please try again.");
                }
            }
        };
        worker.execute();
    }

    /**
     * Renders the current question onto the Question Card.
     */
    private void renderCurrentQuestion() {
        if (activeQuestions.isEmpty() || currentQuestionIndex >= activeQuestions.size()) {
            showEmptyState("No question available.");
            return;
        }

        Question q = activeQuestions.get(currentQuestionIndex);

        // Update Badges
        progressLabel.setText(" Question " + (currentQuestionIndex + 1) + " of " + activeQuestions.size() + " ");
        String topicName = topicNameMap.getOrDefault(q.getTopicId(), "Topic " + q.getTopicId());
        topicBadgeLabel.setText(" " + topicName + " ");

        Difficulty diff = q.getDifficulty();
        difficultyBadgeLabel.setText(" " + diff.name() + " ");
        if (diff == Difficulty.EASY) {
            difficultyBadgeLabel.setForeground(COLOR_SUCCESS);
            difficultyBadgeLabel.setBackground(COLOR_SUCCESS_BG);
        } else if (diff == Difficulty.HARD) {
            difficultyBadgeLabel.setForeground(COLOR_DANGER);
            difficultyBadgeLabel.setBackground(COLOR_DANGER_BG);
        } else {
            difficultyBadgeLabel.setForeground(new Color(180, 83, 9));
            difficultyBadgeLabel.setBackground(COLOR_WARNING_BG);
        }

        QuestionType qType = q.getQuestionType();
        typeBadgeLabel.setText(" " + qType.name() + " ");

        // Title and Description
        questionTitleLabel.setText(q.getTitle());
        questionDescriptionArea.setText(q.getDescription());
        questionStatusLabel.setText(" ");

        // Build Question-Specific Answer Input Area
        buildAnswerAreaForQuestion(q);

        // Update Bookmark Button State
        updateBookmarkStatusForQuestion(q);
    }

    private void buildAnswerAreaForQuestion(Question q) {
        answerContainer.removeAll();
        mcqButtonGroup = new ButtonGroup();
        mcqRadioButtons.clear();
        textAnswerField = null;
        codeOrConceptualArea = null;

        QuestionType qType = q.getQuestionType();

        if (qType == QuestionType.MCQ) {
            List<String> options = parseMcqOptions(q.getDescription());
            if (!options.isEmpty()) {
                for (String opt : options) {
                    JRadioButton rb = new JRadioButton(opt);
                    rb.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    rb.setBackground(BG_CARD);
                    rb.setFocusPainted(false);
                    rb.setAlignmentX(Component.LEFT_ALIGNMENT);
                    mcqButtonGroup.add(rb);
                    mcqRadioButtons.add(rb);
                    answerContainer.add(rb);
                    answerContainer.add(Box.createRigidArea(new Dimension(0, 6)));
                }
            } else {
                // If options are not embedded in description, provide letter options A, B, C, D
                String[] letters = {"A", "B", "C", "D"};
                for (String letter : letters) {
                    JRadioButton rb = new JRadioButton("Option " + letter);
                    rb.setActionCommand(letter);
                    rb.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    rb.setBackground(BG_CARD);
                    rb.setFocusPainted(false);
                    rb.setAlignmentX(Component.LEFT_ALIGNMENT);
                    mcqButtonGroup.add(rb);
                    mcqRadioButtons.add(rb);
                    answerContainer.add(rb);
                    answerContainer.add(Box.createRigidArea(new Dimension(0, 6)));
                }
            }
        } else if (qType == QuestionType.CONCEPTUAL) {
            JLabel prompt = new JLabel("Enter your conceptual explanation / solution key:");
            prompt.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            prompt.setForeground(COLOR_TEXT_MUTED);
            prompt.setAlignmentX(Component.LEFT_ALIGNMENT);
            answerContainer.add(prompt);
            answerContainer.add(Box.createRigidArea(new Dimension(0, 6)));

            codeOrConceptualArea = new JTextArea(4, 40);
            codeOrConceptualArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            codeOrConceptualArea.setLineWrap(true);
            codeOrConceptualArea.setWrapStyleWord(true);
            codeOrConceptualArea.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_INPUT_BORDER, 1),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
            codeOrConceptualArea.setAlignmentX(Component.LEFT_ALIGNMENT);
            answerContainer.add(codeOrConceptualArea);
        } else {
            // CODING or SQL: Explain that automated judge execution is scheduled for future milestone
            JLabel notice = new JLabel("<html><b>Note:</b> Automated execution and judge verification are scheduled for a future milestone.<br>"
                    + "Self-practice mode: enter your solution below to record your attempt and compare against the reference solution.</html>");
            notice.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            notice.setForeground(new Color(133, 77, 14));
            notice.setBackground(COLOR_WARNING_BG);
            notice.setOpaque(true);
            notice.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(253, 230, 138), 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            notice.setAlignmentX(Component.LEFT_ALIGNMENT);
            answerContainer.add(notice);
            answerContainer.add(Box.createRigidArea(new Dimension(0, 8)));

            codeOrConceptualArea = new JTextArea(6, 40);
            codeOrConceptualArea.setFont(new Font("Consolas", Font.PLAIN, 13));
            codeOrConceptualArea.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_INPUT_BORDER, 1),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
            codeOrConceptualArea.setAlignmentX(Component.LEFT_ALIGNMENT);
            answerContainer.add(codeOrConceptualArea);
        }

        answerContainer.revalidate();
        answerContainer.repaint();
    }

    private void updateBookmarkStatusForQuestion(Question q) {
        if (bookmarkService == null || authService == null || !authService.isAuthenticated()) {
            bookmarkButton.setText("☆ Bookmark");
            bookmarkButton.setBackground(new Color(51, 65, 85));
            return;
        }

        Optional<User> userOpt = authService.getCurrentUser();
        if (userOpt.isEmpty()) {
            bookmarkButton.setText("☆ Bookmark");
            return;
        }

        UUID userId = userOpt.get().getId();
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return bookmarkService.isBookmarked(userId, q.getId());
            }

            @Override
            protected void done() {
                try {
                    boolean bookmarked = get();
                    setBookmarkButtonVisualState(bookmarked);
                } catch (Exception e) {
                    log.error("Unable to check bookmark status for question {}", q.getId(), e);
                }
            }
        };
        worker.execute();
    }

    private void setBookmarkButtonVisualState(boolean bookmarked) {
        if (bookmarked) {
            bookmarkButton.setText("★ Bookmarked");
            bookmarkButton.setForeground(COLOR_BOOKMARK_ACTIVE);
            bookmarkButton.setBackground(COLOR_BOOKMARK_BG);
        } else {
            bookmarkButton.setText("☆ Bookmark");
            bookmarkButton.setForeground(Color.WHITE);
            bookmarkButton.setBackground(new Color(51, 65, 85));
        }
    }

    private void toggleCurrentQuestionBookmark() {
        if (bookmarkService == null || authService == null || !authService.isAuthenticated()) {
            questionStatusLabel.setText("You must be signed in to bookmark questions.");
            return;
        }

        if (activeQuestions.isEmpty() || currentQuestionIndex >= activeQuestions.size()) {
            return;
        }

        Optional<User> userOpt = authService.getCurrentUser();
        if (userOpt.isEmpty()) return;

        Question q = activeQuestions.get(currentQuestionIndex);
        UUID userId = userOpt.get().getId();

        bookmarkButton.setEnabled(false);
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return bookmarkService.toggleBookmark(userId, q.getId(), "Practice question bookmark");
            }

            @Override
            protected void done() {
                bookmarkButton.setEnabled(true);
                try {
                    boolean newStatus = get();
                    setBookmarkButtonVisualState(newStatus);
                    log.info("Toggled bookmark for question {}: now {}", q.getId(), newStatus);
                } catch (Exception e) {
                    log.error("Failed to toggle bookmark", e);
                    questionStatusLabel.setText("Unable to update bookmark.");
                }
            }
        };
        worker.execute();
    }

    /**
     * Submits an answer or skips the current question.
     */
    public void performSubmit(boolean isSkipped) {
        if (activeQuestions.isEmpty() || currentQuestionIndex >= activeQuestions.size()) {
            return;
        }

        Question q = activeQuestions.get(currentQuestionIndex);
        String submittedText = "";

        if (!isSkipped) {
            if (q.getQuestionType() == QuestionType.MCQ) {
                for (JRadioButton rb : mcqRadioButtons) {
                    if (rb.isSelected()) {
                        submittedText = rb.getActionCommand() != null && !rb.getActionCommand().isBlank()
                                ? rb.getActionCommand() : rb.getText();
                        break;
                    }
                }
                if (submittedText.isBlank()) {
                    questionStatusLabel.setText("Please select an option to submit.");
                    return;
                }
            } else if (codeOrConceptualArea != null) {
                submittedText = codeOrConceptualArea.getText() != null ? codeOrConceptualArea.getText().trim() : "";
                if (submittedText.isBlank()) {
                    questionStatusLabel.setText("Please enter an answer before submitting (or click Skip).");
                    return;
                }
            }
        }

        submitButton.setEnabled(false);
        skipButton.setEnabled(false);
        questionStatusLabel.setText("Processing submission...");

        final String finalAnswer = submittedText;
        final AttemptStatus status;

        if (isSkipped) {
            status = AttemptStatus.SKIPPED;
        } else {
            status = evaluateAnswer(q, finalAnswer);
        }

        // Record Attempt via AttemptService off EDT
        final UUID userId = authService != null && authService.getCurrentUser().isPresent()
                ? authService.getCurrentUser().get().getId() : null;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                if (attemptService != null && userId != null) {
                    attemptService.recordAttempt(userId, q.getId(), status, finalAnswer, 100);
                }
                return null;
            }

            @Override
            protected void done() {
                submitButton.setEnabled(true);
                skipButton.setEnabled(true);
                questionStatusLabel.setText(" ");
                try {
                    get();
                    renderResultCard(q, finalAnswer, status);
                    showCard(CARD_RESULT);
                } catch (Exception e) {
                    log.error("Failed to record attempt", e);
                    questionStatusLabel.setText("Unable to record attempt. Please try again.");
                }
            }
        };
        worker.execute();
    }

    /**
     * Evaluates correctness using the question's reference solution.
     */
    private AttemptStatus evaluateAnswer(Question q, String studentAnswer) {
        String solution = q.getSolution() != null ? q.getSolution().trim() : "";
        QuestionType type = q.getQuestionType();

        if (type == QuestionType.MCQ) {
            return isMcqCorrect(studentAnswer, solution) ? AttemptStatus.SOLVED : AttemptStatus.FAILED;
        } else if (type == QuestionType.CONCEPTUAL) {
            if (!solution.isBlank() && studentAnswer.equalsIgnoreCase(solution)) {
                return AttemptStatus.SOLVED;
            }
            // Conceptual questions receive ATTEMPTED for comparison against reference solution
            return AttemptStatus.ATTEMPTED;
        } else {
            // CODING / SQL: recorded as ATTEMPTED pending automated judge in future milestone
            return AttemptStatus.ATTEMPTED;
        }
    }

    private boolean isMcqCorrect(String selected, String expected) {
        if (selected == null || expected == null || expected.isBlank()) {
            return false;
        }
        String s = selected.trim().toLowerCase();
        String e = expected.trim().toLowerCase();

        if (s.equals(e)) return true;
        if (e.length() == 1 && (s.startsWith(e + ")") || s.startsWith(e + "."))) return true;
        if (s.contains(e) || e.contains(s)) return true;

        if (s.length() >= 2 && (s.charAt(1) == ')' || s.charAt(1) == '.')) {
            String letter = s.substring(0, 1).trim();
            if (letter.equalsIgnoreCase(e)) return true;
            String textAfterLetter = s.substring(2).trim();
            if (textAfterLetter.equalsIgnoreCase(e) || textAfterLetter.contains(e)) return true;
        }
        return false;
    }

    private void renderResultCard(Question q, String studentAnswer, AttemptStatus status) {
        String topicName = topicNameMap.getOrDefault(q.getTopicId(), "Topic " + q.getTopicId());
        resultTopicDifficultyLabel.setText("Topic: " + topicName + " | Difficulty: " + q.getDifficulty().name()
                + " | Question " + (currentQuestionIndex + 1) + " of " + activeQuestions.size());
        resultTitleLabel.setText(q.getTitle());

        studentAnswerDisplayArea.setText(!studentAnswer.isBlank() ? studentAnswer : "(No answer provided / Skipped)");
        String solution = q.getSolution() != null && !q.getSolution().isBlank()
                ? q.getSolution() : "No reference solution provided for this question.";
        solutionDisplayArea.setText(solution);

        if (status == AttemptStatus.SOLVED) {
            resultBannerPanel.setBackground(COLOR_SUCCESS_BG);
            resultBannerPanel.setBorder(BorderFactory.createLineBorder(COLOR_SUCCESS, 1, true));
            resultBannerLabel.setText("✓ Correct! Question solved successfully.");
            resultBannerLabel.setForeground(COLOR_SUCCESS);
        } else if (status == AttemptStatus.FAILED) {
            resultBannerPanel.setBackground(COLOR_DANGER_BG);
            resultBannerPanel.setBorder(BorderFactory.createLineBorder(COLOR_DANGER, 1, true));
            resultBannerLabel.setText("✗ Incorrect. Review the reference solution below.");
            resultBannerLabel.setForeground(COLOR_DANGER);
        } else if (status == AttemptStatus.SKIPPED) {
            resultBannerPanel.setBackground(COLOR_WARNING_BG);
            resultBannerPanel.setBorder(BorderFactory.createLineBorder(COLOR_WARNING, 1, true));
            resultBannerLabel.setText("↷ Question Skipped. Reference solution displayed below.");
            resultBannerLabel.setForeground(COLOR_WARNING);
        } else {
            resultBannerPanel.setBackground(new Color(239, 246, 255));
            resultBannerPanel.setBorder(BorderFactory.createLineBorder(COLOR_PRIMARY, 1, true));
            resultBannerLabel.setText("ℹ Attempt Recorded. Compare your submission with the reference solution.");
            resultBannerLabel.setForeground(COLOR_PRIMARY);
        }

        // Configure Next button
        if (currentQuestionIndex < activeQuestions.size() - 1) {
            nextQuestionButton.setText("Next Question →");
        } else {
            nextQuestionButton.setText("Finish Practice (Filters)");
        }
    }

    public void nextQuestion() {
        if (currentQuestionIndex < activeQuestions.size() - 1) {
            currentQuestionIndex++;
            renderCurrentQuestion();
            showCard(CARD_QUESTION);
        } else {
            // End of practice session
            showCard(CARD_FILTERS);
            filtersStatusLabel.setForeground(COLOR_SUCCESS);
            filtersStatusLabel.setText("Practice session completed! Select new filters to practice more.");
        }
    }

    public void showEmptyState(String message) {
        emptyStateMessageLabel.setText(message != null ? message : "No questions found for this combination.");
        showCard(CARD_EMPTY);
    }

    public void resetToHome() {
        showCard(CARD_FILTERS);
        refreshTopics();
    }

    public void showCard(String cardName) {
        cardLayout.show(cardsContainer, cardName);
        this.currentCard = cardName;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    public static List<String> parseMcqOptions(String description) {
        if (description == null || description.isBlank()) {
            return List.of();
        }
        List<String> options = new ArrayList<>();
        String[] lines = description.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.matches("(?i)^[A-D][\\.\\)]\\s*.*")) {
                options.add(trimmed);
            }
        }
        return options.size() >= 2 ? options : List.of();
    }

    private String formatDifficultyName(Difficulty diff) {
        if (diff == null) return "All";
        String s = diff.name();
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    // =========================================================================
    // Getters for Testing
    // =========================================================================

    public JComboBox<TopicItem> getTopicComboBox() {
        return topicComboBox;
    }

    public JComboBox<DifficultyItem> getDifficultyComboBox() {
        return difficultyComboBox;
    }

    public JButton getStartPracticeButton() {
        return startPracticeButton;
    }

    public JButton getBackToDashboardFromFiltersButton() {
        return backToDashboardFromFiltersButton;
    }

    public JButton getSubmitButton() {
        return submitButton;
    }

    public JButton getSkipButton() {
        return skipButton;
    }

    public JButton getNextQuestionButton() {
        return nextQuestionButton;
    }

    public JButton getBookmarkButton() {
        return bookmarkButton;
    }

    public String getCurrentCard() {
        return currentCard;
    }

    public JLabel getFiltersStatusLabel() {
        return filtersStatusLabel;
    }

    public JLabel getQuestionTitleLabel() {
        return questionTitleLabel;
    }

    public JLabel getResultBannerLabel() {
        return resultBannerLabel;
    }

    public List<Question> getActiveQuestions() {
        return activeQuestions;
    }

    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public List<JRadioButton> getMcqRadioButtons() {
        return mcqRadioButtons;
    }

    public JTextArea getCodeOrConceptualArea() {
        return codeOrConceptualArea;
    }

    // =========================================================================
    // Supporting Value Objects for Dropdowns
    // =========================================================================

    public static class TopicItem {
        private final Long id;
        private final String name;

        public TopicItem(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TopicItem topicItem = (TopicItem) o;
            return Objects.equals(id, topicItem.id);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(id);
        }
    }

    public static class DifficultyItem {
        private final Difficulty difficulty;
        private final String label;

        public DifficultyItem(Difficulty difficulty, String label) {
            this.difficulty = difficulty;
            this.label = label;
        }

        public Difficulty getDifficulty() {
            return difficulty;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DifficultyItem that = (DifficultyItem) o;
            return difficulty == that.difficulty;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(difficulty);
        }
    }
}
