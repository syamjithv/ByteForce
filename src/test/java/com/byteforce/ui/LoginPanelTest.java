package com.byteforce.ui;

import com.byteforce.domain.Role;
import com.byteforce.domain.User;
import com.byteforce.exception.AuthenticationException;
import com.byteforce.exception.ValidationException;
import com.byteforce.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginPanelTest {

    @Mock
    private AuthService mockAuthService;

    private AtomicBoolean loginSuccessCallbackInvoked;
    private LoginPanel loginPanel;

    @BeforeEach
    void setUp() {
        loginSuccessCallbackInvoked = new AtomicBoolean(false);
        loginPanel = new LoginPanel(mockAuthService, () -> loginSuccessCallbackInvoked.set(true));
    }

    @Test
    @DisplayName("LoginPanel can be constructed with default constructor")
    void shouldConstructWithDefaultConstructor() {
        LoginPanel panel = new LoginPanel();
        assertNotNull(panel);
        assertNotNull(panel.getEmailField());
        assertNotNull(panel.getPasswordField());
        assertNotNull(panel.getLoginButton());
        assertNotNull(panel.getMessageLabel());
    }

    @Test
    @DisplayName("Should display error when email is empty")
    void shouldDisplayErrorWhenEmailIsEmpty() {
        loginPanel.setCredentials("", "ValidPassword123!");
        loginPanel.performLogin();

        assertEquals("Please enter your email.", loginPanel.getMessageLabel().getText().trim());
        verify(mockAuthService, never()).login(anyString(), anyString());
        assertFalse(loginSuccessCallbackInvoked.get());
    }

    @Test
    @DisplayName("Should display error when password is empty")
    void shouldDisplayErrorWhenPasswordIsEmpty() {
        loginPanel.setCredentials("student@byteforce.com", "");
        loginPanel.performLogin();

        assertEquals("Please enter your password.", loginPanel.getMessageLabel().getText().trim());
        verify(mockAuthService, never()).login(anyString(), anyString());
        assertFalse(loginSuccessCallbackInvoked.get());
    }

    @Test
    @DisplayName("Should reset fields and error messages on reset()")
    void shouldResetFieldsAndErrorMessages() {
        loginPanel.setCredentials("student@byteforce.com", "");
        loginPanel.performLogin();
        assertEquals("Please enter your password.", loginPanel.getMessageLabel().getText().trim());

        loginPanel.reset();
        assertEquals("", loginPanel.getEmailField().getText());
        assertEquals("", new String(loginPanel.getPasswordField().getPassword()));
        assertEquals("", loginPanel.getMessageLabel().getText().trim());
        assertTrue(loginPanel.getLoginButton().isEnabled());
    }

    @Test
    @DisplayName("Should invoke AuthService and trigger callback upon successful authentication")
    void shouldTriggerCallbackOnSuccessfulAuthentication() throws Exception {
        User student = User.create("student@byteforce.com", "hash", "Student User", Role.STUDENT);
        when(mockAuthService.login("student@byteforce.com", "Password123!")).thenReturn(student);

        loginPanel.setCredentials("student@byteforce.com", "Password123!");
        loginPanel.performLogin();

        // Wait briefly for SwingWorker execution
        long deadline = System.currentTimeMillis() + 3000;
        while (!loginSuccessCallbackInvoked.get() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }

        assertTrue(loginSuccessCallbackInvoked.get(), "Login success callback should be invoked");
        verify(mockAuthService).login("student@byteforce.com", "Password123!");
        assertEquals("", new String(loginPanel.getPasswordField().getPassword()), "Password field should be cleared");
    }

    @Test
    @DisplayName("Should display friendly error message when authentication fails")
    void shouldDisplayErrorOnAuthenticationFailure() throws Exception {
        when(mockAuthService.login("student@byteforce.com", "WrongPassword!"))
                .thenThrow(new AuthenticationException("Invalid email or password."));

        loginPanel.setCredentials("student@byteforce.com", "WrongPassword!");
        loginPanel.performLogin();

        // Wait for SwingWorker execution and EDT error message processing
        long deadline = System.currentTimeMillis() + 3000;
        while (!"Invalid email or password.".equals(loginPanel.getMessageLabel().getText().trim())
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }

        assertEquals("Invalid email or password.", loginPanel.getMessageLabel().getText().trim());
        assertFalse(loginSuccessCallbackInvoked.get());
        assertTrue(loginPanel.getLoginButton().isEnabled(), "Login button should be re-enabled");
    }

    @Test
    @DisplayName("Should display validation message when service throws ValidationException")
    void shouldDisplayValidationMessage() throws Exception {
        when(mockAuthService.login("invalid-email", "Short!"))
                .thenThrow(new ValidationException("Email address format is invalid."));

        loginPanel.setCredentials("invalid-email", "Short!");
        loginPanel.performLogin();

        long deadline = System.currentTimeMillis() + 3000;
        while (!loginPanel.getLoginButton().isEnabled() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }

        assertEquals("Email address format is invalid.", loginPanel.getMessageLabel().getText().trim());
        assertFalse(loginSuccessCallbackInvoked.get());
        assertTrue(loginPanel.getLoginButton().isEnabled());
    }
}
