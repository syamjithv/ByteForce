package com.byteforce.web.util;

import com.byteforce.domain.User;
import jakarta.servlet.http.HttpSession;

import java.util.Optional;

/**
 * Utility for managing user authentication state inside HTTP sessions.
 */
public final class WebSessionUtil {

    public static final String SESSION_USER_KEY = "BYTEFORCE_AUTH_USER";

    private WebSessionUtil() {
    }

    public static void setCurrentUser(HttpSession session, User user) {
        if (session != null) {
            session.setAttribute(SESSION_USER_KEY, user);
        }
    }

    public static Optional<User> getCurrentUser(HttpSession session) {
        if (session == null) {
            return Optional.empty();
        }
        Object userObj = session.getAttribute(SESSION_USER_KEY);
        if (userObj instanceof User user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public static boolean isAuthenticated(HttpSession session) {
        return getCurrentUser(session).isPresent();
    }

    public static void clear(HttpSession session) {
        if (session != null) {
            session.removeAttribute(SESSION_USER_KEY);
            try {
                session.invalidate();
            } catch (IllegalStateException ignored) {
                // Session already invalidated
            }
        }
    }
}
