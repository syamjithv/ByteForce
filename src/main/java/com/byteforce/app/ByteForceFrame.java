package com.byteforce.app;

import com.byteforce.service.AttemptService;
import com.byteforce.service.AuthService;
import com.byteforce.service.BookmarkService;
import com.byteforce.service.QuestionService;
import com.byteforce.service.TopicService;
import com.byteforce.ui.DashboardPanel;
import com.byteforce.ui.LoginPanel;
import com.byteforce.ui.PracticePanel;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;

/**
 * Main application window and shell for ByteForce.
 * Manages top-level views (Login, Dashboard, Practice) using CardLayout.
 */
public class ByteForceFrame extends JFrame {

    public static final String VIEW_LOGIN = "LOGIN";
    public static final String VIEW_DASHBOARD = "DASHBOARD";
    public static final String VIEW_PRACTICE = "PRACTICE";

    private final CardLayout cardLayout;
    private final JPanel cardsPanel;
    private final LoginPanel loginPanel;
    private final DashboardPanel dashboardPanel;
    private final PracticePanel practicePanel;
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
                appContext != null ? appContext.getBookmarkService() : null
        );
    }

    public ByteForceFrame(AuthService authService) {
        this(authService, null, null, null, null);
    }

    public ByteForceFrame(AuthService authService,
                          TopicService topicService,
                          QuestionService questionService,
                          AttemptService attemptService,
                          BookmarkService bookmarkService) {
        setTitle("ByteForce — Placement Readiness Platform");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1024, 700));
        setPreferredSize(new Dimension(1200, 800));
        setLocationRelativeTo(null);

        this.cardLayout = new CardLayout();
        this.cardsPanel = new JPanel(cardLayout);

        // Instantiate views with navigation callbacks
        this.loginPanel = new LoginPanel(authService, this::showDashboard);
        this.dashboardPanel = new DashboardPanel(authService, this::showLogin, this::showPractice);
        this.practicePanel = new PracticePanel(topicService, questionService, attemptService, bookmarkService, authService, this::showDashboard);

        this.cardsPanel.add(loginPanel, VIEW_LOGIN);
        this.cardsPanel.add(dashboardPanel, VIEW_DASHBOARD);
        this.cardsPanel.add(practicePanel, VIEW_PRACTICE);

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
     * Switches view by name.
     */
    public void showView(String viewName) {
        if (VIEW_LOGIN.equalsIgnoreCase(viewName)) {
            showLogin();
        } else if (VIEW_DASHBOARD.equalsIgnoreCase(viewName)) {
            showDashboard();
        } else if (VIEW_PRACTICE.equalsIgnoreCase(viewName)) {
            showPractice();
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
}
