package com.byteforce.ui;

import com.byteforce.domain.User;
import com.byteforce.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Optional;

/**
 * Student Dashboard view for ByteForce.
 * Displays the authenticated user's identity, provides logout capability,
 * and exposes placeholder action for the upcoming Practice milestone.
 */
public class DashboardPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(DashboardPanel.class);

    // Color Palette
    private static final Color BG_PAGE = new Color(248, 250, 252);       // Slate-50
    private static final Color BG_HEADER = new Color(15, 23, 42);        // Slate-900
    private static final Color BG_CARD = Color.WHITE;
    private static final Color BORDER_CARD = new Color(226, 232, 240);   // Slate-200
    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);   // Blue-600
    private static final Color COLOR_PRIMARY_HOVER = new Color(29, 78, 216);
    private static final Color COLOR_TEXT_MAIN = new Color(15, 23, 42);
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);
    private static final Color COLOR_DANGER = new Color(220, 38, 38);
    private static final Color COLOR_DANGER_BG = new Color(254, 242, 242);

    private final AuthService authService;
    private final Runnable onLogout;

    private final JLabel welcomeLabel;
    private final JLabel studentNameValueLabel;
    private final JLabel studentEmailValueLabel;
    private final JLabel studentRoleValueLabel;
    private final JButton logoutButton;
    private final JButton practiceButton;

    public DashboardPanel() {
        this(null, null);
    }

    public DashboardPanel(AuthService authService, Runnable onLogout) {
        super(new BorderLayout());
        this.authService = authService;
        this.onLogout = onLogout;

        setBackground(BG_PAGE);

        // 1. Navigation / Top Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_HEADER);
        topBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 60));
        topBar.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));

        JLabel brandLabel = new JLabel("ByteForce — Student Dashboard");
        brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        brandLabel.setForeground(Color.WHITE);
        topBar.add(brandLabel, BorderLayout.WEST);

        logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logoutButton.setForeground(COLOR_DANGER);
        logoutButton.setBackground(COLOR_DANGER_BG);
        logoutButton.setFocusPainted(false);
        logoutButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(252, 165, 165), 1),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));
        logoutButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutButton.addActionListener(e -> handleLogout());
        topBar.add(logoutButton, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // 2. Main Content Body
        JPanel contentContainer = new JPanel();
        contentContainer.setLayout(new BoxLayout(contentContainer, BoxLayout.Y_AXIS));
        contentContainer.setBackground(BG_PAGE);
        contentContainer.setBorder(BorderFactory.createEmptyBorder(32, 40, 32, 40));

        // Welcome Card
        JPanel profileCard = new JPanel();
        profileCard.setLayout(new BoxLayout(profileCard, BoxLayout.Y_AXIS));
        profileCard.setBackground(BG_CARD);
        profileCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(24, 28, 24, 28)
        ));
        profileCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        profileCard.setMaximumSize(new Dimension(800, 240));

        welcomeLabel = new JLabel("Welcome back!");
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        welcomeLabel.setForeground(COLOR_TEXT_MAIN);
        welcomeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Placement Readiness Overview");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(COLOR_TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        profileCard.add(welcomeLabel);
        profileCard.add(Box.createRigidArea(new Dimension(0, 4)));
        profileCard.add(subtitle);
        profileCard.add(Box.createRigidArea(new Dimension(0, 20)));

        // Profile Details Grid
        JPanel detailsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 32, 8));
        detailsPanel.setBackground(BG_CARD);
        detailsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        studentNameValueLabel = new JLabel("-");
        detailsPanel.add(createDetailItem("Student Name", studentNameValueLabel));

        studentEmailValueLabel = new JLabel("-");
        detailsPanel.add(createDetailItem("Email Address", studentEmailValueLabel));

        studentRoleValueLabel = new JLabel("-");
        detailsPanel.add(createDetailItem("Account Role", studentRoleValueLabel));

        profileCard.add(detailsPanel);
        contentContainer.add(profileCard);
        contentContainer.add(Box.createRigidArea(new Dimension(0, 24)));

        // Practice Placeholder Card
        JPanel practiceCard = new JPanel();
        practiceCard.setLayout(new BoxLayout(practiceCard, BoxLayout.Y_AXIS));
        practiceCard.setBackground(BG_CARD);
        practiceCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(24, 28, 24, 28)
        ));
        practiceCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        practiceCard.setMaximumSize(new Dimension(800, 160));

        JLabel practiceTitle = new JLabel("Practice & Problem Solving");
        practiceTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        practiceTitle.setForeground(COLOR_TEXT_MAIN);
        practiceTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel practiceDesc = new JLabel("Access curated coding, algorithm, and interview preparation questions.");
        practiceDesc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        practiceDesc.setForeground(COLOR_TEXT_MUTED);
        practiceDesc.setAlignmentX(Component.LEFT_ALIGNMENT);

        practiceButton = new JButton("Practice Questions (Milestone 2)");
        practiceButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        practiceButton.setForeground(Color.WHITE);
        practiceButton.setBackground(COLOR_PRIMARY);
        practiceButton.setOpaque(true);
        practiceButton.setBorderPainted(false);
        practiceButton.setFocusPainted(false);
        practiceButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        practiceButton.setPreferredSize(new Dimension(260, 38));
        practiceButton.setMaximumSize(new Dimension(260, 38));
        practiceButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        practiceButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(
                    this,
                    "The Practice module is scheduled for implementation in Milestone 2.",
                    "Practice Workflow",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });

        practiceCard.add(practiceTitle);
        practiceCard.add(Box.createRigidArea(new Dimension(0, 6)));
        practiceCard.add(practiceDesc);
        practiceCard.add(Box.createRigidArea(new Dimension(0, 16)));
        practiceCard.add(practiceButton);

        contentContainer.add(practiceCard);

        add(contentContainer, BorderLayout.CENTER);

        refresh();
    }

    private JPanel createDetailItem(String title, JLabel valueLabel) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_CARD);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLbl.setForeground(COLOR_TEXT_MUTED);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        valueLabel.setForeground(COLOR_TEXT_MAIN);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(titleLbl);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(valueLabel);
        return panel;
    }

    /**
     * Confirms authentication state and updates displayed student identity.
     */
    public void refresh() {
        if (authService != null && authService.isAuthenticated()) {
            Optional<User> userOpt = authService.getCurrentUser();
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                welcomeLabel.setText("Welcome, " + user.getFullName() + "!");
                studentNameValueLabel.setText(user.getFullName());
                studentEmailValueLabel.setText(user.getEmail());
                studentRoleValueLabel.setText(user.getRole() != null ? user.getRole().name() : "STUDENT");
                logoutButton.setEnabled(true);
                practiceButton.setEnabled(true);
                log.info("Dashboard refreshed for user: {}", user.getEmail());
                return;
            }
        }

        // Unauthenticated or default state
        welcomeLabel.setText("Welcome to ByteForce");
        studentNameValueLabel.setText("Not authenticated");
        studentEmailValueLabel.setText("-");
        studentRoleValueLabel.setText("-");
        logoutButton.setEnabled(false);
        practiceButton.setEnabled(false);
    }

    private void handleLogout() {
        if (authService != null) {
            authService.logout();
            log.info("User logged out successfully via Dashboard.");
        }
        if (onLogout != null) {
            onLogout.run();
        }
    }

    // Accessors for testing
    public JLabel getWelcomeLabel() {
        return welcomeLabel;
    }

    public JLabel getStudentNameValueLabel() {
        return studentNameValueLabel;
    }

    public JLabel getStudentEmailValueLabel() {
        return studentEmailValueLabel;
    }

    public JLabel getStudentRoleValueLabel() {
        return studentRoleValueLabel;
    }

    public JButton getLogoutButton() {
        return logoutButton;
    }

    public JButton getPracticeButton() {
        return practiceButton;
    }
}
