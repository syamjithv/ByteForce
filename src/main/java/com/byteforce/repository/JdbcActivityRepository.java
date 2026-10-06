package com.byteforce.repository;

import com.byteforce.domain.Activity;
import com.byteforce.domain.ActivityType;
import com.byteforce.exception.ByteForceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Production-ready JDBC implementation of {@link ActivityRepository}.
 */
public class JdbcActivityRepository implements ActivityRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcActivityRepository.class);

    private static final String SELECT_BASE = """
            SELECT id, user_id, activity_type, description, created_at
            FROM activities
            """;

    private final DataSource dataSource;

    public JdbcActivityRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public Activity save(Activity activity) {
        Objects.requireNonNull(activity, "activity must not be null");
        String sql = """
                INSERT INTO activities (user_id, activity_type, description, created_at)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, activity.getUserId().toString());
            ps.setString(2, activity.getActivityType().name());
            ps.setString(3, activity.getDescription());
            ps.setTimestamp(4, Timestamp.from(activity.getCreatedAt()));

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long generatedId = keys.getLong(1);
                    return activity.withId(generatedId);
                } else {
                    throw new ByteForceException("Failed to retrieve generated ID for inserted activity.");
                }
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while inserting activity for user: " + activity.getUserId(), e);
        }
    }

    @Override
    public Optional<Activity> findById(long id) {
        if (id <= 0) {
            return Optional.empty();
        }
        String sql = SELECT_BASE + " WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToActivity(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding activity by ID: " + id, e);
        }
    }

    @Override
    public List<Activity> findByUserId(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        String sql = SELECT_BASE + " WHERE user_id = ? ORDER BY created_at DESC, id DESC";
        List<Activity> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToActivity(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding activities for user: " + userId, e);
        }
    }

    @Override
    public List<Activity> findRecentByUserId(UUID userId, int limit) {
        if (userId == null || limit <= 0) {
            return List.of();
        }
        String sql = SELECT_BASE + " WHERE user_id = ? ORDER BY created_at DESC, id DESC";
        List<Activity> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setMaxRows(limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToActivity(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding recent activities for user: " + userId, e);
        }
    }

    @Override
    public long countByUserId(UUID userId) {
        if (userId == null) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM activities WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while counting activities for user: " + userId, e);
        }
    }

    @Override
    public boolean deleteById(long id) {
        if (id <= 0) {
            return false;
        }
        String sql = "DELETE FROM activities WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while deleting activity by ID: " + id, e);
        }
    }

    private Activity mapRowToActivity(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        UUID userId = UUID.fromString(rs.getString("user_id"));
        String typeStr = rs.getString("activity_type");
        ActivityType type = ActivityType.ATTEMPTED_QUESTION;
        if (typeStr != null) {
            try {
                type = ActivityType.valueOf(typeStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown activity type '{}' for activity ID {}, defaulting to ATTEMPTED_QUESTION", typeStr, id);
            }
        }
        String description = rs.getString("description");
        Timestamp createdTs = rs.getTimestamp("created_at");
        Instant createdAt = createdTs != null ? createdTs.toInstant() : Instant.now();

        return new Activity(id, userId, type, description, createdAt);
    }
}
