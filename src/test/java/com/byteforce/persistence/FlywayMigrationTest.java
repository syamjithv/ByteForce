package com.byteforce.persistence;

import com.byteforce.config.AppConfig;
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
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayMigrationTest {

    private HikariDataSource dataSource;

    @BeforeEach
    void setUp() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("FlywayTestPool-" + UUID.randomUUID());
        hikariConfig.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        hikariConfig.setUsername("sa");
        hikariConfig.setPassword("");
        hikariConfig.setMaximumPoolSize(3);
        dataSource = new HikariDataSource(hikariConfig);
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Test
    @DisplayName("Should apply V1 through V10 migrations and create all core tables including company tables and concept progress")
    void shouldApplyInitialMigrationSuccessfully() throws SQLException {
        int migrationsExecuted = DatabaseMigrator.migrate(dataSource, "classpath:db/migration");
        assertEquals(10, migrationsExecuted, "Flyway migrations should execute V1 through V10 scripts");

        // Verify that all core entities exist as tables
        Set<String> expectedTables = Set.of(
                "roles",
                "users",
                "user_roles",
                "student_profiles",
                "topics",
                "questions",
                "question_attempts",
                "bookmarks",
                "activities",
                "assessments",
                "assessment_questions",
                "assessment_attempts",
                "assessment_answers",
                "subjects",
                "concepts",
                "learning_resources",
                "remember_items",
                "concept_relationships",
                "aptitude_questions",
                "staged_questions",
                "user_remember_reviews",
                "companies",
                "company_topics",
                "company_questions",
                "company_aptitude_questions",
                "company_assessments",
                "company_interview_categories",
                "user_concept_progress"
        );

        Set<String> actualTables = new HashSet<>();
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                actualTables.add(rs.getString("TABLE_NAME").toLowerCase());
            }
        }

        for (String expectedTable : expectedTables) {
            assertTrue(actualTables.contains(expectedTable.toLowerCase()),
                    "Missing expected table: " + expectedTable + ". Actual tables: " + actualTables);
        }

        // Verify that Flyway metadata table exists
        assertTrue(actualTables.contains("flyway_schema_history"));
    }

    @Test
    @DisplayName("Should seed default STUDENT and ADMIN roles during V1 migration")
    void shouldSeedDefaultRoles() throws SQLException {
        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");

        Set<String> roles = new HashSet<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM roles")) {
            while (rs.next()) {
                roles.add(rs.getString("name"));
            }
        }

        assertTrue(roles.contains("STUDENT"), "Seed data must contain STUDENT role");
        assertTrue(roles.contains("ADMIN"), "Seed data must contain ADMIN role");
    }

    @Test
    @DisplayName("Should verify foreign key constraints and data insertion across core tables")
    void shouldSupportDataInsertionAndForeignKeyConstraints() throws SQLException {
        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");

        String userId = UUID.randomUUID().toString();
        String email = "candidate@byteforce.com";

        try (Connection conn = dataSource.getConnection()) {
            // 1. Insert user
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)")) {
                ps.setString(1, userId);
                ps.setString(2, email);
                ps.setString(3, "$2a$12$testHashVal");
                ps.executeUpdate();
            }

            // 2. Assign role
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO user_roles (user_id, role_id) VALUES (?, (SELECT id FROM roles WHERE name = 'STUDENT'))")) {
                ps.setString(1, userId);
                ps.executeUpdate();
            }

            // 3. Create student profile
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO student_profiles (id, user_id, full_name, college, graduation_year) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, userId);
                ps.setString(3, "John Doe");
                ps.setString(4, "Tech University");
                ps.setInt(5, 2026);
                ps.executeUpdate();
            }

            // 4. Verify join query
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT u.email, sp.full_name, r.name as role_name " +
                            "FROM users u " +
                            "JOIN user_roles ur ON u.id = ur.user_id " +
                            "JOIN roles r ON ur.role_id = r.id " +
                            "JOIN student_profiles sp ON u.id = sp.user_id " +
                            "WHERE u.id = ?")) {
                ps.setString(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(email, rs.getString("email"));
                    assertEquals("John Doe", rs.getString("full_name"));
                    assertEquals("STUDENT", rs.getString("role_name"));
                }
            }
        }
    }

    @Test
    @DisplayName("Migration should be idempotent when re-run on an up-to-date schema")
    void migrationShouldBeIdempotent() {
        int firstRun = DatabaseMigrator.migrate(dataSource, "classpath:db/migration");
        assertEquals(10, firstRun);

        int secondRun = DatabaseMigrator.migrate(dataSource, "classpath:db/migration");
        assertEquals(0, secondRun, "Subsequent migration run should execute 0 scripts");
    }

    @Test
    @DisplayName("Should verify V2 columns on questions, assessments, and assessment_answers")
    void shouldVerifyV2Enhancements() throws SQLException {
        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");

        try (Connection conn = dataSource.getConnection()) {
            // Verify questions.question_type exists
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT question_type FROM questions WHERE 1=0")) {
                assertNotNull(rs.getMetaData());
            }

            // Verify assessments.difficulty and assessments.topic_id exist
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT difficulty, topic_id FROM assessments WHERE 1=0")) {
                assertNotNull(rs.getMetaData());
            }

            // Verify assessment_answers columns exist
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT id, assessment_attempt_id, question_id, submitted_answer, marks_awarded, status, answered_at FROM assessment_answers WHERE 1=0")) {
                assertNotNull(rs.getMetaData());
            }
        }
    }

    @Test
    @DisplayName("Should verify V4 enhancements: avatar_url exists, mascot column is removed, and Learn tables exist")
    void shouldVerifyV4EnhancementsAndMascotRemoval() throws SQLException {
        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");

        try (Connection conn = dataSource.getConnection()) {
            // Verify avatar_url exists
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT avatar_url FROM student_profiles WHERE 1=0")) {
                assertNotNull(rs.getMetaData());
            }

            // Verify mascot column has been dropped
            boolean mascotColumnFound = false;
            try (ResultSet cols = conn.getMetaData().getColumns(null, null, "student_profiles", "mascot")) {
                if (cols.next()) {
                    mascotColumnFound = true;
                }
            }
            org.junit.jupiter.api.Assertions.assertFalse(mascotColumnFound, "Mascot column should be removed by V4 migration");

            // Verify topics.subject_id exists
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT subject_id FROM topics WHERE 1=0")) {
                assertNotNull(rs.getMetaData());
            }

            // Verify subjects, concepts, learning_resources queries work
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, name, description FROM subjects WHERE 1=0")) {
                assertNotNull(rs.getMetaData());
            }
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, topic_id, title, key_points, example FROM concepts WHERE 1=0")) {
                assertNotNull(rs.getMetaData());
            }
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, concept_id, title, resource_type, url FROM learning_resources WHERE 1=0")) {
                assertNotNull(rs.getMetaData());
            }
        }
    }

    @Test
    @DisplayName("Should verify V5 enhancements: remember_items and concept_relationships tables and schema")
    void shouldVerifyV5RememberAndBrainMapsTables() throws SQLException {
        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");

        try (Connection conn = dataSource.getConnection()) {
            // Verify remember_items table structure
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, concept_id, type, content, display_order, active, created_at, updated_at FROM remember_items WHERE 1=0")) {
                assertNotNull(rs.getMetaData());
                assertEquals(8, rs.getMetaData().getColumnCount());
            }

            // Verify concept_relationships table structure
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, source_concept_id, target_concept_id, relationship_type, description, display_order, created_at, updated_at FROM concept_relationships WHERE 1=0")) {
                assertNotNull(rs.getMetaData());
                assertEquals(8, rs.getMetaData().getColumnCount());
            }
        }
    }

    @Test
    @DisplayName("Should verify V6 enhancements: aptitude_questions table and schema")
    void shouldVerifyV6AptitudeTable() throws SQLException {
        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");

        try (Connection conn = dataSource.getConnection()) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, category, topic, difficulty, question, option_a, option_b, option_c, option_d, correct_answer, explanation, active, created_at, updated_at FROM aptitude_questions WHERE 1=0")) {
                assertNotNull(rs.getMetaData());
                assertTrue(rs.getMetaData().getColumnCount() >= 14);
            }
        }
    }

    @Test
    @DisplayName("Should verify V7 enhancements: provenance columns, staged_questions table, and topic extensions")
    void shouldVerifyV7ProvenanceAndStaging() throws SQLException {
        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");

        try (Connection conn = dataSource.getConnection()) {
            // Verify provenance columns on questions table
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT source_repo, source_url, license, author, attribution, review_status FROM questions WHERE 1=0")) {
                assertNotNull(rs.getMetaData());
                assertEquals(6, rs.getMetaData().getColumnCount());
            }

            // Verify provenance columns on aptitude_questions table
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT source_repo, source_url, license, author, attribution, review_status FROM aptitude_questions WHERE 1=0")) {
                assertNotNull(rs.getMetaData());
                assertEquals(6, rs.getMetaData().getColumnCount());
            }

            // Verify staged_questions table
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, batch_id, source_repo, source_path, license, author, attribution, category_or_subject, topic, difficulty, question_type, question_text, option_a, option_b, option_c, option_d, correct_answer, solution, status, rejection_reason, content_hash, target_table, target_id, created_at FROM staged_questions WHERE 1=0")) {
                assertNotNull(rs.getMetaData());
                assertEquals(24, rs.getMetaData().getColumnCount());
            }

            // Verify new subjects exist
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM subjects WHERE id IN ('computer-networks', 'computer-organization', 'theory-of-computation')")) {
                assertTrue(rs.next());
                assertEquals(3, rs.getInt(1));
            }
        }
    }

    @Test
    @DisplayName("Should verify V8 enhancements: user_remember_reviews table and schema")
    void shouldVerifyV8MemoryReviewStateTable() throws SQLException {
        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");

        try (Connection conn = dataSource.getConnection()) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, user_id, remember_item_id, due_at, last_reviewed_at, review_count, successful_review_count, state, difficulty, stability, retrievability, last_rating, created_at, updated_at FROM user_remember_reviews WHERE 1=0")) {
                assertNotNull(rs.getMetaData());
                assertEquals(14, rs.getMetaData().getColumnCount());
            }
        }
    }

    @Test
    @DisplayName("Should verify V9 enhancements: companies table and initial 12 seed records")
    void shouldVerifyV9CompanyPracticeTables() throws SQLException {
        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");

        try (Connection conn = dataSource.getConnection()) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, name, slug, logo_path, website_url, short_description, description, active, last_reviewed_at, created_at, updated_at FROM companies WHERE 1=0")) {
                assertNotNull(rs.getMetaData());
                assertEquals(11, rs.getMetaData().getColumnCount());
            }

            // Verify initial 12 companies seeded
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM companies")) {
                assertTrue(rs.next());
                assertEquals(12, rs.getInt(1), "Should seed exactly 12 initial companies");
            }
        }
    }

    @Test
    @DisplayName("Should skip migration when flyway.enabled is configured to false")
    void shouldSkipMigrationWhenDisabled() {
        Properties props = new Properties();
        props.setProperty(AppConfig.KEY_DB_URL, "jdbc:h2:mem:disabled_test");
        props.setProperty(AppConfig.KEY_DB_USERNAME, "sa");
        props.setProperty(AppConfig.KEY_FLYWAY_ENABLED, "false");

        AppConfig config = AppConfig.from(props, key -> null);
        int executed = DatabaseMigrator.migrate(dataSource, config);

        assertEquals(0, executed);
    }
}
