package com.byteforce.repository;

import com.byteforce.domain.Company;
import com.byteforce.domain.CompanyInterviewCategory;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Production JDBC implementation of {@link CompanyRepository}.
 */
public class JdbcCompanyRepository implements CompanyRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcCompanyRepository.class);

    private static final String SELECT_BASE = """
            SELECT id, name, slug, logo_path, website_url, short_description, description, active, last_reviewed_at, created_at, updated_at
            FROM companies
            """;

    private final DataSource dataSource;

    public JdbcCompanyRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public Optional<Company> findById(long id) {
        if (id <= 0) return Optional.empty();
        String sql = SELECT_BASE + " WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToCompany(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Error finding company by id: " + id, e);
        }
    }

    @Override
    public Optional<Company> findBySlug(String slug) {
        if (slug == null || slug.isBlank()) return Optional.empty();
        String sql = SELECT_BASE + " WHERE slug = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, slug.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToCompany(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new ByteForceException("Error finding company by slug: " + slug, e);
        }
    }

    @Override
    public List<Company> findAll() {
        String sql = SELECT_BASE + " ORDER BY name ASC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Company> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRowToCompany(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new ByteForceException("Error retrieving all companies", e);
        }
    }

    @Override
    public List<Company> findAllActive() {
        String sql = SELECT_BASE + " WHERE active = TRUE ORDER BY name ASC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Company> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRowToCompany(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new ByteForceException("Error retrieving active companies", e);
        }
    }

    @Override
    public Company save(Company company) {
        Objects.requireNonNull(company, "company must not be null");
        String sql = """
                INSERT INTO companies (name, slug, logo_path, website_url, short_description, description, active, last_reviewed_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, company.getName());
            ps.setString(2, company.getSlug().trim().toLowerCase());
            ps.setString(3, company.getLogoPath());
            ps.setString(4, company.getWebsiteUrl());
            ps.setString(5, company.getShortDescription());
            ps.setString(6, company.getDescription());
            ps.setBoolean(7, company.isActive());
            ps.setTimestamp(8, company.getLastReviewedAt() != null ? Timestamp.from(company.getLastReviewedAt()) : null);
            ps.setTimestamp(9, Timestamp.from(company.getCreatedAt()));
            ps.setTimestamp(10, Timestamp.from(company.getUpdatedAt()));

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new ByteForceException("Failed to insert company record.");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long newId = keys.getLong(1);
                    return new Company(newId, company.getName(), company.getSlug(), company.getLogoPath(),
                            company.getWebsiteUrl(), company.getShortDescription(), company.getDescription(),
                            company.isActive(), company.getLastReviewedAt(), company.getCreatedAt(), company.getUpdatedAt());
                }
                throw new ByteForceException("No generated key returned for company insert.");
            }
        } catch (SQLException e) {
            throw new ByteForceException("Error saving company: " + company.getName(), e);
        }
    }

    @Override
    public void update(Company company) {
        Objects.requireNonNull(company, "company must not be null");
        String sql = """
                UPDATE companies
                SET name = ?, slug = ?, logo_path = ?, website_url = ?, short_description = ?, description = ?, active = ?, last_reviewed_at = ?, updated_at = ?
                WHERE id = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, company.getName());
            ps.setString(2, company.getSlug().trim().toLowerCase());
            ps.setString(3, company.getLogoPath());
            ps.setString(4, company.getWebsiteUrl());
            ps.setString(5, company.getShortDescription());
            ps.setString(6, company.getDescription());
            ps.setBoolean(7, company.isActive());
            ps.setTimestamp(8, company.getLastReviewedAt() != null ? Timestamp.from(company.getLastReviewedAt()) : null);
            ps.setTimestamp(9, Timestamp.from(Instant.now()));
            ps.setLong(10, company.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ByteForceException("Error updating company id: " + company.getId(), e);
        }
    }

    @Override
    public void deleteById(long id) {
        String sql = "DELETE FROM companies WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ByteForceException("Error deleting company id: " + id, e);
        }
    }

    @Override
    public void setCompanyActive(long id, boolean active) {
        String sql = "UPDATE companies SET active = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ByteForceException("Error updating active status for company id: " + id, e);
        }
    }

    // ==========================================
    // Topics Relationship
    // ==========================================
    @Override
    public List<Long> findTopicIdsByCompanyId(long companyId) {
        String sql = "SELECT topic_id FROM company_topics WHERE company_id = ? ORDER BY topic_id ASC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (rs.next()) ids.add(rs.getLong(1));
                return ids;
            }
        } catch (SQLException e) {
            throw new ByteForceException("Error finding topics for company id: " + companyId, e);
        }
    }

    @Override
    public void linkTopic(long companyId, long topicId) {
        String sql = "INSERT INTO company_topics (company_id, topic_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE company_id = company_id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, companyId);
            ps.setLong(2, topicId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ByteForceException("Error linking topic " + topicId + " to company " + companyId, e);
        }
    }

    @Override
    public void unlinkTopic(long companyId, long topicId) {
        String sql = "DELETE FROM company_topics WHERE company_id = ? AND topic_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, companyId);
            ps.setLong(2, topicId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ByteForceException("Error unlinking topic " + topicId + " from company " + companyId, e);
        }
    }

    @Override
    public void setCompanyTopics(long companyId, List<Long> topicIds) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement del = conn.prepareStatement("DELETE FROM company_topics WHERE company_id = ?")) {
                    del.setLong(1, companyId);
                    del.executeUpdate();
                }
                if (topicIds != null && !topicIds.isEmpty()) {
                    try (PreparedStatement ins = conn.prepareStatement("INSERT INTO company_topics (company_id, topic_id) VALUES (?, ?)")) {
                        for (Long tid : topicIds) {
                            ins.setLong(1, companyId);
                            ins.setLong(2, tid);
                            ins.addBatch();
                        }
                        ins.executeBatch();
                    }
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new ByteForceException("Error setting topics for company id: " + companyId, e);
        }
    }

    // ==========================================
    // Questions Relationship
    // ==========================================
    @Override
    public List<Long> findQuestionIdsByCompanyId(long companyId) {
        String sql = "SELECT question_id FROM company_questions WHERE company_id = ? ORDER BY question_id ASC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (rs.next()) ids.add(rs.getLong(1));
                return ids;
            }
        } catch (SQLException e) {
            throw new ByteForceException("Error finding questions for company id: " + companyId, e);
        }
    }

    @Override
    public void linkQuestion(long companyId, long questionId) {
        String sql = "INSERT INTO company_questions (company_id, question_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE company_id = company_id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, companyId);
            ps.setLong(2, questionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ByteForceException("Error linking question " + questionId + " to company " + companyId, e);
        }
    }

    @Override
    public void unlinkQuestion(long companyId, long questionId) {
        String sql = "DELETE FROM company_questions WHERE company_id = ? AND question_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, companyId);
            ps.setLong(2, questionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ByteForceException("Error unlinking question " + questionId + " from company " + companyId, e);
        }
    }

    @Override
    public void setCompanyQuestions(long companyId, List<Long> questionIds) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement del = conn.prepareStatement("DELETE FROM company_questions WHERE company_id = ?")) {
                    del.setLong(1, companyId);
                    del.executeUpdate();
                }
                if (questionIds != null && !questionIds.isEmpty()) {
                    try (PreparedStatement ins = conn.prepareStatement("INSERT INTO company_questions (company_id, question_id) VALUES (?, ?)")) {
                        for (Long qid : questionIds) {
                            ins.setLong(1, companyId);
                            ins.setLong(2, qid);
                            ins.addBatch();
                        }
                        ins.executeBatch();
                    }
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new ByteForceException("Error setting questions for company id: " + companyId, e);
        }
    }

    // ==========================================
    // Aptitude Questions Relationship
    // ==========================================
    @Override
    public List<Long> findAptitudeQuestionIdsByCompanyId(long companyId) {
        String sql = "SELECT aptitude_question_id FROM company_aptitude_questions WHERE company_id = ? ORDER BY aptitude_question_id ASC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (rs.next()) ids.add(rs.getLong(1));
                return ids;
            }
        } catch (SQLException e) {
            throw new ByteForceException("Error finding aptitude questions for company id: " + companyId, e);
        }
    }

    @Override
    public void linkAptitudeQuestion(long companyId, long aptitudeQuestionId) {
        String sql = "INSERT INTO company_aptitude_questions (company_id, aptitude_question_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE company_id = company_id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, companyId);
            ps.setLong(2, aptitudeQuestionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ByteForceException("Error linking aptitude question " + aptitudeQuestionId + " to company " + companyId, e);
        }
    }

    @Override
    public void unlinkAptitudeQuestion(long companyId, long aptitudeQuestionId) {
        String sql = "DELETE FROM company_aptitude_questions WHERE company_id = ? AND aptitude_question_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, companyId);
            ps.setLong(2, aptitudeQuestionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ByteForceException("Error unlinking aptitude question " + aptitudeQuestionId + " from company " + companyId, e);
        }
    }

    @Override
    public void setCompanyAptitudeQuestions(long companyId, List<Long> aptitudeQuestionIds) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement del = conn.prepareStatement("DELETE FROM company_aptitude_questions WHERE company_id = ?")) {
                    del.setLong(1, companyId);
                    del.executeUpdate();
                }
                if (aptitudeQuestionIds != null && !aptitudeQuestionIds.isEmpty()) {
                    try (PreparedStatement ins = conn.prepareStatement("INSERT INTO company_aptitude_questions (company_id, aptitude_question_id) VALUES (?, ?)")) {
                        for (Long aqid : aptitudeQuestionIds) {
                            ins.setLong(1, companyId);
                            ins.setLong(2, aqid);
                            ins.addBatch();
                        }
                        ins.executeBatch();
                    }
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new ByteForceException("Error setting aptitude questions for company id: " + companyId, e);
        }
    }

    // ==========================================
    // Assessments Relationship
    // ==========================================
    @Override
    public List<Long> findAssessmentIdsByCompanyId(long companyId) {
        String sql = "SELECT assessment_id FROM company_assessments WHERE company_id = ? ORDER BY assessment_id ASC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (rs.next()) ids.add(rs.getLong(1));
                return ids;
            }
        } catch (SQLException e) {
            throw new ByteForceException("Error finding assessments for company id: " + companyId, e);
        }
    }

    @Override
    public void linkAssessment(long companyId, long assessmentId) {
        String sql = "INSERT INTO company_assessments (company_id, assessment_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE company_id = company_id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, companyId);
            ps.setLong(2, assessmentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ByteForceException("Error linking assessment " + assessmentId + " to company " + companyId, e);
        }
    }

    @Override
    public void unlinkAssessment(long companyId, long assessmentId) {
        String sql = "DELETE FROM company_assessments WHERE company_id = ? AND assessment_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, companyId);
            ps.setLong(2, assessmentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ByteForceException("Error unlinking assessment " + assessmentId + " from company " + companyId, e);
        }
    }

    @Override
    public void setCompanyAssessments(long companyId, List<Long> assessmentIds) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement del = conn.prepareStatement("DELETE FROM company_assessments WHERE company_id = ?")) {
                    del.setLong(1, companyId);
                    del.executeUpdate();
                }
                if (assessmentIds != null && !assessmentIds.isEmpty()) {
                    try (PreparedStatement ins = conn.prepareStatement("INSERT INTO company_assessments (company_id, assessment_id) VALUES (?, ?)")) {
                        for (Long aid : assessmentIds) {
                            ins.setLong(1, companyId);
                            ins.setLong(2, aid);
                            ins.addBatch();
                        }
                        ins.executeBatch();
                    }
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new ByteForceException("Error setting assessments for company id: " + companyId, e);
        }
    }

    // ==========================================
    // Interview Categories
    // ==========================================
    @Override
    public List<CompanyInterviewCategory> findInterviewCategoriesByCompanyId(long companyId) {
        String sql = "SELECT id, company_id, title, category_type, description, display_order, created_at FROM company_interview_categories WHERE company_id = ? ORDER BY display_order ASC, id ASC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                List<CompanyInterviewCategory> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new CompanyInterviewCategory(
                            rs.getLong("id"),
                            rs.getLong("company_id"),
                            rs.getString("title"),
                            rs.getString("category_type"),
                            rs.getString("description"),
                            rs.getInt("display_order"),
                            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : Instant.now()
                    ));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new ByteForceException("Error finding interview categories for company id: " + companyId, e);
        }
    }

    @Override
    public CompanyInterviewCategory saveInterviewCategory(CompanyInterviewCategory category) {
        Objects.requireNonNull(category, "category must not be null");
        String sql = "INSERT INTO company_interview_categories (company_id, title, category_type, description, display_order, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, category.getCompanyId());
            ps.setString(2, category.getTitle());
            ps.setString(3, category.getCategoryType());
            ps.setString(4, category.getDescription());
            ps.setInt(5, category.getDisplayOrder());
            ps.setTimestamp(6, Timestamp.from(category.getCreatedAt()));

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return new CompanyInterviewCategory(id, category.getCompanyId(), category.getTitle(),
                            category.getCategoryType(), category.getDescription(), category.getDisplayOrder(), category.getCreatedAt());
                }
                throw new ByteForceException("No generated key returned for interview category.");
            }
        } catch (SQLException e) {
            throw new ByteForceException("Error saving interview category for company: " + category.getCompanyId(), e);
        }
    }

    @Override
    public void deleteInterviewCategory(long categoryId) {
        String sql = "DELETE FROM company_interview_categories WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, categoryId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ByteForceException("Error deleting interview category id: " + categoryId, e);
        }
    }

    private Company mapRowToCompany(ResultSet rs) throws SQLException {
        Timestamp reviewed = rs.getTimestamp("last_reviewed_at");
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");

        return new Company(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("slug"),
                rs.getString("logo_path"),
                rs.getString("website_url"),
                rs.getString("short_description"),
                rs.getString("description"),
                rs.getBoolean("active"),
                reviewed != null ? reviewed.toInstant() : null,
                created != null ? created.toInstant() : Instant.now(),
                updated != null ? updated.toInstant() : Instant.now()
        );
    }
}
