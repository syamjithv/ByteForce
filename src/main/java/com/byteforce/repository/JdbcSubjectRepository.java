package com.byteforce.repository;

import com.byteforce.domain.Subject;
import com.byteforce.exception.ByteForceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Production JDBC implementation of {@link SubjectRepository}.
 */
public class JdbcSubjectRepository implements SubjectRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcSubjectRepository.class);

    private static final String SELECT_BASE = """
            SELECT id, name, description, display_order
            FROM subjects
            """;

    private final DataSource dataSource;

    public JdbcSubjectRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public List<Subject> findAll() {
        String sql = SELECT_BASE + " ORDER BY display_order ASC, name ASC";
        List<Subject> subjects = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                subjects.add(mapRowToSubject(rs));
            }
            return subjects;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while fetching all subjects", e);
        }
    }

    @Override
    public Optional<Subject> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String sql = SELECT_BASE + " WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToSubject(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while finding subject by ID: " + id, e);
        }
    }

    @Override
    public Subject save(Subject subject) {
        Objects.requireNonNull(subject, "subject must not be null");
        if (existsById(subject.getId())) {
            return update(subject);
        } else {
            return insert(subject);
        }
    }

    private Subject insert(Subject subject) {
        String sql = """
                INSERT INTO subjects (id, name, description, display_order)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, subject.getId());
            ps.setString(2, subject.getName());
            ps.setString(3, subject.getDescription());
            ps.setInt(4, subject.getDisplayOrder());
            ps.executeUpdate();
            return subject;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while inserting subject: " + subject.getId(), e);
        }
    }

    private Subject update(Subject subject) {
        String sql = """
                UPDATE subjects
                SET name = ?, description = ?, display_order = ?
                WHERE id = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, subject.getName());
            ps.setString(2, subject.getDescription());
            ps.setInt(3, subject.getDisplayOrder());
            ps.setString(4, subject.getId());
            ps.executeUpdate();
            return subject;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while updating subject: " + subject.getId(), e);
        }
    }

    @Override
    public boolean existsById(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        String sql = "SELECT 1 FROM subjects WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Database error while checking subject existence: " + id, e);
        }
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        String sql = "DELETE FROM subjects WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.trim().toLowerCase());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while deleting subject with ID: " + id, e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM subjects";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new ByteForceException("Database error while counting subjects", e);
        }
    }

    private Subject mapRowToSubject(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("name");
        String description = rs.getString("description");
        int displayOrder = rs.getInt("display_order");
        return new Subject(id, name, description, displayOrder, List.of());
    }
}
