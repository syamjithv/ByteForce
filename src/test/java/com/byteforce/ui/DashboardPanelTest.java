package com.byteforce.ui;

import com.byteforce.domain.Role;
import com.byteforce.domain.User;
import com.byteforce.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardPanelTest {

    @Mock
    private AuthService mockAuthService;

    private AtomicBoolean logoutCallbackInvoked;
    private DashboardPanel dashboardPanel;

    @BeforeEach
    void setUp() {
        logoutCallbackInvoked = new AtomicBoolean(false);
        dashboardPanel = new DashboardPanel(mockAuthService, () -> logoutCallbackInvoked.set(true));
    }

    @Test
    @DisplayName("DashboardPanel can be constructed with default constructor")
    void shouldConstructWithDefaultConstructor() {
        DashboardPanel panel = new DashboardPanel();
        assertNotNull(panel);
        assertNotNull(panel.getWelcomeLabel());
        assertNotNull(panel.getStudentNameValueLabel());
        assertNotNull(panel.getStudentEmailValueLabel());
        assertNotNull(panel.getLogoutButton());
        assertNotNull(panel.getPracticeButton());
    }

    @Test
    @DisplayName("Should display student identity when authenticated")
    void shouldDisplayStudentIdentityWhenAuthenticated() {
        User student = User.create("student@byteforce.com", "hash", "Alice Developer", Role.STUDENT);
        when(mockAuthService.isAuthenticated()).thenReturn(true);
        when(mockAuthService.getCurrentUser()).thenReturn(Optional.of(student));

        dashboardPanel.refresh();

        assertEquals("Welcome, Alice Developer!", dashboardPanel.getWelcomeLabel().getText());
        assertEquals("Alice Developer", dashboardPanel.getStudentNameValueLabel().getText());
        assertEquals("student@byteforce.com", dashboardPanel.getStudentEmailValueLabel().getText());
        assertEquals("STUDENT", dashboardPanel.getStudentRoleValueLabel().getText());
        assertTrue(dashboardPanel.getLogoutButton().isEnabled());
        assertTrue(dashboardPanel.getPracticeButton().isEnabled());
    }

    @Test
    @DisplayName("Should show placeholder text when unauthenticated")
    void shouldShowPlaceholderWhenUnauthenticated() {
        when(mockAuthService.isAuthenticated()).thenReturn(false);

        dashboardPanel.refresh();

        assertEquals("Welcome to ByteForce", dashboardPanel.getWelcomeLabel().getText());
        assertEquals("Not authenticated", dashboardPanel.getStudentNameValueLabel().getText());
        assertFalse(dashboardPanel.getLogoutButton().isEnabled());
        assertFalse(dashboardPanel.getPracticeButton().isEnabled());
    }

    @Test
    @DisplayName("Logout button should call AuthService.logout() and trigger callback")
    void shouldCallAuthServiceLogoutOnButtonClick() {
        User student = User.create("student@byteforce.com", "hash", "Alice Developer", Role.STUDENT);
        when(mockAuthService.isAuthenticated()).thenReturn(true);
        when(mockAuthService.getCurrentUser()).thenReturn(Optional.of(student));

        dashboardPanel.refresh();
        dashboardPanel.getLogoutButton().doClick();

        verify(mockAuthService).logout();
        assertTrue(logoutCallbackInvoked.get(), "Logout callback should be invoked");
    }
}
