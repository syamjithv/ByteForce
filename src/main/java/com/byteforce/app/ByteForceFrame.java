package com.byteforce.app;

import com.byteforce.service.AuthService;
import com.byteforce.ui.DashboardPanel;
import com.byteforce.ui.LoginPanel;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;

/**
 * Main application window and shell for ByteForce.
 * Manages top-level views using CardLayout.
 */
public class ByteForceFrame extends JFrame {

    public static final String VIEW_LOGIN = "LOGIN";
    public static final String VIEW_DASHBOARD = "DASHBOARD";

    private final CardLayout cardLayout;
    private final JPanel cardsPanel;
    private final LoginPanel loginPanel;
    private final DashboardPanel dashboardPanel;
    private String currentView;

    public ByteForceFrame() {
        this((AuthService) null);
    }

    public ByteForceFrame(AppContext appContext) {
        this(appContext != null ? appContext.getAuthService() : null);
    }

    public ByteForceFrame(AuthService authService) {
        setTitle("ByteForce — Placement Readiness Platform");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1024, 700));
        setPreferredSize(new Dimension(1200, 800));
        setLocationRelativeTo(null);

        this.cardLayout = new CardLayout();
        this.cardsPanel = new JPanel(cardLayout);

        // Instantiate views with navigation callbacks
        this.loginPanel = new LoginPanel(authService, this::showDashboard);
        this.dashboardPanel = new DashboardPanel(authService, this::showLogin);

        this.cardsPanel.add(loginPanel, VIEW_LOGIN);
        this.cardsPanel.add(dashboardPanel, VIEW_DASHBOARD);

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
     * Switches view by name.
     */
    public void showView(String viewName) {
        if (VIEW_LOGIN.equalsIgnoreCase(viewName)) {
            showLogin();
        } else if (VIEW_DASHBOARD.equalsIgnoreCase(viewName)) {
            showDashboard();
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
}
