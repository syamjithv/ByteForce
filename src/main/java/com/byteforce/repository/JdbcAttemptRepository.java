package com.byteforce.repository;

import com.byteforce.domain.AttemptStatus;
import com.byteforce.domain.Difficulty;
import com.byteforce.domain.QuestionAttempt;
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
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Production-ready JDBC implementation of {@link AttemptRepository}.
 */
public class JdbcAttemptRepository implements AttemptRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcAttemptRepository.class);

    private static final String SELECT_BASE = """
            SELECT id, user_id, question_id, status, code_snippet, execution_time_ms, attempted_at
            FROM question_attempts
            """;

    private final DataSource dataSource;

    public JdbcAttemptRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public Optional<QuestionAttempt> findById(long id) {
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
            throw new ByteForceException("Database error while finding attempt by ID: " + id, e);
        }
    }

    @Override
    public QuestionAttempt save(QuestionAttempt attempt) {
        Objects.requireNonNull(attempt, "attempt must not be null");
        if (attempt.getId() <= 0) {
            return insert(attempt);
        } else {
            return update(attempt);
        }
    }

    private QuestionAttempt insert(QuestionAttempt attempt) {
        String sql = """
                INSERT INTO question_attempts (user_id, question_id, status, code_snippet, execution_time_ms, attempted_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, attempt.getUserId().toString());
            ps.setLong(2, attempt.getQuestionId());
            ps.setString(3, attempt.getStatus().name());
            ps.setString(4, attempt.getCodeSnippet());
            if (attempt.getExecutionTimeMs() != null) {
                ps.setInt(5, attempt.getExecutionTimeMs());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.setTimestamp(6, Timestamp.from(attempt.getAttemptedAt()));

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long generatedId = keys.getLong(1);
                    return attempt.withId(generatedId);
                } else {
                    throw new ByteForceException("Failed to retrieve generated ID for inserted attempt.");
                }
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while inserting question attempt", e);
        }
    }

    private QuestionAttempt update(QuestionAttempt attempt) {
        String sql = """
                UPDATE question_attempts
                SET status = ?, code_snippet = ?, execution_time_ms = ?
                WHERE id = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, attempt.getStatus().name());
            ps.setString(2, attempt.getCodeSnippet());
            if (attempt.getExecutionTimeMs() != null) {
                ps.setInt(3, attempt.getExecutionTimeMs());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setLong(4, attempt.getId());

            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new ResourceNotFoundException("Cannot update non-existent attempt with ID: " + attempt.getId());
            }
            return attempt;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while updating question attempt with ID: " + attempt.getId(), e);
        }
    }

    @Override
    public List<QuestionAttempt> findByUserId(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        String sql = SELECT_BASE + " WHERE user_id = ? ORDER BY attempted_at DESC, id DESC";
        return queryList(sql, ps -> ps.setString(1, userId.toString()));
    }

    @Override
    public List<QuestionAttempt> findByQuestionId(long questionId) {
        if (questionId <= 0) {
            return List.of();
        }
        String sql = SELECT_BASE + " WHERE question_id = ? ORDER BY attempted_at DESC, id DESC";
        return queryList(sql, ps -> ps.setLong(1, questionId));
    }

    @Override
    public List<QuestionAttempt> findByUserIdAndQuestionId(UUID userId, long questionId) {
        if (userId == null || questionId <= 0) {
            return List.of();
        }
        String sql = SELECT_BASE + " WHERE user_id = ? AND question_id = ? ORDER BY attempted_at DESC, id DESC";
        return queryList(sql, ps -> {
            ps.setString(1, userId.toString());
            ps.setLong(2, questionId);
        });
    }

    @Override
    public Optional<QuestionAttempt> findLatestByUserIdAndQuestionId(UUID userId, long questionId) {
        if (userId == null || questionId <= 0) {
            return Optional.empty();
        }
        String sql = SELECT_BASE + " WHERE user_id = ? AND question_id = ? ORDER BY attempted_at DESC, id DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setLong(2, questionId);
            ps.setMaxRows(1);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToAttempt(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding latest attempt for user " + userId + " and question " + questionId, e);
        }
    }

    @Override
    public long countByUserId(UUID userId) {
        if (userId == null) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM question_attempts WHERE user_id = ?";
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
            throw new ByteForceException("Database error while counting attempts for user: " + userId, e);
        }
    }

    @Override
    public long countSolvedByUserId(UUID userId) {
        if (userId == null) {
            return 0;
        }
        String sql = "SELECT COUNT(DISTINCT question_id) FROM question_attempts WHERE user_id = ? AND status = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setString(2, AttemptStatus.SOLVED.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while counting solved questions for user: " + userId, e);
        }
    }

    @Override
    public Map<Difficulty, Long> countSolvedByDifficulty(UUID userId) {
        if (userId == null) {
            return Map.of();
        }
        String sql = """
                SELECT q.difficulty, COUNT(DISTINCT a.question_id)
                FROM question_attempts a
                JOIN questions q ON a.question_id = q.id
                WHERE a.user_id = ? AND a.status = ?
                GROUP BY q.difficulty
                """;
        Map<Difficulty, Long> map = new EnumMap<>(Difficulty.class);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setString(2, AttemptStatus.SOLVED.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String diffStr = rs.getString(1);
                    long count = rs.getLong(2);
                    try {
                        map.put(Difficulty.valueOf(diffStr.trim().toUpperCase()), count);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            return Collections.unmodifiableMap(map);
        } catch (SQLException e) {
            throw new ByteForceException("Database error while counting solved by difficulty for user: " + userId, e);
        }
    }

    @Override
    public Map<String, Long> countSolvedByTopic(UUID userId) {
        if (userId == null) {
            return Map.of();
        }
        String sql = """
                SELECT t.name, COUNT(DISTINCT a.question_id)
                FROM question_attempts a
                JOIN questions q ON a.question_id = q.id
                JOIN topics t ON q.topic_id = t.id
                WHERE a.user_id = ? AND a.status = ?
                GROUP BY t.name
                """;
        Map<String, Long> map = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setString(2, AttemptStatus.SOLVED.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString(1), rs.getLong(2));
                }
            }
            return Collections.unmodifiableMap(map);
        } catch (SQLException e) {
            throw new ByteForceException("Database error while counting solved by topic for user: " + userId, e);
        }
    }

    @Override
    public boolean hasSolved(UUID userId, long questionId) {
        if (userId == null || questionId <= 0) {
            return false;
        }
        String sql = "SELECT 1 FROM question_attempts WHERE user_id = ? AND question_id = ? AND status = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setLong(2, questionId);
            ps.setString(3, AttemptStatus.SOLVED.name());
            ps.setMaxRows(1);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while checking solved status for user " + userId + " and question " + questionId, e);
        }
    }

    @Override
    public List<QuestionAttempt> findRecentAttempts(UUID userId, int limit) {
        if (userId == null || limit <= 0) {
            return List.of();
        }
        String sql = SELECT_BASE + " WHERE user_id = ? ORDER BY attempted_at DESC, id DESC";
        List<QuestionAttempt> attempts = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setMaxRows(limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    attempts.add(mapRowToAttempt(rs));
                }
            }
            return attempts;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding recent attempts for user: " + userId, e);
        }
    }

    @Override
    public boolean deleteById(long id) {
        if (id <= 0) {
            return false;
        }
        String sql = "DELETE FROM question_attempts WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while deleting attempt by ID: " + id, e);
        }
    }

    @FunctionalInterface
    private interface StatementSetter {
        void setValues(PreparedStatement ps) throws SQLException;
    }

    private List<QuestionAttempt> queryList(String sql, StatementSetter setter) {
        List<QuestionAttempt> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setter.setValues(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToAttempt(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while querying attempts", e);
        }
    }

    private QuestionAttempt mapRowToAttempt(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        UUID userId = UUID.fromString(rs.getString("user_id"));
        long questionId = rs.getLong("question_id");
        String statusStr = rs.getString("status");
        AttemptStatus status = AttemptStatus.ATTEMPTED;
        if (statusStr != null) {
            try {
                status = AttemptStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown attempt status '{}' for attempt ID {}, defaulting to ATTEMPTED", statusStr, id);
            }
        }
        String codeSnippet = rs.getString("code_snippet");
        int timeMs = rs.getInt("execution_time_ms");
        Integer executionTimeMs = rs.wasNull() ? null : timeMs;
        Timestamp attemptedTs = rs.getTimestamp("attempted_at");
        Instant attemptedAt = attemptedTs != null ? attemptedTs.toInstant() : Instant.now();

        return new QuestionAttempt(id, userId, questionId, status, codeSnippet, executionTimeMs, attemptedAt);
    }
}
