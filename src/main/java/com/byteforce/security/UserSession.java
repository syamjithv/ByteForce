package com.byteforce.security;

import com.byteforce.domain.Role;
import com.byteforce.domain.User;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Thread-safe session holder representing the currently authenticated user in the desktop application.
 */
public final class UserSession {

    private static final UserSession INSTANCE = new UserSession();

    private volatile User currentUser;
    private volatile Instant loginTime;

    public UserSession() {
        this.currentUser = null;
        this.loginTime = null;
    }

    /**
     * Returns the global application singleton session.
     */
    public static UserSession getInstance() {
        return INSTANCE;
    }

    /**
     * Authenticates and starts a session for the given user.
     *
     * @param user the authenticated User entity
     */
    public synchronized void login(User user) {
        Objects.requireNonNull(user, "User must not be null");
        this.currentUser = user;
        this.loginTime = Instant.now();
    }

    /**
     * Checks if a user is currently authenticated in this session.
     */
    public boolean isAuthenticated() {
        return currentUser != null;
    }

    /**
     * Returns the currently authenticated user, or empty if unauthenticated.
     */
    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    /**
     * Returns the timestamp when the current session was initiated.
     */
    public Optional<Instant> getLoginTime() {
        return Optional.ofNullable(loginTime);
    }

    /**
     * Checks whether the current user holds the specified role.
     */
    public boolean hasRole(Role role) {
        return currentUser != null && currentUser.getRole() == role;
    }

    /**
     * Clears the current session and logs out the active user.
     */
    public synchronized void logout() {
        this.currentUser = null;
        this.loginTime = null;
    }

    /**
     * Alias for logout to clear session state.
     */
    public synchronized void clear() {
        logout();
    }
}
