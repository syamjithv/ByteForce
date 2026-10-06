package com.byteforce.repository;

import com.byteforce.domain.AssessmentAnswer;
import com.byteforce.domain.AttemptStatus;
import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;
import com.byteforce.domain.QuestionType;
import com.byteforce.domain.Role;
import com.byteforce.domain.Topic;
import com.byteforce.domain.User;
import com.byteforce.exception.ByteForceException;
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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcAssessmentAnswerRepositoryTest {

    private HikariDataSource dataSource;
    private JdbcAssessmentAnswerRepository answerRepository;

    private User testUser;
    private Topic testTopic;
    private Question testQuestion1;
    private Question testQuestion2;
    private long testAssessmentId;
    private long testAttemptId;

    @BeforeEach
    void setUp() throws SQLException {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("AssessmentAnswerRepoTestPool-" + UUID.randomUUID());
        hikariConfig.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        hikariConfig.setUsername("sa");
        hikariConfig.setPassword("");
        hikariConfig.setMaximumPoolSize(5);

        dataSource = new HikariDataSource(hikariConfig);
        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");

        answerRepository = new JdbcAssessmentAnswerRepository(dataSource);

        // Seed dependencies: User, Topic, Questions, Assessment, and Assessment Attempt
        JdbcUserRepository userRepo = new JdbcUserRepository(dataSource);
        testUser = userRepo.save(User.create("candidate@byteforce.com", "$2a$12$passwordhash", "Candidate Name", Role.STUDENT));

        JdbcTopicRepository topicRepo = new JdbcTopicRepository(dataSource);
        testTopic = topicRepo.save(Topic.create("Algorithms", "algorithms", "Algorithm practice", 1));

        JdbcQuestionRepository questionRepo = new JdbcQuestionRepository(dataSource);
        testQuestion1 = questionRepo.save(Question.create(testTopic.getId(), "Binary Search", "binary-search",
                "Implement binary search", Difficulty.EASY, QuestionType.CODING, "def binary_search(): pass"));
        testQuestion2 = questionRepo.save(Question.create(testTopic.getId(), "Time Complexity MCQ", "time-complexity-mcq",
                "What is the average time complexity of QuickSort?", Difficulty.MEDIUM, QuestionType.MCQ, "O(n log n)"));

        try (Connection conn = dataSource.getConnection()) {
            // Seed assessment
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO assessments (title, description, duration_minutes, total_marks, difficulty, topic_id) " +
                            "VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, "Mock Placement Test 1");
                ps.setString(2, "Test description");
                ps.setInt(3, 60);
                ps.setInt(4, 100);
                ps.setString(5, "MEDIUM");
                ps.setLong(6, testTopic.getId());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    assertTrue(rs.next());
                    testAssessmentId = rs.getLong(1);
                }
            }

            // Seed assessment attempt
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO assessment_attempts (assessment_id, user_id, score, status) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, testAssessmentId);
                ps.setString(2, testUser.getId().toString());
                ps.setInt(3, 0);
                ps.setString(4, "IN_PROGRESS");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    assertTrue(rs.next());
                    testAttemptId = rs.getLong(1);
                }
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
    @DisplayName("Should create and retrieve a solved assessment answer with marks and submitted answer")
    void shouldCreateAndRetrieveSolvedAnswer() {
        AssessmentAnswer answer = AssessmentAnswer.create(testAttemptId, testQuestion1.getId(),
                "def binary_search(arr, target): return 0", 10, AttemptStatus.SOLVED);
        AssessmentAnswer saved = answerRepository.save(answer);

        assertTrue(saved.getId() > 0);
        assertEquals(testAttemptId, saved.getAssessmentAttemptId());
        assertEquals(testQuestion1.getId(), saved.getQuestionId());
        assertEquals(10, saved.getMarksAwarded());
        assertEquals(AttemptStatus.SOLVED, saved.getStatus());
        assertTrue(saved.isSolved());
        assertFalse(saved.isSkipped());
        assertNotNull(saved.getAnsweredAt());

        // Find by ID
        Optional<AssessmentAnswer> byId = answerRepository.findById(saved.getId());
        assertTrue(byId.isPresent());
        assertEquals("def binary_search(arr, target): return 0", byId.get().getSubmittedAnswer());

        // Find by attempt and question
        Optional<AssessmentAnswer> byAttemptAndQ = answerRepository.findByAttemptAndQuestion(testAttemptId, testQuestion1.getId());
        assertTrue(byAttemptAndQ.isPresent());
        assertEquals(saved.getId(), byAttemptAndQ.get().getId());
    }

    @Test
    @DisplayName("Should record a skipped/unanswered question with NULL submitted answer and 0 marks")
    void shouldRecordSkippedAnswer() {
        AssessmentAnswer skippedAnswer = AssessmentAnswer.skipped(testAttemptId, testQuestion2.getId());
        AssessmentAnswer saved = answerRepository.save(skippedAnswer);

        assertTrue(saved.getId() > 0);
        assertNull(saved.getSubmittedAnswer());
        assertEquals(0, saved.getMarksAwarded());
        assertEquals(AttemptStatus.SKIPPED, saved.getStatus());
        assertTrue(saved.isSkipped());
        assertFalse(saved.isSolved());
        assertNull(saved.getAnsweredAt());

        Optional<AssessmentAnswer> retrieved = answerRepository.findById(saved.getId());
        assertTrue(retrieved.isPresent());
        assertTrue(retrieved.get().isSkipped());
        assertNull(retrieved.get().getSubmittedAnswer());
        assertNull(retrieved.get().getAnsweredAt());
    }

    @Test
    @DisplayName("Should record a failed assessment answer")
    void shouldRecordFailedAnswer() {
        AssessmentAnswer answer = AssessmentAnswer.create(testAttemptId, testQuestion1.getId(),
                "def binary_search(): return -1", 0, AttemptStatus.FAILED);
        AssessmentAnswer saved = answerRepository.save(answer);

        assertEquals(AttemptStatus.FAILED, saved.getStatus());
        assertEquals(0, saved.getMarksAwarded());
        assertFalse(saved.isSolved());
        assertFalse(saved.isSkipped());
    }

    @Test
    @DisplayName("Should enforce unique constraint on (assessment_attempt_id, question_id)")
    void shouldEnforceUniqueConstraint() {
        AssessmentAnswer first = AssessmentAnswer.create(testAttemptId, testQuestion1.getId(), "ans 1", 5, AttemptStatus.SOLVED);
        answerRepository.save(first);

        AssessmentAnswer duplicate = AssessmentAnswer.create(testAttemptId, testQuestion1.getId(), "ans 2", 10, AttemptStatus.SOLVED);
        assertThrows(ByteForceException.class, () -> answerRepository.save(duplicate));
    }

    @Test
    @DisplayName("Should enforce foreign key constraint when assessment_attempt_id does not exist")
    void shouldEnforceAttemptForeignKeyConstraint() {
        AssessmentAnswer orphanAnswer = AssessmentAnswer.create(999999L, testQuestion1.getId(), "ans", 5, AttemptStatus.SOLVED);
        assertThrows(ByteForceException.class, () -> answerRepository.save(orphanAnswer));
    }

    @Test
    @DisplayName("Should enforce foreign key constraint when question_id does not exist")
    void shouldEnforceQuestionForeignKeyConstraint() {
        AssessmentAnswer orphanAnswer = AssessmentAnswer.create(testAttemptId, 999999L, "ans", 5, AttemptStatus.SOLVED);
        assertThrows(ByteForceException.class, () -> answerRepository.save(orphanAnswer));
    }

    @Test
    @DisplayName("Should update an existing assessment answer")
    void shouldUpdateExistingAnswer() {
        AssessmentAnswer initial = AssessmentAnswer.skipped(testAttemptId, testQuestion1.getId());
        AssessmentAnswer saved = answerRepository.save(initial);
        assertEquals(AttemptStatus.SKIPPED, saved.getStatus());

        AssessmentAnswer updated = saved.withAnswer("Updated correct answer", 15, AttemptStatus.SOLVED);
        AssessmentAnswer reSaved = answerRepository.save(updated);

        assertEquals(saved.getId(), reSaved.getId());
        assertEquals(15, reSaved.getMarksAwarded());
        assertEquals(AttemptStatus.SOLVED, reSaved.getStatus());
        assertEquals("Updated correct answer", reSaved.getSubmittedAnswer());

        Optional<AssessmentAnswer> retrieved = answerRepository.findById(saved.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(15, retrieved.get().getMarksAwarded());
        assertEquals(AttemptStatus.SOLVED, retrieved.get().getStatus());
    }

    @Test
    @DisplayName("Should retrieve all answers and count for a given assessment attempt")
    void shouldRetrieveAndCountAnswersForAttempt() {
        answerRepository.save(AssessmentAnswer.create(testAttemptId, testQuestion1.getId(), "ans 1", 10, AttemptStatus.SOLVED));
        answerRepository.save(AssessmentAnswer.skipped(testAttemptId, testQuestion2.getId()));

        List<AssessmentAnswer> answers = answerRepository.findByAttemptId(testAttemptId);
        assertEquals(2, answers.size());
        assertEquals(2, answerRepository.countByAttemptId(testAttemptId));
    }

    @Test
    @DisplayName("Should delete assessment answer by ID")
    void shouldDeleteAnswerById() {
        AssessmentAnswer saved = answerRepository.save(AssessmentAnswer.skipped(testAttemptId, testQuestion1.getId()));
        assertTrue(answerRepository.deleteById(saved.getId()));
        assertFalse(answerRepository.findById(saved.getId()).isPresent());
        assertFalse(answerRepository.deleteById(saved.getId()));
    }
}
