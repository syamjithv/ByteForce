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
        assertNotNull(panel.getAssessmentsButton());
        assertNotNull(panel.getLearnButton());
        assertNotNull(panel.getTrackButton());
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
        assertTrue(dashboardPanel.getAssessmentsButton().isEnabled());
        assertTrue(dashboardPanel.getLearnButton().isEnabled());
        assertTrue(dashboardPanel.getTrackButton().isEnabled());
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
        assertFalse(dashboardPanel.getAssessmentsButton().isEnabled());
        assertFalse(dashboardPanel.getLearnButton().isEnabled());
        assertFalse(dashboardPanel.getTrackButton().isEnabled());
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

    @Test
    @DisplayName("Practice button should trigger onStartPractice callback when configured")
    void shouldTriggerStartPracticeCallbackOnButtonClick() {
        AtomicBoolean practiceCallbackInvoked = new AtomicBoolean(false);
        DashboardPanel panel = new DashboardPanel(mockAuthService, () -> {}, () -> practiceCallbackInvoked.set(true));

        User student = User.create("student@byteforce.com", "hash", "Alice Developer", Role.STUDENT);
        when(mockAuthService.isAuthenticated()).thenReturn(true);
        when(mockAuthService.getCurrentUser()).thenReturn(Optional.of(student));

        panel.refresh();
        panel.getPracticeButton().doClick();

        assertTrue(practiceCallbackInvoked.get(), "Start practice callback should be invoked");
    }

    @Test
    @DisplayName("Assessments button should trigger onStartAssessments callback when configured")
    void shouldTriggerStartAssessmentsCallbackOnButtonClick() {
        AtomicBoolean assessmentsCallbackInvoked = new AtomicBoolean(false);
        DashboardPanel panel = new DashboardPanel(mockAuthService, () -> {}, () -> {}, () -> assessmentsCallbackInvoked.set(true));

        User student = User.create("student@byteforce.com", "hash", "Alice Developer", Role.STUDENT);
        when(mockAuthService.isAuthenticated()).thenReturn(true);
        when(mockAuthService.getCurrentUser()).thenReturn(Optional.of(student));

        panel.refresh();
        panel.getAssessmentsButton().doClick();

        assertTrue(assessmentsCallbackInvoked.get(), "Start assessments callback should be invoked");
    }

    @Test
    @DisplayName("Learn button should trigger onStartLearn callback when configured")
    void shouldTriggerStartLearnCallbackOnButtonClick() {
        AtomicBoolean learnCallbackInvoked = new AtomicBoolean(false);
        DashboardPanel panel = new DashboardPanel(mockAuthService, () -> {}, () -> {}, () -> {}, () -> learnCallbackInvoked.set(true));

        User student = User.create("student@byteforce.com", "hash", "Alice Developer", Role.STUDENT);
        when(mockAuthService.isAuthenticated()).thenReturn(true);
        when(mockAuthService.getCurrentUser()).thenReturn(Optional.of(student));

        panel.refresh();
        panel.getLearnButton().doClick();

        assertTrue(learnCallbackInvoked.get(), "Start learn callback should be invoked");
    }

    @Test
    @DisplayName("Track button should trigger onStartTrack callback when configured")
    void shouldTriggerStartTrackCallbackOnButtonClick() {
        AtomicBoolean trackCallbackInvoked = new AtomicBoolean(false);
        DashboardPanel panel = new DashboardPanel(mockAuthService, () -> {}, () -> {}, () -> {}, () -> {}, () -> trackCallbackInvoked.set(true));

        User student = User.create("student@byteforce.com", "hash", "Alice Developer", Role.STUDENT);
        when(mockAuthService.isAuthenticated()).thenReturn(true);
        when(mockAuthService.getCurrentUser()).thenReturn(Optional.of(student));

        panel.refresh();
        panel.getTrackButton().doClick();

        assertTrue(trackCallbackInvoked.get(), "Start track callback should be invoked");
    }
}
