package com.byteforce.repository;

import com.byteforce.domain.Role;
import com.byteforce.domain.StudentProfile;
import com.byteforce.domain.User;
import com.byteforce.exception.ByteForceException;
import com.byteforce.persistence.DatabaseMigrator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcStudentProfileRepositoryTest {

    private HikariDataSource dataSource;
    private JdbcUserRepository userRepository;
    private JdbcStudentProfileRepository profileRepository;

    private User defaultUser;

    @BeforeEach
    void setUp() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("ProfileRepoTestPool-" + UUID.randomUUID());
        hikariConfig.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        hikariConfig.setUsername("sa");
        hikariConfig.setPassword("");
        hikariConfig.setMaximumPoolSize(5);

        dataSource = new HikariDataSource(hikariConfig);
        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");

        userRepository = new JdbcUserRepository(dataSource);
        profileRepository = new JdbcStudentProfileRepository(dataSource);

        defaultUser = userRepository.save(User.create("student.profile@byteforce.com", "$2a$12$hash", "Student Profile User", Role.STUDENT));
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Test
    @DisplayName("Should retrieve profile created during user registration and update details successfully")
    void shouldSaveAndRetrieveProfileSuccessfully() {
        // Registration creates an initial profile with full_name
        Optional<StudentProfile> initialOpt = profileRepository.findByUserId(defaultUser.getId());
        assertTrue(initialOpt.isPresent());
        StudentProfile initial = initialOpt.get();
        assertEquals("Student Profile User", initial.getFullName());
        assertNull(initial.getPhone());
        assertNull(initial.getCollege());
        assertNull(initial.getGraduationYear());

        StudentProfile updatedData = initial
                .withFullName("Alex Rivera")
                .withPhone("+1-555-0199")
                .withCollege("MIT")
                .withGraduationYear(2026);

        StudentProfile saved = profileRepository.save(updatedData);
        assertEquals("Alex Rivera", saved.getFullName());
        assertEquals("+1-555-0199", saved.getPhone());
        assertEquals("MIT", saved.getCollege());
        assertEquals(2026, saved.getGraduationYear());

        StudentProfile reloaded = profileRepository.findByUserId(defaultUser.getId()).orElseThrow();
        assertEquals(initial.getId(), reloaded.getId());
        assertEquals("Alex Rivera", reloaded.getFullName());
        assertEquals("+1-555-0199", reloaded.getPhone());
        assertEquals("MIT", reloaded.getCollege());
        assertEquals(2026, reloaded.getGraduationYear());
    }

    @Test
    @DisplayName("Should insert a brand new profile if none exists")
    void shouldInsertNewProfile() {
        User user2 = userRepository.save(User.create("u2@byteforce.com", "$2a$12$hash", "User Two", Role.STUDENT));
        // Delete the auto-created profile to test insert path
        profileRepository.deleteByUserId(user2.getId());
        assertFalse(profileRepository.existsByUserId(user2.getId()));

        StudentProfile newProfile = StudentProfile.create(user2.getId(), "User Two Custom", "+91-9876543210", "Stanford", 2025);
        StudentProfile saved = profileRepository.save(newProfile);

        assertNotNull(saved);
        assertTrue(profileRepository.existsByUserId(user2.getId()));

        StudentProfile found = profileRepository.findById(saved.getId()).orElseThrow();
        assertEquals(user2.getId(), found.getUserId());
        assertEquals("User Two Custom", found.getFullName());
        assertEquals("+91-9876543210", found.getPhone());
        assertEquals("Stanford", found.getCollege());
        assertEquals(2025, found.getGraduationYear());
    }

    @Test
    @DisplayName("Should delete student profile by user ID")
    void shouldDeleteByUserId() {
        assertTrue(profileRepository.existsByUserId(defaultUser.getId()));
        assertTrue(profileRepository.deleteByUserId(defaultUser.getId()));
        assertFalse(profileRepository.existsByUserId(defaultUser.getId()));
        assertFalse(profileRepository.deleteByUserId(defaultUser.getId()), "Second delete should return false");
        assertFalse(profileRepository.deleteByUserId(null));
    }

    @Test
    @DisplayName("Should return empty Optional for null ID or missing user ID")
    void shouldHandleMissingAndNullIds() {
        assertFalse(profileRepository.findById(null).isPresent());
        assertFalse(profileRepository.findById(UUID.randomUUID()).isPresent());
        assertFalse(profileRepository.findByUserId(null).isPresent());
        assertFalse(profileRepository.findByUserId(UUID.randomUUID()).isPresent());
        assertFalse(profileRepository.existsByUserId(null));
        assertFalse(profileRepository.existsByUserId(UUID.randomUUID()));
    }

    @Test
    @DisplayName("Should throw ByteForceException on foreign key violation")
    void shouldThrowExceptionOnNonExistentUser() {
        StudentProfile orphan = StudentProfile.create(UUID.randomUUID(), "Ghost", null, null, null);
        assertThrows(ByteForceException.class, () -> profileRepository.save(orphan));
    }

    @Test
    @DisplayName("Should throw NullPointerException on null profile save")
    void shouldRejectNullProfileOnSave() {
        assertThrows(NullPointerException.class, () -> profileRepository.save(null));
    }

    @Test
    @DisplayName("Should handle database errors gracefully")
    void shouldHandleDatabaseErrorsGracefully() throws SQLException {
        DataSource brokenDataSource = mock(DataSource.class);
        when(brokenDataSource.getConnection()).thenThrow(new SQLException("Simulated connection error"));

        JdbcStudentProfileRepository brokenRepo = new JdbcStudentProfileRepository(brokenDataSource);

        assertThrows(ByteForceException.class, () -> brokenRepo.findById(UUID.randomUUID()));
        assertThrows(ByteForceException.class, () -> brokenRepo.findByUserId(UUID.randomUUID()));
        assertThrows(ByteForceException.class, () -> brokenRepo.existsByUserId(UUID.randomUUID()));
        assertThrows(ByteForceException.class, () -> brokenRepo.deleteByUserId(UUID.randomUUID()));
    }
}
