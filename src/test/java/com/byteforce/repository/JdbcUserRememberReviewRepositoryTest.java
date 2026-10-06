package com.byteforce.repository;

import com.byteforce.domain.ReviewRating;
import com.byteforce.domain.ReviewState;
import com.byteforce.domain.UserRememberReview;
import com.byteforce.persistence.DatabaseMigrator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcUserRememberReviewRepositoryTest {

    private HikariDataSource dataSource;
    private JdbcUserRememberReviewRepository repository;
    private UUID testUserId;
    private long testConceptId;
    private long testRememberItemId1;
    private long testRememberItemId2;

    @BeforeEach
    void setUp() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setPoolName("ReviewRepoTestPool-" + UUID.randomUUID());
        config.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(3);
        dataSource = new HikariDataSource(config);

        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");
        repository = new JdbcUserRememberReviewRepository(dataSource);

        testUserId = UUID.randomUUID();
        seedTestData();
    }

    private void seedTestData() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            // Seed user
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)")) {
                ps.setString(1, testUserId.toString());
                ps.setString(2, "review.test@byteforce.com");
                ps.setString(3, "dummyhash");
                ps.executeUpdate();
            }

            // Seed subject & topic & concept
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO subjects (id, name, description) VALUES ('test-sub', 'Test Subject', 'Desc')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO topics (name, slug, subject_id) VALUES ('Test Topic', 'test-topic', 'test-sub')", PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.executeUpdate();
                var rs = ps.getGeneratedKeys();
                rs.next();
                long topicId = rs.getLong(1);

                try (PreparedStatement ps2 = conn.prepareStatement("INSERT INTO concepts (topic_id, title, slug, short_explanation, display_order) VALUES (?, 'Deadlock', 'deadlock', 'Deadlock explanation', 1)", PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps2.setLong(1, topicId);
                    ps2.executeUpdate();
                    var rs2 = ps2.getGeneratedKeys();
                    rs2.next();
                    testConceptId = rs2.getLong(1);
                }
            }

            // Seed two remember items
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO remember_items (concept_id, type, content, display_order, active) VALUES (?, 'KEY_FACT', 'Mutual exclusion, hold and wait, no preemption, circular wait', 1, true)", PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, testConceptId);
                ps.executeUpdate();
                var rs = ps.getGeneratedKeys();
                rs.next();
                testRememberItemId1 = rs.getLong(1);
            }

            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO remember_items (concept_id, type, content, display_order, active) VALUES (?, 'COMMON_CONFUSION', 'Deadlock vs Starvation', 2, true)", PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, testConceptId);
                ps.executeUpdate();
                var rs = ps.getGeneratedKeys();
                rs.next();
                testRememberItemId2 = rs.getLong(1);
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
    @DisplayName("Save and retrieve user review state by user and remember item")
    void testSaveAndRetrieve() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        UserRememberReview review = UserRememberReview.createInitial(testUserId, testRememberItemId1, now);

        UserRememberReview saved = repository.save(review);
        assertTrue(saved.getId() > 0);

        Optional<UserRememberReview> fetched = repository.findByUserIdAndRememberItemId(testUserId, testRememberItemId1);
        assertTrue(fetched.isPresent());
        assertEquals(saved.getId(), fetched.get().getId());
        assertEquals(testUserId, fetched.get().getUserId());
        assertEquals(testRememberItemId1, fetched.get().getRememberItemId());
        assertEquals(ReviewState.NEW, fetched.get().getState());
    }

    @Test
    @DisplayName("Unique user + remember_item constraint: saving existing user/item updates rather than duplicates")
    void testUniqueUserItemConstraint() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        UserRememberReview review1 = UserRememberReview.createInitial(testUserId, testRememberItemId1, now);
        UserRememberReview saved1 = repository.save(review1);

        // Attempt to insert duplicate initial row for same user and remember item
        UserRememberReview review2 = UserRememberReview.createInitial(testUserId, testRememberItemId1, now.plus(1, ChronoUnit.DAYS));
        UserRememberReview saved2 = repository.save(review2);

        assertEquals(saved1.getId(), saved2.getId(), "Should update existing record without throwing duplicate constraint failure");
        assertEquals(1, repository.countReviewsByUserId(testUserId));
    }

    @Test
    @DisplayName("Query due reviews: returns only items where due_at <= cutoff for the specific user")
    void testFindDueReviews() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        Instant tomorrow = now.plus(1, ChronoUnit.DAYS);

        // item 1 is due (yesterday)
        repository.save(UserRememberReview.createInitial(testUserId, testRememberItemId1, yesterday));
        // item 2 is not due (tomorrow)
        repository.save(UserRememberReview.createInitial(testUserId, testRememberItemId2, tomorrow));

        List<UserRememberReview> due = repository.findDueReviewsByUserId(testUserId, now, 10);
        assertEquals(1, due.size());
        assertEquals(testRememberItemId1, due.get(0).getRememberItemId());

        long dueCount = repository.countDueReviewsByUserId(testUserId, now);
        assertEquals(1L, dueCount);
    }

    @Test
    @DisplayName("User isolation: User A cannot access or query User B's review states")
    void testUserIsolation() throws SQLException {
        UUID otherUserId = UUID.randomUUID();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)")) {
            ps.setString(1, otherUserId.toString());
            ps.setString(2, "other@byteforce.com");
            ps.setString(3, "dummyhash");
            ps.executeUpdate();
        }

        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        repository.save(UserRememberReview.createInitial(testUserId, testRememberItemId1, now));

        // otherUser queries reviews
        List<UserRememberReview> otherReviews = repository.findByUserId(otherUserId);
        assertTrue(otherReviews.isEmpty(), "Other user should see 0 reviews");

        Optional<UserRememberReview> otherFetch = repository.findByUserIdAndRememberItemId(otherUserId, testRememberItemId1);
        assertFalse(otherFetch.isPresent(), "Other user cannot retrieve another user's review state");
    }

    @Test
    @DisplayName("Update review state with review results")
    void testUpdateReviewResult() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        UserRememberReview initial = repository.save(UserRememberReview.createInitial(testUserId, testRememberItemId1, now));

        Instant nextDue = now.plus(3, ChronoUnit.DAYS);
        UserRememberReview updated = initial.withReviewResult(
                ReviewRating.GOOD,
                ReviewState.REVIEW,
                4.8,
                3.2,
                0.9,
                nextDue,
                now
        );

        UserRememberReview saved = repository.save(updated);
        assertEquals(1, saved.getReviewCount());
        assertEquals(1, saved.getSuccessfulReviewCount());
        assertEquals(ReviewState.REVIEW, saved.getState());
        assertEquals(ReviewRating.GOOD, saved.getLastRating());
        assertEquals(4.8, saved.getDifficulty());
        assertEquals(3.2, saved.getStability());

        Optional<UserRememberReview> reloaded = repository.findById(saved.getId());
        assertTrue(reloaded.isPresent());
        assertEquals(ReviewState.REVIEW, reloaded.get().getState());
        assertEquals(ReviewRating.GOOD, reloaded.get().getLastRating());
    }

    @Test
    @DisplayName("Delete review state by ID and by user/item")
    void testDelete() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        UserRememberReview saved = repository.save(UserRememberReview.createInitial(testUserId, testRememberItemId1, now));
        assertTrue(repository.deleteById(saved.getId()));
        assertFalse(repository.findById(saved.getId()).isPresent());
    }
}
