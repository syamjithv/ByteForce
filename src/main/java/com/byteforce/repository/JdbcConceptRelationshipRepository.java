package com.byteforce.repository;

import com.byteforce.domain.ConceptRelationship;
import com.byteforce.domain.ConceptRelationshipType;
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
 * Production JDBC implementation of {@link ConceptRelationshipRepository}.
 */
public class JdbcConceptRelationshipRepository implements ConceptRelationshipRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcConceptRelationshipRepository.class);

    private static final String SELECT_BASE = """
            SELECT id, source_concept_id, target_concept_id, relationship_type, description,
                   display_order, created_at, updated_at
            FROM concept_relationships
            """;

    private final DataSource dataSource;

    public JdbcConceptRelationshipRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public List<ConceptRelationship> findBySourceConceptId(long conceptId) {
        if (conceptId <= 0) {
            return List.of();
        }
        String sql = SELECT_BASE + " WHERE source_concept_id = ? ORDER BY display_order ASC, id ASC";
        List<ConceptRelationship> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, conceptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToRelationship(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while fetching relationships for source concept: " + conceptId, e);
        }
    }

    @Override
    public List<ConceptRelationship> findByTargetConceptId(long conceptId) {
        if (conceptId <= 0) {
            return List.of();
        }
        String sql = SELECT_BASE + " WHERE target_concept_id = ? ORDER BY display_order ASC, id ASC";
        List<ConceptRelationship> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, conceptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToRelationship(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while fetching relationships for target concept: " + conceptId, e);
        }
    }

    @Override
    public List<ConceptRelationship> findByConceptId(long conceptId) {
        if (conceptId <= 0) {
            return List.of();
        }
        String sql = SELECT_BASE + " WHERE source_concept_id = ? OR target_concept_id = ? ORDER BY display_order ASC, id ASC";
        List<ConceptRelationship> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, conceptId);
            ps.setLong(2, conceptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToRelationship(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while fetching all relationships for concept: " + conceptId, e);
        }
    }

    @Override
    public List<ConceptRelationship> findAll() {
        String sql = SELECT_BASE + " ORDER BY source_concept_id ASC, display_order ASC, id ASC";
        List<ConceptRelationship> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToRelationship(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while fetching all concept relationships", e);
        }
    }

    @Override
    public Optional<ConceptRelationship> findById(long id) {
        if (id <= 0) {
            return Optional.empty();
        }
        String sql = SELECT_BASE + " WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToRelationship(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding concept relationship by ID: " + id, e);
        }
    }

    @Override
    public ConceptRelationship save(ConceptRelationship rel) {
        Objects.requireNonNull(rel, "relationship must not be null");
        if (rel.getId() > 0 && existsById(rel.getId())) {
            return update(rel);
        } else {
            return insert(rel);
        }
    }

    private ConceptRelationship insert(ConceptRelationship rel) {
        String sql = """
                INSERT INTO concept_relationships (source_concept_id, target_concept_id, relationship_type,
                                                   description, display_order, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, rel.getSourceConceptId());
            ps.setLong(2, rel.getTargetConceptId());
            ps.setString(3, rel.getRelationshipType().name());
            ps.setString(4, rel.getDescription());
            ps.setInt(5, rel.getDisplayOrder());
            ps.setTimestamp(6, Timestamp.from(rel.getCreatedAt()));
            ps.setTimestamp(7, Timestamp.from(rel.getUpdatedAt()));

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long generatedId = keys.getLong(1);
                    return rel.withId(generatedId);
                } else {
                    return rel;
                }
            }
        } catch (SQLException e) {
            if (isDuplicateKeyViolation(e)) {
                log.debug("Concept relationship already exists: {} -> {}", rel.getSourceConceptId(), rel.getTargetConceptId());
                return rel;
            }
            throw new ByteForceException("Database error while inserting concept relationship", e);
        }
    }

    private ConceptRelationship update(ConceptRelationship rel) {
        String sql = """
                UPDATE concept_relationships
                SET source_concept_id = ?, target_concept_id = ?, relationship_type = ?,
                    description = ?, display_order = ?, updated_at = ?
                WHERE id = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, rel.getSourceConceptId());
            ps.setLong(2, rel.getTargetConceptId());
            ps.setString(3, rel.getRelationshipType().name());
            ps.setString(4, rel.getDescription());
            ps.setInt(5, rel.getDisplayOrder());
            ps.setTimestamp(6, Timestamp.from(Instant.now()));
            ps.setLong(7, rel.getId());

            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new ResourceNotFoundException("Cannot update non-existent concept relationship with ID: " + rel.getId());
            }
            return rel;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while updating concept relationship with ID: " + rel.getId(), e);
        }
    }

    @Override
    public boolean exists(long sourceConceptId, long targetConceptId, ConceptRelationshipType type) {
        String sql = "SELECT 1 FROM concept_relationships WHERE source_concept_id = ? AND target_concept_id = ? AND relationship_type = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sourceConceptId);
            ps.setLong(2, targetConceptId);
            ps.setString(3, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while checking relationship existence", e);
        }
    }

    public boolean existsById(long id) {
        if (id <= 0) {
            return false;
        }
        String sql = "SELECT 1 FROM concept_relationships WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while checking relationship existence by ID: " + id, e);
        }
    }

    @Override
    public boolean deleteById(long id) {
        if (id <= 0) {
            return false;
        }
        String sql = "DELETE FROM concept_relationships WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while deleting concept relationship with ID: " + id, e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM concept_relationships";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while counting concept relationships", e);
        }
    }

    private ConceptRelationship mapRowToRelationship(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        long sourceConceptId = rs.getLong("source_concept_id");
        long targetConceptId = rs.getLong("target_concept_id");
        String typeStr = rs.getString("relationship_type");
        String description = rs.getString("description");
        int displayOrder = rs.getInt("display_order");
        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");

        ConceptRelationshipType type;
        try {
            type = ConceptRelationshipType.valueOf(typeStr);
        } catch (Exception e) {
            type = ConceptRelationshipType.RELATED;
        }

        Instant createdAt = createdTs != null ? createdTs.toInstant() : Instant.now();
        Instant updatedAt = updatedTs != null ? updatedTs.toInstant() : Instant.now();

        return new ConceptRelationship(id, sourceConceptId, targetConceptId, type, description, displayOrder, createdAt, updatedAt);
    }

    private boolean isDuplicateKeyViolation(SQLException e) {
        if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
            return true;
        }
        int errorCode = e.getErrorCode();
        return errorCode == 1062 || errorCode == 23505;
    }
}
