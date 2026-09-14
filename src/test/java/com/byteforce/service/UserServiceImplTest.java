package com.byteforce.service;

import com.byteforce.domain.ActivityType;
import com.byteforce.domain.Role;
import com.byteforce.domain.StudentProfile;
import com.byteforce.domain.User;
import com.byteforce.exception.AuthenticationException;
import com.byteforce.exception.ResourceNotFoundException;
import com.byteforce.exception.ValidationException;
import com.byteforce.repository.StudentProfileRepository;
import com.byteforce.repository.UserRepository;
import com.byteforce.security.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private ActivityService activityService;

    private UserServiceImpl userService;

    private User sampleUser;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, passwordHasher, studentProfileRepository, activityService);
        sampleUser = new User(userId, "john.doe@example.com", "$2a$12$hashedPassword", "John Doe", Role.STUDENT, Instant.now(), Instant.now());
    }

    @Test
    @DisplayName("getUserById should return user when found")
    void getUserByIdShouldReturnUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        Optional<User> result = userService.getUserById(userId);

        assertTrue(result.isPresent());
        assertEquals("John Doe", result.get().getFullName());
    }

    @Test
    @DisplayName("getUserById should return empty for null id")
    void getUserByIdShouldReturnEmptyForNullId() {
        Optional<User> result = userService.getUserById(null);
        assertTrue(result.isEmpty());
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getUserByEmail should normalize email and return user")
    void getUserByEmailShouldNormalizeAndReturnUser() {
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(sampleUser));

        Optional<User> result = userService.getUserByEmail("  John.Doe@Example.com ");

        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getId());
    }

    @Test
    @DisplayName("getUserByEmail should return empty for null or blank email")
    void getUserByEmailShouldReturnEmptyForBlank() {
        assertTrue(userService.getUserByEmail(null).isEmpty());
        assertTrue(userService.getUserByEmail("   ").isEmpty());
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("updateProfile should update user full name and student profile")
    void updateProfileShouldUpdateName() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        StudentProfile existingProfile = StudentProfile.create(userId, "John Doe", "1234567890", "Tech Univ", 2026);
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));

        User updated = userService.updateProfile(userId, "Johnathan Doe");

        assertEquals("Johnathan Doe", updated.getFullName());
        verify(studentProfileRepository).save(any(StudentProfile.class));
    }

    @Test
    @DisplayName("updateProfile should throw when userId is null or name is blank")
    void updateProfileShouldValidateInput() {
        assertThrows(ValidationException.class, () -> userService.updateProfile(null, "John"));
        assertThrows(ValidationException.class, () -> userService.updateProfile(userId, "  "));
    }

    @Test
    @DisplayName("updateProfile should throw ResourceNotFoundException when user does not exist")
    void updateProfileShouldThrowWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updateProfile(userId, "Jane"));
    }

    @Test
    @DisplayName("getStudentProfile should return profile when found")
    void getStudentProfileShouldReturnProfile() {
        StudentProfile profile = StudentProfile.create(userId, "John Doe", "1234567890", "Tech Univ", 2026);
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        Optional<StudentProfile> result = userService.getStudentProfile(userId);

        assertTrue(result.isPresent());
        assertEquals("Tech Univ", result.get().getCollege());
    }

    @Test
    @DisplayName("getStudentProfile should return empty when userId is null")
    void getStudentProfileShouldReturnEmptyForNull() {
        assertTrue(userService.getStudentProfile(null).isEmpty());
    }

    @Test
    @DisplayName("updateStudentProfile should create or update profile and record activity")
    void updateStudentProfileShouldSaveAndRecordActivity() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        StudentProfile saved = userService.updateStudentProfile(
                userId, "Johnathan Doe", "9876543210", "National Institute", 2027);

        assertNotNull(saved);
        assertEquals("Johnathan Doe", saved.getFullName());
        assertEquals("9876543210", saved.getPhone());
        assertEquals("National Institute", saved.getCollege());
        assertEquals(2027, saved.getGraduationYear());

        verify(activityService).recordActivity(eq(userId), eq(ActivityType.PROFILE_UPDATED), anyString());
    }

    @Test
    @DisplayName("updateStudentProfile should throw when inputs are invalid")
    void updateStudentProfileShouldValidate() {
        assertThrows(ValidationException.class, () ->
                userService.updateStudentProfile(null, "Name", "Phone", "College", 2025));
        assertThrows(ValidationException.class, () ->
                userService.updateStudentProfile(userId, "   ", "Phone", "College", 2025));
    }

    @Test
    @DisplayName("changePassword should succeed when current password matches and new password is valid")
    void changePasswordShouldSucceed() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(passwordHasher.matches("OldPass123!", sampleUser.getPasswordHash())).thenReturn(true);
        when(passwordHasher.hash("NewStrongPass456!")).thenReturn("$2a$12$newHashedPassword");

        userService.changePassword(userId, "OldPass123!", "NewStrongPass456!");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("$2a$12$newHashedPassword", userCaptor.getValue().getPasswordHash());
    }

    @Test
    @DisplayName("changePassword should throw AuthenticationException when old password is incorrect")
    void changePasswordShouldThrowOnWrongCurrentPassword() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(passwordHasher.matches("WrongPass123!", sampleUser.getPasswordHash())).thenReturn(false);

        assertThrows(AuthenticationException.class, () ->
                userService.changePassword(userId, "WrongPass123!", "NewStrongPass456!"));
    }

    @Test
    @DisplayName("changePassword should throw ValidationException when new password does not meet requirements")
    void changePasswordShouldThrowOnWeakNewPassword() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(passwordHasher.matches("OldPass123!", sampleUser.getPasswordHash())).thenReturn(true);

        assertThrows(ValidationException.class, () ->
                userService.changePassword(userId, "OldPass123!", "weak"));
    }

    @Test
    @DisplayName("changePassword should throw ValidationException for blank passwords")
    void changePasswordShouldValidateBlanks() {
        assertThrows(ValidationException.class, () -> userService.changePassword(null, "OldPass123!", "NewPass123!"));
        assertThrows(ValidationException.class, () -> userService.changePassword(userId, "  ", "NewPass123!"));
        assertThrows(ValidationException.class, () -> userService.changePassword(userId, "OldPass123!", "  "));
    }

    @Test
    @DisplayName("existsByEmail should return true when email exists and false when not")
    void existsByEmailShouldCheckRepository() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        assertTrue(userService.existsByEmail("test@example.com"));
        assertFalse(userService.existsByEmail("notfound@example.com"));
        assertFalse(userService.existsByEmail(null));
        assertFalse(userService.existsByEmail("  "));
    }
}
