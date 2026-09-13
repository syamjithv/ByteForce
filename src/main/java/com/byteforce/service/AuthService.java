package com.byteforce.service;

import com.byteforce.domain.User;
import com.byteforce.security.UserSession;

import java.util.Optional;

/**
 * Service interface for user registration, authentication, and session management.
 */
public interface AuthService {

    /**
     * Registers a new student user.
     *
     * @param email    the user's email address
     * @param password the raw password meeting complexity standards
     * @param fullName the user's full name
     * @return the newly registered User
     */
    User register(String email, String password, String fullName);

    /**
     * Authenticates a user with email and password and starts a session upon success.
     *
     * @param email    the user's email address
     * @param password the raw password
     * @return the authenticated User
     */
    User login(String email, String password);

    /**
     * Terminates the current active session.
     */
    void logout();

    /**
     * Returns the currently authenticated user in the active session, if any.
     */
    Optional<User> getCurrentUser();

    /**
     * Checks if there is an active authenticated user in the current session.
     */
    boolean isAuthenticated();

    /**
     * Returns the underlying session holder.
     */
    UserSession getSession();
}

