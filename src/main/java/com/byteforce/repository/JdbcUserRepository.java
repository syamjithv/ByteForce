package com.byteforce.repository;

import com.byteforce.domain.Role;
import com.byteforce.domain.User;
import com.byteforce.exception.ByteForceException;
import com.byteforce.exception.DuplicateUserException;
import com.byteforce.validation.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Production-ready JDBC implementation of {@link UserRepository}.
 * Interacts with users, user_roles, roles, and student_profiles tables
 * using prepared statements and transaction boundaries.
 */
public class JdbcUserRepository implements UserRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcUserRepository.class);

    private static final String SELECT_USER_BASE_SQL = """
            SELECT u.id, u.email, u.password_hash, u.created_at, u.updated_at,
                   r.name AS role_name,
                   sp.full_name
            FROM users u
            LEFT JOIN user_roles ur ON u.id = ur.user_id
            LEFT JOIN roles r ON ur.role_id = r.id
            LEFT JOIN student_profiles sp ON u.id = sp.user_id
            """;

    private final DataSource dataSource;

    public JdbcUserRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public Optional<User> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }

        String sql = SELECT_USER_BASE_SQL + " WHERE u.id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding user by ID: " + id, e);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        String normalizedEmail = EmailValidator.normalize(email);
        String sql = SELECT_USER_BASE_SQL + " WHERE u.email = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizedEmail);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding user by email: " + normalizedEmail, e);
        }
    }

    @Override
    public User save(User user) {
        Objects.requireNonNull(user, "user must not be null");

        String normalizedEmail = EmailValidator.normalize(user.getEmail());

        try (Connection conn = dataSource.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);

                boolean isExisting = existsById(conn, user.getId());

                if (isEmailTaken(conn, normalizedEmail, isExisting ? user.getId() : null)) {
                    throw new DuplicateUserException("A user with email '" + normalizedEmail + "' already exists.");
                }

                if (isExisting) {
                    updateUser(conn, user, normalizedEmail);
                } else {
                    insertUser(conn, user, normalizedEmail);
                }

                conn.commit();
                return user;
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    log.error("Failed to rollback transaction during save for user ID {}", user.getId(), rollbackEx);
                }

                if (isDuplicateKeyViolation(e)) {
                    throw new DuplicateUserException("A user with email '" + normalizedEmail + "' already exists.", e);
                }
                throw new ByteForceException("Failed to save user: " + user.getId(), e);
            } finally {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (SQLException e) {
                    log.warn("Failed to restore auto-commit mode", e);
                }
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database connection error while saving user: " + user.getId(), e);
        }
    }

    private boolean existsById(Connection conn, UUID id) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean isEmailTaken(Connection conn, String normalizedEmail, UUID excludeUserId) throws SQLException {
        String sql = excludeUserId == null
                ? "SELECT 1 FROM users WHERE email = ?"
                : "SELECT 1 FROM users WHERE email = ? AND id <> ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizedEmail);
            if (excludeUserId != null) {
                ps.setString(2, excludeUserId.toString());
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Long getRoleId(Connection conn, String roleName) throws SQLException {
        String sql = "SELECT id FROM roles WHERE name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roleName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }
        return null;
    }

    private void insertUser(Connection conn, User user, String normalizedEmail) throws SQLException {
        Long roleId = getRoleId(conn, user.getRole().name());
        if (roleId == null) {
            throw new ByteForceException("Required role '" + user.getRole().name() + "' not found in database roles table.");
        }

        // 1. Insert into users table
        String insertUserSql = "INSERT INTO users (id, email, password_hash, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertUserSql)) {
            ps.setString(1, user.getId().toString());
            ps.setString(2, normalizedEmail);
            ps.setString(3, user.getPasswordHash());
            ps.setTimestamp(4, Timestamp.from(user.getCreatedAt()));
            ps.setTimestamp(5, Timestamp.from(user.getUpdatedAt()));
            ps.executeUpdate();
        }

        // 2. Insert into user_roles table
        String insertUserRoleSql = "INSERT INTO user_roles (user_id, role_id, assigned_at) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertUserRoleSql)) {
            ps.setString(1, user.getId().toString());
            ps.setLong(2, roleId);
            ps.setTimestamp(3, Timestamp.from(user.getCreatedAt()));
            ps.executeUpdate();
        }

        // 3. Insert into student_profiles table
        String insertProfileSql = "INSERT INTO student_profiles (id, user_id, full_name, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertProfileSql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, user.getId().toString());
            ps.setString(3, user.getFullName());
            ps.setTimestamp(4, Timestamp.from(user.getCreatedAt()));
            ps.setTimestamp(5, Timestamp.from(user.getUpdatedAt()));
            ps.executeUpdate();
        }
    }

    private void updateUser(Connection conn, User user, String normalizedEmail) throws SQLException {
        Long roleId = getRoleId(conn, user.getRole().name());
        if (roleId == null) {
            throw new ByteForceException("Required role '" + user.getRole().name() + "' not found in database roles table.");
        }

        // 1. Update users table
        String updateUserSql = "UPDATE users SET email = ?, password_hash = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(updateUserSql)) {
            ps.setString(1, normalizedEmail);
            ps.setString(2, user.getPasswordHash());
            ps.setTimestamp(3, Timestamp.from(user.getUpdatedAt()));
            ps.setString(4, user.getId().toString());
            ps.executeUpdate();
        }

        // 2. Update user_roles table (clean replace)
        String deleteRoleSql = "DELETE FROM user_roles WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(deleteRoleSql)) {
            ps.setString(1, user.getId().toString());
            ps.executeUpdate();
        }

        String insertUserRoleSql = "INSERT INTO user_roles (user_id, role_id, assigned_at) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertUserRoleSql)) {
            ps.setString(1, user.getId().toString());
            ps.setLong(2, roleId);
            ps.setTimestamp(3, Timestamp.from(user.getUpdatedAt()));
            ps.executeUpdate();
        }

        // 3. Update student_profiles table
        String updateProfileSql = "UPDATE student_profiles SET full_name = ?, updated_at = ? WHERE user_id = ?";
        int updated;
        try (PreparedStatement ps = conn.prepareStatement(updateProfileSql)) {
            ps.setString(1, user.getFullName());
            ps.setTimestamp(2, Timestamp.from(user.getUpdatedAt()));
            ps.setString(3, user.getId().toString());
            updated = ps.executeUpdate();
        }

        if (updated == 0) {
            String insertProfileSql = "INSERT INTO student_profiles (id, user_id, full_name, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertProfileSql)) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, user.getId().toString());
                ps.setString(3, user.getFullName());
                ps.setTimestamp(4, Timestamp.from(user.getCreatedAt()));
                ps.setTimestamp(5, Timestamp.from(user.getUpdatedAt()));
                ps.executeUpdate();
            }
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");

        Timestamp createdTs = rs.getTimestamp("created_at");
        Instant createdAt = createdTs != null ? createdTs.toInstant() : Instant.now();

        Timestamp updatedTs = rs.getTimestamp("updated_at");
        Instant updatedAt = updatedTs != null ? updatedTs.toInstant() : Instant.now();

        String roleName = rs.getString("role_name");
        Role role = Role.STUDENT;
        if (roleName != null && !roleName.isBlank()) {
            try {
                role = Role.valueOf(roleName.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown role '{}' for user ID {}, falling back to STUDENT", roleName, id);
            }
        }

        String fullName = rs.getString("full_name");
        if (fullName == null || fullName.isBlank()) {
            fullName = email;
        }

        return new User(id, email, passwordHash, fullName, role, createdAt, updatedAt);
    }

    private boolean isDuplicateKeyViolation(SQLException e) {
        if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
            return true;
        }
        int errorCode = e.getErrorCode();
        if (errorCode == 1062 || errorCode == 23505) {
            return true;
        }
        String msg = e.getMessage();
        if (msg != null) {
            String lower = msg.toLowerCase();
            return lower.contains("duplicate") || lower.contains("unique") || lower.contains("constraint");
        }
        return false;
    }
}
