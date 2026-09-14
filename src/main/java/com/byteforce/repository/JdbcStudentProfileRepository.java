package com.byteforce.repository;

import com.byteforce.domain.StudentProfile;
import com.byteforce.exception.ByteForceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Production-ready JDBC implementation of {@link StudentProfileRepository}.
 */
public class JdbcStudentProfileRepository implements StudentProfileRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcStudentProfileRepository.class);

    private static final String SELECT_BASE = """
            SELECT id, user_id, full_name, phone, college, graduation_year, created_at, updated_at
            FROM student_profiles
            """;

    private final DataSource dataSource;

    public JdbcStudentProfileRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public Optional<StudentProfile> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        String sql = SELECT_BASE + " WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToProfile(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding student profile by ID: " + id, e);
        }
    }

    @Override
    public Optional<StudentProfile> findByUserId(UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }
        String sql = SELECT_BASE + " WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToProfile(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding student profile for user: " + userId, e);
        }
    }

    @Override
    public StudentProfile save(StudentProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");

        if (existsByUserId(profile.getUserId())) {
            return update(profile);
        } else {
            return insert(profile);
        }
    }

    private StudentProfile insert(StudentProfile profile) {
        String sql = """
                INSERT INTO student_profiles (id, user_id, full_name, phone, college, graduation_year, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, profile.getId().toString());
            ps.setString(2, profile.getUserId().toString());
            ps.setString(3, profile.getFullName());
            ps.setString(4, profile.getPhone());
            ps.setString(5, profile.getCollege());
            if (profile.getGraduationYear() != null) {
                ps.setInt(6, profile.getGraduationYear());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            ps.setTimestamp(7, Timestamp.from(profile.getCreatedAt()));
            ps.setTimestamp(8, Timestamp.from(profile.getUpdatedAt()));

            ps.executeUpdate();
            return profile;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while inserting student profile for user: " + profile.getUserId(), e);
        }
    }

    private StudentProfile update(StudentProfile profile) {
        String sql = """
                UPDATE student_profiles
                SET full_name = ?, phone = ?, college = ?, graduation_year = ?, updated_at = ?
                WHERE user_id = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            Instant now = Instant.now();
            ps.setString(1, profile.getFullName());
            ps.setString(2, profile.getPhone());
            ps.setString(3, profile.getCollege());
            if (profile.getGraduationYear() != null) {
                ps.setInt(4, profile.getGraduationYear());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setTimestamp(5, Timestamp.from(now));
            ps.setString(6, profile.getUserId().toString());

            int updated = ps.executeUpdate();
            if (updated == 0) {
                return insert(profile);
            }
            return new StudentProfile(profile.getId(), profile.getUserId(), profile.getFullName(),
                    profile.getPhone(), profile.getCollege(), profile.getGraduationYear(),
                    profile.getCreatedAt(), now);
        } catch (SQLException e) {
            throw new ByteForceException("Database error while updating student profile for user: " + profile.getUserId(), e);
        }
    }

    @Override
    public boolean deleteByUserId(UUID userId) {
        if (userId == null) {
            return false;
        }
        String sql = "DELETE FROM student_profiles WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while deleting student profile for user: " + userId, e);
        }
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        if (userId == null) {
            return false;
        }
        String sql = "SELECT 1 FROM student_profiles WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while checking profile existence for user: " + userId, e);
        }
    }

    private StudentProfile mapRowToProfile(ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        UUID userId = UUID.fromString(rs.getString("user_id"));
        String fullName = rs.getString("full_name");
        String phone = rs.getString("phone");
        String college = rs.getString("college");
        int year = rs.getInt("graduation_year");
        Integer graduationYear = rs.wasNull() ? null : year;

        Timestamp createdTs = rs.getTimestamp("created_at");
        Instant createdAt = createdTs != null ? createdTs.toInstant() : Instant.now();

        Timestamp updatedTs = rs.getTimestamp("updated_at");
        Instant updatedAt = updatedTs != null ? updatedTs.toInstant() : Instant.now();

        return new StudentProfile(id, userId, fullName, phone, college, graduationYear, createdAt, updatedAt);
    }
}
