package com.byteforce.ui;

import com.byteforce.domain.Concept;
import com.byteforce.domain.LearningResource;
import com.byteforce.domain.ResourceType;
import com.byteforce.domain.Subject;
import com.byteforce.domain.Topic;
import com.byteforce.service.LearnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Learn module view for ByteForce placement readiness platform.
 * Supports hierarchical navigation: Subject/Topic List -> Concept List -> Concept Detail with Resources.
 */
public class LearnPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(LearnPanel.class);

    // Card identifiers
    public static final String CARD_SUBJECTS = "LEARN_SUBJECTS";
    public static final String CARD_CONCEPTS = "LEARN_CONCEPTS";
    public static final String CARD_DETAIL = "LEARN_DETAIL";
    public static final String CARD_EMPTY = "LEARN_EMPTY";

    // Design System Color Palette
    private static final Color BG_PAGE = new Color(248, 250, 252);          // Slate-50
    private static final Color BG_HEADER = new Color(15, 23, 42);           // Slate-900
    private static final Color BG_CARD = Color.WHITE;
    private static final Color BORDER_CARD = new Color(226, 232, 240);      // Slate-200
    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);      // Blue-600
    private static final Color COLOR_PRIMARY_HOVER = new Color(29, 78, 216);// Blue-700
    private static final Color COLOR_TEXT_MAIN = new Color(15, 23, 42);     // Slate-900
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139); // Slate-500
    private static final Color COLOR_BADGE_BG = new Color(241, 245, 249);   // Slate-100
    private static final Color COLOR_CODE_BG = new Color(30, 41, 59);       // Slate-800
    private static final Color COLOR_CODE_TEXT = new Color(241, 245, 249);

    // Resource Badge Colors
    private static final Color COLOR_PDF_BG = new Color(254, 242, 242);     // Red-50
    private static final Color COLOR_PDF_TEXT = new Color(220, 38, 38);     // Red-600
    private static final Color COLOR_VIDEO_BG = new Color(243, 232, 255);   // Purple-50
    private static final Color COLOR_VIDEO_TEXT = new Color(147, 51, 234);  // Purple-600
    private static final Color COLOR_ARTICLE_BG = new Color(239, 246, 255); // Blue-50
    private static final Color COLOR_ARTICLE_TEXT = new Color(37, 99, 235); // Blue-600
    private static final Color COLOR_EXT_BG = new Color(241, 245, 249);     // Slate-100
    private static final Color COLOR_EXT_TEXT = new Color(71, 85, 105);     // Slate-600

    private final LearnService learnService;
    private final Runnable onBackToDashboard;

    private final CardLayout cardLayout;
    private final JPanel cardsContainer;
    private String currentCard;

    // Active Selection State
    private Subject selectedSubject;
    private Topic selectedTopic;
    private Concept selectedConcept;
    private final List<Subject> availableSubjects = new ArrayList<>();
    private final List<Concept> activeTopicConcepts = new ArrayList<>();

    // --- Components: SUBJECTS CARD ---
    private JPanel subjectsGridContainer;
    private JTextField subjectsSearchField;
    private JLabel subjectsStatusLabel;

    // --- Components: CONCEPTS CARD ---
    private JLabel conceptsBreadcrumbLabel;
    private JLabel conceptsTopicTitleLabel;
    private JLabel conceptsTopicDescLabel;
    private JPanel conceptsListContainer;
    private JLabel conceptsStatusLabel;
    private JButton backToSubjectsFromConceptsButton;

    // --- Components: DETAIL CARD ---
    private JLabel detailBreadcrumbLabel;
    private JLabel detailConceptTitleLabel;
    private JLabel detailTopicBadge;
    private JTextArea detailExplanationArea;
    private JPanel detailKeyPointsContainer;
    private JTextArea detailExampleArea;
    private JPanel detailResourcesContainer;
    private JButton backToConceptsFromDetailButton;

    // --- Components: EMPTY CARD ---
    private JLabel emptyTitleLabel;
    private JLabel emptyMessageLabel;
    private JButton backToSubjectsFromEmptyButton;

    public LearnPanel() {
        this(null, null);
    }

    public LearnPanel(LearnService learnService, Runnable onBackToDashboard) {
        super(new BorderLayout());
        this.learnService = learnService;
        this.onBackToDashboard = onBackToDashboard;

        this.cardLayout = new CardLayout();
        this.cardsContainer = new JPanel(cardLayout);

        cardsContainer.add(createSubjectsCard(), CARD_SUBJECTS);
        cardsContainer.add(createConceptsCard(), CARD_CONCEPTS);
        cardsContainer.add(createDetailCard(), CARD_DETAIL);
        cardsContainer.add(createEmptyCard(), CARD_EMPTY);

        add(cardsContainer, BorderLayout.CENTER);

        showCard(CARD_SUBJECTS);
        loadSubjects();
    }

    // =========================================================================
    // Card Construction
    // =========================================================================

    private JPanel createSubjectsCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PAGE);

        JPanel topBar = createTopBar("ByteForce — Placement Learning & Concepts", () -> {
            if (onBackToDashboard != null) {
                onBackToDashboard.run();
            }
        });
        panel.add(topBar, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(BG_PAGE);
        body.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        // Subtitle & Search Bar
        JPanel searchBarPanel = new JPanel(new BorderLayout(16, 0));
        searchBarPanel.setBackground(BG_PAGE);
        searchBarPanel.setMaximumSize(new Dimension(1000, 48));
        searchBarPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel introLbl = new JLabel("Curated Computer Science Subjects & Placement Topics");
        introLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        introLbl.setForeground(COLOR_TEXT_MAIN);
        searchBarPanel.add(introLbl, BorderLayout.WEST);

        subjectsSearchField = new JTextField();
        subjectsSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subjectsSearchField.setPreferredSize(new Dimension(280, 36));
        subjectsSearchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        subjectsSearchField.setToolTipText("Search concepts across all subjects...");
        subjectsSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterSubjectsOrSearch(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterSubjectsOrSearch(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterSubjectsOrSearch(); }
        });
        searchBarPanel.add(subjectsSearchField, BorderLayout.EAST);

        body.add(searchBarPanel);
        body.add(Box.createRigidArea(new Dimension(0, 8)));

        subjectsStatusLabel = new JLabel("Explore fundamental placement topics below:");
        subjectsStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subjectsStatusLabel.setForeground(COLOR_TEXT_MUTED);
        subjectsStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(subjectsStatusLabel);
        body.add(Box.createRigidArea(new Dimension(0, 20)));

        // Subjects Grid Container
        subjectsGridContainer = new JPanel();
        subjectsGridContainer.setLayout(new BoxLayout(subjectsGridContainer, BoxLayout.Y_AXIS));
        subjectsGridContainer.setBackground(BG_PAGE);
        subjectsGridContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane scrollPane = new JScrollPane(subjectsGridContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(BG_PAGE);
        scrollPane.getViewport().setBackground(BG_PAGE);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        body.add(scrollPane);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createConceptsCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PAGE);

        // Header Bar with Back Button
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_HEADER);
        topBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 56));
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));

        backToSubjectsFromConceptsButton = createStyledButton("← Back to Subjects", new Color(51, 65, 85), Color.WHITE);
        backToSubjectsFromConceptsButton.addActionListener(e -> {
            showCard(CARD_SUBJECTS);
        });
        topBar.add(backToSubjectsFromConceptsButton, BorderLayout.WEST);

        conceptsBreadcrumbLabel = new JLabel("Subject > Topic");
        conceptsBreadcrumbLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        conceptsBreadcrumbLabel.setForeground(new Color(148, 163, 184));
        topBar.add(conceptsBreadcrumbLabel, BorderLayout.EAST);

        panel.add(topBar, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(BG_PAGE);
        body.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        // Topic Title & Description
        conceptsTopicTitleLabel = new JLabel("Topic Concepts");
        conceptsTopicTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        conceptsTopicTitleLabel.setForeground(COLOR_TEXT_MAIN);
        conceptsTopicTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        conceptsTopicDescLabel = new JLabel("Select a concept below to study theory, key takeaways, and code examples.");
        conceptsTopicDescLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        conceptsTopicDescLabel.setForeground(COLOR_TEXT_MUTED);
        conceptsTopicDescLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        body.add(conceptsTopicTitleLabel);
        body.add(Box.createRigidArea(new Dimension(0, 6)));
        body.add(conceptsTopicDescLabel);
        body.add(Box.createRigidArea(new Dimension(0, 20)));

        conceptsStatusLabel = new JLabel("Loading concepts...");
        conceptsStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        conceptsStatusLabel.setForeground(COLOR_TEXT_MUTED);
        conceptsStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(conceptsStatusLabel);
        body.add(Box.createRigidArea(new Dimension(0, 12)));

        conceptsListContainer = new JPanel();
        conceptsListContainer.setLayout(new BoxLayout(conceptsListContainer, BoxLayout.Y_AXIS));
        conceptsListContainer.setBackground(BG_PAGE);
        conceptsListContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane scrollPane = new JScrollPane(conceptsListContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(BG_PAGE);
        scrollPane.getViewport().setBackground(BG_PAGE);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        body.add(scrollPane);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createDetailCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PAGE);

        // Header Bar with Back Button
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_HEADER);
        topBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 56));
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));

        backToConceptsFromDetailButton = createStyledButton("← Back to Concepts", new Color(51, 65, 85), Color.WHITE);
        backToConceptsFromDetailButton.addActionListener(e -> {
            showCard(CARD_CONCEPTS);
        });
        topBar.add(backToConceptsFromDetailButton, BorderLayout.WEST);

        detailBreadcrumbLabel = new JLabel("Topic > Concept");
        detailBreadcrumbLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        detailBreadcrumbLabel.setForeground(new Color(148, 163, 184));
        topBar.add(detailBreadcrumbLabel, BorderLayout.EAST);

        panel.add(topBar, BorderLayout.NORTH);

        // Detail Body
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(BG_PAGE);
        body.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        // Concept Header Card
        JPanel headerCard = new JPanel();
        headerCard.setLayout(new BoxLayout(headerCard, BoxLayout.Y_AXIS));
        headerCard.setBackground(BG_CARD);
        headerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)
        ));
        headerCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerCard.setMaximumSize(new Dimension(1000, 200));

        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        badgeRow.setBackground(BG_CARD);
        badgeRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        detailTopicBadge = createBadge("Topic: -", COLOR_PRIMARY, new Color(239, 246, 255));
        badgeRow.add(detailTopicBadge);
        headerCard.add(badgeRow);
        headerCard.add(Box.createRigidArea(new Dimension(0, 10)));

        detailConceptTitleLabel = new JLabel("Concept Title");
        detailConceptTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        detailConceptTitleLabel.setForeground(COLOR_TEXT_MAIN);
        detailConceptTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerCard.add(detailConceptTitleLabel);
        headerCard.add(Box.createRigidArea(new Dimension(0, 12)));

        detailExplanationArea = new JTextArea("Concept explanation...");
        detailExplanationArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        detailExplanationArea.setForeground(new Color(51, 65, 85));
        detailExplanationArea.setBackground(BG_CARD);
        detailExplanationArea.setLineWrap(true);
        detailExplanationArea.setWrapStyleWord(true);
        detailExplanationArea.setEditable(false);
        detailExplanationArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerCard.add(detailExplanationArea);

        body.add(headerCard);
        body.add(Box.createRigidArea(new Dimension(0, 20)));

        // Key Points Card
        JPanel keyPointsCard = new JPanel();
        keyPointsCard.setLayout(new BoxLayout(keyPointsCard, BoxLayout.Y_AXIS));
        keyPointsCard.setBackground(BG_CARD);
        keyPointsCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)
        ));
        keyPointsCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        keyPointsCard.setMaximumSize(new Dimension(1000, 300));

        JLabel keyPointsHeading = new JLabel("Key Placement Takeaways");
        keyPointsHeading.setFont(new Font("Segoe UI", Font.BOLD, 16));
        keyPointsHeading.setForeground(COLOR_TEXT_MAIN);
        keyPointsHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        keyPointsCard.add(keyPointsHeading);
        keyPointsCard.add(Box.createRigidArea(new Dimension(0, 12)));

        detailKeyPointsContainer = new JPanel();
        detailKeyPointsContainer.setLayout(new BoxLayout(detailKeyPointsContainer, BoxLayout.Y_AXIS));
        detailKeyPointsContainer.setBackground(BG_CARD);
        detailKeyPointsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        keyPointsCard.add(detailKeyPointsContainer);

        body.add(keyPointsCard);
        body.add(Box.createRigidArea(new Dimension(0, 20)));

        // Example / Code Card
        JPanel exampleCard = new JPanel();
        exampleCard.setLayout(new BoxLayout(exampleCard, BoxLayout.Y_AXIS));
        exampleCard.setBackground(BG_CARD);
        exampleCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)
        ));
        exampleCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        exampleCard.setMaximumSize(new Dimension(1000, 300));

        JLabel exampleHeading = new JLabel("Code & Practical Example");
        exampleHeading.setFont(new Font("Segoe UI", Font.BOLD, 16));
        exampleHeading.setForeground(COLOR_TEXT_MAIN);
        exampleHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        exampleCard.add(exampleHeading);
        exampleCard.add(Box.createRigidArea(new Dimension(0, 12)));

        detailExampleArea = new JTextArea();
        detailExampleArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        detailExampleArea.setForeground(COLOR_CODE_TEXT);
        detailExampleArea.setBackground(COLOR_CODE_BG);
        detailExampleArea.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        detailExampleArea.setLineWrap(false);
        detailExampleArea.setEditable(false);

        JScrollPane exampleScroll = new JScrollPane(detailExampleArea);
        exampleScroll.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85), 1));
        exampleScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        exampleScroll.setPreferredSize(new Dimension(950, 150));
        exampleCard.add(exampleScroll);

        body.add(exampleCard);
        body.add(Box.createRigidArea(new Dimension(0, 20)));

        // Learning Resources Card
        JPanel resourcesCard = new JPanel();
        resourcesCard.setLayout(new BoxLayout(resourcesCard, BoxLayout.Y_AXIS));
        resourcesCard.setBackground(BG_CARD);
        resourcesCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)
        ));
        resourcesCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        resourcesCard.setMaximumSize(new Dimension(1000, 300));

        JLabel resourcesHeading = new JLabel("Curated Learning Resources");
        resourcesHeading.setFont(new Font("Segoe UI", Font.BOLD, 16));
        resourcesHeading.setForeground(COLOR_TEXT_MAIN);
        resourcesHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        resourcesCard.add(resourcesHeading);
        resourcesCard.add(Box.createRigidArea(new Dimension(0, 12)));

        detailResourcesContainer = new JPanel();
        detailResourcesContainer.setLayout(new BoxLayout(detailResourcesContainer, BoxLayout.Y_AXIS));
        detailResourcesContainer.setBackground(BG_CARD);
        detailResourcesContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        resourcesCard.add(detailResourcesContainer);

        body.add(resourcesCard);

        JScrollPane mainScroll = new JScrollPane(body);
        mainScroll.setBorder(BorderFactory.createEmptyBorder());
        mainScroll.setBackground(BG_PAGE);
        mainScroll.getViewport().setBackground(BG_PAGE);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(mainScroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createEmptyCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PAGE);

        JPanel topBar = createTopBar("ByteForce — Learning Module", () -> {
            if (onBackToDashboard != null) {
                onBackToDashboard.run();
            }
        });
        panel.add(topBar, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(BG_PAGE);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(80, 40, 80, 40));

        emptyTitleLabel = new JLabel("No Content Available");
        emptyTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        emptyTitleLabel.setForeground(COLOR_TEXT_MAIN);
        emptyTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        emptyMessageLabel = new JLabel("No learning subjects or concepts match your selection.");
        emptyMessageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        emptyMessageLabel.setForeground(COLOR_TEXT_MUTED);
        emptyMessageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        backToSubjectsFromEmptyButton = createStyledButton("Return to All Subjects", COLOR_PRIMARY, Color.WHITE);
        backToSubjectsFromEmptyButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backToSubjectsFromEmptyButton.addActionListener(e -> {
            showCard(CARD_SUBJECTS);
            loadSubjects();
        });

        centerPanel.add(emptyTitleLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        centerPanel.add(emptyMessageLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 24)));
        centerPanel.add(backToSubjectsFromEmptyButton);

        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTopBar(String title, Runnable onBack) {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_HEADER);
        topBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 56));
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));

        JLabel brandLabel = new JLabel(title);
        brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        brandLabel.setForeground(Color.WHITE);
        topBar.add(brandLabel, BorderLayout.WEST);

        JButton backBtn = createStyledButton("← Back to Dashboard", new Color(51, 65, 85), Color.WHITE);
        backBtn.addActionListener(e -> {
            if (onBack != null) onBack.run();
        });
        topBar.add(backBtn, BorderLayout.EAST);
        return topBar;
    }

    // =========================================================================
    // Data Loading & Screen Transitions
    // =========================================================================

    public void loadSubjects() {
        if (learnService == null) {
            showEmpty("Learning service is currently unavailable.");
            return;
        }

        subjectsStatusLabel.setText("Loading placement subjects...");

        SwingWorker<List<Subject>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Subject> doInBackground() {
                return learnService.getAllSubjects();
            }

            @Override
            protected void done() {
                try {
                    availableSubjects.clear();
                    availableSubjects.addAll(get());
                    renderSubjectsList(availableSubjects);
                    if (availableSubjects.isEmpty()) {
                        showEmpty("No placement learning subjects are currently configured.");
                    } else {
                        subjectsStatusLabel.setText(String.format("Showing %d core CS placement subject(s):", availableSubjects.size()));
                    }
                } catch (Exception e) {
                    log.error("Failed to load subjects", e);
                    showEmpty("Unable to load subjects from repository.");
                }
            }
        };
        worker.execute();
    }

    private void filterSubjectsOrSearch() {
        String query = subjectsSearchField != null ? subjectsSearchField.getText().trim() : "";
        if (query.isEmpty()) {
            renderSubjectsList(availableSubjects);
            subjectsStatusLabel.setText(String.format("Showing %d core CS placement subject(s):", availableSubjects.size()));
            return;
        }

        if (learnService == null) return;

        SwingWorker<List<Concept>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Concept> doInBackground() {
                return learnService.searchConcepts(query);
            }

            @Override
            protected void done() {
                try {
                    List<Concept> searchResults = get();
                    renderSearchResults(query, searchResults);
                } catch (Exception e) {
                    log.error("Search failed", e);
                }
            }
        };
        worker.execute();
    }

    private void renderSubjectsList(List<Subject> subjects) {
        subjectsGridContainer.removeAll();

        for (Subject subject : subjects) {
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(BG_CARD);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_CARD, 1, true),
                    BorderFactory.createEmptyBorder(20, 24, 20, 24)
            ));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.setMaximumSize(new Dimension(1000, 260));

            JLabel titleLbl = new JLabel(subject.getName());
            titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
            titleLbl.setForeground(COLOR_TEXT_MAIN);
            titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel descLbl = new JLabel(subject.getDescription());
            descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            descLbl.setForeground(COLOR_TEXT_MUTED);
            descLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

            card.add(titleLbl);
            card.add(Box.createRigidArea(new Dimension(0, 6)));
            card.add(descLbl);
            card.add(Box.createRigidArea(new Dimension(0, 16)));

            // Topics Row
            JLabel topicsSectionLbl = new JLabel("Topics covered:");
            topicsSectionLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            topicsSectionLbl.setForeground(COLOR_TEXT_MUTED);
            topicsSectionLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(topicsSectionLbl);
            card.add(Box.createRigidArea(new Dimension(0, 8)));

            JPanel topicsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
            topicsRow.setBackground(BG_CARD);
            topicsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

            for (Topic topic : subject.getTopics()) {
                JButton topicBtn = new JButton(topic.getName() + " →");
                topicBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
                topicBtn.setForeground(COLOR_PRIMARY);
                topicBtn.setBackground(new Color(239, 246, 255));
                topicBtn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(191, 219, 254), 1, true),
                        BorderFactory.createEmptyBorder(6, 14, 6, 14)
                ));
                topicBtn.setFocusPainted(false);
                topicBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                topicBtn.addActionListener(e -> selectTopic(subject, topic));
                topicsRow.add(topicBtn);
            }

            card.add(topicsRow);
            subjectsGridContainer.add(card);
            subjectsGridContainer.add(Box.createRigidArea(new Dimension(0, 16)));
        }

        subjectsGridContainer.revalidate();
        subjectsGridContainer.repaint();
    }

    private void renderSearchResults(String query, List<Concept> results) {
        subjectsGridContainer.removeAll();

        subjectsStatusLabel.setText(String.format("Found %d concept(s) matching \"%s\":", results.size(), query));

        if (results.isEmpty()) {
            JLabel noRes = new JLabel("No concepts matched your search query.");
            noRes.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            noRes.setForeground(COLOR_TEXT_MUTED);
            subjectsGridContainer.add(noRes);
        } else {
            for (Concept concept : results) {
                JPanel card = createConceptCard(concept);
                subjectsGridContainer.add(card);
                subjectsGridContainer.add(Box.createRigidArea(new Dimension(0, 12)));
            }
        }

        subjectsGridContainer.revalidate();
        subjectsGridContainer.repaint();
    }

    public void selectTopic(Subject subject, Topic topic) {
        this.selectedSubject = subject;
        this.selectedTopic = topic;

        String subName = subject != null ? subject.getName() : "Subject";
        String topName = topic != null ? topic.getName() : "Topic";
        conceptsBreadcrumbLabel.setText(subName + " > " + topName);
        conceptsTopicTitleLabel.setText(topName);
        conceptsTopicDescLabel.setText(topic != null && topic.getDescription() != null
                ? topic.getDescription()
                : "Fundamental placement interview concepts.");

        showCard(CARD_CONCEPTS);

        if (learnService == null || topic == null) {
            return;
        }

        conceptsStatusLabel.setText("Loading concepts for " + topName + "...");

        SwingWorker<List<Concept>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Concept> doInBackground() {
                return learnService.getConceptsForTopic(topic.getId());
            }

            @Override
            protected void done() {
                try {
                    activeTopicConcepts.clear();
                    activeTopicConcepts.addAll(get());
                    renderConceptsList(activeTopicConcepts);
                    if (activeTopicConcepts.isEmpty()) {
                        conceptsStatusLabel.setText("No concepts currently added under this topic.");
                    } else {
                        conceptsStatusLabel.setText(String.format("Displaying %d concept(s) under %s:", activeTopicConcepts.size(), topName));
                    }
                } catch (Exception e) {
                    log.error("Failed to load concepts for topic {}", topic.getId(), e);
                    conceptsStatusLabel.setText("Error loading concepts.");
                }
            }
        };
        worker.execute();
    }

    private void renderConceptsList(List<Concept> concepts) {
        conceptsListContainer.removeAll();

        for (Concept concept : concepts) {
            JPanel card = createConceptCard(concept);
            conceptsListContainer.add(card);
            conceptsListContainer.add(Box.createRigidArea(new Dimension(0, 14)));
        }

        conceptsListContainer.revalidate();
        conceptsListContainer.repaint();
    }

    private JPanel createConceptCard(Concept concept) {
        JPanel card = new JPanel(new BorderLayout(16, 0));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(1000, 100));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(BG_CARD);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setBackground(BG_CARD);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLbl = new JLabel(concept.getTitle());
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLbl.setForeground(COLOR_TEXT_MAIN);
        titleRow.add(titleLbl);

        if (concept.getTopicName() != null && !concept.getTopicName().isBlank()) {
            JLabel badge = createBadge(concept.getTopicName(), COLOR_TEXT_MUTED, COLOR_BADGE_BG);
            titleRow.add(badge);
        }

        JLabel resBadge = createBadge(concept.getResources().size() + " Resource(s)", new Color(22, 163, 74), new Color(240, 253, 244));
        titleRow.add(resBadge);

        left.add(titleRow);
        left.add(Box.createRigidArea(new Dimension(0, 6)));

        JLabel descLbl = new JLabel(concept.getShortExplanation());
        descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descLbl.setForeground(COLOR_TEXT_MUTED);
        descLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(descLbl);

        card.add(left, BorderLayout.CENTER);

        JButton studyBtn = createStyledButton("Study Concept →", COLOR_PRIMARY, Color.WHITE);
        studyBtn.setPreferredSize(new Dimension(150, 36));
        studyBtn.addActionListener(e -> selectConceptForDetail(concept));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        right.setBackground(BG_CARD);
        right.add(studyBtn);
        card.add(right, BorderLayout.EAST);

        return card;
    }

    public void selectConceptForDetail(Concept concept) {
        this.selectedConcept = concept;

        String topicText = concept.getTopicName() != null && !concept.getTopicName().isBlank()
                ? concept.getTopicName()
                : (selectedTopic != null ? selectedTopic.getName() : "Topic");

        detailBreadcrumbLabel.setText(topicText + " > " + concept.getTitle());
        detailConceptTitleLabel.setText(concept.getTitle());
        detailTopicBadge.setText(" Topic: " + topicText + " ");
        detailExplanationArea.setText(concept.getShortExplanation());

        // Key Points
        detailKeyPointsContainer.removeAll();
        for (String kp : concept.getKeyPoints()) {
            JPanel bulletRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
            bulletRow.setBackground(BG_CARD);
            bulletRow.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel bullet = new JLabel("•");
            bullet.setFont(new Font("Segoe UI", Font.BOLD, 16));
            bullet.setForeground(COLOR_PRIMARY);

            JLabel pointText = new JLabel(kp);
            pointText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            pointText.setForeground(new Color(51, 65, 85));

            bulletRow.add(bullet);
            bulletRow.add(pointText);
            detailKeyPointsContainer.add(bulletRow);
        }

        // Example
        detailExampleArea.setText(concept.getExample() != null && !concept.getExample().isBlank()
                ? concept.getExample()
                : "// No code example provided for this theoretical concept.");
        detailExampleArea.setCaretPosition(0);

        // Resources
        detailResourcesContainer.removeAll();
        if (concept.getResources().isEmpty()) {
            JLabel noRes = new JLabel("No external resources currently linked to this concept.");
            noRes.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            noRes.setForeground(COLOR_TEXT_MUTED);
            detailResourcesContainer.add(noRes);
        } else {
            for (LearningResource res : concept.getResources()) {
                JPanel resCard = new JPanel(new BorderLayout(16, 0));
                resCard.setBackground(BG_PAGE);
                resCard.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_CARD, 1, true),
                        BorderFactory.createEmptyBorder(10, 16, 10, 16)
                ));
                resCard.setAlignmentX(Component.LEFT_ALIGNMENT);
                resCard.setMaximumSize(new Dimension(950, 70));

                JPanel resInfo = new JPanel();
                resInfo.setLayout(new BoxLayout(resInfo, BoxLayout.Y_AXIS));
                resInfo.setBackground(BG_PAGE);

                JPanel typeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
                typeRow.setBackground(BG_PAGE);
                typeRow.setAlignmentX(Component.LEFT_ALIGNMENT);

                JLabel typeBadge = createResourceTypeBadge(res.getResourceType());
                typeRow.add(typeBadge);

                JLabel resTitleLbl = new JLabel(res.getTitle());
                resTitleLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
                resTitleLbl.setForeground(COLOR_TEXT_MAIN);
                typeRow.add(resTitleLbl);

                resInfo.add(typeRow);
                resInfo.add(Box.createRigidArea(new Dimension(0, 4)));

                JLabel resDescLbl = new JLabel(res.getDescription() != null && !res.getDescription().isBlank()
                        ? res.getDescription()
                        : res.getUrl());
                resDescLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                resDescLbl.setForeground(COLOR_TEXT_MUTED);
                resDescLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
                resInfo.add(resDescLbl);

                resCard.add(resInfo, BorderLayout.CENTER);

                JButton openBtn = createStyledButton("Open Resource ↗", new Color(15, 23, 42), Color.WHITE);
                openBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
                openBtn.setPreferredSize(new Dimension(135, 30));
                openBtn.addActionListener(e -> openResourceExternal(res.getUrl()));

                JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
                btnWrap.setBackground(BG_PAGE);
                btnWrap.add(openBtn);
                resCard.add(btnWrap, BorderLayout.EAST);

                detailResourcesContainer.add(resCard);
                detailResourcesContainer.add(Box.createRigidArea(new Dimension(0, 8)));
            }
        }

        detailKeyPointsContainer.revalidate();
        detailKeyPointsContainer.repaint();
        detailResourcesContainer.revalidate();
        detailResourcesContainer.repaint();

        showCard(CARD_DETAIL);
    }

    public void openResourceExternal(String url) {
        log.info("Requesting to open external resource URL: {}", url);
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) {
                log.error("Unable to open resource URL in browser: {}", url, ex);
                JOptionPane.showMessageDialog(this,
                        "Could not open browser for link:\n" + url,
                        "Open Resource",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            log.info("Desktop browsing not supported in this runtime environment.");
            JOptionPane.showMessageDialog(this,
                    "External Resource Link:\n" + url,
                    "Learning Resource",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void showEmpty(String message) {
        emptyMessageLabel.setText(message != null ? message : "No learning content available.");
        showCard(CARD_EMPTY);
    }

    public void resetToHome() {
        if (subjectsSearchField != null) {
            subjectsSearchField.setText("");
        }
        selectedSubject = null;
        selectedTopic = null;
        selectedConcept = null;
        showCard(CARD_SUBJECTS);
        loadSubjects();
    }

    public void showCard(String cardName) {
        cardLayout.show(cardsContainer, cardName);
        this.currentCard = cardName;
    }

    // =========================================================================
    // UI Helpers & Styling
    // =========================================================================

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        return btn;
    }

    private JLabel createBadge(String text, Color fg, Color bg) {
        JLabel badge = new JLabel(" " + text + " ");
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setForeground(fg);
        badge.setBackground(bg);
        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        return badge;
    }

    private JLabel createResourceTypeBadge(ResourceType type) {
        Color bg = switch (type) {
            case PDF -> COLOR_PDF_BG;
            case VIDEO -> COLOR_VIDEO_BG;
            case ARTICLE -> COLOR_ARTICLE_BG;
            case EXTERNAL -> COLOR_EXT_BG;
        };
        Color fg = switch (type) {
            case PDF -> COLOR_PDF_TEXT;
            case VIDEO -> COLOR_VIDEO_TEXT;
            case ARTICLE -> COLOR_ARTICLE_TEXT;
            case EXTERNAL -> COLOR_EXT_TEXT;
        };
        return createBadge(type.name(), fg, bg);
    }

    // =========================================================================
    // Getters for Testing
    // =========================================================================

    public String getCurrentCard() {
        return currentCard;
    }

    public List<Subject> getAvailableSubjects() {
        return availableSubjects;
    }

    public List<Concept> getActiveTopicConcepts() {
        return activeTopicConcepts;
    }

    public Subject getSelectedSubject() {
        return selectedSubject;
    }

    public Topic getSelectedTopic() {
        return selectedTopic;
    }

    public Concept getSelectedConcept() {
        return selectedConcept;
    }

    public JTextField getSubjectsSearchField() {
        return subjectsSearchField;
    }

    public JLabel getSubjectsStatusLabel() {
        return subjectsStatusLabel;
    }

    public JLabel getConceptsTopicTitleLabel() {
        return conceptsTopicTitleLabel;
    }

    public JLabel getConceptsBreadcrumbLabel() {
        return conceptsBreadcrumbLabel;
    }

    public JLabel getDetailConceptTitleLabel() {
        return detailConceptTitleLabel;
    }

    public JTextArea getDetailExplanationArea() {
        return detailExplanationArea;
    }

    public JTextArea getDetailExampleArea() {
        return detailExampleArea;
    }

    public JLabel getEmptyTitleLabel() {
        return emptyTitleLabel;
    }

    public JLabel getEmptyMessageLabel() {
        return emptyMessageLabel;
    }

    public JButton getBackToSubjectsFromConceptsButton() {
        return backToSubjectsFromConceptsButton;
    }

    public JButton getBackToConceptsFromDetailButton() {
        return backToConceptsFromDetailButton;
    }

    public JButton getBackToSubjectsFromEmptyButton() {
        return backToSubjectsFromEmptyButton;
    }
}
