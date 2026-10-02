package com.byteforce.repository;

import com.byteforce.domain.AssessmentAttempt;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Production-ready JDBC implementation of {@link AssessmentAttemptRepository}.
 */
public class JdbcAssessmentAttemptRepository implements AssessmentAttemptRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcAssessmentAttemptRepository.class);

    private static final String SELECT_BASE = """
            SELECT id, assessment_id, user_id, score, status, started_at, completed_at
            FROM assessment_attempts
            """;

    private final DataSource dataSource;

    public JdbcAssessmentAttemptRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public AssessmentAttempt save(AssessmentAttempt attempt) {
        Objects.requireNonNull(attempt, "attempt must not be null");
        if (attempt.getId() <= 0) {
            return insert(attempt);
        } else {
            return update(attempt);
        }
    }

    private AssessmentAttempt insert(AssessmentAttempt attempt) {
        String sql = """
                INSERT INTO assessment_attempts (assessment_id, user_id, score, status, started_at, completed_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, attempt.getAssessmentId());
            ps.setString(2, attempt.getUserId().toString());
            ps.setInt(3, attempt.getScore());
            ps.setString(4, attempt.getStatus());
            ps.setTimestamp(5, Timestamp.from(attempt.getStartedAt()));
            if (attempt.getCompletedAt() != null) {
                ps.setTimestamp(6, Timestamp.from(attempt.getCompletedAt()));
            } else {
                ps.setNull(6, Types.TIMESTAMP);
            }

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return attempt.withId(id);
                } else {
                    throw new ByteForceException("Failed to retrieve generated ID for assessment attempt.");
                }
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while inserting assessment attempt", e);
        }
    }

    private AssessmentAttempt update(AssessmentAttempt attempt) {
        String sql = """
                UPDATE assessment_attempts
                SET score = ?, status = ?, completed_at = ?
                WHERE id = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attempt.getScore());
            ps.setString(2, attempt.getStatus());
            if (attempt.getCompletedAt() != null) {
                ps.setTimestamp(3, Timestamp.from(attempt.getCompletedAt()));
            } else {
                ps.setNull(3, Types.TIMESTAMP);
            }
            ps.setLong(4, attempt.getId());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new ByteForceException("Assessment attempt with ID " + attempt.getId() + " not found for update.");
            }
            return attempt;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while updating assessment attempt ID: " + attempt.getId(), e);
        }
    }

    @Override
    public Optional<AssessmentAttempt> findById(long id) {
        if (id <= 0) {
            return Optional.empty();
        }
        String sql = SELECT_BASE + " WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToAttempt(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding assessment attempt by ID: " + id, e);
        }
    }

    @Override
    public List<AssessmentAttempt> findByUserId(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        String sql = SELECT_BASE + " WHERE user_id = ? ORDER BY started_at DESC";
        List<AssessmentAttempt> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToAttempt(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while querying attempts for user: " + userId, e);
        }
    }

    @Override
    public List<AssessmentAttempt> findByAssessmentId(long assessmentId) {
        if (assessmentId <= 0) {
            return List.of();
        }
        String sql = SELECT_BASE + " WHERE assessment_id = ? ORDER BY started_at DESC";
        List<AssessmentAttempt> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, assessmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToAttempt(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while querying attempts for assessment ID: " + assessmentId, e);
        }
    }

    @Override
    public boolean updateScoreAndStatus(long attemptId, int score, String status, Instant completedAt) {
        if (attemptId <= 0) {
            return false;
        }
        String sql = "UPDATE assessment_attempts SET score = ?, status = ?, completed_at = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, score);
            ps.setString(2, status != null ? status : "COMPLETED");
            if (completedAt != null) {
                ps.setTimestamp(3, Timestamp.from(completedAt));
            } else {
                ps.setNull(3, Types.TIMESTAMP);
            }
            ps.setLong(4, attemptId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while updating status for attempt ID: " + attemptId, e);
        }
    }

    private AssessmentAttempt mapRowToAttempt(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        long assessmentId = rs.getLong("assessment_id");
        UUID userId = UUID.fromString(rs.getString("user_id"));
        int score = rs.getInt("score");
        String status = rs.getString("status");
        Timestamp startedAtTs = rs.getTimestamp("started_at");
        Instant startedAt = startedAtTs != null ? startedAtTs.toInstant() : Instant.now();
        Timestamp completedAtTs = rs.getTimestamp("completed_at");
        Instant completedAt = completedAtTs != null ? completedAtTs.toInstant() : null;

        return new AssessmentAttempt(id, assessmentId, userId, score, status, startedAt, completedAt);
    }
}
