package com.byteforce.repository;

import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;
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
 * Production-ready JDBC implementation of {@link QuestionRepository}.
 */
public class JdbcQuestionRepository implements QuestionRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcQuestionRepository.class);

    private static final String SELECT_BASE = """
            SELECT id, topic_id, title, slug, description, difficulty, solution, created_at, updated_at
            FROM questions
            """;

    private final DataSource dataSource;

    public JdbcQuestionRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public Optional<Question> findById(long id) {
        if (id <= 0) {
            return Optional.empty();
        }
        String sql = SELECT_BASE + " WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToQuestion(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding question by ID: " + id, e);
        }
    }

    @Override
    public Optional<Question> findBySlug(String slug) {
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
                    return Optional.of(mapRowToQuestion(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding question by slug: " + normalized, e);
        }
    }

    @Override
    public List<Question> findAll() {
        String sql = SELECT_BASE + " ORDER BY id ASC";
        return queryList(sql, ps -> {});
    }

    @Override
    public List<Question> findByTopicId(long topicId) {
        if (topicId <= 0) {
            return List.of();
        }
        String sql = SELECT_BASE + " WHERE topic_id = ? ORDER BY id ASC";
        return queryList(sql, ps -> ps.setLong(1, topicId));
    }

    @Override
    public List<Question> findByDifficulty(Difficulty difficulty) {
        if (difficulty == null) {
            return List.of();
        }
        String sql = SELECT_BASE + " WHERE difficulty = ? ORDER BY id ASC";
        return queryList(sql, ps -> ps.setString(1, difficulty.name()));
    }

    @Override
    public List<Question> findByTopicIdAndDifficulty(long topicId, Difficulty difficulty) {
        if (topicId <= 0 || difficulty == null) {
            return List.of();
        }
        String sql = SELECT_BASE + " WHERE topic_id = ? AND difficulty = ? ORDER BY id ASC";
        return queryList(sql, ps -> {
            ps.setLong(1, topicId);
            ps.setString(2, difficulty.name());
        });
    }

    @Override
    public List<Question> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        String sql = SELECT_BASE + " WHERE LOWER(title) LIKE ? OR LOWER(description) LIKE ? ORDER BY id ASC";
        return queryList(sql, ps -> {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
        });
    }

    @Override
    public Question save(Question question) {
        Objects.requireNonNull(question, "question must not be null");
        if (question.getId() <= 0) {
            return insert(question);
        } else {
            return update(question);
        }
    }

    private Question insert(Question question) {
        String sql = """
                INSERT INTO questions (topic_id, title, slug, description, difficulty, solution, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, question.getTopicId());
            ps.setString(2, question.getTitle());
            ps.setString(3, question.getSlug());
            ps.setString(4, question.getDescription());
            ps.setString(5, question.getDifficulty().name());
            ps.setString(6, question.getSolution());
            ps.setTimestamp(7, Timestamp.from(question.getCreatedAt()));
            ps.setTimestamp(8, Timestamp.from(question.getUpdatedAt()));

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long generatedId = keys.getLong(1);
                    return question.withId(generatedId);
                } else {
                    throw new ByteForceException("Failed to retrieve generated ID for inserted question.");
                }
            }
        } catch (SQLException e) {
            if (isDuplicateKeyViolation(e)) {
                throw new ByteForceException("A question with slug '" + question.getSlug() + "' already exists.", e);
            }
            throw new ByteForceException("Database error while inserting question: " + question.getTitle(), e);
        }
    }

    private Question update(Question question) {
        String sql = """
                UPDATE questions
                SET topic_id = ?, title = ?, slug = ?, description = ?, difficulty = ?, solution = ?, updated_at = ?
                WHERE id = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, question.getTopicId());
            ps.setString(2, question.getTitle());
            ps.setString(3, question.getSlug());
            ps.setString(4, question.getDescription());
            ps.setString(5, question.getDifficulty().name());
            ps.setString(6, question.getSolution());
            Instant now = Instant.now();
            ps.setTimestamp(7, Timestamp.from(now));
            ps.setLong(8, question.getId());

            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new ResourceNotFoundException("Cannot update non-existent question with ID: " + question.getId());
            }
            return new Question(question.getId(), question.getTopicId(), question.getTitle(),
                    question.getSlug(), question.getDescription(), question.getDifficulty(),
                    question.getSolution(), question.getCreatedAt(), now);
        } catch (SQLException e) {
            if (isDuplicateKeyViolation(e)) {
                throw new ByteForceException("A question with slug '" + question.getSlug() + "' already exists.", e);
            }
            throw new ByteForceException("Database error while updating question with ID: " + question.getId(), e);
        }
    }

    @Override
    public boolean deleteById(long id) {
        if (id <= 0) {
            return false;
        }
        String sql = "DELETE FROM questions WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while deleting question with ID: " + id, e);
        }
    }

    @Override
    public boolean existsById(long id) {
        if (id <= 0) {
            return false;
        }
        String sql = "SELECT 1 FROM questions WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while checking question existence by ID: " + id, e);
        }
    }

    @Override
    public boolean existsBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return false;
        }
        String sql = "SELECT 1 FROM questions WHERE slug = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, slug.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while checking question existence by slug: " + slug, e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM questions";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while counting questions", e);
        }
    }

    @Override
    public long countByTopicId(long topicId) {
        if (topicId <= 0) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM questions WHERE topic_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while counting questions for topic: " + topicId, e);
        }
    }

    @Override
    public long countByDifficulty(Difficulty difficulty) {
        if (difficulty == null) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM questions WHERE difficulty = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, difficulty.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while counting questions for difficulty: " + difficulty, e);
        }
    }

    @FunctionalInterface
    private interface StatementSetter {
        void setValues(PreparedStatement ps) throws SQLException;
    }

    private List<Question> queryList(String sql, StatementSetter setter) {
        List<Question> questions = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setter.setValues(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapRowToQuestion(rs));
                }
            }
            return questions;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while querying questions", e);
        }
    }

    private Question mapRowToQuestion(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        long topicId = rs.getLong("topic_id");
        String title = rs.getString("title");
        String slug = rs.getString("slug");
        String description = rs.getString("description");
        String difficultyStr = rs.getString("difficulty");
        Difficulty difficulty = Difficulty.MEDIUM;
        if (difficultyStr != null) {
            try {
                difficulty = Difficulty.valueOf(difficultyStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown difficulty '{}' for question ID {}, defaulting to MEDIUM", difficultyStr, id);
            }
        }
        String solution = rs.getString("solution");
        Timestamp createdTs = rs.getTimestamp("created_at");
        Instant createdAt = createdTs != null ? createdTs.toInstant() : Instant.now();
        Timestamp updatedTs = rs.getTimestamp("updated_at");
        Instant updatedAt = updatedTs != null ? updatedTs.toInstant() : Instant.now();

        return new Question(id, topicId, title, slug, description, difficulty, solution, createdAt, updatedAt);
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
