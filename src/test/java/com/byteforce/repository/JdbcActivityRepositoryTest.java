package com.byteforce.repository;

import com.byteforce.domain.Activity;
import com.byteforce.domain.ActivityType;
import com.byteforce.domain.Role;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcActivityRepositoryTest {

    private HikariDataSource dataSource;
    private JdbcUserRepository userRepository;
    private JdbcActivityRepository activityRepository;

    private User defaultUser;

    @BeforeEach
    void setUp() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("ActivityRepoTestPool-" + UUID.randomUUID());
        hikariConfig.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        hikariConfig.setUsername("sa");
        hikariConfig.setPassword("");
        hikariConfig.setMaximumPoolSize(5);

        dataSource = new HikariDataSource(hikariConfig);
        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");

        userRepository = new JdbcUserRepository(dataSource);
        activityRepository = new JdbcActivityRepository(dataSource);

        defaultUser = userRepository.save(User.create("active.user@byteforce.com", "$2a$12$hash", "Active User", Role.STUDENT));
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Test
    @DisplayName("Should save and retrieve an activity successfully")
    void shouldSaveAndRetrieveActivitySuccessfully() {
        Activity activity = Activity.create(defaultUser.getId(), ActivityType.SOLVED_QUESTION, "Solved 'Two Sum' in 15ms");
        Activity saved = activityRepository.save(activity);

        assertNotNull(saved);
        assertTrue(saved.getId() > 0);
        assertEquals(defaultUser.getId(), saved.getUserId());
        assertEquals(ActivityType.SOLVED_QUESTION, saved.getActivityType());
        assertEquals("Solved 'Two Sum' in 15ms", saved.getDescription());

        Optional<Activity> retrievedOpt = activityRepository.findById(saved.getId());
        assertTrue(retrievedOpt.isPresent());

        Activity retrieved = retrievedOpt.get();
        assertEquals(saved.getId(), retrieved.getId());
        assertEquals(defaultUser.getId(), retrieved.getUserId());
        assertEquals(ActivityType.SOLVED_QUESTION, retrieved.getActivityType());
        assertEquals("Solved 'Two Sum' in 15ms", retrieved.getDescription());
        assertEquals(activity.getCreatedAt().truncatedTo(ChronoUnit.SECONDS),
                retrieved.getCreatedAt().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("Should find recent activities with limit ordered by created_at DESC")
    void shouldFindRecentActivitiesWithLimit() {
        for (int i = 1; i <= 5; i++) {
            activityRepository.save(Activity.create(defaultUser.getId(), ActivityType.ATTEMPTED_QUESTION, "Attempt " + i));
        }

        List<Activity> recent3 = activityRepository.findRecentByUserId(defaultUser.getId(), 3);
        assertEquals(3, recent3.size());

        List<Activity> recent10 = activityRepository.findRecentByUserId(defaultUser.getId(), 10);
        assertEquals(5, recent10.size());

        assertTrue(activityRepository.findRecentByUserId(null, 5).isEmpty());
        assertTrue(activityRepository.findRecentByUserId(defaultUser.getId(), 0).isEmpty());
        assertTrue(activityRepository.findRecentByUserId(defaultUser.getId(), -1).isEmpty());
    }

    @Test
    @DisplayName("Should find all activities for user")
    void shouldFindByUserId() {
        activityRepository.save(Activity.create(defaultUser.getId(), ActivityType.REGISTRATION, "Joined ByteForce"));
        activityRepository.save(Activity.create(defaultUser.getId(), ActivityType.LOGIN, "Logged in"));

        List<Activity> list = activityRepository.findByUserId(defaultUser.getId());
        assertEquals(2, list.size());

        assertTrue(activityRepository.findByUserId(null).isEmpty());
        assertTrue(activityRepository.findByUserId(UUID.randomUUID()).isEmpty());
    }

    @Test
    @DisplayName("Should count activities for user")
    void shouldCountByUserId() {
        assertEquals(0, activityRepository.countByUserId(defaultUser.getId()));

        activityRepository.save(Activity.create(defaultUser.getId(), ActivityType.BOOKMARK_ADDED, "Bookmarked Q1"));
        assertEquals(1, activityRepository.countByUserId(defaultUser.getId()));

        assertEquals(0, activityRepository.countByUserId(null));
        assertEquals(0, activityRepository.countByUserId(UUID.randomUUID()));
    }

    @Test
    @DisplayName("Should delete activity by ID")
    void shouldDeleteById() {
        Activity saved = activityRepository.save(Activity.create(defaultUser.getId(), ActivityType.LOGIN, "Session started"));

        assertTrue(activityRepository.deleteById(saved.getId()));
        assertFalse(activityRepository.findById(saved.getId()).isPresent());
        assertFalse(activityRepository.deleteById(saved.getId()));
        assertFalse(activityRepository.deleteById(-1));
        assertFalse(activityRepository.deleteById(0));
    }

    @Test
    @DisplayName("Should throw ByteForceException on foreign key constraint violation")
    void shouldThrowExceptionOnInvalidForeignKey() {
        Activity orphan = Activity.create(UUID.randomUUID(), ActivityType.LOGIN, "Ghost");
        assertThrows(ByteForceException.class, () -> activityRepository.save(orphan));
    }

    @Test
    @DisplayName("Should handle database errors gracefully")
    void shouldHandleDatabaseErrorsGracefully() throws SQLException {
        DataSource brokenDataSource = mock(DataSource.class);
        when(brokenDataSource.getConnection()).thenThrow(new SQLException("Simulated connection error"));

        JdbcActivityRepository brokenRepo = new JdbcActivityRepository(brokenDataSource);

        assertThrows(ByteForceException.class, () -> brokenRepo.findById(1L));
        assertThrows(ByteForceException.class, () -> brokenRepo.findByUserId(UUID.randomUUID()));
        assertThrows(ByteForceException.class, () -> brokenRepo.findRecentByUserId(UUID.randomUUID(), 5));
        assertThrows(ByteForceException.class, () -> brokenRepo.countByUserId(UUID.randomUUID()));
        assertThrows(ByteForceException.class, () -> brokenRepo.deleteById(1L));
    }
}
