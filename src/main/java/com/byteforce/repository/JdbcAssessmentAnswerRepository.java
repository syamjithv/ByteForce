package com.byteforce.repository;

import com.byteforce.domain.AssessmentAnswer;
import com.byteforce.domain.AttemptStatus;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Production-ready JDBC implementation of {@link AssessmentAnswerRepository}.
 */
public class JdbcAssessmentAnswerRepository implements AssessmentAnswerRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcAssessmentAnswerRepository.class);

    private static final String SELECT_BASE = """
            SELECT id, assessment_attempt_id, question_id, submitted_answer, marks_awarded, status, answered_at
            FROM assessment_answers
            """;

    private final DataSource dataSource;

    public JdbcAssessmentAnswerRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public Optional<AssessmentAnswer> findById(long id) {
        if (id <= 0) {
            return Optional.empty();
        }
        String sql = SELECT_BASE + " WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToAnswer(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding assessment answer by ID: " + id, e);
        }
    }

    @Override
    public Optional<AssessmentAnswer> findByAttemptAndQuestion(long attemptId, long questionId) {
        if (attemptId <= 0 || questionId <= 0) {
            return Optional.empty();
        }
        String sql = SELECT_BASE + " WHERE assessment_attempt_id = ? AND question_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, attemptId);
            ps.setLong(2, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToAnswer(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding answer for attempt " + attemptId + " and question " + questionId, e);
        }
    }

    @Override
    public List<AssessmentAnswer> findByAttemptId(long attemptId) {
        if (attemptId <= 0) {
            return List.of();
        }
        String sql = SELECT_BASE + " WHERE assessment_attempt_id = ? ORDER BY id ASC";
        List<AssessmentAnswer> answers = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    answers.add(mapRowToAnswer(rs));
                }
            }
            return answers;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding answers for attempt ID: " + attemptId, e);
        }
    }

    @Override
    public AssessmentAnswer save(AssessmentAnswer answer) {
        Objects.requireNonNull(answer, "answer must not be null");
        if (answer.getId() <= 0) {
            return insert(answer);
        } else {
            return update(answer);
        }
    }

    private AssessmentAnswer insert(AssessmentAnswer answer) {
        String sql = """
                INSERT INTO assessment_answers (assessment_attempt_id, question_id, submitted_answer, marks_awarded, status, answered_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, answer.getAssessmentAttemptId());
            ps.setLong(2, answer.getQuestionId());
            if (answer.getSubmittedAnswer() != null) {
                ps.setString(3, answer.getSubmittedAnswer());
            } else {
                ps.setNull(3, Types.VARCHAR);
            }
            ps.setInt(4, answer.getMarksAwarded());
            ps.setString(5, answer.getStatus().name());
            if (answer.getAnsweredAt() != null) {
                ps.setTimestamp(6, Timestamp.from(answer.getAnsweredAt()));
            } else {
                ps.setNull(6, Types.TIMESTAMP);
            }

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long generatedId = keys.getLong(1);
                    return answer.withId(generatedId);
                } else {
                    throw new ByteForceException("Failed to retrieve generated ID for inserted assessment answer.");
                }
            }
        } catch (SQLException e) {
            if (isDuplicateKeyViolation(e)) {
                throw new ByteForceException("An answer already exists for attempt " + answer.getAssessmentAttemptId()
                        + " and question " + answer.getQuestionId(), e);
            }
            throw new ByteForceException("Database error while inserting assessment answer", e);
        }
    }

    private AssessmentAnswer update(AssessmentAnswer answer) {
        String sql = """
                UPDATE assessment_answers
                SET assessment_attempt_id = ?, question_id = ?, submitted_answer = ?, marks_awarded = ?, status = ?, answered_at = ?
                WHERE id = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, answer.getAssessmentAttemptId());
            ps.setLong(2, answer.getQuestionId());
            if (answer.getSubmittedAnswer() != null) {
                ps.setString(3, answer.getSubmittedAnswer());
            } else {
                ps.setNull(3, Types.VARCHAR);
            }
            ps.setInt(4, answer.getMarksAwarded());
            ps.setString(5, answer.getStatus().name());
            if (answer.getAnsweredAt() != null) {
                ps.setTimestamp(6, Timestamp.from(answer.getAnsweredAt()));
            } else {
                ps.setNull(6, Types.TIMESTAMP);
            }
            ps.setLong(7, answer.getId());

            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new ResourceNotFoundException("Cannot update non-existent assessment answer with ID: " + answer.getId());
            }
            return answer;
        } catch (SQLException e) {
            if (isDuplicateKeyViolation(e)) {
                throw new ByteForceException("An answer already exists for attempt " + answer.getAssessmentAttemptId()
                        + " and question " + answer.getQuestionId(), e);
            }
            throw new ByteForceException("Database error while updating assessment answer with ID: " + answer.getId(), e);
        }
    }

    @Override
    public boolean deleteById(long id) {
        if (id <= 0) {
            return false;
        }
        String sql = "DELETE FROM assessment_answers WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while deleting assessment answer with ID: " + id, e);
        }
    }

    @Override
    public long countByAttemptId(long attemptId) {
        if (attemptId <= 0) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM assessment_answers WHERE assessment_attempt_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while counting answers for attempt ID: " + attemptId, e);
        }
    }

    private AssessmentAnswer mapRowToAnswer(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        long attemptId = rs.getLong("assessment_attempt_id");
        long questionId = rs.getLong("question_id");
        String submittedAnswer = rs.getString("submitted_answer");
        int marksAwarded = rs.getInt("marks_awarded");
        String statusStr = rs.getString("status");
        AttemptStatus status = AttemptStatus.SKIPPED;
        if (statusStr != null) {
            try {
                status = AttemptStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown answer status '{}' for ID {}, defaulting to SKIPPED", statusStr, id);
            }
        }
        Timestamp answeredTs = rs.getTimestamp("answered_at");
        Instant answeredAt = answeredTs != null ? answeredTs.toInstant() : null;

        return new AssessmentAnswer(id, attemptId, questionId, submittedAnswer, marksAwarded, status, answeredAt);
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
