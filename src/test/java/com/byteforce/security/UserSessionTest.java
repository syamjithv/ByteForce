package com.byteforce.security;

import com.byteforce.domain.Role;
import com.byteforce.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserSessionTest {

    private UserSession session;
    private User testStudent;
    private User testAdmin;

    @BeforeEach
    void setUp() {
        session = new UserSession(); // Create isolated instance for test
        testStudent = User.create("student@byteforce.com", "$2a$12$hash1", "Alice Student", Role.STUDENT);
        testAdmin = User.create("admin@byteforce.com", "$2a$12$hash2", "Bob Admin", Role.ADMIN);
    }

    @Test
    @DisplayName("Fresh session should be unauthenticated")
    void shouldBeUnauthenticatedInitially() {
        assertFalse(session.isAuthenticated());
        assertTrue(session.getCurrentUser().isEmpty());
        assertTrue(session.getLoginTime().isEmpty());
        assertFalse(session.hasRole(Role.STUDENT));
        assertFalse(session.hasRole(Role.ADMIN));
    }

    @Test
    @DisplayName("login() should establish active user session with timestamp and correct role")
    void shouldLoginSuccessfully() {
        session.login(testStudent);

        assertTrue(session.isAuthenticated());
        assertTrue(session.getCurrentUser().isPresent());
        assertEquals(testStudent, session.getCurrentUser().get());
        assertTrue(session.getLoginTime().isPresent());
        assertTrue(session.hasRole(Role.STUDENT));
        assertFalse(session.hasRole(Role.ADMIN));
    }

    @Test
    @DisplayName("logout() should clear session state")
    void shouldLogoutSuccessfully() {
        session.login(testAdmin);
        assertTrue(session.isAuthenticated());

        session.logout();

        assertFalse(session.isAuthenticated());
        assertTrue(session.getCurrentUser().isEmpty());
        assertTrue(session.getLoginTime().isEmpty());
        assertFalse(session.hasRole(Role.ADMIN));
    }

    @Test
    @DisplayName("clear() should function as an alias to logout()")
    void shouldClearSession() {
        session.login(testStudent);
        session.clear();

        assertFalse(session.isAuthenticated());
    }

    @Test
    @DisplayName("login(null) must throw NullPointerException")
    void shouldRejectNullUserOnLogin() {
        assertThrows(NullPointerException.class, () -> session.login(null));
    }

    @Test
    @DisplayName("Singleton instance should be accessible and non-null")
    void shouldProvideSingletonInstance() {
        UserSession globalSession = UserSession.getInstance();
        assertNotNull(globalSession);
    }
}
