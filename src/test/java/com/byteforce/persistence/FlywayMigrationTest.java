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
    @DisplayName("Should apply V1 migration and create all 12 core tables from database-design.md")
    void shouldApplyInitialMigrationSuccessfully() throws SQLException {
        int migrationsExecuted = DatabaseMigrator.migrate(dataSource, "classpath:db/migration");
        assertEquals(1, migrationsExecuted, "Initial V1 migration should execute exactly 1 script");

        // Verify that all 12 planned core entities exist as tables
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
                "assessment_attempts"
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
        assertEquals(1, firstRun);

        int secondRun = DatabaseMigrator.migrate(dataSource, "classpath:db/migration");
        assertEquals(0, secondRun, "Subsequent migration run should execute 0 scripts");
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
