package com.byteforce.repository;

import com.byteforce.domain.ContinueLearningView;
import com.byteforce.domain.RecentlyViewedConcept;
import com.byteforce.domain.UserConceptProgress;
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
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Production-ready JDBC implementation of {@link UserConceptProgressRepository}.
 */
public class JdbcUserConceptProgressRepository implements UserConceptProgressRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcUserConceptProgressRepository.class);

    private final DataSource dataSource;

    public JdbcUserConceptProgressRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public Optional<UserConceptProgress> findByUserAndConcept(UUID userId, long conceptId) {
        Objects.requireNonNull(userId, "userId must not be null");
        if (conceptId <= 0) return Optional.empty();

        String sql = """
                SELECT id, user_id, concept_id, completed, completed_at, last_viewed_at
                FROM user_concept_progress
                WHERE user_id = ? AND concept_id = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setLong(2, conceptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Error querying concept progress for user: " + userId + ", concept: " + conceptId, e);
        }
    }

    @Override
    public UserConceptProgress save(UserConceptProgress progress) {
        Objects.requireNonNull(progress, "progress must not be null");
        Optional<UserConceptProgress> existing = findByUserAndConcept(progress.getUserId(), progress.getConceptId());
        if (existing.isPresent()) {
            String sql = """
                    UPDATE user_concept_progress
                    SET completed = ?, completed_at = ?, last_viewed_at = ?
                    WHERE id = ?
                    """;
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBoolean(1, progress.isCompleted());
                if (progress.getCompletedAt() != null) {
                    ps.setTimestamp(2, Timestamp.from(progress.getCompletedAt()));
                } else {
                    ps.setNull(2, Types.TIMESTAMP);
                }
                ps.setTimestamp(3, Timestamp.from(progress.getLastViewedAt()));
                ps.setLong(4, existing.get().getId());
                ps.executeUpdate();
                return progress.withId(existing.get().getId());
            } catch (SQLException e) {
                throw new ByteForceException("Error updating user concept progress", e);
            }
        } else {
            String sql = """
                    INSERT INTO user_concept_progress (user_id, concept_id, completed, completed_at, last_viewed_at)
                    VALUES (?, ?, ?, ?, ?)
                    """;
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, progress.getUserId().toString());
                ps.setLong(2, progress.getConceptId());
                ps.setBoolean(3, progress.isCompleted());
                if (progress.getCompletedAt() != null) {
                    ps.setTimestamp(4, Timestamp.from(progress.getCompletedAt()));
                } else {
                    ps.setNull(4, Types.TIMESTAMP);
                }
                ps.setTimestamp(5, Timestamp.from(progress.getLastViewedAt()));
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return progress.withId(keys.getLong(1));
                    }
                    return progress;
                }
            } catch (SQLException e) {
                throw new ByteForceException("Error inserting user concept progress", e);
            }
        }
    }

    @Override
    public void recordView(UUID userId, long conceptId) {
        Objects.requireNonNull(userId, "userId must not be null");
        if (conceptId <= 0) return;

        Instant now = Instant.now();
        Optional<UserConceptProgress> existing = findByUserAndConcept(userId, conceptId);
        if (existing.isPresent()) {
            String sql = "UPDATE user_concept_progress SET last_viewed_at = ? WHERE user_id = ? AND concept_id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setTimestamp(1, Timestamp.from(now));
                ps.setString(2, userId.toString());
                ps.setLong(3, conceptId);
                ps.executeUpdate();
            } catch (SQLException e) {
                log.warn("Error updating last_viewed_at for user {} concept {}: {}", userId, conceptId, e.getMessage());
            }
        } else {
            String sql = """
                    INSERT INTO user_concept_progress (user_id, concept_id, completed, completed_at, last_viewed_at)
                    VALUES (?, ?, FALSE, NULL, ?)
                    """;
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, userId.toString());
                ps.setLong(2, conceptId);
                ps.setTimestamp(3, Timestamp.from(now));
                ps.executeUpdate();
            } catch (SQLException e) {
                log.warn("Error recording concept view for user {} concept {}: {}", userId, conceptId, e.getMessage());
            }
        }
    }

    @Override
    public boolean setCompleted(UUID userId, long conceptId, boolean completed) {
        Objects.requireNonNull(userId, "userId must not be null");
        if (conceptId <= 0) return false;

        Instant now = Instant.now();
        Optional<UserConceptProgress> existing = findByUserAndConcept(userId, conceptId);
        if (existing.isPresent()) {
            String sql = "UPDATE user_concept_progress SET completed = ?, completed_at = ?, last_viewed_at = ? WHERE user_id = ? AND concept_id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBoolean(1, completed);
                if (completed) {
                    ps.setTimestamp(2, Timestamp.from(now));
                } else {
                    ps.setNull(2, Types.TIMESTAMP);
                }
                ps.setTimestamp(3, Timestamp.from(now));
                ps.setString(4, userId.toString());
                ps.setLong(5, conceptId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new ByteForceException("Error updating concept completion for user: " + userId, e);
            }
        } else {
            String sql = """
                    INSERT INTO user_concept_progress (user_id, concept_id, completed, completed_at, last_viewed_at)
                    VALUES (?, ?, ?, ?, ?)
                    """;
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, userId.toString());
                ps.setLong(2, conceptId);
                ps.setBoolean(3, completed);
                if (completed) {
                    ps.setTimestamp(4, Timestamp.from(now));
                } else {
                    ps.setNull(4, Types.TIMESTAMP);
                }
                ps.setTimestamp(5, Timestamp.from(now));
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new ByteForceException("Error setting concept completion for user: " + userId, e);
            }
        }
    }

    @Override
    public List<RecentlyViewedConcept> findRecentlyViewed(UUID userId, int limit) {
        Objects.requireNonNull(userId, "userId must not be null");
        int max = limit > 0 ? limit : 10;

        String sql = """
                SELECT ucp.concept_id, c.title AS concept_title, c.topic_id, t.name AS topic_name,
                       t.subject_id, COALESCE(s.name, '') AS subject_name,
                       ucp.completed, ucp.last_viewed_at
                FROM user_concept_progress ucp
                JOIN concepts c ON ucp.concept_id = c.id
                JOIN topics t ON c.topic_id = t.id
                LEFT JOIN subjects s ON t.subject_id = s.id
                WHERE ucp.user_id = ?
                ORDER BY ucp.last_viewed_at DESC
                LIMIT ?
                """;

        List<RecentlyViewedConcept> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setInt(2, max);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long conceptId = rs.getLong("concept_id");
                    String title = rs.getString("concept_title");
                    long topicId = rs.getLong("topic_id");
                    String topicName = rs.getString("topic_name");
                    String subjectId = rs.getString("subject_id");
                    String subjectName = rs.getString("subject_name");
                    boolean completed = rs.getBoolean("completed");
                    Timestamp lvTs = rs.getTimestamp("last_viewed_at");
                    Instant lastViewed = lvTs != null ? lvTs.toInstant() : Instant.now();

                    list.add(new RecentlyViewedConcept(
                            conceptId, title, topicId, topicName, subjectId, subjectName, completed, lastViewed
                    ));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new ByteForceException("Error finding recently viewed concepts for user: " + userId, e);
        }
    }

    @Override
    public Optional<ContinueLearningView> findContinueLearning(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        // First attempt: most recently viewed unfinished concept
        String sql = """
                SELECT ucp.concept_id, c.title AS concept_title, c.topic_id, t.name AS topic_name,
                       t.subject_id, COALESCE(s.name, '') AS subject_name
                FROM user_concept_progress ucp
                JOIN concepts c ON ucp.concept_id = c.id
                JOIN topics t ON c.topic_id = t.id
                LEFT JOIN subjects s ON t.subject_id = s.id
                WHERE ucp.user_id = ? AND ucp.completed = FALSE
                ORDER BY ucp.last_viewed_at DESC
                LIMIT 1
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long conceptId = rs.getLong("concept_id");
                    String conceptTitle = rs.getString("concept_title");
                    long topicId = rs.getLong("topic_id");
                    String topicName = rs.getString("topic_name");
                    String subjectId = rs.getString("subject_id");
                    String subjectName = rs.getString("subject_name");

                    // Compute position within topic
                    int[] pos = calculateConceptPositionInTopic(conn, topicId, conceptId);
                    return Optional.of(new ContinueLearningView(
                            conceptId, conceptTitle, topicId, topicName, subjectId, subjectName, pos[0], pos[1]
                    ));
                }
            }
        } catch (SQLException e) {
            throw new ByteForceException("Error finding continue learning for user: " + userId, e);
        }

        // Second attempt: if all viewed are completed, find next uncompleted concept in the topic of the most recently viewed concept
        String sqlNext = """
                SELECT c.id AS concept_id, c.title AS concept_title, c.topic_id, t.name AS topic_name,
                       t.subject_id, COALESCE(s.name, '') AS subject_name
                FROM concepts c
                JOIN topics t ON c.topic_id = t.id
                LEFT JOIN subjects s ON t.subject_id = s.id
                WHERE c.topic_id = (
                    SELECT c2.topic_id FROM user_concept_progress ucp2
                    JOIN concepts c2 ON ucp2.concept_id = c2.id
                    WHERE ucp2.user_id = ?
                    ORDER BY ucp2.last_viewed_at DESC
                    LIMIT 1
                )
                AND c.id NOT IN (
                    SELECT concept_id FROM user_concept_progress
                    WHERE user_id = ? AND completed = TRUE
                )
                ORDER BY c.display_order ASC, c.id ASC
                LIMIT 1
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlNext)) {
            ps.setString(1, userId.toString());
            ps.setString(2, userId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long conceptId = rs.getLong("concept_id");
                    String conceptTitle = rs.getString("concept_title");
                    long topicId = rs.getLong("topic_id");
                    String topicName = rs.getString("topic_name");
                    String subjectId = rs.getString("subject_id");
                    String subjectName = rs.getString("subject_name");

                    int[] pos = calculateConceptPositionInTopic(conn, topicId, conceptId);
                    return Optional.of(new ContinueLearningView(
                            conceptId, conceptTitle, topicId, topicName, subjectId, subjectName, pos[0], pos[1]
                    ));
                }
            }
        } catch (SQLException e) {
            log.warn("Error resolving next concept for continue learning: {}", e.getMessage());
        }

        return Optional.empty();
    }

    private int[] calculateConceptPositionInTopic(Connection conn, long topicId, long targetConceptId) throws SQLException {
        String sql = "SELECT id FROM concepts WHERE topic_id = ? ORDER BY display_order ASC, id ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                int index = 1;
                int foundIndex = 1;
                while (rs.next()) {
                    long id = rs.getLong("id");
                    if (id == targetConceptId) {
                        foundIndex = index;
                    }
                    index++;
                }
                int total = index - 1;
                return new int[]{foundIndex, Math.max(total, 1)};
            }
        }
    }

    @Override
    public long countCompletedByTopic(UUID userId, long topicId) {
        Objects.requireNonNull(userId, "userId must not be null");
        if (topicId <= 0) return 0;

        String sql = """
                SELECT COUNT(DISTINCT ucp.concept_id)
                FROM user_concept_progress ucp
                JOIN concepts c ON ucp.concept_id = c.id
                WHERE ucp.user_id = ? AND c.topic_id = ? AND ucp.completed = TRUE
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setLong(2, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new ByteForceException("Error counting completed concepts for topic: " + topicId, e);
        }
    }

    @Override
    public long countCompletedBySubject(UUID userId, String subjectId) {
        Objects.requireNonNull(userId, "userId must not be null");
        if (subjectId == null || subjectId.isBlank()) return 0;

        String sql = """
                SELECT COUNT(DISTINCT ucp.concept_id)
                FROM user_concept_progress ucp
                JOIN concepts c ON ucp.concept_id = c.id
                JOIN topics t ON c.topic_id = t.id
                WHERE ucp.user_id = ? AND t.subject_id = ? AND ucp.completed = TRUE
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setString(2, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new ByteForceException("Error counting completed concepts for subject: " + subjectId, e);
        }
    }

    @Override
    public Set<Long> findCompletedConceptIds(UUID userId, Collection<Long> conceptIds) {
        Objects.requireNonNull(userId, "userId must not be null");
        if (conceptIds == null || conceptIds.isEmpty()) return Collections.emptySet();

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT concept_id FROM user_concept_progress WHERE user_id = ? AND completed = TRUE AND concept_id IN (");
        int count = 0;
        for (Long id : conceptIds) {
            if (count > 0) sb.append(",");
            sb.append(id);
            count++;
        }
        sb.append(")");

        Set<Long> set = new HashSet<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sb.toString())) {
            ps.setString(1, userId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    set.add(rs.getLong("concept_id"));
                }
            }
            return set;
        } catch (SQLException e) {
            throw new ByteForceException("Error finding completed concept ids", e);
        }
    }

    @Override
    public Map<Long, Boolean> getCompletionMapForTopic(UUID userId, long topicId) {
        Objects.requireNonNull(userId, "userId must not be null");
        if (topicId <= 0) return Collections.emptyMap();

        String sql = """
                SELECT ucp.concept_id, ucp.completed
                FROM user_concept_progress ucp
                JOIN concepts c ON ucp.concept_id = c.id
                WHERE ucp.user_id = ? AND c.topic_id = ?
                """;
        Map<Long, Boolean> map = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setLong(2, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getLong("concept_id"), rs.getBoolean("completed"));
                }
            }
            return map;
        } catch (SQLException e) {
            throw new ByteForceException("Error getting completion map for topic: " + topicId, e);
        }
    }

    private UserConceptProgress mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        UUID userId = UUID.fromString(rs.getString("user_id"));
        long conceptId = rs.getLong("concept_id");
        boolean completed = rs.getBoolean("completed");
        Timestamp compTs = rs.getTimestamp("completed_at");
        Instant completedAt = compTs != null ? compTs.toInstant() : null;
        Timestamp lvTs = rs.getTimestamp("last_viewed_at");
        Instant lastViewedAt = lvTs != null ? lvTs.toInstant() : Instant.now();

        return new UserConceptProgress(id, userId, conceptId, completed, completedAt, lastViewedAt);
    }
}
