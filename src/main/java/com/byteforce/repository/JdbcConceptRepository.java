package com.byteforce.repository;

import com.byteforce.domain.Concept;
import com.byteforce.domain.LearningResource;
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Production JDBC implementation of {@link ConceptRepository}.
 */
public class JdbcConceptRepository implements ConceptRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcConceptRepository.class);

    private static final String SELECT_BASE = """
            SELECT c.id, c.topic_id, t.name AS topic_name, c.title, c.slug,
                   c.short_explanation, c.key_points, c.example, c.display_order
            FROM concepts c
            LEFT JOIN topics t ON c.topic_id = t.id
            """;

    private final DataSource dataSource;
    private final LearningResourceRepository resourceRepository;

    public JdbcConceptRepository(DataSource dataSource, LearningResourceRepository resourceRepository) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
        this.resourceRepository = Objects.requireNonNull(resourceRepository, "resourceRepository must not be null");
    }

    @Override
    public Optional<Concept> findById(long id) {
        if (id <= 0) {
            return Optional.empty();
        }
        String sql = SELECT_BASE + " WHERE c.id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Concept concept = mapRowToConcept(rs);
                    List<LearningResource> resources = resourceRepository.findByConceptId(concept.getId());
                    return Optional.of(Concept.create(
                            concept.getId(),
                            concept.getTopicId(),
                            concept.getTopicName(),
                            concept.getTitle(),
                            concept.getShortExplanation(),
                            concept.getKeyPoints(),
                            concept.getExample(),
                            resources
                    ));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding concept by ID: " + id, e);
        }
    }

    @Override
    public List<Concept> findByTopicId(long topicId) {
        if (topicId <= 0) {
            return List.of();
        }
        String sql = SELECT_BASE + " WHERE c.topic_id = ? ORDER BY c.display_order ASC, c.id ASC";
        List<Concept> concepts = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Concept concept = mapRowToConcept(rs);
                    List<LearningResource> resources = resourceRepository.findByConceptId(concept.getId());
                    concepts.add(Concept.create(
                            concept.getId(),
                            concept.getTopicId(),
                            concept.getTopicName(),
                            concept.getTitle(),
                            concept.getShortExplanation(),
                            concept.getKeyPoints(),
                            concept.getExample(),
                            resources
                    ));
                }
            }
            return concepts;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while fetching concepts for topic ID: " + topicId, e);
        }
    }

    @Override
    public List<Concept> findAll() {
        String sql = SELECT_BASE + " ORDER BY c.display_order ASC, c.id ASC";
        List<Concept> concepts = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Concept concept = mapRowToConcept(rs);
                List<LearningResource> resources = resourceRepository.findByConceptId(concept.getId());
                concepts.add(Concept.create(
                        concept.getId(),
                        concept.getTopicId(),
                        concept.getTopicName(),
                        concept.getTitle(),
                        concept.getShortExplanation(),
                        concept.getKeyPoints(),
                        concept.getExample(),
                        resources
                ));
            }
            return concepts;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while fetching all concepts", e);
        }
    }

    @Override
    public List<Concept> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        String pattern = "%" + query.trim().toLowerCase() + "%";
        String sql = SELECT_BASE + """
                WHERE LOWER(c.title) LIKE ?
                   OR LOWER(c.short_explanation) LIKE ?
                   OR LOWER(c.key_points) LIKE ?
                   OR LOWER(t.name) LIKE ?
                ORDER BY c.display_order ASC, c.id ASC
                """;
        List<Concept> concepts = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Concept concept = mapRowToConcept(rs);
                    List<LearningResource> resources = resourceRepository.findByConceptId(concept.getId());
                    concepts.add(Concept.create(
                            concept.getId(),
                            concept.getTopicId(),
                            concept.getTopicName(),
                            concept.getTitle(),
                            concept.getShortExplanation(),
                            concept.getKeyPoints(),
                            concept.getExample(),
                            resources
                    ));
                }
            }
            return concepts;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while searching concepts with query: " + query, e);
        }
    }

    @Override
    public Concept save(Concept concept) {
        Objects.requireNonNull(concept, "concept must not be null");
        Concept savedConcept;
        if (concept.getId() > 0 && existsById(concept.getId())) {
            savedConcept = update(concept);
        } else if (concept.getId() > 0) {
            savedConcept = insertWithId(concept);
        } else {
            savedConcept = insert(concept);
        }

        // Synchronize attached resources
        if (concept.getResources() != null) {
            resourceRepository.deleteByConceptId(savedConcept.getId());
            int order = 1;
            for (LearningResource res : concept.getResources()) {
                resourceRepository.saveForConcept(savedConcept.getId(), res, order++);
            }
        }
        return savedConcept;
    }

    private Concept insert(Concept concept) {
        String sql = """
                INSERT INTO concepts (topic_id, title, slug, short_explanation, key_points, example, display_order)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, concept.getTopicId());
            ps.setString(2, concept.getTitle());
            ps.setString(3, toSlug(concept.getTitle()));
            ps.setString(4, concept.getShortExplanation());
            ps.setString(5, serializeKeyPoints(concept.getKeyPoints()));
            ps.setString(6, concept.getExample());
            ps.setInt(7, 0);

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long generatedId = keys.getLong(1);
                    return concept.withId(generatedId);
                } else {
                    throw new ByteForceException("Failed to retrieve generated ID for concept.");
                }
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while inserting concept: " + concept.getTitle(), e);
        }
    }

    private Concept insertWithId(Concept concept) {
        String sql = """
                INSERT INTO concepts (id, topic_id, title, slug, short_explanation, key_points, example, display_order)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, concept.getId());
            ps.setLong(2, concept.getTopicId());
            ps.setString(3, concept.getTitle());
            ps.setString(4, toSlug(concept.getTitle()));
            ps.setString(5, concept.getShortExplanation());
            ps.setString(6, serializeKeyPoints(concept.getKeyPoints()));
            ps.setString(7, concept.getExample());
            ps.setInt(8, 0);

            ps.executeUpdate();
            return concept;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while inserting concept with ID: " + concept.getId(), e);
        }
    }

    private Concept update(Concept concept) {
        String sql = """
                UPDATE concepts
                SET topic_id = ?, title = ?, slug = ?, short_explanation = ?, key_points = ?, example = ?
                WHERE id = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, concept.getTopicId());
            ps.setString(2, concept.getTitle());
            ps.setString(3, toSlug(concept.getTitle()));
            ps.setString(4, concept.getShortExplanation());
            ps.setString(5, serializeKeyPoints(concept.getKeyPoints()));
            ps.setString(6, concept.getExample());
            ps.setLong(7, concept.getId());

            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new ResourceNotFoundException("Cannot update non-existent concept with ID: " + concept.getId());
            }
            return concept;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while updating concept with ID: " + concept.getId(), e);
        }
    }

    @Override
    public boolean existsById(long id) {
        if (id <= 0) {
            return false;
        }
        String sql = "SELECT 1 FROM concepts WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while checking concept existence: " + id, e);
        }
    }

    @Override
    public boolean deleteById(long id) {
        if (id <= 0) {
            return false;
        }
        String sql = "DELETE FROM concepts WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while deleting concept with ID: " + id, e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM concepts";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while counting concepts", e);
        }
    }

    private Concept mapRowToConcept(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        long topicId = rs.getLong("topic_id");
        String topicName = rs.getString("topic_name");
        String title = rs.getString("title");
        String shortExplanation = rs.getString("short_explanation");
        String keyPointsRaw = rs.getString("key_points");
        String example = rs.getString("example");

        return Concept.create(
                id,
                topicId,
                topicName != null ? topicName : "",
                title,
                shortExplanation != null ? shortExplanation : "",
                parseKeyPoints(keyPointsRaw),
                example != null ? example : "",
                List.of()
        );
    }

    private List<String> parseKeyPoints(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split("\\r?\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String serializeKeyPoints(List<String> keyPoints) {
        if (keyPoints == null || keyPoints.isEmpty()) {
            return "";
        }
        return String.join("\n", keyPoints);
    }

    private String toSlug(String title) {
        if (title == null) return "concept";
        return title.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}
