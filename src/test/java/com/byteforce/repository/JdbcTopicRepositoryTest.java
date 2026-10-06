package com.byteforce.repository;

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

class JdbcTopicRepositoryTest {

    private HikariDataSource dataSource;
    private JdbcTopicRepository topicRepository;

    @BeforeEach
    void setUp() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("TopicRepoTestPool-" + UUID.randomUUID());
        hikariConfig.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        hikariConfig.setUsername("sa");
        hikariConfig.setPassword("");
        hikariConfig.setMaximumPoolSize(5);

        dataSource = new HikariDataSource(hikariConfig);
        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");

        topicRepository = new JdbcTopicRepository(dataSource);
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Test
    @DisplayName("Should save and retrieve a new topic successfully")
    void shouldSaveAndRetrieveTopicSuccessfully() {
        Topic topic = Topic.create("Dynamic Programming", "dynamic-programming", "DP algorithms and memoization", 10);
        Topic saved = topicRepository.save(topic);

        assertNotNull(saved);
        assertTrue(saved.getId() > 0, "Generated ID should be positive");
        assertEquals("Dynamic Programming", saved.getName());
        assertEquals("dynamic-programming", saved.getSlug());
        assertEquals("DP algorithms and memoization", saved.getDescription());
        assertEquals(10, saved.getDisplayOrder());

        Optional<Topic> retrievedOpt = topicRepository.findById(saved.getId());
        assertTrue(retrievedOpt.isPresent(), "Topic should be found by ID");

        Topic retrieved = retrievedOpt.get();
        assertEquals(saved.getId(), retrieved.getId());
        assertEquals("Dynamic Programming", retrieved.getName());
        assertEquals("dynamic-programming", retrieved.getSlug());
        assertEquals("DP algorithms and memoization", retrieved.getDescription());
        assertEquals(10, retrieved.getDisplayOrder());
        assertEquals(topic.getCreatedAt().truncatedTo(ChronoUnit.SECONDS),
                retrieved.getCreatedAt().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("Should find topic by slug with case-insensitivity and whitespace tolerance")
    void shouldFindTopicBySlug() {
        Topic topic = Topic.create("Binary Trees", "binary-trees", "Tree traversals and properties", 2);
        topicRepository.save(topic);

        Optional<Topic> exact = topicRepository.findBySlug("binary-trees");
        assertTrue(exact.isPresent());
        assertEquals("Binary Trees", exact.get().getName());

        Optional<Topic> upper = topicRepository.findBySlug("  BINARY-TREES  ");
        assertTrue(upper.isPresent());
        assertEquals("Binary Trees", upper.get().getName());

        Optional<Topic> missing = topicRepository.findBySlug("unknown-slug");
        assertFalse(missing.isPresent());

        assertFalse(topicRepository.findBySlug(null).isPresent());
        assertFalse(topicRepository.findBySlug("   ").isPresent());
    }

    @Test
    @DisplayName("Should find topic by name case-insensitively")
    void shouldFindTopicByName() {
        Topic topic = Topic.create("Graphs", "graphs", "BFS, DFS, Dijkstra", 3);
        topicRepository.save(topic);

        Optional<Topic> exact = topicRepository.findByName("Graphs");
        assertTrue(exact.isPresent());

        Optional<Topic> lower = topicRepository.findByName("  graphs  ");
        assertTrue(lower.isPresent());

        Optional<Topic> missing = topicRepository.findByName("Trie");
        assertFalse(missing.isPresent());

        assertFalse(topicRepository.findByName(null).isPresent());
        assertFalse(topicRepository.findByName("   ").isPresent());
    }

    @Test
    @DisplayName("Should return all topics ordered by display order then name")
    void shouldFindAllTopicsOrderedByDisplayOrderAndName() {
        topicRepository.save(Topic.create("Topic C", "topic-c", "Desc C", 2));
        topicRepository.save(Topic.create("Topic A", "topic-a", "Desc A", 1));
        topicRepository.save(Topic.create("Topic B", "topic-b", "Desc B", 1));

        List<Topic> all = topicRepository.findAll();
        assertEquals(3, all.size());
        assertEquals("Topic A", all.get(0).getName());
        assertEquals("Topic B", all.get(1).getName());
        assertEquals("Topic C", all.get(2).getName());
    }

    @Test
    @DisplayName("Should update existing topic attributes successfully")
    void shouldUpdateExistingTopicSuccessfully() {
        Topic topic = Topic.create("Sorting", "sorting", "Basic sorts", 1);
        Topic saved = topicRepository.save(topic);

        Topic toUpdate = saved.withName("Advanced Sorting")
                .withSlug("advanced-sorting")
                .withDescription("Quick, Merge, and Heap sorts")
                .withDisplayOrder(5);

        Topic updated = topicRepository.save(toUpdate);
        assertEquals("Advanced Sorting", updated.getName());

        Topic retrieved = topicRepository.findById(saved.getId()).orElseThrow();
        assertEquals("Advanced Sorting", retrieved.getName());
        assertEquals("advanced-sorting", retrieved.getSlug());
        assertEquals("Quick, Merge, and Heap sorts", retrieved.getDescription());
        assertEquals(5, retrieved.getDisplayOrder());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent topic")
    void shouldThrowResourceNotFoundExceptionOnMissingTopicUpdate() {
        Topic nonExistent = new Topic(99999L, "Ghost", "ghost", "Desc", 1, java.time.Instant.now());
        assertThrows(ResourceNotFoundException.class, () -> topicRepository.save(nonExistent));
    }

    @Test
    @DisplayName("Should delete topic by ID successfully")
    void shouldDeleteTopicById() {
        Topic topic = Topic.create("Heap", "heap", "Min/Max heaps", 4);
        Topic saved = topicRepository.save(topic);

        assertTrue(topicRepository.deleteById(saved.getId()));
        assertFalse(topicRepository.findById(saved.getId()).isPresent());
        assertFalse(topicRepository.deleteById(saved.getId()), "Second delete should return false");
        assertFalse(topicRepository.deleteById(-1), "Invalid ID delete should return false");
        assertFalse(topicRepository.deleteById(0), "Zero ID delete should return false");
    }

    @Test
    @DisplayName("Should check topic existence by ID, slug, and name accurately")
    void shouldCheckTopicExistence() {
        Topic topic = Topic.create("Recursion", "recursion", "Recursive thinking", 1);
        Topic saved = topicRepository.save(topic);

        assertTrue(topicRepository.existsById(saved.getId()));
        assertFalse(topicRepository.existsById(99999L));
        assertFalse(topicRepository.existsById(-1));
        assertFalse(topicRepository.existsById(0));

        assertTrue(topicRepository.existsBySlug("recursion"));
        assertTrue(topicRepository.existsBySlug("  RECURSION  "));
        assertFalse(topicRepository.existsBySlug("non-existent"));
        assertFalse(topicRepository.existsBySlug(null));
        assertFalse(topicRepository.existsBySlug("  "));

        assertTrue(topicRepository.existsByName("Recursion"));
        assertTrue(topicRepository.existsByName("  recursion  "));
        assertFalse(topicRepository.existsByName("NonExistent"));
        assertFalse(topicRepository.existsByName(null));
        assertFalse(topicRepository.existsByName("  "));
    }

    @Test
    @DisplayName("Should count total topics in database accurately")
    void shouldCountTopics() {
        assertEquals(0, topicRepository.count());

        topicRepository.save(Topic.create("T1", "t1", "Desc", 1));
        assertEquals(1, topicRepository.count());

        topicRepository.save(Topic.create("T2", "t2", "Desc", 2));
        assertEquals(2, topicRepository.count());
    }

    @Test
    @DisplayName("Should throw ByteForceException on duplicate name or slug")
    void shouldThrowExceptionOnDuplicateNameOrSlug() {
        Topic topic = Topic.create("Bit Manipulation", "bit-manipulation", "XOR tricks", 5);
        topicRepository.save(topic);

        Topic duplicateName = Topic.create("Bit Manipulation", "different-slug", "Other", 6);
        assertThrows(ByteForceException.class, () -> topicRepository.save(duplicateName));

        Topic duplicateSlug = Topic.create("Different Name", "bit-manipulation", "Other", 7);
        assertThrows(ByteForceException.class, () -> topicRepository.save(duplicateSlug));
    }

    @Test
    @DisplayName("Should reject null topic entity on save")
    void shouldRejectNullTopicOnSave() {
        assertThrows(NullPointerException.class, () -> topicRepository.save(null));
    }

    @Test
    @DisplayName("Should return empty Optional for negative or zero ID")
    void shouldReturnEmptyOptionalForInvalidId() {
        assertFalse(topicRepository.findById(0).isPresent());
        assertFalse(topicRepository.findById(-1).isPresent());
    }

    @Test
    @DisplayName("Should throw ByteForceException on database failures")
    void shouldHandleDatabaseFailuresGracefully() throws SQLException {
        DataSource brokenDataSource = mock(DataSource.class);
        when(brokenDataSource.getConnection()).thenThrow(new SQLException("Connection closed"));

        JdbcTopicRepository brokenRepo = new JdbcTopicRepository(brokenDataSource);

        assertThrows(ByteForceException.class, () -> brokenRepo.findById(1L));
        assertThrows(ByteForceException.class, () -> brokenRepo.findBySlug("slug"));
        assertThrows(ByteForceException.class, () -> brokenRepo.findByName("name"));
        assertThrows(ByteForceException.class, () -> brokenRepo.findAll());
        assertThrows(ByteForceException.class, () -> brokenRepo.count());
        assertThrows(ByteForceException.class, () -> brokenRepo.existsById(1L));
        assertThrows(ByteForceException.class, () -> brokenRepo.existsBySlug("slug"));
        assertThrows(ByteForceException.class, () -> brokenRepo.existsByName("name"));
        assertThrows(ByteForceException.class, () -> brokenRepo.deleteById(1L));
    }
}
