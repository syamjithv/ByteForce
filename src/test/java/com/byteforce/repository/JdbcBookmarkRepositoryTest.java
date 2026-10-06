package com.byteforce.repository;

import com.byteforce.domain.Bookmark;
import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcBookmarkRepositoryTest {

    private HikariDataSource dataSource;
    private JdbcUserRepository userRepository;
    private JdbcTopicRepository topicRepository;
    private JdbcQuestionRepository questionRepository;
    private JdbcBookmarkRepository bookmarkRepository;

    private User defaultUser;
    private Question defaultQuestion;

    @BeforeEach
    void setUp() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("BookmarkRepoTestPool-" + UUID.randomUUID());
        hikariConfig.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        hikariConfig.setUsername("sa");
        hikariConfig.setPassword("");
        hikariConfig.setMaximumPoolSize(5);

        dataSource = new HikariDataSource(hikariConfig);
        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");

        userRepository = new JdbcUserRepository(dataSource);
        topicRepository = new JdbcTopicRepository(dataSource);
        questionRepository = new JdbcQuestionRepository(dataSource);
        bookmarkRepository = new JdbcBookmarkRepository(dataSource);

        defaultUser = userRepository.save(User.create("student@byteforce.com", "$2a$12$hash", "Student User", Role.STUDENT));
        Topic topic = topicRepository.save(Topic.create("Trees", "trees", "Tree questions", 1));
        defaultQuestion = questionRepository.save(Question.create(topic.getId(), "Tree Inorder", "tree-inorder", "Traverse inorder", Difficulty.EASY, null));
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Test
    @DisplayName("Should save and retrieve a new bookmark successfully")
    void shouldSaveAndRetrieveBookmarkSuccessfully() {
        Bookmark bookmark = Bookmark.create(defaultUser.getId(), defaultQuestion.getId(), "Review before interview");
        Bookmark saved = bookmarkRepository.save(bookmark);

        assertNotNull(saved);
        assertTrue(saved.getId() > 0);
        assertEquals(defaultUser.getId(), saved.getUserId());
        assertEquals(defaultQuestion.getId(), saved.getQuestionId());
        assertEquals("Review before interview", saved.getNotes());

        Optional<Bookmark> retrievedOpt = bookmarkRepository.findById(saved.getId());
        assertTrue(retrievedOpt.isPresent());

        Bookmark retrieved = retrievedOpt.get();
        assertEquals(saved.getId(), retrieved.getId());
        assertEquals(defaultUser.getId(), retrieved.getUserId());
        assertEquals(defaultQuestion.getId(), retrieved.getQuestionId());
        assertEquals("Review before interview", retrieved.getNotes());
        assertEquals(bookmark.getCreatedAt().truncatedTo(ChronoUnit.SECONDS),
                retrieved.getCreatedAt().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("Should find bookmark by user ID and question ID")
    void shouldFindByUserIdAndQuestionId() {
        bookmarkRepository.save(Bookmark.create(defaultUser.getId(), defaultQuestion.getId(), "Note 1"));

        Optional<Bookmark> found = bookmarkRepository.findByUserIdAndQuestionId(defaultUser.getId(), defaultQuestion.getId());
        assertTrue(found.isPresent());
        assertEquals("Note 1", found.get().getNotes());

        assertFalse(bookmarkRepository.findByUserIdAndQuestionId(defaultUser.getId(), 99999L).isPresent());
        assertFalse(bookmarkRepository.findByUserIdAndQuestionId(UUID.randomUUID(), defaultQuestion.getId()).isPresent());
        assertFalse(bookmarkRepository.findByUserIdAndQuestionId(null, defaultQuestion.getId()).isPresent());
        assertFalse(bookmarkRepository.findByUserIdAndQuestionId(defaultUser.getId(), -1).isPresent());
        assertFalse(bookmarkRepository.findByUserIdAndQuestionId(defaultUser.getId(), 0).isPresent());
    }

    @Test
    @DisplayName("Should find all bookmarks for a specific user ordered by created_at DESC")
    void shouldFindByUserId() {
        Topic topic = topicRepository.save(Topic.create("Graphs", "graphs", "Graph questions", 2));
        Question q2 = questionRepository.save(Question.create(topic.getId(), "BFS", "bfs", "Desc", Difficulty.EASY, null));

        bookmarkRepository.save(Bookmark.create(defaultUser.getId(), defaultQuestion.getId(), "First"));
        bookmarkRepository.save(Bookmark.create(defaultUser.getId(), q2.getId(), "Second"));

        List<Bookmark> bookmarks = bookmarkRepository.findByUserId(defaultUser.getId());
        assertEquals(2, bookmarks.size());

        assertTrue(bookmarkRepository.findByUserId(null).isEmpty());
        assertTrue(bookmarkRepository.findByUserId(UUID.randomUUID()).isEmpty());
    }

    @Test
    @DisplayName("Should find all bookmarks for a specific question")
    void shouldFindByQuestionId() {
        User user2 = userRepository.save(User.create("user2@byteforce.com", "$2a$12$hash2", "User Two", Role.STUDENT));

        bookmarkRepository.save(Bookmark.create(defaultUser.getId(), defaultQuestion.getId(), "User 1 note"));
        bookmarkRepository.save(Bookmark.create(user2.getId(), defaultQuestion.getId(), "User 2 note"));

        List<Bookmark> bookmarks = bookmarkRepository.findByQuestionId(defaultQuestion.getId());
        assertEquals(2, bookmarks.size());

        assertTrue(bookmarkRepository.findByQuestionId(99999L).isEmpty());
        assertTrue(bookmarkRepository.findByQuestionId(-1).isEmpty());
        assertTrue(bookmarkRepository.findByQuestionId(0).isEmpty());
    }

    @Test
    @DisplayName("Should accurately check if a question is bookmarked by a user")
    void shouldCheckIsBookmarked() {
        assertFalse(bookmarkRepository.isBookmarked(defaultUser.getId(), defaultQuestion.getId()));

        bookmarkRepository.save(Bookmark.create(defaultUser.getId(), defaultQuestion.getId(), "Note"));
        assertTrue(bookmarkRepository.isBookmarked(defaultUser.getId(), defaultQuestion.getId()));

        assertFalse(bookmarkRepository.isBookmarked(defaultUser.getId(), 99999L));
        assertFalse(bookmarkRepository.isBookmarked(UUID.randomUUID(), defaultQuestion.getId()));
        assertFalse(bookmarkRepository.isBookmarked(null, defaultQuestion.getId()));
        assertFalse(bookmarkRepository.isBookmarked(defaultUser.getId(), -1));
        assertFalse(bookmarkRepository.isBookmarked(defaultUser.getId(), 0));
    }

    @Test
    @DisplayName("Should update notes when saving bookmark with existing ID")
    void shouldUpdateNotesWhenSavingBookmarkWithId() {
        Bookmark initial = bookmarkRepository.save(Bookmark.create(defaultUser.getId(), defaultQuestion.getId(), "Old note"));
        Bookmark updated = initial.withNotes("Revised note for interview");

        Bookmark saved = bookmarkRepository.save(updated);
        assertEquals("Revised note for interview", saved.getNotes());

        Bookmark reloaded = bookmarkRepository.findById(initial.getId()).orElseThrow();
        assertEquals("Revised note for interview", reloaded.getNotes());
    }

    @Test
    @DisplayName("Should update notes on save if bookmark already exists for user and question even with id 0")
    void shouldUpdateNotesWhenSavingDuplicateUserAndQuestion() {
        Bookmark first = bookmarkRepository.save(Bookmark.create(defaultUser.getId(), defaultQuestion.getId(), "Original note"));
        assertEquals(1, bookmarkRepository.countByUserId(defaultUser.getId()));

        // Save again with id 0 for the same user and question
        Bookmark duplicateAttempt = Bookmark.create(defaultUser.getId(), defaultQuestion.getId(), "Overwritten note");
        Bookmark result = bookmarkRepository.save(duplicateAttempt);

        assertEquals(first.getId(), result.getId());
        assertEquals("Overwritten note", result.getNotes());
        assertEquals(1, bookmarkRepository.countByUserId(defaultUser.getId()), "Should not create a duplicate bookmark");

        Bookmark reloaded = bookmarkRepository.findById(first.getId()).orElseThrow();
        assertEquals("Overwritten note", reloaded.getNotes());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent bookmark ID")
    void shouldThrowResourceNotFoundExceptionOnMissingBookmarkUpdate() {
        Bookmark nonExistent = new Bookmark(99999L, defaultUser.getId(), defaultQuestion.getId(), "Ghost", java.time.Instant.now());
        assertThrows(ResourceNotFoundException.class, () -> bookmarkRepository.save(nonExistent));
    }

    @Test
    @DisplayName("Should delete bookmark by ID successfully")
    void shouldDeleteById() {
        Bookmark saved = bookmarkRepository.save(Bookmark.create(defaultUser.getId(), defaultQuestion.getId(), "Note"));

        assertTrue(bookmarkRepository.deleteById(saved.getId()));
        assertFalse(bookmarkRepository.findById(saved.getId()).isPresent());
        assertFalse(bookmarkRepository.deleteById(saved.getId()));
        assertFalse(bookmarkRepository.deleteById(-1));
        assertFalse(bookmarkRepository.deleteById(0));
    }

    @Test
    @DisplayName("Should delete bookmark by user ID and question ID successfully")
    void shouldDeleteByUserIdAndQuestionId() {
        bookmarkRepository.save(Bookmark.create(defaultUser.getId(), defaultQuestion.getId(), "Note"));

        assertTrue(bookmarkRepository.deleteByUserIdAndQuestionId(defaultUser.getId(), defaultQuestion.getId()));
        assertFalse(bookmarkRepository.isBookmarked(defaultUser.getId(), defaultQuestion.getId()));
        assertFalse(bookmarkRepository.deleteByUserIdAndQuestionId(defaultUser.getId(), defaultQuestion.getId()));

        assertFalse(bookmarkRepository.deleteByUserIdAndQuestionId(null, defaultQuestion.getId()));
        assertFalse(bookmarkRepository.deleteByUserIdAndQuestionId(defaultUser.getId(), -1));
        assertFalse(bookmarkRepository.deleteByUserIdAndQuestionId(defaultUser.getId(), 0));
    }

    @Test
    @DisplayName("Should count bookmarks for user accurately")
    void shouldCountByUserId() {
        assertEquals(0, bookmarkRepository.countByUserId(defaultUser.getId()));

        Topic topic = topicRepository.save(Topic.create("Math", "math", "Math problems", 3));
        Question q2 = questionRepository.save(Question.create(topic.getId(), "Primes", "primes", "Desc", Difficulty.EASY, null));

        bookmarkRepository.save(Bookmark.create(defaultUser.getId(), defaultQuestion.getId(), "B1"));
        assertEquals(1, bookmarkRepository.countByUserId(defaultUser.getId()));

        bookmarkRepository.save(Bookmark.create(defaultUser.getId(), q2.getId(), "B2"));
        assertEquals(2, bookmarkRepository.countByUserId(defaultUser.getId()));

        assertEquals(0, bookmarkRepository.countByUserId(null));
        assertEquals(0, bookmarkRepository.countByUserId(UUID.randomUUID()));
    }

    @Test
    @DisplayName("Should throw ByteForceException when foreign key constraint fails")
    void shouldThrowExceptionOnInvalidForeignKeys() {
        UUID nonExistentUserId = UUID.randomUUID();
        Bookmark invalidUser = Bookmark.create(nonExistentUserId, defaultQuestion.getId(), "Note");
        assertThrows(ByteForceException.class, () -> bookmarkRepository.save(invalidUser));

        Bookmark invalidQuestion = Bookmark.create(defaultUser.getId(), 99999L, "Note");
        assertThrows(ByteForceException.class, () -> bookmarkRepository.save(invalidQuestion));
    }

    @Test
    @DisplayName("Should reject null bookmark on save")
    void shouldRejectNullBookmarkOnSave() {
        assertThrows(NullPointerException.class, () -> bookmarkRepository.save(null));
    }

    @Test
    @DisplayName("Should return empty Optional for invalid bookmark IDs")
    void shouldReturnEmptyOptionalForInvalidId() {
        assertFalse(bookmarkRepository.findById(0).isPresent());
        assertFalse(bookmarkRepository.findById(-1).isPresent());
    }

    @Test
    @DisplayName("Should throw ByteForceException on database connection failures")
    void shouldHandleDatabaseFailuresGracefully() throws SQLException {
        DataSource brokenDataSource = mock(DataSource.class);
        when(brokenDataSource.getConnection()).thenThrow(new SQLException("Connection closed"));

        JdbcBookmarkRepository brokenRepo = new JdbcBookmarkRepository(brokenDataSource);

        assertThrows(ByteForceException.class, () -> brokenRepo.findById(1L));
        assertThrows(ByteForceException.class, () -> brokenRepo.findByUserIdAndQuestionId(UUID.randomUUID(), 1L));
        assertThrows(ByteForceException.class, () -> brokenRepo.findByUserId(UUID.randomUUID()));
        assertThrows(ByteForceException.class, () -> brokenRepo.findByQuestionId(1L));
        assertThrows(ByteForceException.class, () -> brokenRepo.isBookmarked(UUID.randomUUID(), 1L));
        assertThrows(ByteForceException.class, () -> brokenRepo.deleteById(1L));
        assertThrows(ByteForceException.class, () -> brokenRepo.deleteByUserIdAndQuestionId(UUID.randomUUID(), 1L));
        assertThrows(ByteForceException.class, () -> brokenRepo.countByUserId(UUID.randomUUID()));
    }
}
