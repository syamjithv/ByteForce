package com.byteforce.repository;

import com.byteforce.domain.Assessment;
import com.byteforce.domain.AssessmentAttempt;
import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;
import com.byteforce.domain.QuestionType;
import com.byteforce.domain.Role;
import com.byteforce.domain.Topic;
import com.byteforce.domain.User;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcAssessmentRepositoryTest {

    private HikariDataSource dataSource;
    private JdbcAssessmentRepository assessmentRepository;
    private JdbcAssessmentAttemptRepository attemptRepository;
    private JdbcTopicRepository topicRepository;
    private JdbcQuestionRepository questionRepository;
    private JdbcUserRepository userRepository;

    private User testUser;
    private Topic testTopic;
    private Question testQ1;
    private Question testQ2;

    @BeforeEach
    void setUp() throws SQLException {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("AssessmentRepoTestPool-" + UUID.randomUUID());
        hikariConfig.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        hikariConfig.setUsername("sa");
        hikariConfig.setPassword("");
        hikariConfig.setMaximumPoolSize(5);

        dataSource = new HikariDataSource(hikariConfig);
        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");

        assessmentRepository = new JdbcAssessmentRepository(dataSource);
        attemptRepository = new JdbcAssessmentAttemptRepository(dataSource);
        topicRepository = new JdbcTopicRepository(dataSource);
        questionRepository = new JdbcQuestionRepository(dataSource);
        userRepository = new JdbcUserRepository(dataSource);

        testUser = userRepository.save(User.create("student@byteforce.com", "hash", "Student Name", Role.STUDENT));
        testTopic = topicRepository.save(Topic.create("Data Structures", "data-structures", "DS topics", 1));

        testQ1 = questionRepository.save(Question.create(
                testTopic.getId(), "Arrays vs LinkedList", "arrays-vs-ll",
                "Explain difference", Difficulty.EASY, QuestionType.CONCEPTUAL, "Contiguous vs pointer"
        ));

        testQ2 = questionRepository.save(Question.create(
                testTopic.getId(), "Binary Search MCQ", "bs-mcq",
                "Complexity?\nA) O(1)\nB) O(log n)", Difficulty.MEDIUM, QuestionType.MCQ, "B) O(log n)"
        ));
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Test
    @DisplayName("Should save and find assessment by ID")
    void shouldSaveAndFindAssessmentById() {
        Assessment assessment = Assessment.create(
                "Full Stack Placement Test",
                "Algorithms, SQL, and System Design",
                90,
                100,
                Difficulty.MEDIUM,
                testTopic.getId()
        );

        Assessment saved = assessmentRepository.save(assessment);
        assertNotNull(saved);
        assertTrue(saved.getId() > 0);
        assertEquals("Full Stack Placement Test", saved.getTitle());
        assertEquals(90, saved.getDurationMinutes());
        assertEquals(100, saved.getTotalMarks());
        assertEquals(Difficulty.MEDIUM, saved.getDifficulty());

        Optional<Assessment> found = assessmentRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Full Stack Placement Test", found.get().getTitle());
    }

    @Test
    @DisplayName("findAllActive should only return active assessments")
    void shouldFindAllActiveAssessments() {
        Assessment active1 = assessmentRepository.save(Assessment.create("Test 1", "Desc", 45, 50, Difficulty.EASY, null));
        Assessment active2 = assessmentRepository.save(Assessment.create("Test 2", "Desc", 60, 100, Difficulty.HARD, null));
        Assessment inactive = assessmentRepository.save(
                new Assessment(0, "Inactive Test", "Desc", 30, 25, false, Difficulty.EASY, null, Instant.now())
        );

        List<Assessment> activeList = assessmentRepository.findAllActive();
        assertTrue(activeList.stream().anyMatch(a -> a.getId() == active1.getId()));
        assertTrue(activeList.stream().anyMatch(a -> a.getId() == active2.getId()));
        assertFalse(activeList.stream().anyMatch(a -> a.getId() == inactive.getId()));
    }

    @Test
    @DisplayName("Should find questions and marks mapped to an assessment")
    void shouldFindQuestionsAndMarksForAssessment() throws SQLException {
        Assessment assessment = assessmentRepository.save(
                Assessment.create("DS Mock Test", "DS test", 60, 50, Difficulty.MEDIUM, testTopic.getId())
        );

        // Associate questions in assessment_questions
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO assessment_questions (assessment_id, question_id, marks, question_order) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, assessment.getId());
            ps.setLong(2, testQ1.getId());
            ps.setInt(3, 20);
            ps.setInt(4, 1);
            ps.addBatch();

            ps.setLong(1, assessment.getId());
            ps.setLong(2, testQ2.getId());
            ps.setInt(3, 30);
            ps.setInt(4, 2);
            ps.addBatch();

            ps.executeBatch();
        }

        List<Question> questions = assessmentRepository.findQuestionsByAssessmentId(assessment.getId());
        assertEquals(2, questions.size());
        assertEquals(testQ1.getId(), questions.get(0).getId());
        assertEquals(testQ2.getId(), questions.get(1).getId());

        Map<Long, Integer> marksMap = assessmentRepository.getQuestionMarks(assessment.getId());
        assertEquals(2, marksMap.size());
        assertEquals(20, marksMap.get(testQ1.getId()));
        assertEquals(30, marksMap.get(testQ2.getId()));
    }

    @Test
    @DisplayName("Should save, find, and update score on assessment attempts")
    void shouldSaveAndManageAssessmentAttempts() {
        Assessment assessment = assessmentRepository.save(
                Assessment.create("Attempt Test Assessment", "Desc", 30, 100, Difficulty.EASY, null)
        );

        AssessmentAttempt attempt = AssessmentAttempt.start(assessment.getId(), testUser.getId());
        AssessmentAttempt saved = attemptRepository.save(attempt);

        assertNotNull(saved);
        assertTrue(saved.getId() > 0);
        assertEquals("IN_PROGRESS", saved.getStatus());
        assertEquals(0, saved.getScore());

        Optional<AssessmentAttempt> found = attemptRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(assessment.getId(), found.get().getAssessmentId());
        assertEquals(testUser.getId(), found.get().getUserId());

        Instant completionTime = Instant.now();
        attemptRepository.updateScoreAndStatus(saved.getId(), 85, "COMPLETED", completionTime);

        Optional<AssessmentAttempt> updated = attemptRepository.findById(saved.getId());
        assertTrue(updated.isPresent());
        assertEquals(85, updated.get().getScore());
        assertEquals("COMPLETED", updated.get().getStatus());
        assertTrue(updated.get().isCompleted());

        List<AssessmentAttempt> userAttempts = attemptRepository.findByUserId(testUser.getId());
        assertEquals(1, userAttempts.size());
        assertEquals(saved.getId(), userAttempts.get(0).getId());
    }
}
