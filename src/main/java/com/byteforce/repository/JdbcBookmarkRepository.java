package com.byteforce.repository;

import com.byteforce.domain.Bookmark;
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
 * Production-ready JDBC implementation of {@link BookmarkRepository}.
 */
public class JdbcBookmarkRepository implements BookmarkRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcBookmarkRepository.class);

    private static final String SELECT_BASE = """
            SELECT id, user_id, question_id, notes, created_at
            FROM bookmarks
            """;

    private final DataSource dataSource;

    public JdbcBookmarkRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public Optional<Bookmark> findById(long id) {
        if (id <= 0) {
            return Optional.empty();
        }
        String sql = SELECT_BASE + " WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToBookmark(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding bookmark by ID: " + id, e);
        }
    }

    @Override
    public Optional<Bookmark> findByUserIdAndQuestionId(UUID userId, long questionId) {
        if (userId == null || questionId <= 0) {
            return Optional.empty();
        }
        String sql = SELECT_BASE + " WHERE user_id = ? AND question_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setLong(2, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToBookmark(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding bookmark for user " + userId + " and question " + questionId, e);
        }
    }

    @Override
    public List<Bookmark> findByUserId(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        String sql = SELECT_BASE + " WHERE user_id = ? ORDER BY created_at DESC";
        List<Bookmark> bookmarks = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bookmarks.add(mapRowToBookmark(rs));
                }
            }
            return bookmarks;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding bookmarks for user: " + userId, e);
        }
    }

    @Override
    public List<Bookmark> findByQuestionId(long questionId) {
        if (questionId <= 0) {
            return List.of();
        }
        String sql = SELECT_BASE + " WHERE question_id = ? ORDER BY created_at DESC";
        List<Bookmark> bookmarks = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bookmarks.add(mapRowToBookmark(rs));
                }
            }
            return bookmarks;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding bookmarks for question: " + questionId, e);
        }
    }

    @Override
    public boolean isBookmarked(UUID userId, long questionId) {
        if (userId == null || questionId <= 0) {
            return false;
        }
        String sql = "SELECT 1 FROM bookmarks WHERE user_id = ? AND question_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setLong(2, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while checking bookmark status for user " + userId + " and question " + questionId, e);
        }
    }

    @Override
    public Bookmark save(Bookmark bookmark) {
        Objects.requireNonNull(bookmark, "bookmark must not be null");

        if (bookmark.getId() > 0) {
            return update(bookmark);
        }

        // Check if bookmark already exists for this (user_id, question_id)
        Optional<Bookmark> existingOpt = findByUserIdAndQuestionId(bookmark.getUserId(), bookmark.getQuestionId());
        if (existingOpt.isPresent()) {
            Bookmark existing = existingOpt.get();
            Bookmark toUpdate = new Bookmark(existing.getId(), existing.getUserId(), existing.getQuestionId(),
                    bookmark.getNotes(), existing.getCreatedAt());
            return update(toUpdate);
        }

        return insert(bookmark);
    }

    private Bookmark insert(Bookmark bookmark) {
        String sql = """
                INSERT INTO bookmarks (user_id, question_id, notes, created_at)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, bookmark.getUserId().toString());
            ps.setLong(2, bookmark.getQuestionId());
            ps.setString(3, bookmark.getNotes());
            ps.setTimestamp(4, Timestamp.from(bookmark.getCreatedAt()));

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long generatedId = keys.getLong(1);
                    return bookmark.withId(generatedId);
                } else {
                    throw new ByteForceException("Failed to retrieve generated ID for inserted bookmark.");
                }
            }
        } catch (SQLException e) {
            if (isDuplicateKeyViolation(e)) {
                Optional<Bookmark> existingOpt = findByUserIdAndQuestionId(bookmark.getUserId(), bookmark.getQuestionId());
                if (existingOpt.isPresent()) {
                    Bookmark existing = existingOpt.get();
                    return update(new Bookmark(existing.getId(), existing.getUserId(), existing.getQuestionId(),
                            bookmark.getNotes(), existing.getCreatedAt()));
                }
            }
            throw new ByteForceException("Database error while inserting bookmark for user " + bookmark.getUserId(), e);
        }
    }

    private Bookmark update(Bookmark bookmark) {
        String sql = "UPDATE bookmarks SET notes = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bookmark.getNotes());
            ps.setLong(2, bookmark.getId());

            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new ResourceNotFoundException("Cannot update non-existent bookmark with ID: " + bookmark.getId());
            }
            return bookmark;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while updating bookmark with ID: " + bookmark.getId(), e);
        }
    }

    @Override
    public boolean deleteById(long id) {
        if (id <= 0) {
            return false;
        }
        String sql = "DELETE FROM bookmarks WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while deleting bookmark by ID: " + id, e);
        }
    }

    @Override
    public boolean deleteByUserIdAndQuestionId(UUID userId, long questionId) {
        if (userId == null || questionId <= 0) {
            return false;
        }
        String sql = "DELETE FROM bookmarks WHERE user_id = ? AND question_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setLong(2, questionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while deleting bookmark for user " + userId + " and question " + questionId, e);
        }
    }

    @Override
    public long countByUserId(UUID userId) {
        if (userId == null) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM bookmarks WHERE user_id = ?";
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
            throw new ByteForceException("Database error while counting bookmarks for user: " + userId, e);
        }
    }

    private Bookmark mapRowToBookmark(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        UUID userId = UUID.fromString(rs.getString("user_id"));
        long questionId = rs.getLong("question_id");
        String notes = rs.getString("notes");
        Timestamp createdTs = rs.getTimestamp("created_at");
        Instant createdAt = createdTs != null ? createdTs.toInstant() : Instant.now();

        return new Bookmark(id, userId, questionId, notes, createdAt);
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
