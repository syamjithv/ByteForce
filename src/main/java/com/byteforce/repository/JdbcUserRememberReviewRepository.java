package com.byteforce.repository;

import com.byteforce.domain.ReviewRating;
import com.byteforce.domain.ReviewState;
import com.byteforce.domain.UserRememberReview;
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
import java.util.UUID;

/**
 * Production JDBC implementation of {@link UserRememberReviewRepository}.
 * Employs parameterized prepared statements and strict user isolation.
 */
public class JdbcUserRememberReviewRepository implements UserRememberReviewRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcUserRememberReviewRepository.class);

    private static final String SELECT_BASE = """
            SELECT id, user_id, remember_item_id, due_at, last_reviewed_at, review_count,
                   successful_review_count, state, difficulty, stability, retrievability,
                   last_rating, created_at, updated_at
            FROM user_remember_reviews
            """;

    private final DataSource dataSource;

    public JdbcUserRememberReviewRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public UserRememberReview save(UserRememberReview review) {
        Objects.requireNonNull(review, "review must not be null");
        if (review.getId() > 0 && existsById(review.getId())) {
            return update(review);
        } else {
            return insert(review);
        }
    }

    private UserRememberReview insert(UserRememberReview review) {
        String sql = """
                INSERT INTO user_remember_reviews (
                    user_id, remember_item_id, due_at, last_reviewed_at, review_count,
                    successful_review_count, state, difficulty, stability, retrievability,
                    last_rating, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, review.getUserId().toString());
            ps.setLong(2, review.getRememberItemId());
            ps.setTimestamp(3, Timestamp.from(review.getDueAt()));
            ps.setTimestamp(4, review.getLastReviewedAt() != null ? Timestamp.from(review.getLastReviewedAt()) : null);
            ps.setInt(5, review.getReviewCount());
            ps.setInt(6, review.getSuccessfulReviewCount());
            ps.setString(7, review.getState().name());
            ps.setDouble(8, review.getDifficulty());
            ps.setDouble(9, review.getStability());
            ps.setDouble(10, review.getRetrievability());
            ps.setString(11, review.getLastRating() != null ? review.getLastRating().name() : null);
            ps.setTimestamp(12, Timestamp.from(review.getCreatedAt()));
            ps.setTimestamp(13, Timestamp.from(review.getUpdatedAt()));

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return review.withId(id);
                }
                return review;
            }
        } catch (SQLException e) {
            if (isDuplicateKeyViolation(e)) {
                // Already exists for this (user_id, remember_item_id). Fetch and update instead.
                Optional<UserRememberReview> existing = findByUserIdAndRememberItemId(review.getUserId(), review.getRememberItemId());
                if (existing.isPresent()) {
                    UserRememberReview toUpdate = review.withId(existing.get().getId());
                    return update(toUpdate);
                }
            }
            throw new ByteForceException("Database error while inserting review state for user: " + review.getUserId(), e);
        }
    }

    private UserRememberReview update(UserRememberReview review) {
        String sql = """
                UPDATE user_remember_reviews
                SET due_at = ?, last_reviewed_at = ?, review_count = ?, successful_review_count = ?,
                    state = ?, difficulty = ?, stability = ?, retrievability = ?,
                    last_rating = ?, updated_at = ?
                WHERE id = ? AND user_id = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(review.getDueAt()));
            ps.setTimestamp(2, review.getLastReviewedAt() != null ? Timestamp.from(review.getLastReviewedAt()) : null);
            ps.setInt(3, review.getReviewCount());
            ps.setInt(4, review.getSuccessfulReviewCount());
            ps.setString(5, review.getState().name());
            ps.setDouble(6, review.getDifficulty());
            ps.setDouble(7, review.getStability());
            ps.setDouble(8, review.getRetrievability());
            ps.setString(9, review.getLastRating() != null ? review.getLastRating().name() : null);
            ps.setTimestamp(10, Timestamp.from(Instant.now()));
            ps.setLong(11, review.getId());
            ps.setString(12, review.getUserId().toString());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new ResourceNotFoundException("Cannot update non-existent or inaccessible review ID: " + review.getId());
            }
            return review;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while updating review ID: " + review.getId(), e);
        }
    }

    @Override
    public Optional<UserRememberReview> findById(long id) {
        if (id <= 0) {
            return Optional.empty();
        }
        String sql = SELECT_BASE + " WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding review by ID: " + id, e);
        }
    }

    @Override
    public Optional<UserRememberReview> findByUserIdAndRememberItemId(UUID userId, long rememberItemId) {
        Objects.requireNonNull(userId, "userId must not be null");
        if (rememberItemId <= 0) {
            return Optional.empty();
        }
        String sql = SELECT_BASE + " WHERE user_id = ? AND remember_item_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setLong(2, rememberItemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding review for user and remember item", e);
        }
    }

    @Override
    public List<UserRememberReview> findDueReviewsByUserId(UUID userId, Instant dueCutoff, int limit) {
        Objects.requireNonNull(userId, "userId must not be null");
        Instant cutoff = dueCutoff != null ? dueCutoff : Instant.now();
        int safeLimit = Math.max(1, Math.min(limit, 500));

        String sql = SELECT_BASE + " WHERE user_id = ? AND due_at <= ? ORDER BY due_at ASC, id ASC LIMIT ?";
        List<UserRememberReview> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setTimestamp(2, Timestamp.from(cutoff));
            ps.setInt(3, safeLimit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
            return results;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while querying due reviews for user: " + userId, e);
        }
    }

    @Override
    public List<UserRememberReview> findByUserId(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        String sql = SELECT_BASE + " WHERE user_id = ? ORDER BY due_at ASC";
        List<UserRememberReview> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
            return results;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while fetching all reviews for user: " + userId, e);
        }
    }

    @Override
    public List<UserRememberReview> findByUserIdAndConceptId(UUID userId, long conceptId) {
        Objects.requireNonNull(userId, "userId must not be null");
        if (conceptId <= 0) {
            return List.of();
        }
        String sql = """
                SELECT urr.id, urr.user_id, urr.remember_item_id, urr.due_at, urr.last_reviewed_at,
                       urr.review_count, urr.successful_review_count, urr.state, urr.difficulty,
                       urr.stability, urr.retrievability, urr.last_rating, urr.created_at, urr.updated_at
                FROM user_remember_reviews urr
                JOIN remember_items ri ON urr.remember_item_id = ri.id
                WHERE urr.user_id = ? AND ri.concept_id = ?
                ORDER BY urr.due_at ASC
                """;
        List<UserRememberReview> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setLong(2, conceptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
            return results;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while fetching concept reviews for user: " + userId, e);
        }
    }

    @Override
    public long countDueReviewsByUserId(UUID userId, Instant dueCutoff) {
        Objects.requireNonNull(userId, "userId must not be null");
        Instant cutoff = dueCutoff != null ? dueCutoff : Instant.now();
        String sql = "SELECT COUNT(*) FROM user_remember_reviews WHERE user_id = ? AND due_at <= ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setTimestamp(2, Timestamp.from(cutoff));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0L;
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while counting due reviews", e);
        }
    }

    @Override
    public long countReviewsByUserId(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        String sql = "SELECT COUNT(*) FROM user_remember_reviews WHERE user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0L;
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while counting total user reviews", e);
        }
    }

    @Override
    public List<UserRememberReview> findRecentlyReviewedByUserId(UUID userId, int limit) {
        Objects.requireNonNull(userId, "userId must not be null");
        int safeLimit = Math.max(1, Math.min(limit, 100));
        String sql = SELECT_BASE + " WHERE user_id = ? AND last_reviewed_at IS NOT NULL ORDER BY last_reviewed_at DESC LIMIT ?";
        List<UserRememberReview> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setInt(2, safeLimit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
            return results;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding recently reviewed items", e);
        }
    }

    @Override
    public List<UserRememberReview> findWeakReviewsByUserId(UUID userId, int limit) {
        Objects.requireNonNull(userId, "userId must not be null");
        int safeLimit = Math.max(1, Math.min(limit, 100));
        String sql = SELECT_BASE + """
                 WHERE user_id = ? AND (state = 'RELEARNING' OR difficulty >= 6.5 OR (review_count > 1 AND successful_review_count < review_count))
                 ORDER BY difficulty DESC, stability ASC LIMIT ?
                """;
        List<UserRememberReview> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setInt(2, safeLimit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
            return results;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding weak memory reviews", e);
        }
    }

    @Override
    public boolean deleteById(long id) {
        if (id <= 0) {
            return false;
        }
        String sql = "DELETE FROM user_remember_reviews WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while deleting review by ID: " + id, e);
        }
    }

    @Override
    public boolean deleteByUserIdAndRememberItemId(UUID userId, long rememberItemId) {
        Objects.requireNonNull(userId, "userId must not be null");
        if (rememberItemId <= 0) {
            return false;
        }
        String sql = "DELETE FROM user_remember_reviews WHERE user_id = ? AND remember_item_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setLong(2, rememberItemId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while deleting review by user and item", e);
        }
    }

    private boolean existsById(long id) {
        String sql = "SELECT 1 FROM user_remember_reviews WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while checking review existence by ID", e);
        }
    }

    private UserRememberReview mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        UUID userId = UUID.fromString(rs.getString("user_id"));
        long rememberItemId = rs.getLong("remember_item_id");
        Timestamp dueTs = rs.getTimestamp("due_at");
        Timestamp lastReviewedTs = rs.getTimestamp("last_reviewed_at");
        int reviewCount = rs.getInt("review_count");
        int successfulReviewCount = rs.getInt("successful_review_count");
        String stateStr = rs.getString("state");
        double difficulty = rs.getDouble("difficulty");
        double stability = rs.getDouble("stability");
        double retrievability = rs.getDouble("retrievability");
        String lastRatingStr = rs.getString("last_rating");
        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");

        ReviewState state;
        try {
            state = ReviewState.valueOf(stateStr);
        } catch (Exception e) {
            state = ReviewState.NEW;
        }

        ReviewRating lastRating = null;
        if (lastRatingStr != null && !lastRatingStr.isBlank()) {
            try {
                lastRating = ReviewRating.valueOf(lastRatingStr);
            } catch (Exception ignored) {}
        }

        Instant dueAt = dueTs != null ? dueTs.toInstant() : Instant.now();
        Instant lastReviewedAt = lastReviewedTs != null ? lastReviewedTs.toInstant() : null;
        Instant createdAt = createdTs != null ? createdTs.toInstant() : Instant.now();
        Instant updatedAt = updatedTs != null ? updatedTs.toInstant() : Instant.now();

        return new UserRememberReview(
                id,
                userId,
                rememberItemId,
                dueAt,
                lastReviewedAt,
                reviewCount,
                successfulReviewCount,
                state,
                difficulty,
                stability,
                retrievability,
                lastRating,
                createdAt,
                updatedAt
        );
    }

    private boolean isDuplicateKeyViolation(SQLException e) {
        if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
            return true;
        }
        int errorCode = e.getErrorCode();
        return errorCode == 1062 || errorCode == 23505;
    }
}
