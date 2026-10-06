package com.byteforce.repository;

import com.byteforce.domain.ContinueLearningView;
import com.byteforce.domain.RecentlyViewedConcept;
import com.byteforce.domain.Role;
import com.byteforce.domain.User;
import com.byteforce.domain.UserConceptProgress;
import com.byteforce.persistence.DatabaseMigrator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcUserConceptProgressRepositoryTest {

    private HikariDataSource dataSource;
    private JdbcUserRepository userRepository;
    private JdbcUserConceptProgressRepository progressRepository;
    private User testUser;
    private long testTopicId;
    private long testConceptId1;
    private long testConceptId2;

    @BeforeEach
    void setUp() throws Exception {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("ConceptProgressRepoTestPool-" + UUID.randomUUID());
        hikariConfig.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        hikariConfig.setUsername("sa");
        hikariConfig.setPassword("");
        hikariConfig.setMaximumPoolSize(5);

        dataSource = new HikariDataSource(hikariConfig);
        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");

        userRepository = new JdbcUserRepository(dataSource);
        progressRepository = new JdbcUserConceptProgressRepository(dataSource);

        testUser = userRepository.save(User.create(
                "student-" + UUID.randomUUID() + "@byteforce.com",
                "hashed-password",
                "Test Student",
                Role.STUDENT));

        try (Connection conn = dataSource.getConnection()) {
            // Seed subject
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO subjects (id, name, description) VALUES ('dsa', 'Data Structures', 'DSA Desc')")) {
                ps.executeUpdate();
            }
            // Seed topic
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO topics (name, slug, subject_id) VALUES ('Arrays', 'arrays', 'dsa')", PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                rs.next();
                testTopicId = rs.getLong(1);
            }
            // Seed concept 1
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO concepts (topic_id, title, slug, short_explanation, display_order) VALUES (?, 'Prefix Sum', 'prefix-sum', 'Prefix explanation', 1)", PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, testTopicId);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                rs.next();
                testConceptId1 = rs.getLong(1);
            }
            // Seed concept 2
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO concepts (topic_id, title, slug, short_explanation, display_order) VALUES (?, 'Two Pointers', 'two-pointers', 'Pointers explanation', 2)", PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, testTopicId);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                rs.next();
                testConceptId2 = rs.getLong(1);
            }
        }
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Test
    @DisplayName("Record view persists and retrieves progress record")
    void testRecordViewAndFind() {
        progressRepository.recordView(testUser.getId(), testConceptId1);

        Optional<UserConceptProgress> opt = progressRepository.findByUserAndConcept(testUser.getId(), testConceptId1);
        assertTrue(opt.isPresent());
        UserConceptProgress p = opt.get();
        assertEquals(testUser.getId(), p.getUserId());
        assertEquals(testConceptId1, p.getConceptId());
        assertFalse(p.isCompleted());
        assertNotNull(p.getLastViewedAt());
    }

    @Test
    @DisplayName("Marking as learned sets completed and timestamp")
    void testSetCompleted() {
        progressRepository.recordView(testUser.getId(), testConceptId1);

        progressRepository.setCompleted(testUser.getId(), testConceptId1, true);
        Optional<UserConceptProgress> opt = progressRepository.findByUserAndConcept(testUser.getId(), testConceptId1);
        assertTrue(opt.isPresent());
        assertTrue(opt.get().isCompleted());
        assertNotNull(opt.get().getCompletedAt());

        // Toggle back to uncompleted
        progressRepository.setCompleted(testUser.getId(), testConceptId1, false);
        opt = progressRepository.findByUserAndConcept(testUser.getId(), testConceptId1);
        assertTrue(opt.isPresent());
        assertFalse(opt.get().isCompleted());
    }

    @Test
    @DisplayName("Recently viewed returns ordered history")
    void testFindRecentlyViewed() throws Exception {
        progressRepository.recordView(testUser.getId(), testConceptId1);
        Thread.sleep(10); // Ensure distinct timestamp
        progressRepository.recordView(testUser.getId(), testConceptId2);

        List<RecentlyViewedConcept> list = progressRepository.findRecentlyViewed(testUser.getId(), 5);
        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals(testConceptId2, list.get(0).conceptId());
        assertEquals(testConceptId1, list.get(1).conceptId());
    }

    @Test
    @DisplayName("Continue learning returns most recent unfinished concept")
    void testFindContinueLearning() throws Exception {
        progressRepository.recordView(testUser.getId(), testConceptId1);
        Thread.sleep(10);
        progressRepository.recordView(testUser.getId(), testConceptId2);

        // Mark concept 2 as completed
        progressRepository.setCompleted(testUser.getId(), testConceptId2, true);

        // Continue learning should return concept 1 since concept 2 is completed
        Optional<ContinueLearningView> contOpt = progressRepository.findContinueLearning(testUser.getId());
        assertTrue(contOpt.isPresent());
        assertEquals(testConceptId1, contOpt.get().conceptId());
    }

    @Test
    @DisplayName("Completion map and counts return accurate statistics")
    void testCompletionCounts() {
        progressRepository.recordView(testUser.getId(), testConceptId1);
        progressRepository.setCompleted(testUser.getId(), testConceptId1, true);

        long topicCompleted = progressRepository.countCompletedByTopic(testUser.getId(), testTopicId);
        assertEquals(1L, topicCompleted);

        long subjectCompleted = progressRepository.countCompletedBySubject(testUser.getId(), "dsa");
        assertEquals(1L, subjectCompleted);

        Map<Long, Boolean> map = progressRepository.getCompletionMapForTopic(testUser.getId(), testTopicId);
        assertNotNull(map);
        assertTrue(map.getOrDefault(testConceptId1, false));
        assertFalse(map.getOrDefault(testConceptId2, false));
    }
}
