package com.byteforce.repository;

import com.byteforce.domain.Topic;
import com.byteforce.exception.ByteForceException;
import com.byteforce.exception.ResourceNotFoundException;
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

/**
 * Production-ready JDBC implementation of {@link TopicRepository}.
 */
public class JdbcTopicRepository implements TopicRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcTopicRepository.class);

    private static final String SELECT_BASE = """
            SELECT id, name, slug, description, display_order, created_at
            FROM topics
            """;

    private final DataSource dataSource;

    public JdbcTopicRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public Optional<Topic> findById(long id) {
        if (id <= 0) {
            return Optional.empty();
        }
        String sql = SELECT_BASE + " WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToTopic(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding topic by ID: " + id, e);
        }
    }

    @Override
    public Optional<Topic> findBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }
        String normalized = slug.trim().toLowerCase();
        String sql = SELECT_BASE + " WHERE slug = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalized);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToTopic(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding topic by slug: " + normalized, e);
        }
    }

    @Override
    public Optional<Topic> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String normalized = name.trim();
        String sql = SELECT_BASE + " WHERE LOWER(name) = LOWER(?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalized);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToTopic(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding topic by name: " + normalized, e);
        }
    }

    @Override
    public List<Topic> findAll() {
        String sql = SELECT_BASE + " ORDER BY display_order ASC, name ASC";
        List<Topic> topics = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                topics.add(mapRowToTopic(rs));
            }
            return topics;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while fetching all topics", e);
        }
    }

    @Override
    public Topic save(Topic topic) {
        Objects.requireNonNull(topic, "topic must not be null");
        if (topic.getId() <= 0) {
            return insert(topic);
        } else {
            return update(topic);
        }
    }

    private Topic insert(Topic topic) {
        String sql = """
                INSERT INTO topics (name, slug, description, display_order, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, topic.getName());
            ps.setString(2, topic.getSlug());
            ps.setString(3, topic.getDescription());
            ps.setInt(4, topic.getDisplayOrder());
            ps.setTimestamp(5, Timestamp.from(topic.getCreatedAt()));

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long generatedId = keys.getLong(1);
                    return topic.withId(generatedId);
                } else {
                    throw new ByteForceException("Failed to retrieve generated ID for inserted topic.");
                }
            }
        } catch (SQLException e) {
            if (isDuplicateKeyViolation(e)) {
                throw new ByteForceException("A topic with name '" + topic.getName() + "' or slug '" + topic.getSlug() + "' already exists.", e);
            }
            throw new ByteForceException("Database error while inserting topic: " + topic.getName(), e);
        }
    }

    private Topic update(Topic topic) {
        String sql = """
                UPDATE topics
                SET name = ?, slug = ?, description = ?, display_order = ?
                WHERE id = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, topic.getName());
            ps.setString(2, topic.getSlug());
            ps.setString(3, topic.getDescription());
            ps.setInt(4, topic.getDisplayOrder());
            ps.setLong(5, topic.getId());

            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new ResourceNotFoundException("Cannot update non-existent topic with ID: " + topic.getId());
            }
            return topic;
        } catch (SQLException e) {
            if (isDuplicateKeyViolation(e)) {
                throw new ByteForceException("A topic with name '" + topic.getName() + "' or slug '" + topic.getSlug() + "' already exists.", e);
            }
            throw new ByteForceException("Database error while updating topic with ID: " + topic.getId(), e);
        }
    }

    @Override
    public boolean deleteById(long id) {
        if (id <= 0) {
            return false;
        }
        String sql = "DELETE FROM topics WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while deleting topic with ID: " + id, e);
        }
    }

    @Override
    public boolean existsById(long id) {
        if (id <= 0) {
            return false;
        }
        String sql = "SELECT 1 FROM topics WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while checking topic existence by ID: " + id, e);
        }
    }

    @Override
    public boolean existsBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return false;
        }
        String sql = "SELECT 1 FROM topics WHERE slug = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, slug.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while checking topic existence by slug: " + slug, e);
        }
    }

    @Override
    public boolean existsByName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String sql = "SELECT 1 FROM topics WHERE LOWER(name) = LOWER(?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while checking topic existence by name: " + name, e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM topics";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while counting topics", e);
        }
    }

    private Topic mapRowToTopic(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String name = rs.getString("name");
        String slug = rs.getString("slug");
        String description = rs.getString("description");
        int displayOrder = rs.getInt("display_order");
        Timestamp createdTs = rs.getTimestamp("created_at");
        Instant createdAt = createdTs != null ? createdTs.toInstant() : Instant.now();

        return new Topic(id, name, slug, description, displayOrder, createdAt);
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
