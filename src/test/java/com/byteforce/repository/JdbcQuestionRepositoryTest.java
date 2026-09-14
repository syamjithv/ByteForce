package com.byteforce.repository;

import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;
import com.byteforce.domain.Topic;
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
import java.time.Instant;
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

class JdbcQuestionRepositoryTest {

    private HikariDataSource dataSource;
    private JdbcTopicRepository topicRepository;
    private JdbcQuestionRepository questionRepository;
    private Topic defaultTopic;

    @BeforeEach
    void setUp() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("QuestionRepoTestPool-" + UUID.randomUUID());
        hikariConfig.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        hikariConfig.setUsername("sa");
        hikariConfig.setPassword("");
        hikariConfig.setMaximumPoolSize(5);

        dataSource = new HikariDataSource(hikariConfig);
        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");

        topicRepository = new JdbcTopicRepository(dataSource);
        questionRepository = new JdbcQuestionRepository(dataSource);

        defaultTopic = topicRepository.save(Topic.create("Data Structures", "data-structures", "DS problems", 1));
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Test
    @DisplayName("Should save and retrieve a new question successfully")
    void shouldSaveAndRetrieveQuestionSuccessfully() {
        Question question = Question.create(
                defaultTopic.getId(),
                "Two Sum",
                "two-sum",
                "Given an array of integers, return indices of the two numbers such that they add up to target.",
                Difficulty.EASY,
                "Use a hash map for O(n) solution."
        );

        Question saved = questionRepository.save(question);
        assertNotNull(saved);
        assertTrue(saved.getId() > 0);
        assertEquals("Two Sum", saved.getTitle());
        assertEquals("two-sum", saved.getSlug());
        assertEquals(Difficulty.EASY, saved.getDifficulty());
        assertEquals(defaultTopic.getId(), saved.getTopicId());

        Optional<Question> retrievedOpt = questionRepository.findById(saved.getId());
        assertTrue(retrievedOpt.isPresent());

        Question retrieved = retrievedOpt.get();
        assertEquals(saved.getId(), retrieved.getId());
        assertEquals(defaultTopic.getId(), retrieved.getTopicId());
        assertEquals("Two Sum", retrieved.getTitle());
        assertEquals("two-sum", retrieved.getSlug());
        assertEquals(question.getDescription(), retrieved.getDescription());
        assertEquals(Difficulty.EASY, retrieved.getDifficulty());
        assertEquals("Use a hash map for O(n) solution.", retrieved.getSolution());
        assertEquals(question.getCreatedAt().truncatedTo(ChronoUnit.SECONDS),
                retrieved.getCreatedAt().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("Should find question by slug with whitespace and case tolerance")
    void shouldFindQuestionBySlug() {
        Question question = Question.create(
                defaultTopic.getId(),
                "Valid Palindrome",
                "valid-palindrome",
                "Check if string is palindrome.",
                Difficulty.EASY,
                "Two pointer approach."
        );
        questionRepository.save(question);

        Optional<Question> exact = questionRepository.findBySlug("valid-palindrome");
        assertTrue(exact.isPresent());
        assertEquals("Valid Palindrome", exact.get().getTitle());

        Optional<Question> upperWithSpaces = questionRepository.findBySlug("  VALID-PALINDROME  ");
        assertTrue(upperWithSpaces.isPresent());
        assertEquals("Valid Palindrome", upperWithSpaces.get().getTitle());

        assertFalse(questionRepository.findBySlug("unknown-slug").isPresent());
        assertFalse(questionRepository.findBySlug(null).isPresent());
        assertFalse(questionRepository.findBySlug("   ").isPresent());
    }

    @Test
    @DisplayName("Should retrieve all questions ordered by ID")
    void shouldFindAllQuestions() {
        questionRepository.save(Question.create(defaultTopic.getId(), "Q1", "q1", "Desc1", Difficulty.EASY, null));
        questionRepository.save(Question.create(defaultTopic.getId(), "Q2", "q2", "Desc2", Difficulty.MEDIUM, null));

        List<Question> all = questionRepository.findAll();
        assertEquals(2, all.size());
        assertEquals("Q1", all.get(0).getTitle());
        assertEquals("Q2", all.get(1).getTitle());
    }

    @Test
    @DisplayName("Should find questions by topic ID")
    void shouldFindByTopicId() {
        Topic secondTopic = topicRepository.save(Topic.create("Algorithms", "algorithms", "Algo problems", 2));

        questionRepository.save(Question.create(defaultTopic.getId(), "DS Q1", "ds-q1", "Desc", Difficulty.EASY, null));
        questionRepository.save(Question.create(defaultTopic.getId(), "DS Q2", "ds-q2", "Desc", Difficulty.HARD, null));
        questionRepository.save(Question.create(secondTopic.getId(), "Algo Q1", "algo-q1", "Desc", Difficulty.MEDIUM, null));

        List<Question> dsQuestions = questionRepository.findByTopicId(defaultTopic.getId());
        assertEquals(2, dsQuestions.size());

        List<Question> algoQuestions = questionRepository.findByTopicId(secondTopic.getId());
        assertEquals(1, algoQuestions.size());
        assertEquals("Algo Q1", algoQuestions.get(0).getTitle());

        assertTrue(questionRepository.findByTopicId(99999L).isEmpty());
        assertTrue(questionRepository.findByTopicId(-1).isEmpty());
        assertTrue(questionRepository.findByTopicId(0).isEmpty());
    }

    @Test
    @DisplayName("Should find questions by difficulty")
    void shouldFindByDifficulty() {
        questionRepository.save(Question.create(defaultTopic.getId(), "Easy Q", "easy-q", "Desc", Difficulty.EASY, null));
        questionRepository.save(Question.create(defaultTopic.getId(), "Med Q", "med-q", "Desc", Difficulty.MEDIUM, null));
        questionRepository.save(Question.create(defaultTopic.getId(), "Hard Q", "hard-q", "Desc", Difficulty.HARD, null));

        List<Question> easyList = questionRepository.findByDifficulty(Difficulty.EASY);
        assertEquals(1, easyList.size());
        assertEquals("Easy Q", easyList.get(0).getTitle());

        List<Question> hardList = questionRepository.findByDifficulty(Difficulty.HARD);
        assertEquals(1, hardList.size());
        assertEquals("Hard Q", hardList.get(0).getTitle());

        assertTrue(questionRepository.findByDifficulty(null).isEmpty());
    }

    @Test
    @DisplayName("Should find questions by topic ID and difficulty")
    void shouldFindByTopicIdAndDifficulty() {
        Topic secondTopic = topicRepository.save(Topic.create("Math", "math", "Math problems", 3));

        questionRepository.save(Question.create(defaultTopic.getId(), "DS Easy", "ds-easy", "Desc", Difficulty.EASY, null));
        questionRepository.save(Question.create(defaultTopic.getId(), "DS Hard", "ds-hard", "Desc", Difficulty.HARD, null));
        questionRepository.save(Question.create(secondTopic.getId(), "Math Easy", "math-easy", "Desc", Difficulty.EASY, null));

        List<Question> dsEasy = questionRepository.findByTopicIdAndDifficulty(defaultTopic.getId(), Difficulty.EASY);
        assertEquals(1, dsEasy.size());
        assertEquals("DS Easy", dsEasy.get(0).getTitle());

        assertTrue(questionRepository.findByTopicIdAndDifficulty(defaultTopic.getId(), Difficulty.MEDIUM).isEmpty());
        assertTrue(questionRepository.findByTopicIdAndDifficulty(-1, Difficulty.EASY).isEmpty());
        assertTrue(questionRepository.findByTopicIdAndDifficulty(defaultTopic.getId(), null).isEmpty());
    }

    @Test
    @DisplayName("Should search questions by keyword across title and description")
    void shouldSearchQuestionsByKeyword() {
        questionRepository.save(Question.create(defaultTopic.getId(), "Binary Search", "binary-search",
                "Search in a sorted array", Difficulty.EASY, null));
        questionRepository.save(Question.create(defaultTopic.getId(), "Search 2D Matrix", "search-2d-matrix",
                "Matrix row and column sorted search", Difficulty.MEDIUM, null));
        questionRepository.save(Question.create(defaultTopic.getId(), "Invert Tree", "invert-tree",
                "Reverse left and right child pointers", Difficulty.EASY, null));

        List<Question> searchResults = questionRepository.search("search");
        assertEquals(2, searchResults.size());

        List<Question> pointerResults = questionRepository.search("pointers");
        assertEquals(1, pointerResults.size());
        assertEquals("Invert Tree", pointerResults.get(0).getTitle());

        List<Question> emptyResults = questionRepository.search("nonexistentkeyword");
        assertTrue(emptyResults.isEmpty());

        List<Question> allOnBlank = questionRepository.search("   ");
        assertEquals(3, allOnBlank.size());

        List<Question> allOnNull = questionRepository.search(null);
        assertEquals(3, allOnNull.size());
    }

    @Test
    @DisplayName("Should update existing question attributes successfully")
    void shouldUpdateExistingQuestionSuccessfully() {
        Question initial = Question.create(
                defaultTopic.getId(),
                "Old Title",
                "old-title",
                "Old Description",
                Difficulty.EASY,
                "Old Solution"
        );
        Question saved = questionRepository.save(initial);

        Question updatedData = saved
                .withTitle("Updated Title")
                .withSlug("updated-title")
                .withDescription("Updated Description")
                .withDifficulty(Difficulty.HARD)
                .withSolution("New Optimized Solution");

        Question updated = questionRepository.save(updatedData);
        assertEquals("Updated Title", updated.getTitle());
        assertEquals(Difficulty.HARD, updated.getDifficulty());

        Question reloaded = questionRepository.findById(saved.getId()).orElseThrow();
        assertEquals("Updated Title", reloaded.getTitle());
        assertEquals("updated-title", reloaded.getSlug());
        assertEquals("Updated Description", reloaded.getDescription());
        assertEquals(Difficulty.HARD, reloaded.getDifficulty());
        assertEquals("New Optimized Solution", reloaded.getSolution());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent question")
    void shouldThrowResourceNotFoundExceptionOnMissingQuestionUpdate() {
        Question nonExistent = new Question(
                99999L,
                defaultTopic.getId(),
                "Ghost",
                "ghost-slug",
                "Ghost Desc",
                Difficulty.MEDIUM,
                null,
                Instant.now(),
                Instant.now()
        );
        assertThrows(ResourceNotFoundException.class, () -> questionRepository.save(nonExistent));
    }

    @Test
    @DisplayName("Should delete question by ID successfully")
    void shouldDeleteQuestionById() {
        Question question = Question.create(defaultTopic.getId(), "To Delete", "to-delete", "Desc", Difficulty.EASY, null);
        Question saved = questionRepository.save(question);

        assertTrue(questionRepository.deleteById(saved.getId()));
        assertFalse(questionRepository.findById(saved.getId()).isPresent());
        assertFalse(questionRepository.deleteById(saved.getId()));
        assertFalse(questionRepository.deleteById(-1));
        assertFalse(questionRepository.deleteById(0));
    }

    @Test
    @DisplayName("Should check question existence by ID and slug accurately")
    void shouldCheckQuestionExistence() {
        Question question = Question.create(defaultTopic.getId(), "Check Exist", "check-exist", "Desc", Difficulty.EASY, null);
        Question saved = questionRepository.save(question);

        assertTrue(questionRepository.existsById(saved.getId()));
        assertFalse(questionRepository.existsById(99999L));
        assertFalse(questionRepository.existsById(-1));
        assertFalse(questionRepository.existsById(0));

        assertTrue(questionRepository.existsBySlug("check-exist"));
        assertTrue(questionRepository.existsBySlug("  CHECK-EXIST  "));
        assertFalse(questionRepository.existsBySlug("fake-slug"));
        assertFalse(questionRepository.existsBySlug(null));
        assertFalse(questionRepository.existsBySlug("  "));
    }

    @Test
    @DisplayName("Should accurately count questions globally, by topic, and by difficulty")
    void shouldCountQuestions() {
        Topic topicB = topicRepository.save(Topic.create("Topic B", "topic-b", "Desc", 2));

        assertEquals(0, questionRepository.count());
        assertEquals(0, questionRepository.countByTopicId(defaultTopic.getId()));
        assertEquals(0, questionRepository.countByDifficulty(Difficulty.EASY));

        questionRepository.save(Question.create(defaultTopic.getId(), "Q1", "q1", "Desc", Difficulty.EASY, null));
        questionRepository.save(Question.create(defaultTopic.getId(), "Q2", "q2", "Desc", Difficulty.MEDIUM, null));
        questionRepository.save(Question.create(topicB.getId(), "Q3", "q3", "Desc", Difficulty.EASY, null));

        assertEquals(3, questionRepository.count());
        assertEquals(2, questionRepository.countByTopicId(defaultTopic.getId()));
        assertEquals(1, questionRepository.countByTopicId(topicB.getId()));
        assertEquals(0, questionRepository.countByTopicId(99999L));
        assertEquals(0, questionRepository.countByTopicId(-1));

        assertEquals(2, questionRepository.countByDifficulty(Difficulty.EASY));
        assertEquals(1, questionRepository.countByDifficulty(Difficulty.MEDIUM));
        assertEquals(0, questionRepository.countByDifficulty(Difficulty.HARD));
        assertEquals(0, questionRepository.countByDifficulty(null));
    }

    @Test
    @DisplayName("Should throw ByteForceException on duplicate slug")
    void shouldThrowExceptionOnDuplicateSlug() {
        Question q1 = Question.create(defaultTopic.getId(), "Title 1", "same-slug", "Desc 1", Difficulty.EASY, null);
        questionRepository.save(q1);

        Question q2 = Question.create(defaultTopic.getId(), "Title 2", "same-slug", "Desc 2", Difficulty.HARD, null);
        assertThrows(ByteForceException.class, () -> questionRepository.save(q2));
    }

    @Test
    @DisplayName("Should throw ByteForceException when referenced topic does not exist (FK constraint)")
    void shouldThrowExceptionOnInvalidTopicId() {
        Question question = Question.create(99999L, "Orphan", "orphan-slug", "Desc", Difficulty.EASY, null);
        assertThrows(ByteForceException.class, () -> questionRepository.save(question));
    }

    @Test
    @DisplayName("Should reject null question entity on save")
    void shouldRejectNullQuestionOnSave() {
        assertThrows(NullPointerException.class, () -> questionRepository.save(null));
    }

    @Test
    @DisplayName("Should return empty Optional for invalid question IDs")
    void shouldReturnEmptyOptionalForInvalidId() {
        assertFalse(questionRepository.findById(0).isPresent());
        assertFalse(questionRepository.findById(-1).isPresent());
    }

    @Test
    @DisplayName("Should throw ByteForceException on database connection failures")
    void shouldHandleDatabaseFailuresGracefully() throws SQLException {
        DataSource brokenDataSource = mock(DataSource.class);
        when(brokenDataSource.getConnection()).thenThrow(new SQLException("Connection refused"));

        JdbcQuestionRepository brokenRepo = new JdbcQuestionRepository(brokenDataSource);

        assertThrows(ByteForceException.class, () -> brokenRepo.findById(1L));
        assertThrows(ByteForceException.class, () -> brokenRepo.findBySlug("slug"));
        assertThrows(ByteForceException.class, () -> brokenRepo.findAll());
        assertThrows(ByteForceException.class, () -> brokenRepo.findByTopicId(1L));
        assertThrows(ByteForceException.class, () -> brokenRepo.findByDifficulty(Difficulty.EASY));
        assertThrows(ByteForceException.class, () -> brokenRepo.findByTopicIdAndDifficulty(1L, Difficulty.EASY));
        assertThrows(ByteForceException.class, () -> brokenRepo.search("key"));
        assertThrows(ByteForceException.class, () -> brokenRepo.deleteById(1L));
        assertThrows(ByteForceException.class, () -> brokenRepo.existsById(1L));
        assertThrows(ByteForceException.class, () -> brokenRepo.existsBySlug("slug"));
        assertThrows(ByteForceException.class, () -> brokenRepo.count());
        assertThrows(ByteForceException.class, () -> brokenRepo.countByTopicId(1L));
        assertThrows(ByteForceException.class, () -> brokenRepo.countByDifficulty(Difficulty.EASY));
    }
}
