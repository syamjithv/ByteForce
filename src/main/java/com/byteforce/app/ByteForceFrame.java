package com.byteforce.app;

import com.byteforce.service.AssessmentService;
import com.byteforce.service.AttemptService;
import com.byteforce.service.AuthService;
import com.byteforce.service.BookmarkService;
import com.byteforce.service.LearnService;
import com.byteforce.service.QuestionService;
import com.byteforce.service.TopicService;
import com.byteforce.service.TrackService;
import com.byteforce.ui.AssessmentPanel;
import com.byteforce.ui.DashboardPanel;
import com.byteforce.ui.LearnPanel;
import com.byteforce.ui.LoginPanel;
import com.byteforce.ui.PracticePanel;
import com.byteforce.ui.TrackPanel;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;

/**
 * Main application window and shell for ByteForce.
 * Manages top-level views (Login, Dashboard, Practice, Assessments) using CardLayout.
 */
public class ByteForceFrame extends JFrame {

    public static final String VIEW_LOGIN = "LOGIN";
    public static final String VIEW_DASHBOARD = "DASHBOARD";
    public static final String VIEW_PRACTICE = "PRACTICE";
    public static final String VIEW_ASSESSMENTS = "ASSESSMENTS";
    public static final String VIEW_LEARN = "LEARN";
    public static final String VIEW_TRACK = "TRACK";

    private final CardLayout cardLayout;
    private final JPanel cardsPanel;
    private final LoginPanel loginPanel;
    private final DashboardPanel dashboardPanel;
    private final PracticePanel practicePanel;
    private final AssessmentPanel assessmentPanel;
    private final LearnPanel learnPanel;
    private final TrackPanel trackPanel;
    private String currentView;

    public ByteForceFrame() {
        this((AppContext) null);
    }

    public ByteForceFrame(AppContext appContext) {
        this(
                appContext != null ? appContext.getAuthService() : null,
                appContext != null ? appContext.getTopicService() : null,
                appContext != null ? appContext.getQuestionService() : null,
                appContext != null ? appContext.getAttemptService() : null,
                appContext != null ? appContext.getBookmarkService() : null,
                appContext != null ? appContext.getAssessmentService() : null,
                appContext != null ? appContext.getLearnService() : null,
                appContext != null ? appContext.getTrackService() : null
        );
    }

    public ByteForceFrame(AuthService authService) {
        this(authService, null, null, null, null, null, null, null);
    }

    public ByteForceFrame(AuthService authService,
                          TopicService topicService,
                          QuestionService questionService,
                          AttemptService attemptService,
                          BookmarkService bookmarkService) {
        this(authService, topicService, questionService, attemptService, bookmarkService, null, null, null);
    }

    public ByteForceFrame(AuthService authService,
                          TopicService topicService,
                          QuestionService questionService,
                          AttemptService attemptService,
                          BookmarkService bookmarkService,
                          AssessmentService assessmentService) {
        this(authService, topicService, questionService, attemptService, bookmarkService, assessmentService, null, null);
    }

    public ByteForceFrame(AuthService authService,
                          TopicService topicService,
                          QuestionService questionService,
                          AttemptService attemptService,
                          BookmarkService bookmarkService,
                          AssessmentService assessmentService,
                          LearnService learnService) {
        this(authService, topicService, questionService, attemptService, bookmarkService, assessmentService, learnService, null);
    }

    public ByteForceFrame(AuthService authService,
                          TopicService topicService,
                          QuestionService questionService,
                          AttemptService attemptService,
                          BookmarkService bookmarkService,
                          AssessmentService assessmentService,
                          LearnService learnService,
                          TrackService trackService) {
        setTitle("ByteForce — Placement Readiness Platform");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1024, 700));
        setPreferredSize(new Dimension(1200, 800));
        setLocationRelativeTo(null);

        this.cardLayout = new CardLayout();
        this.cardsPanel = new JPanel(cardLayout);

        // Instantiate views with navigation callbacks
        this.loginPanel = new LoginPanel(authService, this::showDashboard);
        this.dashboardPanel = new DashboardPanel(authService, this::showLogin, this::showPractice, this::showAssessments, this::showLearn, this::showTrack);
        this.practicePanel = new PracticePanel(topicService, questionService, attemptService, bookmarkService, authService, this::showDashboard);
        this.assessmentPanel = new AssessmentPanel(assessmentService, authService, this::showDashboard);
        this.learnPanel = new LearnPanel(learnService, this::showDashboard);
        this.trackPanel = new TrackPanel(trackService, authService, this::showDashboard);

        this.cardsPanel.add(loginPanel, VIEW_LOGIN);
        this.cardsPanel.add(dashboardPanel, VIEW_DASHBOARD);
        this.cardsPanel.add(practicePanel, VIEW_PRACTICE);
        this.cardsPanel.add(assessmentPanel, VIEW_ASSESSMENTS);
        this.cardsPanel.add(learnPanel, VIEW_LEARN);
        this.cardsPanel.add(trackPanel, VIEW_TRACK);

        setLayout(new BorderLayout());
        add(cardsPanel, BorderLayout.CENTER);

        showLogin();
        pack();
    }

    /**
     * Navigates to the Login view and resets input fields.
     */
    public void showLogin() {
        loginPanel.reset();
        cardLayout.show(cardsPanel, VIEW_LOGIN);
        this.currentView = VIEW_LOGIN;
    }

    /**
     * Navigates to the Dashboard view and refreshes user details.
     */
    public void showDashboard() {
        dashboardPanel.refresh();
        cardLayout.show(cardsPanel, VIEW_DASHBOARD);
        this.currentView = VIEW_DASHBOARD;
    }

    /**
     * Navigates to the Practice view and refreshes topics.
     */
    public void showPractice() {
        practicePanel.resetToHome();
        cardLayout.show(cardsPanel, VIEW_PRACTICE);
        this.currentView = VIEW_PRACTICE;
    }

    /**
     * Navigates to the Assessments view and loads available tests.
     */
    public void showAssessments() {
        assessmentPanel.resetToHome();
        cardLayout.show(cardsPanel, VIEW_ASSESSMENTS);
        this.currentView = VIEW_ASSESSMENTS;
    }

    /**
     * Navigates to the Learn view and loads subjects.
     */
    public void showLearn() {
        learnPanel.resetToHome();
        cardLayout.show(cardsPanel, VIEW_LEARN);
        this.currentView = VIEW_LEARN;
    }

    /**
     * Navigates to the Track view and refreshes student progress.
     */
    public void showTrack() {
        trackPanel.refresh();
        cardLayout.show(cardsPanel, VIEW_TRACK);
        this.currentView = VIEW_TRACK;
    }

    /**
     * Switches view by name.
     */
    public void showView(String viewName) {
        if (VIEW_LOGIN.equalsIgnoreCase(viewName)) {
            showLogin();
        } else if (VIEW_DASHBOARD.equalsIgnoreCase(viewName)) {
            showDashboard();
        } else if (VIEW_PRACTICE.equalsIgnoreCase(viewName)) {
            showPractice();
        } else if (VIEW_ASSESSMENTS.equalsIgnoreCase(viewName)) {
            showAssessments();
        } else if (VIEW_LEARN.equalsIgnoreCase(viewName)) {
            showLearn();
        } else if (VIEW_TRACK.equalsIgnoreCase(viewName)) {
            showTrack();
        } else {
            cardLayout.show(cardsPanel, viewName);
            this.currentView = viewName;
        }
    }

    public String getCurrentView() {
        return currentView;
    }

    public LoginPanel getLoginPanel() {
        return loginPanel;
    }

    public DashboardPanel getDashboardPanel() {
        return dashboardPanel;
    }

    public PracticePanel getPracticePanel() {
        return practicePanel;
    }

    public AssessmentPanel getAssessmentPanel() {
        return assessmentPanel;
    }

    public LearnPanel getLearnPanel() {
        return learnPanel;
    }

    public TrackPanel getTrackPanel() {
        return trackPanel;
    }
}
