package com.byteforce.ui;

import com.byteforce.domain.Activity;
import com.byteforce.domain.AssessmentAttemptSummary;
import com.byteforce.domain.StudentProgressSummary;
import com.byteforce.domain.TopicPerformance;
import com.byteforce.domain.User;
import com.byteforce.service.AuthService;
import com.byteforce.service.TrackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Student progress and analytics view for ByteForce.
 * Displays overall preparation metrics, topic-level performance,
 * weak areas, assessment history, and activity timeline.
 */
public class TrackPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(TrackPanel.class);

    // Design System Color Palette
    private static final Color BG_PAGE = new Color(248, 250, 252);          // Slate-50
    private static final Color BG_HEADER = new Color(15, 23, 42);           // Slate-900
    private static final Color BG_CARD = Color.WHITE;
    private static final Color BORDER_CARD = new Color(226, 232, 240);      // Slate-200
    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);      // Blue-600
    private static final Color COLOR_TEXT_MAIN = new Color(15, 23, 42);     // Slate-900
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139); // Slate-500

    private static final Color COLOR_SUCCESS = new Color(22, 163, 74);      // Green-600
    private static final Color COLOR_SUCCESS_BG = new Color(240, 253, 244); // Green-50
    private static final Color COLOR_DANGER = new Color(220, 38, 38);       // Red-600
    private static final Color COLOR_DANGER_BG = new Color(254, 242, 242);  // Red-50
    private static final Color COLOR_WARNING = new Color(217, 119, 6);      // Amber-600
    private static final Color COLOR_WARNING_BG = new Color(254, 252, 232); // Amber-50
    private static final Color COLOR_INFO = new Color(79, 70, 229);         // Indigo-600
    private static final Color COLOR_PURPLE = new Color(147, 51, 234);      // Purple-600

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final TrackService trackService;
    private final AuthService authService;
    private final Runnable onBackToDashboard;

    // Header buttons
    private final JButton backButton;
    private final JButton refreshButton;

    // Overview metric labels
    private final JLabel attemptedValueLabel;
    private final JLabel solvedValueLabel;
    private final JLabel failedValueLabel;
    private final JLabel skippedValueLabel;
    private final JLabel accuracyValueLabel;
    private final JLabel assessmentsValueLabel;
    private final JLabel bookmarksValueLabel;

    // Container panels for dynamic sections
    private final JPanel topicCardsContainer;
    private final JLabel emptyTopicLabel;

    private final JPanel weakAreasContainer;
    private final JLabel weakAreasEmptyLabel;

    private final JPanel assessmentHistoryContainer;
    private final JLabel emptyAssessmentLabel;

    private final JPanel activityHistoryContainer;
    private final JLabel emptyActivityLabel;

    private final JLabel learnStatusLabel;

    // Cached current data
    private TrackData currentData;

    public record TrackData(
            StudentProgressSummary summary,
            List<TopicPerformance> topicPerformances,
            List<TopicPerformance> weakAreas,
            List<AssessmentAttemptSummary> assessmentHistory,
            List<Activity> recentActivities
    ) {}

    public TrackPanel() {
        this(null, null, null);
    }

    public TrackPanel(TrackService trackService, AuthService authService, Runnable onBackToDashboard) {
        super(new BorderLayout());
        this.trackService = trackService;
        this.authService = authService;
        this.onBackToDashboard = onBackToDashboard;

        setBackground(BG_PAGE);

        // 1. Top Navigation Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_HEADER);
        topBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 60));
        topBar.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));

        JPanel leftNavPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftNavPanel.setOpaque(false);

        backButton = new JButton("← Back to Dashboard");
        backButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        backButton.setForeground(Color.WHITE);
        backButton.setBackground(new Color(30, 41, 59));
        backButton.setFocusPainted(false);
        backButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> {
            if (onBackToDashboard != null) {
                onBackToDashboard.run();
            }
        });
        leftNavPanel.add(backButton);

        JLabel titleLabel = new JLabel("ByteForce — Track & Progress");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        leftNavPanel.add(titleLabel);

        topBar.add(leftNavPanel, BorderLayout.WEST);

        refreshButton = new JButton("Refresh");
        refreshButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setBackground(COLOR_PRIMARY);
        refreshButton.setFocusPainted(false);
        refreshButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(29, 78, 216), 1),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));
        refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshButton.addActionListener(e -> refresh());
        topBar.add(refreshButton, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // 2. Main Scrollable Content
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(BG_PAGE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(24, 32, 32, 32));

        // --- Section A: Overall Preparation Overview ---
        JPanel overviewCard = createCardPanel("Overall Preparation Overview",
                "Real-time summary of your practice problem solving and test performance");

        JPanel metricsGrid = new JPanel(new GridLayout(1, 7, 12, 0));
        metricsGrid.setOpaque(false);
        metricsGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        metricsGrid.setMaximumSize(new Dimension(1000, 80));

        attemptedValueLabel = new JLabel("0");
        solvedValueLabel = new JLabel("0");
        failedValueLabel = new JLabel("0");
        skippedValueLabel = new JLabel("0");
        accuracyValueLabel = new JLabel("0.0%");
        assessmentsValueLabel = new JLabel("0");
        bookmarksValueLabel = new JLabel("0");

        metricsGrid.add(createMetricBadge("ATTEMPTED", attemptedValueLabel, COLOR_TEXT_MAIN));
        metricsGrid.add(createMetricBadge("SOLVED", solvedValueLabel, COLOR_SUCCESS));
        metricsGrid.add(createMetricBadge("FAILED", failedValueLabel, COLOR_DANGER));
        metricsGrid.add(createMetricBadge("SKIPPED", skippedValueLabel, COLOR_WARNING));
        metricsGrid.add(createMetricBadge("ACCURACY", accuracyValueLabel, COLOR_PRIMARY));
        metricsGrid.add(createMetricBadge("ASSESSMENTS", assessmentsValueLabel, COLOR_INFO));
        metricsGrid.add(createMetricBadge("BOOKMARKS", bookmarksValueLabel, COLOR_PURPLE));

        overviewCard.add(metricsGrid);
        contentPanel.add(overviewCard);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // --- Section B: Practice Performance by Topic ---
        JPanel topicCard = createCardPanel("Practice Performance by Topic",
                "Breakdown of questions attempted and accuracy per subject area");

        topicCardsContainer = new JPanel();
        topicCardsContainer.setLayout(new BoxLayout(topicCardsContainer, BoxLayout.Y_AXIS));
        topicCardsContainer.setOpaque(false);
        topicCardsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        emptyTopicLabel = new JLabel("No practice attempts recorded yet. Practice questions to see topic-level performance.");
        emptyTopicLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        emptyTopicLabel.setForeground(COLOR_TEXT_MUTED);
        topicCardsContainer.add(emptyTopicLabel);

        topicCard.add(topicCardsContainer);
        contentPanel.add(topicCard);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // --- Section C: Areas to Improve ---
        JPanel weakAreasCard = createCardPanel("Areas to Improve",
                "Topics with lower practice performance where you need more practice");

        weakAreasContainer = new JPanel();
        weakAreasContainer.setLayout(new BoxLayout(weakAreasContainer, BoxLayout.Y_AXIS));
        weakAreasContainer.setOpaque(false);
        weakAreasContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        weakAreasEmptyLabel = new JLabel("Complete more practice to identify your weak areas.");
        weakAreasEmptyLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        weakAreasEmptyLabel.setForeground(COLOR_TEXT_MUTED);
        weakAreasContainer.add(weakAreasEmptyLabel);

        weakAreasCard.add(weakAreasContainer);
        contentPanel.add(weakAreasCard);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // --- Section D: Assessment History ---
        JPanel assessmentCard = createCardPanel("Assessment History",
                "Recent completed placement mock test attempts");

        assessmentHistoryContainer = new JPanel();
        assessmentHistoryContainer.setLayout(new BoxLayout(assessmentHistoryContainer, BoxLayout.Y_AXIS));
        assessmentHistoryContainer.setOpaque(false);
        assessmentHistoryContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        emptyAssessmentLabel = new JLabel("No assessments completed yet.");
        emptyAssessmentLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        emptyAssessmentLabel.setForeground(COLOR_TEXT_MUTED);
        assessmentHistoryContainer.add(emptyAssessmentLabel);

        assessmentCard.add(assessmentHistoryContainer);
        contentPanel.add(assessmentCard);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // --- Section E: Recent Activity Timeline ---
        JPanel activityCard = createCardPanel("Recent Activity History",
                "Real activity log of questions, assessments, and logins");

        activityHistoryContainer = new JPanel();
        activityHistoryContainer.setLayout(new BoxLayout(activityHistoryContainer, BoxLayout.Y_AXIS));
        activityHistoryContainer.setOpaque(false);
        activityHistoryContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        emptyActivityLabel = new JLabel("No recent activity recorded.");
        emptyActivityLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        emptyActivityLabel.setForeground(COLOR_TEXT_MUTED);
        activityHistoryContainer.add(emptyActivityLabel);

        activityCard.add(activityHistoryContainer);
        contentPanel.add(activityCard);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // --- Section F: Concept Learning Integration Notice ---
        JPanel learnCard = createCardPanel("Concept Learning Status",
                "Conceptual study progress");

        learnStatusLabel = new JLabel("Learning progress tracking will appear here as you study concepts.");
        learnStatusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        learnStatusLabel.setForeground(COLOR_TEXT_MUTED);
        learnCard.add(learnStatusLabel);

        contentPanel.add(learnCard);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        renderEmptyState();
    }

    private JPanel createCardPanel(String title, String subtitle) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(1000, 3200));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titleLabel.setForeground(COLOR_TEXT_MAIN);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subLabel = new JLabel(subtitle);
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLabel.setForeground(COLOR_TEXT_MUTED);
        subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(titleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 4)));
        card.add(subLabel);
        card.add(Box.createRigidArea(new Dimension(0, 16)));

        return card;
    }

    private JPanel createMetricBadge(String label, JLabel valueLabel, Color valueColor) {
        JPanel badge = new JPanel();
        badge.setLayout(new BoxLayout(badge, BoxLayout.Y_AXIS));
        badge.setBackground(new Color(248, 250, 252));
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(12, 10, 12, 10)
        ));

        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(COLOR_TEXT_MUTED);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueLabel.setForeground(valueColor);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        badge.add(lbl);
        badge.add(Box.createRigidArea(new Dimension(0, 6)));
        badge.add(valueLabel);
        return badge;
    }

    /**
     * Refreshes the track data asynchronously via SwingWorker.
     */
    public void refresh() {
        if (authService == null || !authService.isAuthenticated()) {
            renderEmptyState();
            return;
        }

        Optional<User> userOpt = authService.getCurrentUser();
        if (userOpt.isEmpty() || trackService == null) {
            renderEmptyState();
            return;
        }

        User user = userOpt.get();

        SwingWorker<TrackData, Void> worker = new SwingWorker<>() {
            @Override
            protected TrackData doInBackground() {
                StudentProgressSummary summary = trackService.getOverallProgress(user.getId());
                List<TopicPerformance> topicPerformances = trackService.getTopicPerformances(user.getId());
                List<TopicPerformance> weakAreas = trackService.getWeakAreas(user.getId());
                List<AssessmentAttemptSummary> assessmentHistory = trackService.getAssessmentHistory(user.getId());
                List<Activity> activities = trackService.getRecentActivities(user.getId(), 10);

                return new TrackData(summary, topicPerformances, weakAreas, assessmentHistory, activities);
            }

            @Override
            protected void done() {
                try {
                    TrackData data = get();
                    loadDataDirectly(data);
                } catch (Exception e) {
                    log.error("Failed to load tracking data for user {}", user.getId(), e);
                    renderEmptyState();
                }
            }
        };

        worker.execute();
    }

    /**
     * Synchronously fetches and loads tracking data on the current thread.
     * Useful for deterministic testing and immediate updates.
     */
    public void refreshSync() {
        if (authService == null || !authService.isAuthenticated()) {
            renderEmptyState();
            return;
        }

        Optional<User> userOpt = authService.getCurrentUser();
        if (userOpt.isEmpty() || trackService == null) {
            renderEmptyState();
            return;
        }

        User user = userOpt.get();
        StudentProgressSummary summary = trackService.getOverallProgress(user.getId());
        List<TopicPerformance> topicPerformances = trackService.getTopicPerformances(user.getId());
        List<TopicPerformance> weakAreas = trackService.getWeakAreas(user.getId());
        List<AssessmentAttemptSummary> assessmentHistory = trackService.getAssessmentHistory(user.getId());
        List<Activity> activities = trackService.getRecentActivities(user.getId(), 10);

        loadDataDirectly(new TrackData(summary, topicPerformances, weakAreas, assessmentHistory, activities));
    }

    /**
     * Direct synchronous load for immediate rendering and testing.
     */
    public void loadDataDirectly(TrackData data) {
        this.currentData = data;
        if (data == null || data.summary() == null) {
            renderEmptyState();
            return;
        }

        StudentProgressSummary summary = data.summary();
        attemptedValueLabel.setText(String.valueOf(summary.totalAttempts()));
        solvedValueLabel.setText(String.valueOf(summary.solvedCount()));
        failedValueLabel.setText(String.valueOf(summary.failedCount()));
        skippedValueLabel.setText(String.valueOf(summary.skippedCount()));
        accuracyValueLabel.setText(String.format("%.1f%%", summary.accuracyPercentage()));
        assessmentsValueLabel.setText(String.valueOf(summary.assessmentsCompleted()));
        bookmarksValueLabel.setText(String.valueOf(summary.bookmarkedCount()));

        renderTopicPerformances(data.topicPerformances());
        renderWeakAreas(data.weakAreas());
        renderAssessmentHistory(data.assessmentHistory());
        renderRecentActivities(data.recentActivities());

        revalidate();
        repaint();
    }

    private void renderEmptyState() {
        attemptedValueLabel.setText("0");
        solvedValueLabel.setText("0");
        failedValueLabel.setText("0");
        skippedValueLabel.setText("0");
        accuracyValueLabel.setText("0.0%");
        assessmentsValueLabel.setText("0");
        bookmarksValueLabel.setText("0");

        topicCardsContainer.removeAll();
        topicCardsContainer.add(emptyTopicLabel);

        weakAreasContainer.removeAll();
        weakAreasContainer.add(weakAreasEmptyLabel);

        assessmentHistoryContainer.removeAll();
        assessmentHistoryContainer.add(emptyAssessmentLabel);

        activityHistoryContainer.removeAll();
        activityHistoryContainer.add(emptyActivityLabel);

        revalidate();
        repaint();
    }

    private void renderTopicPerformances(List<TopicPerformance> performances) {
        topicCardsContainer.removeAll();

        if (performances == null || performances.isEmpty()) {
            topicCardsContainer.add(emptyTopicLabel);
            return;
        }

        for (TopicPerformance tp : performances) {
            JPanel row = new JPanel(new BorderLayout(16, 0));
            row.setBackground(BG_CARD);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_CARD),
                    BorderFactory.createEmptyBorder(10, 8, 10, 8)
            ));
            row.setMaximumSize(new Dimension(960, 48));

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            left.setOpaque(false);

            JLabel nameLabel = new JLabel(tp.topicName());
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            nameLabel.setForeground(COLOR_TEXT_MAIN);
            nameLabel.setPreferredSize(new Dimension(200, 24));
            left.add(nameLabel);

            JLabel countLabel = new JLabel(String.format("%d / %d solved (%d failed, %d skipped)",
                    tp.solvedCount(), tp.totalAttempts(), tp.failedCount(), tp.skippedCount()));
            countLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            countLabel.setForeground(COLOR_TEXT_MUTED);
            left.add(countLabel);

            row.add(left, BorderLayout.WEST);

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
            right.setOpaque(false);

            JProgressBar progressBar = new JProgressBar(0, 100);
            progressBar.setValue((int) Math.round(tp.accuracyPercentage()));
            progressBar.setPreferredSize(new Dimension(140, 12));
            progressBar.setForeground(getAccuracyColor(tp.accuracyPercentage()));
            progressBar.setBackground(new Color(241, 245, 249));
            progressBar.setBorderPainted(false);
            right.add(progressBar);

            JLabel pctLabel = new JLabel(String.format("%5.1f%%", tp.accuracyPercentage()));
            pctLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            pctLabel.setForeground(getAccuracyColor(tp.accuracyPercentage()));
            right.add(pctLabel);

            row.add(right, BorderLayout.EAST);
            topicCardsContainer.add(row);
        }
    }

    private void renderWeakAreas(List<TopicPerformance> weakTopics) {
        weakAreasContainer.removeAll();

        if (weakTopics == null || weakTopics.isEmpty()) {
            weakAreasContainer.add(weakAreasEmptyLabel);
            return;
        }

        for (TopicPerformance tp : weakTopics) {
            JPanel card = new JPanel(new BorderLayout(16, 0));
            card.setBackground(COLOR_WARNING_BG);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(253, 230, 138), 1, true),
                    BorderFactory.createEmptyBorder(10, 14, 10, 14)
            ));
            card.setMaximumSize(new Dimension(960, 50));

            JLabel titleLabel = new JLabel("⚠️ " + tp.topicName() + " — " + String.format("%.1f%%", tp.accuracyPercentage()) + " accuracy");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            titleLabel.setForeground(new Color(146, 64, 14));

            JLabel actionLabel = new JLabel(String.format("%d of %d solved. Needs practice.", tp.solvedCount(), tp.totalAttempts()));
            actionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            actionLabel.setForeground(new Color(180, 83, 9));

            card.add(titleLabel, BorderLayout.WEST);
            card.add(actionLabel, BorderLayout.EAST);

            weakAreasContainer.add(card);
            weakAreasContainer.add(Box.createRigidArea(new Dimension(0, 6)));
        }
    }

    private void renderAssessmentHistory(List<AssessmentAttemptSummary> summaries) {
        assessmentHistoryContainer.removeAll();

        if (summaries == null || summaries.isEmpty()) {
            assessmentHistoryContainer.add(emptyAssessmentLabel);
            return;
        }

        for (AssessmentAttemptSummary a : summaries) {
            JPanel row = new JPanel(new BorderLayout(16, 0));
            row.setBackground(BG_CARD);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_CARD),
                    BorderFactory.createEmptyBorder(10, 8, 10, 8)
            ));
            row.setMaximumSize(new Dimension(960, 48));

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            left.setOpaque(false);

            JLabel titleLabel = new JLabel(a.assessmentTitle());
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            titleLabel.setForeground(COLOR_TEXT_MAIN);
            titleLabel.setPreferredSize(new Dimension(280, 24));
            left.add(titleLabel);

            String timeStr = a.completedAt() != null ? DATE_FORMATTER.format(a.completedAt()) : "-";
            JLabel dateLabel = new JLabel("Completed: " + timeStr);
            dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            dateLabel.setForeground(COLOR_TEXT_MUTED);
            left.add(dateLabel);

            row.add(left, BorderLayout.WEST);

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
            right.setOpaque(false);

            JLabel scoreLabel = new JLabel(String.format("Score: %d / %d (%.1f%%)", a.score(), a.totalMarks(), a.percentage()));
            scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            scoreLabel.setForeground(COLOR_PRIMARY);
            right.add(scoreLabel);

            JLabel statusLabel = new JLabel(a.status());
            statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
            statusLabel.setForeground(COLOR_SUCCESS);
            statusLabel.setBackground(COLOR_SUCCESS_BG);
            statusLabel.setOpaque(true);
            statusLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(187, 247, 208), 1),
                    BorderFactory.createEmptyBorder(2, 8, 2, 8)
            ));
            right.add(statusLabel);

            row.add(right, BorderLayout.EAST);
            assessmentHistoryContainer.add(row);
        }
    }

    private void renderRecentActivities(List<Activity> activities) {
        activityHistoryContainer.removeAll();

        if (activities == null || activities.isEmpty()) {
            activityHistoryContainer.add(emptyActivityLabel);
            return;
        }

        for (Activity act : activities) {
            JPanel row = new JPanel(new BorderLayout(16, 0));
            row.setBackground(BG_CARD);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_CARD),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)
            ));
            row.setMaximumSize(new Dimension(960, 42));

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            left.setOpaque(false);

            JLabel typeBadge = new JLabel(act.getActivityType().name());
            typeBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
            typeBadge.setForeground(COLOR_PRIMARY);
            typeBadge.setBackground(new Color(239, 246, 255));
            typeBadge.setOpaque(true);
            typeBadge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(191, 219, 254), 1),
                    BorderFactory.createEmptyBorder(2, 6, 2, 6)
            ));
            left.add(typeBadge);

            JLabel descLabel = new JLabel(act.getDescription() != null ? act.getDescription() : "");
            descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            descLabel.setForeground(COLOR_TEXT_MAIN);
            left.add(descLabel);

            row.add(left, BorderLayout.WEST);

            String timeStr = act.getCreatedAt() != null ? DATE_FORMATTER.format(act.getCreatedAt()) : "-";
            JLabel timeLabel = new JLabel(timeStr);
            timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            timeLabel.setForeground(COLOR_TEXT_MUTED);
            row.add(timeLabel, BorderLayout.EAST);

            activityHistoryContainer.add(row);
        }
    }

    private Color getAccuracyColor(double accuracy) {
        if (accuracy >= 75.0) return COLOR_SUCCESS;
        if (accuracy >= 50.0) return COLOR_WARNING;
        return COLOR_DANGER;
    }

    // Accessors for testing and integration
    public JButton getBackButton() {
        return backButton;
    }

    public JButton getRefreshButton() {
        return refreshButton;
    }

    public JLabel getAttemptedValueLabel() {
        return attemptedValueLabel;
    }

    public JLabel getSolvedValueLabel() {
        return solvedValueLabel;
    }

    public JLabel getFailedValueLabel() {
        return failedValueLabel;
    }

    public JLabel getSkippedValueLabel() {
        return skippedValueLabel;
    }

    public JLabel getAccuracyValueLabel() {
        return accuracyValueLabel;
    }

    public JLabel getAssessmentsValueLabel() {
        return assessmentsValueLabel;
    }

    public JLabel getBookmarksValueLabel() {
        return bookmarksValueLabel;
    }

    public JPanel getTopicCardsContainer() {
        return topicCardsContainer;
    }

    public JLabel getEmptyTopicLabel() {
        return emptyTopicLabel;
    }

    public JPanel getWeakAreasContainer() {
        return weakAreasContainer;
    }

    public JLabel getWeakAreasEmptyLabel() {
        return weakAreasEmptyLabel;
    }

    public JPanel getAssessmentHistoryContainer() {
        return assessmentHistoryContainer;
    }

    public JLabel getEmptyAssessmentLabel() {
        return emptyAssessmentLabel;
    }

    public JPanel getActivityHistoryContainer() {
        return activityHistoryContainer;
    }

    public JLabel getEmptyActivityLabel() {
        return emptyActivityLabel;
    }

    public JLabel getLearnStatusLabel() {
        return learnStatusLabel;
    }

    public TrackData getCurrentData() {
        return currentData;
    }
}
