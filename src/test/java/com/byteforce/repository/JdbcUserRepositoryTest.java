package com.byteforce.repository;

import com.byteforce.domain.Role;
import com.byteforce.domain.User;
import com.byteforce.exception.ByteForceException;
import com.byteforce.exception.DuplicateUserException;
import com.byteforce.persistence.DatabaseMigrator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcUserRepositoryTest {

    private HikariDataSource dataSource;
    private JdbcUserRepository userRepository;

    @BeforeEach
    void setUp() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("UserRepoTestPool-" + UUID.randomUUID());
        hikariConfig.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        hikariConfig.setUsername("sa");
        hikariConfig.setPassword("");
        hikariConfig.setMaximumPoolSize(5);

        dataSource = new HikariDataSource(hikariConfig);
        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");

        userRepository = new JdbcUserRepository(dataSource);
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Test
    @DisplayName("Should save and retrieve a new student user successfully")
    void shouldSaveAndRetrieveUserSuccessfully() {
        User user = User.create("alice@byteforce.com", "$2a$12$securehash1", "Alice Smith", Role.STUDENT);

        User saved = userRepository.save(user);
        assertNotNull(saved);
        assertEquals(user.getId(), saved.getId());

        Optional<User> retrievedOpt = userRepository.findById(user.getId());
        assertTrue(retrievedOpt.isPresent(), "User should be found by ID");

        User retrieved = retrievedOpt.get();
        assertEquals(user.getId(), retrieved.getId());
        assertEquals("alice@byteforce.com", retrieved.getEmail());
        assertEquals("$2a$12$securehash1", retrieved.getPasswordHash());
        assertEquals("Alice Smith", retrieved.getFullName());
        assertEquals(Role.STUDENT, retrieved.getRole());

        // Compare timestamps truncated to seconds to account for database TIMESTAMP resolution
        assertEquals(user.getCreatedAt().truncatedTo(ChronoUnit.SECONDS),
                retrieved.getCreatedAt().truncatedTo(ChronoUnit.SECONDS));
        assertEquals(user.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS),
                retrieved.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("Should find user by email with case-insensitivity and whitespace normalization")
    void shouldFindUserByEmailWithCaseInsensitiveAndWhitespaceTolerance() {
        User user = User.create("candidate.bob@byteforce.com", "$2a$12$securehash2", "Bob Candidate", Role.STUDENT);
        userRepository.save(user);

        Optional<User> foundExact = userRepository.findByEmail("candidate.bob@byteforce.com");
        assertTrue(foundExact.isPresent());
        assertEquals(user.getId(), foundExact.get().getId());

        Optional<User> foundUppercaseWithSpaces = userRepository.findByEmail("   CANDIDATE.BOB@BYTEFORCE.COM   ");
        assertTrue(foundUppercaseWithSpaces.isPresent());
        assertEquals(user.getId(), foundUppercaseWithSpaces.get().getId());
    }

    @Test
    @DisplayName("Should find user by ID")
    void shouldFindUserById() {
        User user = User.create("test.id@byteforce.com", "$2a$12$idhash", "ID Test", Role.STUDENT);
        userRepository.save(user);

        Optional<User> found = userRepository.findById(user.getId());
        assertTrue(found.isPresent());
        assertEquals(user.getId(), found.get().getId());
        assertEquals(user, found.get());
    }

    @Test
    @DisplayName("Should return empty Optional for missing user or invalid lookup queries")
    void shouldReturnEmptyOptionalForMissingUser() {
        Optional<User> missingById = userRepository.findById(UUID.randomUUID());
        assertFalse(missingById.isPresent());

        Optional<User> nullId = userRepository.findById(null);
        assertFalse(nullId.isPresent());

        Optional<User> missingByEmail = userRepository.findByEmail("unknown@byteforce.com");
        assertFalse(missingByEmail.isPresent());

        Optional<User> nullEmail = userRepository.findByEmail(null);
        assertFalse(nullEmail.isPresent());

        Optional<User> blankEmail = userRepository.findByEmail("   ");
        assertFalse(blankEmail.isPresent());
    }

    @Test
    @DisplayName("Should throw DuplicateUserException when saving user with existing email")
    void shouldThrowDuplicateUserExceptionOnDuplicateEmail() {
        User original = User.create("duplicate@byteforce.com", "$2a$12$firsthash", "Original User", Role.STUDENT);
        userRepository.save(original);

        User duplicateSameCase = User.create("duplicate@byteforce.com", "$2a$12$secondhash", "Second User", Role.STUDENT);
        assertThrows(DuplicateUserException.class, () -> userRepository.save(duplicateSameCase));

        User duplicateDifferentCase = User.create("  DUPLICATE@BYTEFORCE.COM  ", "$2a$12$thirdhash", "Third User", Role.STUDENT);
        assertThrows(DuplicateUserException.class, () -> userRepository.save(duplicateDifferentCase));
    }

    @Test
    @DisplayName("Should accurately persist complex BCrypt password hashes")
    void shouldPersistPasswordHashAccurately() {
        String complexHash = "$2a$12$e8k3K8k0v.6ZtqFvW7wEBe/q4z2u3n4p5r6s7t8u9v0w1x2y3z4a5";
        User user = User.create("hashcheck@byteforce.com", complexHash, "Hash Checker", Role.STUDENT);
        userRepository.save(user);

        User loaded = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(complexHash, loaded.getPasswordHash());
    }

    @Test
    @DisplayName("Should persist and distinguish between STUDENT and ADMIN roles")
    void shouldPersistRolesBothStudentAndAdmin() {
        User student = User.create("student.role@byteforce.com", "$2a$12$hash1", "Student User", Role.STUDENT);
        User admin = User.create("admin.role@byteforce.com", "$2a$12$hash2", "Admin User", Role.ADMIN);

        userRepository.save(student);
        userRepository.save(admin);

        User loadedStudent = userRepository.findById(student.getId()).orElseThrow();
        assertEquals(Role.STUDENT, loadedStudent.getRole());

        User loadedAdmin = userRepository.findById(admin.getId()).orElseThrow();
        assertEquals(Role.ADMIN, loadedAdmin.getRole());
    }

    @Test
    @DisplayName("Should update existing user attributes when saving an already existing entity")
    void shouldUpdateExistingUserSuccessfully() {
        User initial = User.create("update.target@byteforce.com", "$2a$12$oldhash", "Old Name", Role.STUDENT);
        userRepository.save(initial);

        User updated = initial
                .withFullName("Updated Full Name")
                .withPasswordHash("$2a$12$newhash")
                .withRole(Role.ADMIN);

        userRepository.save(updated);

        User reloaded = userRepository.findById(initial.getId()).orElseThrow();
        assertEquals("Updated Full Name", reloaded.getFullName());
        assertEquals("$2a$12$newhash", reloaded.getPasswordHash());
        assertEquals(Role.ADMIN, reloaded.getRole());
        assertEquals("update.target@byteforce.com", reloaded.getEmail());
    }

    @Test
    @DisplayName("Should throw DuplicateUserException when updating a user's email to one already registered")
    void shouldThrowDuplicateUserExceptionWhenUpdatingToAlreadyTakenEmail() {
        User user1 = User.create("user1@byteforce.com", "$2a$12$hash1", "User One", Role.STUDENT);
        User user2 = User.create("user2@byteforce.com", "$2a$12$hash2", "User Two", Role.STUDENT);

        userRepository.save(user1);
        userRepository.save(user2);

        User conflictingUpdate = new User(
                user2.getId(),
                "user1@byteforce.com",
                user2.getPasswordHash(),
                user2.getFullName(),
                user2.getRole(),
                user2.getCreatedAt(),
                Instant.now()
        );

        assertThrows(DuplicateUserException.class, () -> userRepository.save(conflictingUpdate));
    }

    @Test
    @DisplayName("Should reject null user entity on save")
    void shouldRejectNullUserOnSave() {
        assertThrows(NullPointerException.class, () -> userRepository.save(null));
    }

    @Test
    @DisplayName("Should throw ByteForceException on database connection or query failures")
    void shouldHandleDatabaseFailuresGracefully() throws SQLException {
        DataSource brokenDataSource = mock(DataSource.class);
        when(brokenDataSource.getConnection()).thenThrow(new SQLException("Simulated connection timeout"));

        JdbcUserRepository brokenRepo = new JdbcUserRepository(brokenDataSource);

        assertThrows(ByteForceException.class, () -> brokenRepo.findById(UUID.randomUUID()));
        assertThrows(ByteForceException.class, () -> brokenRepo.findByEmail("test@byteforce.com"));

        User user = User.create("fail@byteforce.com", "$2a$12$hash", "Failure Test", Role.STUDENT);
        assertThrows(ByteForceException.class, () -> brokenRepo.save(user));
    }
}
