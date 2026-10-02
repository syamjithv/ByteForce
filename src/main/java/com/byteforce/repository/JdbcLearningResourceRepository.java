package com.byteforce.repository;

import com.byteforce.domain.LearningResource;
import com.byteforce.domain.ResourceType;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Production JDBC implementation of {@link LearningResourceRepository}.
 */
public class JdbcLearningResourceRepository implements LearningResourceRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcLearningResourceRepository.class);

    private static final String SELECT_BASE = """
            SELECT id, concept_id, title, resource_type, url, description, display_order
            FROM learning_resources
            """;

    private final DataSource dataSource;

    public JdbcLearningResourceRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public List<LearningResource> findByConceptId(long conceptId) {
        if (conceptId <= 0) {
            return List.of();
        }
        String sql = SELECT_BASE + " WHERE concept_id = ? ORDER BY display_order ASC, id ASC";
        List<LearningResource> resources = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, conceptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resources.add(mapRowToLearningResource(rs));
                }
            }
            return resources;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while fetching learning resources for concept ID: " + conceptId, e);
        }
    }

    @Override
    public List<LearningResource> findAll() {
        String sql = SELECT_BASE + " ORDER BY concept_id ASC, display_order ASC, id ASC";
        List<LearningResource> resources = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resources.add(mapRowToLearningResource(rs));
            }
            return resources;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while fetching all learning resources", e);
        }
    }

    @Override
    public Optional<LearningResource> findById(long id) {
        if (id <= 0) {
            return Optional.empty();
        }
        String sql = SELECT_BASE + " WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToLearningResource(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding learning resource by ID: " + id, e);
        }
    }

    @Override
    public LearningResource save(LearningResource resource) {
        Objects.requireNonNull(resource, "resource must not be null");
        if (resource.getId() > 0 && existsById(resource.getId())) {
            return update(resource);
        } else {
            return insert(resource);
        }
    }

    private LearningResource insert(LearningResource resource) {
        String sql = """
                INSERT INTO learning_resources (concept_id, title, resource_type, url, description, display_order)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, resource.getConceptId());
            ps.setString(2, resource.getTitle());
            ps.setString(3, resource.getResourceType().name());
            ps.setString(4, resource.getUrl());
            ps.setString(5, resource.getDescription());
            ps.setInt(6, 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return resource.withId(keys.getLong(1));
                }
                return resource;
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while inserting learning resource: " + resource.getTitle(), e);
        }
    }

    private LearningResource update(LearningResource resource) {
        String sql = """
                UPDATE learning_resources
                SET concept_id = ?, title = ?, resource_type = ?, url = ?, description = ?
                WHERE id = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, resource.getConceptId());
            ps.setString(2, resource.getTitle());
            ps.setString(3, resource.getResourceType().name());
            ps.setString(4, resource.getUrl());
            ps.setString(5, resource.getDescription());
            ps.setLong(6, resource.getId());
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new ResourceNotFoundException("Cannot update non-existent learning resource ID: " + resource.getId());
            }
            return resource;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while updating learning resource ID: " + resource.getId(), e);
        }
    }

    private boolean existsById(long id) {
        if (id <= 0) return false;
        String sql = "SELECT 1 FROM learning_resources WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while checking learning resource existence", e);
        }
    }

    @Override
    public void saveForConcept(long conceptId, LearningResource resource, int displayOrder) {
        Objects.requireNonNull(resource, "resource must not be null");
        String sql = """
                INSERT INTO learning_resources (concept_id, title, resource_type, url, description, display_order)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, conceptId);
            ps.setString(2, resource.getTitle());
            ps.setString(3, resource.getResourceType().name());
            ps.setString(4, resource.getUrl());
            ps.setString(5, resource.getDescription());
            ps.setInt(6, displayOrder);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ByteForceException("Database error while inserting learning resource for concept ID: " + conceptId, e);
        }
    }

    @Override
    public boolean deleteById(long id) {
        if (id <= 0) {
            return false;
        }
        String sql = "DELETE FROM learning_resources WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while deleting learning resource with ID: " + id, e);
        }
    }

    @Override
    public void deleteByConceptId(long conceptId) {
        if (conceptId <= 0) {
            return;
        }
        String sql = "DELETE FROM learning_resources WHERE concept_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, conceptId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ByteForceException("Database error while deleting learning resources for concept ID: " + conceptId, e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM learning_resources";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while counting learning resources", e);
        }
    }

    private LearningResource mapRowToLearningResource(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        long conceptId = rs.getLong("concept_id");
        String title = rs.getString("title");
        String typeStr = rs.getString("resource_type");
        String url = rs.getString("url");
        String description = rs.getString("description");

        ResourceType resourceType;
        try {
            resourceType = ResourceType.valueOf(typeStr);
        } catch (Exception e) {
            resourceType = ResourceType.EXTERNAL;
        }

        return LearningResource.create(id, conceptId, title, resourceType, url, description);
    }
}
