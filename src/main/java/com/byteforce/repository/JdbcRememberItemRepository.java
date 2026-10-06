package com.byteforce.repository;

import com.byteforce.domain.RememberItem;
import com.byteforce.domain.RememberItemType;
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
 * Production JDBC implementation of {@link RememberItemRepository}.
 */
public class JdbcRememberItemRepository implements RememberItemRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcRememberItemRepository.class);

    private static final String SELECT_BASE = """
            SELECT id, concept_id, type, content, display_order, active, created_at, updated_at
            FROM remember_items
            """;

    private final DataSource dataSource;

    public JdbcRememberItemRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public List<RememberItem> findByConceptId(long conceptId) {
        if (conceptId <= 0) {
            return List.of();
        }
        String sql = SELECT_BASE + " WHERE concept_id = ? AND active = TRUE ORDER BY display_order ASC, id ASC";
        List<RememberItem> items = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, conceptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRowToRememberItem(rs));
                }
            }
            return items;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while fetching remember items for concept ID: " + conceptId, e);
        }
    }

    @Override
    public Optional<RememberItem> findById(long id) {
        if (id <= 0) {
            return Optional.empty();
        }
        String sql = SELECT_BASE + " WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToRememberItem(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding remember item by ID: " + id, e);
        }
    }

    @Override
    public List<RememberItem> findAll() {
        String sql = SELECT_BASE + " WHERE active = TRUE ORDER BY concept_id ASC, display_order ASC, id ASC";
        List<RememberItem> items = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(mapRowToRememberItem(rs));
            }
            return items;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while fetching all remember items", e);
        }
    }

    @Override
    public List<RememberItem> findByType(RememberItemType type) {
        if (type == null) {
            return List.of();
        }
        String sql = SELECT_BASE + " WHERE type = ? AND active = TRUE ORDER BY concept_id ASC, display_order ASC, id ASC";
        List<RememberItem> items = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRowToRememberItem(rs));
                }
            }
            return items;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while fetching remember items by type: " + type, e);
        }
    }

    @Override
    public RememberItem save(RememberItem item) {
        Objects.requireNonNull(item, "item must not be null");
        if (item.getId() > 0 && existsById(item.getId())) {
            return update(item);
        } else if (item.getId() > 0) {
            return insertWithId(item);
        } else {
            return insert(item);
        }
    }

    private RememberItem insert(RememberItem item) {
        String sql = """
                INSERT INTO remember_items (concept_id, type, content, display_order, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, item.getConceptId());
            ps.setString(2, item.getType().name());
            ps.setString(3, item.getContent());
            ps.setInt(4, item.getDisplayOrder());
            ps.setBoolean(5, item.isActive());
            ps.setTimestamp(6, Timestamp.from(item.getCreatedAt()));
            ps.setTimestamp(7, Timestamp.from(item.getUpdatedAt()));

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long generatedId = keys.getLong(1);
                    return item.withId(generatedId);
                } else {
                    throw new ByteForceException("Failed to retrieve generated ID for remember item.");
                }
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while inserting remember item for concept ID: " + item.getConceptId(), e);
        }
    }

    private RememberItem insertWithId(RememberItem item) {
        String sql = """
                INSERT INTO remember_items (id, concept_id, type, content, display_order, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, item.getId());
            ps.setLong(2, item.getConceptId());
            ps.setString(3, item.getType().name());
            ps.setString(4, item.getContent());
            ps.setInt(5, item.getDisplayOrder());
            ps.setBoolean(6, item.isActive());
            ps.setTimestamp(7, Timestamp.from(item.getCreatedAt()));
            ps.setTimestamp(8, Timestamp.from(item.getUpdatedAt()));

            ps.executeUpdate();
            return item;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while inserting remember item with ID: " + item.getId(), e);
        }
    }

    private RememberItem update(RememberItem item) {
        String sql = """
                UPDATE remember_items
                SET concept_id = ?, type = ?, content = ?, display_order = ?, active = ?, updated_at = ?
                WHERE id = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, item.getConceptId());
            ps.setString(2, item.getType().name());
            ps.setString(3, item.getContent());
            ps.setInt(4, item.getDisplayOrder());
            ps.setBoolean(5, item.isActive());
            ps.setTimestamp(6, Timestamp.from(Instant.now()));
            ps.setLong(7, item.getId());

            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new ResourceNotFoundException("Cannot update non-existent remember item with ID: " + item.getId());
            }
            return item;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while updating remember item with ID: " + item.getId(), e);
        }
    }

    @Override
    public boolean existsById(long id) {
        if (id <= 0) {
            return false;
        }
        String sql = "SELECT 1 FROM remember_items WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while checking remember item existence: " + id, e);
        }
    }

    @Override
    public boolean deleteById(long id) {
        if (id <= 0) {
            return false;
        }
        String sql = "DELETE FROM remember_items WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while deleting remember item with ID: " + id, e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM remember_items WHERE active = TRUE";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while counting remember items", e);
        }
    }

    private RememberItem mapRowToRememberItem(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        long conceptId = rs.getLong("concept_id");
        String typeStr = rs.getString("type");
        String content = rs.getString("content");
        int displayOrder = rs.getInt("display_order");
        boolean active = rs.getBoolean("active");
        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");

        RememberItemType type;
        try {
            type = RememberItemType.valueOf(typeStr);
        } catch (Exception e) {
            type = RememberItemType.KEY_FACT;
        }

        Instant createdAt = createdTs != null ? createdTs.toInstant() : Instant.now();
        Instant updatedAt = updatedTs != null ? updatedTs.toInstant() : Instant.now();

        return new RememberItem(id, conceptId, type, content, displayOrder, active, createdAt, updatedAt);
    }
}
