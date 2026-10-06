package com.byteforce.service;

import com.byteforce.domain.Role;
import com.byteforce.domain.User;
import com.byteforce.exception.AuthenticationException;
import com.byteforce.exception.ByteForceException;
import com.byteforce.exception.DuplicateUserException;
import com.byteforce.exception.ValidationException;
import com.byteforce.repository.UserRepository;
import com.byteforce.security.PasswordHasher;
import com.byteforce.security.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    private UserSession userSession;
    private AuthServiceImpl authService;

    private User sampleStudent;
    private final String validRawPassword = "StrongPassword123!";
    private final String samplePasswordHash = "$2a$12$e8k3K8k0v.6ZtqFvW7wEBe/q4z2u3n4p5r6s7t8u9v0w1x2y3z4a5";

    @BeforeEach
    void setUp() {
        userSession = new UserSession(); // Isolated session per test
        authService = new AuthServiceImpl(userRepository, passwordHasher, userSession);

        sampleStudent = User.create("student@byteforce.com", samplePasswordHash, "Alice Walker", Role.STUDENT);
    }

    @Test
    @DisplayName("Should successfully register a new student user")
    void shouldRegisterNewStudentSuccessfully() {
        when(userRepository.findByEmail("student@byteforce.com")).thenReturn(Optional.empty());
        when(passwordHasher.hash(validRawPassword)).thenReturn(samplePasswordHash);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User registered = authService.register("student@byteforce.com", validRawPassword, "Alice Walker");

        assertNotNull(registered);
        assertEquals("student@byteforce.com", registered.getEmail());
        assertEquals("Alice Walker", registered.getFullName());
        assertEquals(samplePasswordHash, registered.getPasswordHash());
        assertEquals(Role.STUDENT, registered.getRole());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertEquals("student@byteforce.com", capturedUser.getEmail());
        assertEquals(Role.STUDENT, capturedUser.getRole());
        assertEquals(samplePasswordHash, capturedUser.getPasswordHash());
    }

    @Test
    @DisplayName("Should normalize email during registration")
    void shouldNormalizeEmailDuringRegistration() {
        String inputEmail = "  StUdEnT.TEST@ByteForce.Com  ";
        String normalizedEmail = "student.test@byteforce.com";

        when(userRepository.findByEmail(normalizedEmail)).thenReturn(Optional.empty());
        when(passwordHasher.hash(validRawPassword)).thenReturn(samplePasswordHash);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User registered = authService.register(inputEmail, validRawPassword, "Bob Builder");

        assertEquals(normalizedEmail, registered.getEmail());
        verify(userRepository).findByEmail(normalizedEmail);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(normalizedEmail, captor.getValue().getEmail());
    }

    @Test
    @DisplayName("Should hash password and never persist raw password")
    void shouldHashPasswordDuringRegistration() {
        when(userRepository.findByEmail("student@byteforce.com")).thenReturn(Optional.empty());
        when(passwordHasher.hash(validRawPassword)).thenReturn(samplePasswordHash);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User registered = authService.register("student@byteforce.com", validRawPassword, "Alice");

        verify(passwordHasher).hash(validRawPassword);
        assertEquals(samplePasswordHash, registered.getPasswordHash());
        assertNotEquals(validRawPassword, registered.getPasswordHash());
    }

    @Test
    @DisplayName("Should reject registration with invalid email formats")
    void shouldRejectRegistrationWithInvalidEmail() {
        assertThrows(ValidationException.class, () -> authService.register(null, validRawPassword, "Alice"));
        assertThrows(ValidationException.class, () -> authService.register("   ", validRawPassword, "Alice"));
        assertThrows(ValidationException.class, () -> authService.register("invalid-email", validRawPassword, "Alice"));
        assertThrows(ValidationException.class, () -> authService.register("missing@domain", validRawPassword, "Alice"));
        assertThrows(ValidationException.class, () -> authService.register("@missingusername.com", validRawPassword, "Alice"));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject registration with invalid or weak passwords")
    void shouldRejectRegistrationWithInvalidPassword() {
        // Too short (< 8 chars)
        ValidationException ex1 = assertThrows(ValidationException.class,
                () -> authService.register("user@test.com", "Short1!", "User"));
        assertFalse(ex1.getErrors().isEmpty());

        // Missing uppercase
        assertThrows(ValidationException.class,
                () -> authService.register("user@test.com", "lowercase1!", "User"));

        // Missing lowercase
        assertThrows(ValidationException.class,
                () -> authService.register("user@test.com", "UPPERCASE1!", "User"));

        // Missing digit
        assertThrows(ValidationException.class,
                () -> authService.register("user@test.com", "NoDigitsHere!", "User"));

        // Missing special character
        assertThrows(ValidationException.class,
                () -> authService.register("user@test.com", "NoSpecial123", "User"));

        // Contains whitespace
        assertThrows(ValidationException.class,
                () -> authService.register("user@test.com", "Pass word123!", "User"));

        // Null or blank
        assertThrows(ValidationException.class,
                () -> authService.register("user@test.com", null, "User"));
        assertThrows(ValidationException.class,
                () -> authService.register("user@test.com", "   ", "User"));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject registration when full name is blank or null")
    void shouldRejectRegistrationWithBlankFullName() {
        assertThrows(ValidationException.class,
                () -> authService.register("user@test.com", validRawPassword, null));
        assertThrows(ValidationException.class,
                () -> authService.register("user@test.com", validRawPassword, "   "));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateUserException when email is already registered")
    void shouldThrowDuplicateUserExceptionOnDuplicateEmail() {
        when(userRepository.findByEmail("student@byteforce.com")).thenReturn(Optional.of(sampleStudent));

        assertThrows(DuplicateUserException.class,
                () -> authService.register("student@byteforce.com", validRawPassword, "Another Name"));

        verify(passwordHasher, never()).hash(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should login successfully and establish user session")
    void shouldLoginSuccessfullyAndEstablishSession() {
        when(userRepository.findByEmail("student@byteforce.com")).thenReturn(Optional.of(sampleStudent));
        when(passwordHasher.matches(validRawPassword, samplePasswordHash)).thenReturn(true);

        assertFalse(userSession.isAuthenticated());

        User authenticated = authService.login("student@byteforce.com", validRawPassword);

        assertEquals(sampleStudent, authenticated);
        assertTrue(userSession.isAuthenticated());
        assertTrue(userSession.getCurrentUser().isPresent());
        assertEquals(sampleStudent, userSession.getCurrentUser().get());
        assertTrue(authService.isAuthenticated());
        assertEquals(Optional.of(sampleStudent), authService.getCurrentUser());
    }

    @Test
    @DisplayName("Should normalize email during login lookup")
    void shouldNormalizeEmailDuringLogin() {
        when(userRepository.findByEmail("student@byteforce.com")).thenReturn(Optional.of(sampleStudent));
        when(passwordHasher.matches(validRawPassword, samplePasswordHash)).thenReturn(true);

        User authenticated = authService.login("  STUDENT@BYTEFORCE.COM  ", validRawPassword);

        assertEquals(sampleStudent, authenticated);
        verify(userRepository).findByEmail("student@byteforce.com");
    }

    @Test
    @DisplayName("Should reject login when user does not exist without establishing session")
    void shouldRejectLoginWithNonexistentUser() {
        when(userRepository.findByEmail("ghost@byteforce.com")).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class,
                () -> authService.login("ghost@byteforce.com", validRawPassword));

        assertFalse(userSession.isAuthenticated());
        assertTrue(userSession.getCurrentUser().isEmpty());
    }

    @Test
    @DisplayName("Should reject login when password does not match without establishing session")
    void shouldRejectLoginWithIncorrectPassword() {
        when(userRepository.findByEmail("student@byteforce.com")).thenReturn(Optional.of(sampleStudent));
        when(passwordHasher.matches("WrongPassword123!", samplePasswordHash)).thenReturn(false);

        assertThrows(AuthenticationException.class,
                () -> authService.login("student@byteforce.com", "WrongPassword123!"));

        assertFalse(userSession.isAuthenticated());
        assertTrue(userSession.getCurrentUser().isEmpty());
    }

    @Test
    @DisplayName("Should reject login with blank or null credentials")
    void shouldRejectLoginWithBlankCredentials() {
        assertThrows(AuthenticationException.class, () -> authService.login(null, "pass"));
        assertThrows(AuthenticationException.class, () -> authService.login("   ", "pass"));
        assertThrows(AuthenticationException.class, () -> authService.login("user@test.com", null));
        assertThrows(AuthenticationException.class, () -> authService.login("user@test.com", "   "));

        assertFalse(userSession.isAuthenticated());
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Should logout active user and clear session")
    void shouldLogoutAndClearSession() {
        userSession.login(sampleStudent);
        assertTrue(authService.isAuthenticated());

        authService.logout();

        assertFalse(authService.isAuthenticated());
        assertTrue(authService.getCurrentUser().isEmpty());
        assertFalse(userSession.isAuthenticated());
    }

    @Test
    @DisplayName("getSession() should return underlying session")
    void shouldReturnUnderlyingSession() {
        assertSame(userSession, authService.getSession());
    }

    @Test
    @DisplayName("Should propagate repository failures during registration")
    void shouldPropagateRepositoryFailuresDuringRegistration() {
        when(userRepository.findByEmail("student@byteforce.com")).thenReturn(Optional.empty());
        when(passwordHasher.hash(validRawPassword)).thenReturn(samplePasswordHash);
        when(userRepository.save(any(User.class))).thenThrow(new ByteForceException("Database disk failure"));

        assertThrows(ByteForceException.class,
                () -> authService.register("student@byteforce.com", validRawPassword, "Alice"));
    }

    @Test
    @DisplayName("Should propagate repository failures during login without establishing session")
    void shouldPropagateRepositoryFailuresDuringLogin() {
        when(userRepository.findByEmail("student@byteforce.com"))
                .thenThrow(new ByteForceException("Connection pool exhausted"));

        assertThrows(ByteForceException.class,
                () -> authService.login("student@byteforce.com", validRawPassword));

        assertFalse(userSession.isAuthenticated());
    }
}
