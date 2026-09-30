package com.byteforce.ui;

import com.byteforce.domain.User;
import com.byteforce.exception.AuthenticationException;
import com.byteforce.exception.ValidationException;
import com.byteforce.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * Login view for ByteForce placement readiness platform.
 * Communicates strictly through AuthService without accessing repositories or database directly.
 * Asynchronous authentication runs via SwingWorker off the Event Dispatch Thread.
 */
public class LoginPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(LoginPanel.class);

    // Color Palette
    private static final Color BG_PAGE = new Color(241, 245, 249);       // Slate-100
    private static final Color BG_CARD = Color.WHITE;
    private static final Color BORDER_CARD = new Color(226, 232, 240);   // Slate-200
    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);   // Blue-600
    private static final Color COLOR_PRIMARY_HOVER = new Color(29, 78, 216); // Blue-700
    private static final Color COLOR_TEXT_MAIN = new Color(15, 23, 42);  // Slate-900
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139); // Slate-500
    private static final Color COLOR_ERROR = new Color(220, 38, 38);     // Red-600
    private static final Color COLOR_INPUT_BORDER = new Color(203, 213, 225); // Slate-300

    private final AuthService authService;
    private final Runnable onLoginSuccess;

    private final JTextField emailField;
    private final JPasswordField passwordField;
    private final JButton loginButton;
    private final JLabel messageLabel;

    public LoginPanel() {
        this(null, null);
    }

    public LoginPanel(AuthService authService, Runnable onLoginSuccess) {
        this.authService = authService;
        this.onLoginSuccess = onLoginSuccess;

        setLayout(new GridBagLayout());
        setBackground(BG_PAGE);

        // Center card container
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBackground(BG_CARD);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CARD, 1, true),
                BorderFactory.createEmptyBorder(36, 40, 36, 40)
        ));
        cardPanel.setPreferredSize(new Dimension(420, 480));
        cardPanel.setMaximumSize(new Dimension(420, 520));

        // 1. Branding Header
        JLabel titleLabel = new JLabel("ByteForce");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));
        titleLabel.setForeground(COLOR_TEXT_MAIN);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Placement Readiness & Practice Platform");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(COLOR_TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel signinHeader = new JLabel("Student Login");
        signinHeader.setFont(new Font("Segoe UI", Font.BOLD, 18));
        signinHeader.setForeground(COLOR_TEXT_MAIN);
        signinHeader.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 2. Email Section
        JLabel emailLabel = new JLabel("Email Address");
        emailLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        emailLabel.setForeground(COLOR_TEXT_MAIN);
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        emailField = new JTextField();
        emailField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        emailField.setPreferredSize(new Dimension(340, 38));
        emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        emailField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_INPUT_BORDER, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        emailField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 3. Password Section
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        passwordLabel.setForeground(COLOR_TEXT_MAIN);
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setPreferredSize(new Dimension(340, 38));
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_INPUT_BORDER, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 4. Message Label (Errors and status)
        messageLabel = new JLabel(" ");
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        messageLabel.setForeground(COLOR_ERROR);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        messageLabel.setPreferredSize(new Dimension(340, 24));
        messageLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        // 5. Login Button
        loginButton = new JButton("Sign In");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setForeground(Color.WHITE);
        loginButton.setBackground(COLOR_PRIMARY);
        loginButton.setOpaque(true);
        loginButton.setBorderPainted(false);
        loginButton.setFocusPainted(false);
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginButton.setPreferredSize(new Dimension(340, 42));
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Hover effect for login button
        loginButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (loginButton.isEnabled()) {
                    loginButton.setBackground(COLOR_PRIMARY_HOVER);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (loginButton.isEnabled()) {
                    loginButton.setBackground(COLOR_PRIMARY);
                }
            }
        });

        // Trigger on click or Enter
        loginButton.addActionListener(e -> performLogin());
        emailField.addActionListener(e -> performLogin());
        passwordField.addActionListener(e -> performLogin());

        // Assembly
        cardPanel.add(titleLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        cardPanel.add(subtitleLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 24)));
        cardPanel.add(signinHeader);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 18)));
        cardPanel.add(emailLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        cardPanel.add(emailField);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 14)));
        cardPanel.add(passwordLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        cardPanel.add(passwordField);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        cardPanel.add(messageLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        cardPanel.add(loginButton);

        // Center cardPanel in LoginPanel
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 20, 20, 20);
        add(cardPanel, gbc);
    }

    /**
     * Executes the login process off the Event Dispatch Thread using SwingWorker.
     */
    public void performLogin() {
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        char[] passwordChars = passwordField.getPassword();
        String password = passwordChars != null ? new String(passwordChars) : "";

        // Client-side prompt validation
        if (email.isBlank()) {
            showError("Please enter your email.");
            emailField.requestFocusInWindow();
            return;
        }

        if (password.isBlank()) {
            showError("Please enter your password.");
            passwordField.requestFocusInWindow();
            return;
        }

        if (authService == null) {
            showError("Authentication service is unavailable.");
            return;
        }

        setLoadingState(true);
        showStatus("Signing in...");

        SwingWorker<User, Void> worker = new SwingWorker<>() {
            @Override
            protected User doInBackground() {
                return authService.login(email, password);
            }

            @Override
            protected void done() {
                setLoadingState(false);
                try {
                    User user = get();
                    log.info("User {} successfully authenticated via UI.", user.getEmail());
                    clearMessage();
                    passwordField.setText("");
                    if (onLoginSuccess != null) {
                        onLoginSuccess.run();
                    }
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    passwordField.setText("");
                    if (cause instanceof AuthenticationException) {
                        showError("Invalid email or password.");
                    } else if (cause instanceof ValidationException) {
                        showError(cause.getMessage());
                    } else {
                        log.error("Unexpected error during login execution", cause);
                        showError("Authentication failed. Please try again.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showError("Authentication was interrupted. Please try again.");
                }
            }
        };
        worker.execute();
    }

    /**
     * Resets input fields, clears messages, and restores control states.
     */
    public void reset() {
        emailField.setText("");
        passwordField.setText("");
        clearMessage();
        setLoadingState(false);
    }

    public void setCredentials(String email, String password) {
        emailField.setText(email != null ? email : "");
        passwordField.setText(password != null ? password : "");
    }

    private void setLoadingState(boolean loading) {
        loginButton.setEnabled(!loading);
        emailField.setEnabled(!loading);
        passwordField.setEnabled(!loading);
        if (loading) {
            loginButton.setText("Signing in...");
        } else {
            loginButton.setText("Sign In");
        }
    }

    private void showError(String message) {
        messageLabel.setForeground(COLOR_ERROR);
        messageLabel.setText(message != null ? message : "An error occurred.");
    }

    private void showStatus(String message) {
        messageLabel.setForeground(COLOR_PRIMARY);
        messageLabel.setText(message != null ? message : "");
    }

    private void clearMessage() {
        messageLabel.setText(" ");
    }

    // Component accessors for testing
    public JTextField getEmailField() {
        return emailField;
    }

    public JPasswordField getPasswordField() {
        return passwordField;
    }

    public JButton getLoginButton() {
        return loginButton;
    }

    public JLabel getMessageLabel() {
        return messageLabel;
    }
}
