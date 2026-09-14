package com.byteforce.service;

import com.byteforce.domain.ActivityType;
import com.byteforce.domain.StudentProfile;
import com.byteforce.domain.User;
import com.byteforce.exception.AuthenticationException;
import com.byteforce.exception.ResourceNotFoundException;
import com.byteforce.exception.ValidationException;
import com.byteforce.repository.StudentProfileRepository;
import com.byteforce.repository.UserRepository;
import com.byteforce.security.PasswordHasher;
import com.byteforce.validation.EmailValidator;
import com.byteforce.validation.PasswordValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Production implementation of {@link UserService}.
 */
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final StudentProfileRepository studentProfileRepository;
    private final ActivityService activityService;

    public UserServiceImpl(UserRepository userRepository, PasswordHasher passwordHasher,
                           StudentProfileRepository studentProfileRepository, ActivityService activityService) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher must not be null");
        this.studentProfileRepository = studentProfileRepository;
        this.activityService = activityService;
    }

    public UserServiceImpl(UserRepository userRepository, PasswordHasher passwordHasher,
                           StudentProfileRepository studentProfileRepository) {
        this(userRepository, passwordHasher, studentProfileRepository, null);
    }

    public UserServiceImpl(UserRepository userRepository, PasswordHasher passwordHasher) {
        this(userRepository, passwordHasher, null, null);
    }

    @Override
    public Optional<User> getUserById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        String normalizedEmail = EmailValidator.normalize(email);
        return userRepository.findByEmail(normalizedEmail);
    }

    @Override
    public User updateProfile(UUID userId, String fullName) {
        if (userId == null) {
            throw new ValidationException("User ID must not be null.");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new ValidationException("Full name must not be blank.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        User updated = user.withFullName(fullName.trim());
        User saved = userRepository.save(updated);

        if (studentProfileRepository != null) {
            studentProfileRepository.findByUserId(userId).ifPresent(p -> {
                studentProfileRepository.save(p.withFullName(fullName.trim()));
            });
        }

        log.info("Updated profile for user ID: {}", userId);
        return saved;
    }

    @Override
    public Optional<StudentProfile> getStudentProfile(UUID userId) {
        if (userId == null || studentProfileRepository == null) {
            return Optional.empty();
        }
        return studentProfileRepository.findByUserId(userId);
    }

    @Override
    public StudentProfile updateStudentProfile(UUID userId, String fullName, String phone, String college, Integer graduationYear) {
        if (userId == null) {
            throw new ValidationException("User ID must not be null.");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new ValidationException("Full name must not be blank.");
        }
        if (studentProfileRepository == null) {
            throw new ValidationException("StudentProfileRepository is not configured.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (!user.getFullName().equals(fullName.trim())) {
            userRepository.save(user.withFullName(fullName.trim()));
        }

        StudentProfile existing = studentProfileRepository.findByUserId(userId)
                .orElseGet(() -> StudentProfile.create(userId, fullName.trim(), phone, college, graduationYear));

        StudentProfile toSave = existing
                .withFullName(fullName.trim())
                .withPhone(phone != null ? phone.trim() : null)
                .withCollege(college != null ? college.trim() : null)
                .withGraduationYear(graduationYear);

        StudentProfile saved = studentProfileRepository.save(toSave);
        log.info("Updated student profile for user ID: {}", userId);

        if (activityService != null) {
            try {
                activityService.recordActivity(userId, ActivityType.PROFILE_UPDATED, "Profile details updated: " + fullName.trim());
            } catch (Exception e) {
                log.warn("Failed to record profile update activity for user {}", userId, e);
            }
        }

        return saved;
    }

    @Override
    public void changePassword(UUID userId, String oldPassword, String newPassword) {
        if (userId == null) {
            throw new ValidationException("User ID must not be null.");
        }
        if (oldPassword == null || oldPassword.isBlank()) {
            throw new ValidationException("Current password must not be blank.");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new ValidationException("New password must not be blank.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (!passwordHasher.matches(oldPassword, user.getPasswordHash())) {
            log.warn("Password change failed: incorrect current password for user ID: {}", userId);
            throw new AuthenticationException("Current password is incorrect.");
        }

        PasswordValidator.ValidationResult result = PasswordValidator.validate(newPassword);
        if (!result.valid()) {
            throw new ValidationException("New password does not meet security requirements: "
                    + result.getErrorMessage(), result.errors());
        }

        String newHash = passwordHasher.hash(newPassword);
        User updated = user.withPasswordHash(newHash);
        userRepository.save(updated);
        log.info("Successfully changed password for user ID: {}", userId);
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String normalizedEmail = EmailValidator.normalize(email);
        return userRepository.findByEmail(normalizedEmail).isPresent();
    }
}
