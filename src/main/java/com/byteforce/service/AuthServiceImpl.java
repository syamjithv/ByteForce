package com.byteforce.service;

import com.byteforce.domain.ActivityType;
import com.byteforce.domain.Role;
import com.byteforce.domain.User;
import com.byteforce.exception.AuthenticationException;
import com.byteforce.exception.DuplicateUserException;
import com.byteforce.exception.ValidationException;
import com.byteforce.repository.UserRepository;
import com.byteforce.security.PasswordHasher;
import com.byteforce.security.UserSession;
import com.byteforce.validation.EmailValidator;
import com.byteforce.validation.PasswordValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * Production implementation of {@link AuthService}.
 * Orchestrates user registration, credential authentication, and session management
 * with validation, password hashing, and repository delegation.
 */
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final UserSession userSession;
    private final ActivityService activityService;

    public AuthServiceImpl(UserRepository userRepository, PasswordHasher passwordHasher,
                           UserSession userSession, ActivityService activityService) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher must not be null");
        this.userSession = Objects.requireNonNull(userSession, "userSession must not be null");
        this.activityService = activityService;
    }

    public AuthServiceImpl(UserRepository userRepository, PasswordHasher passwordHasher, UserSession userSession) {
        this(userRepository, passwordHasher, userSession, null);
    }

    public AuthServiceImpl(UserRepository userRepository, PasswordHasher passwordHasher) {
        this(userRepository, passwordHasher, UserSession.getInstance(), null);
    }

    @Override
    public User register(String email, String password, String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new ValidationException("Full name must not be blank.");
        }

        if (email == null || email.isBlank()) {
            throw new ValidationException("Email must not be blank.");
        }

        String normalizedEmail = EmailValidator.normalize(email);

        if (!EmailValidator.isValid(normalizedEmail)) {
            throw new ValidationException("Invalid email format: " + email);
        }

        if (password == null || password.isBlank()) {
            throw new ValidationException("Password must not be blank.");
        }

        PasswordValidator.ValidationResult validationResult = PasswordValidator.validate(password);
        if (!validationResult.valid()) {
            throw new ValidationException("Password does not satisfy security standards: "
                    + validationResult.getErrorMessage(), validationResult.errors());
        }

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateUserException("A user with email '" + normalizedEmail + "' already exists.");
        }

        String passwordHash = passwordHasher.hash(password);
        User newUser = User.create(normalizedEmail, passwordHash, fullName.trim(), Role.STUDENT);

        User savedUser = userRepository.save(newUser);
        log.info("Registered new student user '{}' with email '{}'", savedUser.getId(), normalizedEmail);

        if (activityService != null) {
            try {
                activityService.recordActivity(savedUser.getId(), ActivityType.REGISTRATION, "Account registered with email: " + normalizedEmail);
            } catch (Exception e) {
                log.warn("Failed to record registration activity for user {}", savedUser.getId(), e);
            }
        }

        return savedUser;
    }

    @Override
    public User login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new AuthenticationException("Email and password must not be blank.");
        }

        String normalizedEmail = EmailValidator.normalize(email);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("Authentication failed: user not found for email '{}'", normalizedEmail);
                    return new AuthenticationException("Invalid email or password.");
                });

        if (!passwordHasher.matches(password, user.getPasswordHash())) {
            log.warn("Authentication failed: invalid credentials for email '{}'", normalizedEmail);
            throw new AuthenticationException("Invalid email or password.");
        }

        userSession.login(user);
        log.info("User '{}' authenticated successfully. Session initiated.", user.getId());

        if (activityService != null) {
            try {
                activityService.recordActivity(user.getId(), ActivityType.LOGIN, "User logged in: " + normalizedEmail);
            } catch (Exception e) {
                log.warn("Failed to record login activity for user {}", user.getId(), e);
            }
        }

        return user;
    }

    @Override
    public void logout() {
        userSession.logout();
        log.info("Active session successfully cleared.");
    }

    @Override
    public Optional<User> getCurrentUser() {
        return userSession.getCurrentUser();
    }

    @Override
    public boolean isAuthenticated() {
        return userSession.isAuthenticated();
    }

    @Override
    public UserSession getSession() {
        return userSession;
    }
}
