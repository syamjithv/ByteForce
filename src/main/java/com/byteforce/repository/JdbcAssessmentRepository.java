package com.byteforce.repository;

import com.byteforce.domain.Assessment;
import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;
import com.byteforce.domain.QuestionType;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Production-ready JDBC implementation of {@link AssessmentRepository}.
 */
public class JdbcAssessmentRepository implements AssessmentRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcAssessmentRepository.class);

    private static final String SELECT_BASE = """
            SELECT id, title, description, duration_minutes, total_marks, is_active, difficulty, topic_id, created_at
            FROM assessments
            """;

    private final DataSource dataSource;

    public JdbcAssessmentRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public Assessment save(Assessment assessment) {
        Objects.requireNonNull(assessment, "assessment must not be null");
        if (assessment.getId() <= 0) {
            return insert(assessment);
        } else {
            return update(assessment);
        }
    }

    private Assessment insert(Assessment assessment) {
        String sql = """
                INSERT INTO assessments (title, description, duration_minutes, total_marks, is_active, difficulty, topic_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, assessment.getTitle());
            if (assessment.getDescription() != null) {
                ps.setString(2, assessment.getDescription());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            ps.setInt(3, assessment.getDurationMinutes());
            ps.setInt(4, assessment.getTotalMarks());
            ps.setBoolean(5, assessment.isActive());
            ps.setString(6, assessment.getDifficulty().name());
            if (assessment.getTopicId() != null) {
                ps.setLong(7, assessment.getTopicId());
            } else {
                ps.setNull(7, Types.BIGINT);
            }
            ps.setTimestamp(8, Timestamp.from(assessment.getCreatedAt()));

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return assessment.withId(id);
                } else {
                    throw new ByteForceException("Failed to retrieve generated ID for assessment.");
                }
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while inserting assessment", e);
        }
    }

    private Assessment update(Assessment assessment) {
        String sql = """
                UPDATE assessments
                SET title = ?, description = ?, duration_minutes = ?, total_marks = ?, is_active = ?, difficulty = ?, topic_id = ?
                WHERE id = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, assessment.getTitle());
            if (assessment.getDescription() != null) {
                ps.setString(2, assessment.getDescription());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            ps.setInt(3, assessment.getDurationMinutes());
            ps.setInt(4, assessment.getTotalMarks());
            ps.setBoolean(5, assessment.isActive());
            ps.setString(6, assessment.getDifficulty().name());
            if (assessment.getTopicId() != null) {
                ps.setLong(7, assessment.getTopicId());
            } else {
                ps.setNull(7, Types.BIGINT);
            }
            ps.setLong(8, assessment.getId());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new ByteForceException("Assessment with ID " + assessment.getId() + " not found for update.");
            }
            return assessment;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while updating assessment with ID: " + assessment.getId(), e);
        }
    }

    @Override
    public Optional<Assessment> findById(long id) {
        if (id <= 0) {
            return Optional.empty();
        }
        String sql = SELECT_BASE + " WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToAssessment(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding assessment by ID: " + id, e);
        }
    }

    @Override
    public List<Assessment> findAllActive() {
        String sql = SELECT_BASE + " WHERE is_active = TRUE ORDER BY id ASC";
        return queryAssessmentList(sql, ps -> {});
    }

    @Override
    public List<Assessment> findAll() {
        String sql = SELECT_BASE + " ORDER BY id ASC";
        return queryAssessmentList(sql, ps -> {});
    }

    @Override
    public List<Question> findQuestionsByAssessmentId(long assessmentId) {
        if (assessmentId <= 0) {
            return List.of();
        }
        String sql = """
                SELECT q.id, q.topic_id, q.title, q.slug, q.description, q.difficulty, q.question_type, q.solution, q.created_at, q.updated_at
                FROM questions q
                JOIN assessment_questions aq ON q.id = aq.question_id
                WHERE aq.assessment_id = ?
                ORDER BY aq.question_order ASC
                """;
        List<Question> questions = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, assessmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapRowToQuestion(rs));
                }
            }
            return questions;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while querying questions for assessment ID: " + assessmentId, e);
        }
    }

    @Override
    public Map<Long, Integer> getQuestionMarks(long assessmentId) {
        if (assessmentId <= 0) {
            return Map.of();
        }
        String sql = "SELECT question_id, marks FROM assessment_questions WHERE assessment_id = ?";
        Map<Long, Integer> marksMap = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, assessmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    marksMap.put(rs.getLong("question_id"), rs.getInt("marks"));
                }
            }
            return marksMap;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while querying question marks for assessment ID: " + assessmentId, e);
        }
    }

    @Override
    public void addQuestionToAssessment(long assessmentId, long questionId, int marks, int questionOrder) {
        String sql = """
                INSERT INTO assessment_questions (assessment_id, question_id, marks, question_order)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, assessmentId);
            ps.setLong(2, questionId);
            ps.setInt(3, Math.max(1, marks));
            ps.setInt(4, Math.max(1, questionOrder));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ByteForceException("Database error while adding question " + questionId + " to assessment " + assessmentId, e);
        }
    }

    @Override
    public void removeQuestionFromAssessment(long assessmentId, long questionId) {
        if (assessmentId <= 0 || questionId <= 0) {
            return;
        }
        String sql = "DELETE FROM assessment_questions WHERE assessment_id = ? AND question_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, assessmentId);
            ps.setLong(2, questionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ByteForceException("Database error while removing question " + questionId + " from assessment " + assessmentId, e);
        }
    }

    @Override
    public void clearQuestionsFromAssessment(long assessmentId) {
        if (assessmentId <= 0) {
            return;
        }
        String sql = "DELETE FROM assessment_questions WHERE assessment_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, assessmentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ByteForceException("Database error while clearing questions for assessment " + assessmentId, e);
        }
    }

    @Override
    public boolean deleteById(long id) {
        if (id <= 0) {
            return false;
        }
        String sql = "DELETE FROM assessments WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while deleting assessment with ID: " + id, e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM assessments";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while counting assessments", e);
        }
    }

    @FunctionalInterface
    private interface PreparedStatementSetter {
        void setValues(PreparedStatement ps) throws SQLException;
    }

    private List<Assessment> queryAssessmentList(String sql, PreparedStatementSetter setter) {
        List<Assessment> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setter.setValues(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToAssessment(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while querying assessments", e);
        }
    }

    private Assessment mapRowToAssessment(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String title = rs.getString("title");
        String description = rs.getString("description");
        int durationMinutes = rs.getInt("duration_minutes");
        int totalMarks = rs.getInt("total_marks");
        boolean isActive = rs.getBoolean("is_active");
        String difficultyStr = rs.getString("difficulty");
        Difficulty difficulty = Difficulty.MEDIUM;
        if (difficultyStr != null) {
            try {
                difficulty = Difficulty.valueOf(difficultyStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                difficulty = Difficulty.MEDIUM;
            }
        }
        Long topicId = rs.getObject("topic_id") != null ? rs.getLong("topic_id") : null;
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        Instant createdAt = createdAtTs != null ? createdAtTs.toInstant() : Instant.now();

        return new Assessment(id, title, description, durationMinutes, totalMarks, isActive, difficulty, topicId, createdAt);
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
                difficulty = Difficulty.MEDIUM;
            }
        }
        String typeStr = rs.getString("question_type");
        QuestionType questionType = QuestionType.CODING;
        if (typeStr != null) {
            try {
                questionType = QuestionType.valueOf(typeStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                questionType = QuestionType.CODING;
            }
        }
        String solution = rs.getString("solution");
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        Instant createdAt = createdAtTs != null ? createdAtTs.toInstant() : Instant.now();
        Timestamp updatedAtTs = rs.getTimestamp("updated_at");
        Instant updatedAt = updatedAtTs != null ? updatedAtTs.toInstant() : Instant.now();

        return new Question(id, topicId, title, slug, description, difficulty, questionType, solution, createdAt, updatedAt);
    }
}
