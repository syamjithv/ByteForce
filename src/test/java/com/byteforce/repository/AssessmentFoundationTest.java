package com.byteforce.repository;

import com.byteforce.domain.Topic;
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
import java.sql.Types;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssessmentFoundationTest {

    private HikariDataSource dataSource;
    private JdbcTopicRepository topicRepository;

    @BeforeEach
    void setUp() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("AssessmentFoundationTestPool-" + UUID.randomUUID());
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
    @DisplayName("Should persist assessment without topic (topic_id is NULL) with default difficulty")
    void shouldPersistAssessmentWithoutTopic() throws SQLException {
        long assessmentId;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO assessments (title, description, duration_minutes, total_marks, is_active, difficulty, topic_id) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "General Mock Placement Test");
            ps.setString(2, "Covers CS Fundamentals, Aptitude and Coding");
            ps.setInt(3, 90);
            ps.setInt(4, 100);
            ps.setBoolean(5, true);
            ps.setString(6, "HARD");
            ps.setNull(7, Types.BIGINT);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertTrue(keys.next());
                assessmentId = keys.getLong(1);
            }
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT title, difficulty, topic_id FROM assessments WHERE id = ?")) {
            ps.setLong(1, assessmentId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("General Mock Placement Test", rs.getString("title"));
                assertEquals("HARD", rs.getString("difficulty"));
                assertNull(rs.getObject("topic_id"));
            }
        }
    }

    @Test
    @DisplayName("Should persist assessment with specific topic association")
    void shouldPersistAssessmentWithTopic() throws SQLException {
        Topic topic = topicRepository.save(Topic.create("Database Management Systems", "dbms", "Relational database topics", 1));

        long assessmentId;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO assessments (title, description, duration_minutes, total_marks, is_active, difficulty, topic_id) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "DBMS Topic Assessment");
            ps.setString(2, "SQL, Normalization and Transactions");
            ps.setInt(3, 45);
            ps.setInt(4, 50);
            ps.setBoolean(5, true);
            ps.setString(6, "MEDIUM");
            ps.setLong(7, topic.getId());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertTrue(keys.next());
                assessmentId = keys.getLong(1);
            }
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT a.title, a.difficulty, a.topic_id, t.name as topic_name " +
                             "FROM assessments a JOIN topics t ON a.topic_id = t.id WHERE a.id = ?")) {
            ps.setLong(1, assessmentId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("DBMS Topic Assessment", rs.getString("title"));
                assertEquals("MEDIUM", rs.getString("difficulty"));
                assertEquals(topic.getId(), rs.getLong("topic_id"));
                assertEquals("Database Management Systems", rs.getString("topic_name"));
            }
        }
    }

    @Test
    @DisplayName("Should set topic_id to NULL on assessment when referenced topic is deleted (ON DELETE SET NULL)")
    void shouldSetTopicIdToNullWhenTopicDeleted() throws SQLException {
        Topic topic = topicRepository.save(Topic.create("Temporary Topic", "temp-topic", "To be deleted", 2));

        long assessmentId;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO assessments (title, description, duration_minutes, total_marks, is_active, difficulty, topic_id) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "Assessment for deletion test");
            ps.setString(2, "Test ON DELETE SET NULL");
            ps.setInt(3, 30);
            ps.setInt(4, 25);
            ps.setBoolean(5, true);
            ps.setString(6, "EASY");
            ps.setLong(7, topic.getId());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertTrue(keys.next());
                assessmentId = keys.getLong(1);
            }
        }

        // Delete topic
        assertTrue(topicRepository.deleteById(topic.getId()));

        // Verify assessment still exists and topic_id is now NULL
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT title, topic_id FROM assessments WHERE id = ?")) {
            ps.setLong(1, assessmentId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("Assessment for deletion test", rs.getString("title"));
                assertNull(rs.getObject("topic_id"), "topic_id must be SET NULL when referenced topic is deleted");
            }
        }
    }
}
