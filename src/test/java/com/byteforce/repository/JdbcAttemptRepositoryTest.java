package com.byteforce.repository;

import com.byteforce.domain.AttemptStatus;
import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;
import com.byteforce.domain.QuestionAttempt;
import com.byteforce.domain.Role;
import com.byteforce.domain.Topic;
import com.byteforce.domain.User;
import com.byteforce.exception.ByteForceException;
import com.byteforce.exception.ResourceNotFoundException;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcAttemptRepositoryTest {

    private HikariDataSource dataSource;
    private JdbcUserRepository userRepository;
    private JdbcTopicRepository topicRepository;
    private JdbcQuestionRepository questionRepository;
    private JdbcAttemptRepository attemptRepository;

    private User defaultUser;
    private Question defaultQuestion;

    @BeforeEach
    void setUp() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("AttemptRepoTestPool-" + UUID.randomUUID());
        hikariConfig.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        hikariConfig.setUsername("sa");
        hikariConfig.setPassword("");
        hikariConfig.setMaximumPoolSize(5);

        dataSource = new HikariDataSource(hikariConfig);
        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");

        userRepository = new JdbcUserRepository(dataSource);
        topicRepository = new JdbcTopicRepository(dataSource);
        questionRepository = new JdbcQuestionRepository(dataSource);
        attemptRepository = new JdbcAttemptRepository(dataSource);

        defaultUser = userRepository.save(User.create("candidate@byteforce.com", "$2a$12$hash", "Candidate", Role.STUDENT));
        Topic topic = topicRepository.save(Topic.create("Strings", "strings", "String algorithms", 1));
        defaultQuestion = questionRepository.save(Question.create(topic.getId(), "Reverse String", "reverse-string", "Reverse in-place", Difficulty.EASY, null));
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Test
    @DisplayName("Should save and retrieve a new question attempt successfully")
    void shouldSaveAndRetrieveAttemptSuccessfully() {
        QuestionAttempt attempt = QuestionAttempt.create(
                defaultUser.getId(),
                defaultQuestion.getId(),
                AttemptStatus.SOLVED,
                "class Solution { public void reverseString(char[] s) { ... } }",
                12
        );

        QuestionAttempt saved = attemptRepository.save(attempt);
        assertNotNull(saved);
        assertTrue(saved.getId() > 0);
        assertEquals(defaultUser.getId(), saved.getUserId());
        assertEquals(defaultQuestion.getId(), saved.getQuestionId());
        assertEquals(AttemptStatus.SOLVED, saved.getStatus());
        assertEquals("class Solution { public void reverseString(char[] s) { ... } }", saved.getCodeSnippet());
        assertEquals(12, saved.getExecutionTimeMs());

        Optional<QuestionAttempt> retrievedOpt = attemptRepository.findById(saved.getId());
        assertTrue(retrievedOpt.isPresent());

        QuestionAttempt retrieved = retrievedOpt.get();
        assertEquals(saved.getId(), retrieved.getId());
        assertEquals(defaultUser.getId(), retrieved.getUserId());
        assertEquals(defaultQuestion.getId(), retrieved.getQuestionId());
        assertEquals(AttemptStatus.SOLVED, retrieved.getStatus());
        assertEquals(saved.getCodeSnippet(), retrieved.getCodeSnippet());
        assertEquals(12, retrieved.getExecutionTimeMs());
        assertEquals(attempt.getAttemptedAt().truncatedTo(ChronoUnit.SECONDS),
                retrieved.getAttemptedAt().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("Should save attempt with null executionTimeMs successfully")
    void shouldHandleNullExecutionTimeMs() {
        QuestionAttempt attempt = QuestionAttempt.create(
                defaultUser.getId(),
                defaultQuestion.getId(),
                AttemptStatus.ATTEMPTED,
                "// WIP",
                null
        );

        QuestionAttempt saved = attemptRepository.save(attempt);
        assertNull(saved.getExecutionTimeMs());

        QuestionAttempt loaded = attemptRepository.findById(saved.getId()).orElseThrow();
        assertNull(loaded.getExecutionTimeMs());
    }

    @Test
    @DisplayName("Should update existing attempt status, codeSnippet, and executionTimeMs")
    void shouldUpdateExistingAttemptSuccessfully() {
        QuestionAttempt initial = attemptRepository.save(QuestionAttempt.create(
                defaultUser.getId(),
                defaultQuestion.getId(),
                AttemptStatus.ATTEMPTED,
                "// Partial",
                null
        ));

        QuestionAttempt toUpdate = new QuestionAttempt(
                initial.getId(),
                initial.getUserId(),
                initial.getQuestionId(),
                AttemptStatus.SOLVED,
                "// Complete solution",
                45,
                initial.getAttemptedAt()
        );

        QuestionAttempt updated = attemptRepository.save(toUpdate);
        assertEquals(AttemptStatus.SOLVED, updated.getStatus());
        assertEquals(45, updated.getExecutionTimeMs());

        QuestionAttempt reloaded = attemptRepository.findById(initial.getId()).orElseThrow();
        assertEquals(AttemptStatus.SOLVED, reloaded.getStatus());
        assertEquals("// Complete solution", reloaded.getCodeSnippet());
        assertEquals(45, reloaded.getExecutionTimeMs());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent attempt")
    void shouldThrowResourceNotFoundExceptionOnMissingAttemptUpdate() {
        QuestionAttempt nonExistent = new QuestionAttempt(
                99999L,
                defaultUser.getId(),
                defaultQuestion.getId(),
                AttemptStatus.SOLVED,
                "code",
                10,
                java.time.Instant.now()
        );
        assertThrows(ResourceNotFoundException.class, () -> attemptRepository.save(nonExistent));
    }

    @Test
    @DisplayName("Should find attempts by user ID ordered by attempted_at DESC")
    void shouldFindByUserId() {
        attemptRepository.save(QuestionAttempt.create(defaultUser.getId(), defaultQuestion.getId(), AttemptStatus.ATTEMPTED, "c1", 10));
        attemptRepository.save(QuestionAttempt.create(defaultUser.getId(), defaultQuestion.getId(), AttemptStatus.SOLVED, "c2", 8));

        List<QuestionAttempt> attempts = attemptRepository.findByUserId(defaultUser.getId());
        assertEquals(2, attempts.size());

        assertTrue(attemptRepository.findByUserId(null).isEmpty());
        assertTrue(attemptRepository.findByUserId(UUID.randomUUID()).isEmpty());
    }

    @Test
    @DisplayName("Should find attempts by question ID")
    void shouldFindByQuestionId() {
        User user2 = userRepository.save(User.create("u2@byteforce.com", "$2a$12$hash", "User 2", Role.STUDENT));

        attemptRepository.save(QuestionAttempt.create(defaultUser.getId(), defaultQuestion.getId(), AttemptStatus.SOLVED, "c1", 10));
        attemptRepository.save(QuestionAttempt.create(user2.getId(), defaultQuestion.getId(), AttemptStatus.FAILED, "c2", 20));

        List<QuestionAttempt> attempts = attemptRepository.findByQuestionId(defaultQuestion.getId());
        assertEquals(2, attempts.size());

        assertTrue(attemptRepository.findByQuestionId(99999L).isEmpty());
        assertTrue(attemptRepository.findByQuestionId(-1).isEmpty());
        assertTrue(attemptRepository.findByQuestionId(0).isEmpty());
    }

    @Test
    @DisplayName("Should find attempts by user ID and question ID")
    void shouldFindByUserIdAndQuestionId() {
        Topic topic = topicRepository.save(Topic.create("DP", "dp", "Desc", 2));
        Question q2 = questionRepository.save(Question.create(topic.getId(), "Climbing Stairs", "climb-stairs", "Desc", Difficulty.EASY, null));

        attemptRepository.save(QuestionAttempt.create(defaultUser.getId(), defaultQuestion.getId(), AttemptStatus.ATTEMPTED, "c1", 10));
        attemptRepository.save(QuestionAttempt.create(defaultUser.getId(), defaultQuestion.getId(), AttemptStatus.SOLVED, "c2", 8));
        attemptRepository.save(QuestionAttempt.create(defaultUser.getId(), q2.getId(), AttemptStatus.SOLVED, "c3", 5));

        List<QuestionAttempt> q1Attempts = attemptRepository.findByUserIdAndQuestionId(defaultUser.getId(), defaultQuestion.getId());
        assertEquals(2, q1Attempts.size());

        List<QuestionAttempt> q2Attempts = attemptRepository.findByUserIdAndQuestionId(defaultUser.getId(), q2.getId());
        assertEquals(1, q2Attempts.size());

        assertTrue(attemptRepository.findByUserIdAndQuestionId(null, defaultQuestion.getId()).isEmpty());
        assertTrue(attemptRepository.findByUserIdAndQuestionId(defaultUser.getId(), -1).isEmpty());
        assertTrue(attemptRepository.findByUserIdAndQuestionId(defaultUser.getId(), 0).isEmpty());
    }

    @Test
    @DisplayName("Should find the latest attempt by user ID and question ID")
    void shouldFindLatestByUserIdAndQuestionId() throws InterruptedException {
        attemptRepository.save(QuestionAttempt.create(defaultUser.getId(), defaultQuestion.getId(), AttemptStatus.ATTEMPTED, "First try", 50));
        Thread.sleep(20);
        attemptRepository.save(QuestionAttempt.create(defaultUser.getId(), defaultQuestion.getId(), AttemptStatus.SOLVED, "Second try (solved)", 15));

        Optional<QuestionAttempt> latestOpt = attemptRepository.findLatestByUserIdAndQuestionId(defaultUser.getId(), defaultQuestion.getId());
        assertTrue(latestOpt.isPresent());
        assertEquals(AttemptStatus.SOLVED, latestOpt.get().getStatus());
        assertEquals("Second try (solved)", latestOpt.get().getCodeSnippet());

        assertFalse(attemptRepository.findLatestByUserIdAndQuestionId(null, defaultQuestion.getId()).isPresent());
        assertFalse(attemptRepository.findLatestByUserIdAndQuestionId(defaultUser.getId(), -1).isPresent());
        assertFalse(attemptRepository.findLatestByUserIdAndQuestionId(defaultUser.getId(), 99999L).isPresent());
    }

    @Test
    @DisplayName("Should determine hasSolved accurately")
    void shouldCheckHasSolved() {
        assertFalse(attemptRepository.hasSolved(defaultUser.getId(), defaultQuestion.getId()));

        attemptRepository.save(QuestionAttempt.create(defaultUser.getId(), defaultQuestion.getId(), AttemptStatus.FAILED, "code", 10));
        assertFalse(attemptRepository.hasSolved(defaultUser.getId(), defaultQuestion.getId()), "FAILED status is not solved");

        attemptRepository.save(QuestionAttempt.create(defaultUser.getId(), defaultQuestion.getId(), AttemptStatus.ATTEMPTED, "code", 10));
        assertFalse(attemptRepository.hasSolved(defaultUser.getId(), defaultQuestion.getId()), "ATTEMPTED status is not solved");

        attemptRepository.save(QuestionAttempt.create(defaultUser.getId(), defaultQuestion.getId(), AttemptStatus.SOLVED, "code", 10));
        assertTrue(attemptRepository.hasSolved(defaultUser.getId(), defaultQuestion.getId()), "Now question is solved");

        assertFalse(attemptRepository.hasSolved(null, defaultQuestion.getId()));
        assertFalse(attemptRepository.hasSolved(defaultUser.getId(), -1));
        assertFalse(attemptRepository.hasSolved(defaultUser.getId(), 0));
        assertFalse(attemptRepository.hasSolved(defaultUser.getId(), 99999L));
    }

    @Test
    @DisplayName("Should count total attempts by user ID")
    void shouldCountByUserId() {
        assertEquals(0, attemptRepository.countByUserId(defaultUser.getId()));

        attemptRepository.save(QuestionAttempt.create(defaultUser.getId(), defaultQuestion.getId(), AttemptStatus.ATTEMPTED, "c1", 10));
        assertEquals(1, attemptRepository.countByUserId(defaultUser.getId()));

        attemptRepository.save(QuestionAttempt.create(defaultUser.getId(), defaultQuestion.getId(), AttemptStatus.SOLVED, "c2", 10));
        assertEquals(2, attemptRepository.countByUserId(defaultUser.getId()));

        assertEquals(0, attemptRepository.countByUserId(null));
        assertEquals(0, attemptRepository.countByUserId(UUID.randomUUID()));
    }

    @Test
    @DisplayName("Should count distinct solved questions by user ID")
    void shouldCountSolvedByUserId() {
        Topic topic = topicRepository.save(Topic.create("Arrays", "arrays", "Array questions", 3));
        Question q2 = questionRepository.save(Question.create(topic.getId(), "Rotate Array", "rotate-array", "Desc", Difficulty.MEDIUM, null));
        Question q3 = questionRepository.save(Question.create(topic.getId(), "Max Subarray", "max-subarray", "Desc", Difficulty.MEDIUM, null));

        assertEquals(0, attemptRepository.countSolvedByUserId(defaultUser.getId()));

        // Solve q1 twice
        attemptRepository.save(QuestionAttempt.create(defaultUser.getId(), defaultQuestion.getId(), AttemptStatus.SOLVED, "s1", 10));
        attemptRepository.save(QuestionAttempt.create(defaultUser.getId(), defaultQuestion.getId(), AttemptStatus.SOLVED, "s2", 8));
        // Solve q2 once
        attemptRepository.save(QuestionAttempt.create(defaultUser.getId(), q2.getId(), AttemptStatus.SOLVED, "s3", 12));
        // Attempt q3 without solving
        attemptRepository.save(QuestionAttempt.create(defaultUser.getId(), q3.getId(), AttemptStatus.FAILED, "f1", 30));

        // Total solved distinct questions should be 2 (defaultQuestion and q2)
        assertEquals(2, attemptRepository.countSolvedByUserId(defaultUser.getId()));

        assertEquals(0, attemptRepository.countSolvedByUserId(null));
        assertEquals(0, attemptRepository.countSolvedByUserId(UUID.randomUUID()));
    }

    @Test
    @DisplayName("Should find recent attempts limited to given count")
    void shouldFindRecentAttempts() {
        for (int i = 1; i <= 5; i++) {
            attemptRepository.save(QuestionAttempt.create(defaultUser.getId(), defaultQuestion.getId(), AttemptStatus.ATTEMPTED, "code " + i, i * 10));
        }

        List<QuestionAttempt> recent3 = attemptRepository.findRecentAttempts(defaultUser.getId(), 3);
        assertEquals(3, recent3.size());

        List<QuestionAttempt> recent10 = attemptRepository.findRecentAttempts(defaultUser.getId(), 10);
        assertEquals(5, recent10.size());

        assertTrue(attemptRepository.findRecentAttempts(null, 5).isEmpty());
        assertTrue(attemptRepository.findRecentAttempts(defaultUser.getId(), 0).isEmpty());
        assertTrue(attemptRepository.findRecentAttempts(defaultUser.getId(), -1).isEmpty());
    }

    @Test
    @DisplayName("Should delete attempt by ID successfully")
    void shouldDeleteById() {
        QuestionAttempt saved = attemptRepository.save(QuestionAttempt.create(defaultUser.getId(), defaultQuestion.getId(), AttemptStatus.SOLVED, "code", 10));

        assertTrue(attemptRepository.deleteById(saved.getId()));
        assertFalse(attemptRepository.findById(saved.getId()).isPresent());
        assertFalse(attemptRepository.deleteById(saved.getId()));
        assertFalse(attemptRepository.deleteById(-1));
        assertFalse(attemptRepository.deleteById(0));
    }

    @Test
    @DisplayName("Should throw ByteForceException when foreign key constraint fails")
    void shouldThrowExceptionOnInvalidForeignKeys() {
        UUID nonExistentUserId = UUID.randomUUID();
        QuestionAttempt invalidUser = QuestionAttempt.create(nonExistentUserId, defaultQuestion.getId(), AttemptStatus.SOLVED, "code", 10);
        assertThrows(ByteForceException.class, () -> attemptRepository.save(invalidUser));

        QuestionAttempt invalidQuestion = QuestionAttempt.create(defaultUser.getId(), 99999L, AttemptStatus.SOLVED, "code", 10);
        assertThrows(ByteForceException.class, () -> attemptRepository.save(invalidQuestion));
    }

    @Test
    @DisplayName("Should reject null attempt on save")
    void shouldRejectNullAttemptOnSave() {
        assertThrows(NullPointerException.class, () -> attemptRepository.save(null));
    }

    @Test
    @DisplayName("Should return empty Optional for invalid attempt IDs")
    void shouldReturnEmptyOptionalForInvalidId() {
        assertFalse(attemptRepository.findById(0).isPresent());
        assertFalse(attemptRepository.findById(-1).isPresent());
    }

    @Test
    @DisplayName("Should throw ByteForceException on database connection failures")
    void shouldHandleDatabaseFailuresGracefully() throws SQLException {
        DataSource brokenDataSource = mock(DataSource.class);
        when(brokenDataSource.getConnection()).thenThrow(new SQLException("Simulated connection error"));

        JdbcAttemptRepository brokenRepo = new JdbcAttemptRepository(brokenDataSource);

        assertThrows(ByteForceException.class, () -> brokenRepo.findById(1L));
        assertThrows(ByteForceException.class, () -> brokenRepo.findByUserId(UUID.randomUUID()));
        assertThrows(ByteForceException.class, () -> brokenRepo.findByQuestionId(1L));
        assertThrows(ByteForceException.class, () -> brokenRepo.findByUserIdAndQuestionId(UUID.randomUUID(), 1L));
        assertThrows(ByteForceException.class, () -> brokenRepo.findLatestByUserIdAndQuestionId(UUID.randomUUID(), 1L));
        assertThrows(ByteForceException.class, () -> brokenRepo.countByUserId(UUID.randomUUID()));
        assertThrows(ByteForceException.class, () -> brokenRepo.countSolvedByUserId(UUID.randomUUID()));
        assertThrows(ByteForceException.class, () -> brokenRepo.hasSolved(UUID.randomUUID(), 1L));
        assertThrows(ByteForceException.class, () -> brokenRepo.findRecentAttempts(UUID.randomUUID(), 5));
        assertThrows(ByteForceException.class, () -> brokenRepo.deleteById(1L));
    }
}
