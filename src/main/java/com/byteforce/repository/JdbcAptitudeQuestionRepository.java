package com.byteforce.repository;

import com.byteforce.domain.AptitudeCategory;
import com.byteforce.domain.AptitudeQuestion;
import com.byteforce.domain.Difficulty;
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
 * Production JDBC implementation of {@link AptitudeQuestionRepository}.
 */
public class JdbcAptitudeQuestionRepository implements AptitudeQuestionRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcAptitudeQuestionRepository.class);

    private static final String SELECT_BASE = """
            SELECT id, category, topic, difficulty, question, option_a, option_b, option_c, option_d,
                   correct_answer, explanation, active, created_at, updated_at
            FROM aptitude_questions
            """;

    private final DataSource dataSource;

    public JdbcAptitudeQuestionRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public Optional<AptitudeQuestion> findById(long id) {
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
            throw new ByteForceException("Database error while finding aptitude question by ID: " + id, e);
        }
    }

    @Override
    public List<AptitudeQuestion> findAll() {
        String sql = SELECT_BASE + " ORDER BY id DESC";
        List<AptitudeQuestion> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToQuestion(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while querying all aptitude questions", e);
        }
    }

    @Override
    public List<AptitudeQuestion> findByCategory(AptitudeCategory category) {
        if (category == null) {
            return findAll();
        }
        String sql = SELECT_BASE + " WHERE category = ? ORDER BY id DESC";
        List<AptitudeQuestion> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToQuestion(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while querying aptitude questions by category: " + category, e);
        }
    }

    @Override
    public List<AptitudeQuestion> findByDifficulty(Difficulty difficulty) {
        if (difficulty == null) {
            return findAll();
        }
        String sql = SELECT_BASE + " WHERE difficulty = ? ORDER BY id DESC";
        List<AptitudeQuestion> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, difficulty.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToQuestion(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while querying aptitude questions by difficulty: " + difficulty, e);
        }
    }

    @Override
    public List<AptitudeQuestion> findByCategoryAndDifficulty(AptitudeCategory category, Difficulty difficulty) {
        if (category == null && difficulty == null) return findAll();
        if (category == null) return findByDifficulty(difficulty);
        if (difficulty == null) return findByCategory(category);

        String sql = SELECT_BASE + " WHERE category = ? AND difficulty = ? ORDER BY id DESC";
        List<AptitudeQuestion> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category.name());
            ps.setString(2, difficulty.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToQuestion(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while querying aptitude questions by category and difficulty", e);
        }
    }

    @Override
    public AptitudeQuestion save(AptitudeQuestion question) {
        Objects.requireNonNull(question, "question must not be null");
        if (question.getId() > 0 && existsById(question.getId())) {
            return update(question);
        } else {
            return insert(question);
        }
    }

    private AptitudeQuestion insert(AptitudeQuestion q) {
        String sql = """
                INSERT INTO aptitude_questions (category, topic, difficulty, question, option_a, option_b, option_c, option_d,
                                                correct_answer, explanation, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, q.getCategory().name());
            ps.setString(2, q.getTopic());
            ps.setString(3, q.getDifficulty().name());
            ps.setString(4, q.getQuestion());
            ps.setString(5, q.getOptionA());
            ps.setString(6, q.getOptionB());
            ps.setString(7, q.getOptionC());
            ps.setString(8, q.getOptionD());
            ps.setString(9, q.getCorrectAnswer());
            ps.setString(10, q.getExplanation());
            ps.setBoolean(11, q.isActive());
            ps.setTimestamp(12, Timestamp.from(q.getCreatedAt()));
            ps.setTimestamp(13, Timestamp.from(q.getUpdatedAt()));

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return q.withId(keys.getLong(1));
                }
                return q;
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while inserting aptitude question", e);
        }
    }

    private AptitudeQuestion update(AptitudeQuestion q) {
        String sql = """
                UPDATE aptitude_questions
                SET category = ?, topic = ?, difficulty = ?, question = ?, option_a = ?, option_b = ?, option_c = ?,
                    option_d = ?, correct_answer = ?, explanation = ?, active = ?, updated_at = ?
                WHERE id = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q.getCategory().name());
            ps.setString(2, q.getTopic());
            ps.setString(3, q.getDifficulty().name());
            ps.setString(4, q.getQuestion());
            ps.setString(5, q.getOptionA());
            ps.setString(6, q.getOptionB());
            ps.setString(7, q.getOptionC());
            ps.setString(8, q.getOptionD());
            ps.setString(9, q.getCorrectAnswer());
            ps.setString(10, q.getExplanation());
            ps.setBoolean(11, q.isActive());
            ps.setTimestamp(12, Timestamp.from(Instant.now()));
            ps.setLong(13, q.getId());

            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new ResourceNotFoundException("Cannot update non-existent aptitude question with ID: " + q.getId());
            }
            return q;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while updating aptitude question with ID: " + q.getId(), e);
        }
    }

    @Override
    public boolean deleteById(long id) {
        if (id <= 0) {
            return false;
        }
        String sql = "DELETE FROM aptitude_questions WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while deleting aptitude question with ID: " + id, e);
        }
    }

    @Override
    public boolean existsById(long id) {
        if (id <= 0) {
            return false;
        }
        String sql = "SELECT 1 FROM aptitude_questions WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while checking aptitude question existence: " + id, e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM aptitude_questions";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while counting aptitude questions", e);
        }
    }

    @Override
    public long countByCategory(AptitudeCategory category) {
        if (category == null) {
            return count();
        }
        String sql = "SELECT COUNT(*) FROM aptitude_questions WHERE category = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while counting aptitude questions for category: " + category, e);
        }
    }

    private AptitudeQuestion mapRowToQuestion(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String categoryStr = rs.getString("category");
        String topic = rs.getString("topic");
        String difficultyStr = rs.getString("difficulty");
        String question = rs.getString("question");
        String optionA = rs.getString("option_a");
        String optionB = rs.getString("option_b");
        String optionC = rs.getString("option_c");
        String optionD = rs.getString("option_d");
        String correctAnswer = rs.getString("correct_answer");
        String explanation = rs.getString("explanation");
        boolean active = rs.getBoolean("active");
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        Timestamp updatedAtTs = rs.getTimestamp("updated_at");

        AptitudeCategory category;
        try {
            category = AptitudeCategory.valueOf(categoryStr);
        } catch (Exception e) {
            category = AptitudeCategory.QUANTITATIVE;
        }

        Difficulty difficulty;
        try {
            difficulty = Difficulty.valueOf(difficultyStr);
        } catch (Exception e) {
            difficulty = Difficulty.MEDIUM;
        }

        Instant createdAt = createdAtTs != null ? createdAtTs.toInstant() : Instant.now();
        Instant updatedAt = updatedAtTs != null ? updatedAtTs.toInstant() : Instant.now();

        return new AptitudeQuestion(id, category, topic, difficulty, question, optionA, optionB, optionC, optionD, correctAnswer, explanation, active, createdAt, updatedAt);
    }
}
