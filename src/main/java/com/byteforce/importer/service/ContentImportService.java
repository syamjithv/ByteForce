package com.byteforce.importer.service;

import com.byteforce.domain.Difficulty;
import com.byteforce.domain.QuestionType;
import com.byteforce.exception.ByteForceException;
import com.byteforce.importer.model.ImportAuditReport;
import com.byteforce.importer.model.StagedQuestionRecord;
import com.byteforce.importer.util.ContentDeduplicator;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Production-safe transactional acquisition and import service.
 * Manages staging, validation, deduplication, and production insertion into MySQL.
 */
public class ContentImportService {

    private static final Logger log = LoggerFactory.getLogger(ContentImportService.class);

    private final DataSource dataSource;
    private final ContentDeduplicator deduplicator = new ContentDeduplicator();
    private final Map<String, Long> topicSlugToId = new HashMap<>();
    private final Map<String, Long> topicNameToId = new HashMap<>();

    public ContentImportService(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    /**
     * Initializes deduplication hashes and topic mappings from the current MySQL database.
     */
    public void initialize() {
        log.info("Initializing ContentImportService with existing database state...");
        try (Connection conn = dataSource.getConnection()) {
            // 1. Load topic mappings
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, name, slug FROM topics")) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String name = rs.getString("name");
                    String slug = rs.getString("slug");
                    if (slug != null) topicSlugToId.put(slug.toLowerCase(Locale.ROOT), id);
                    if (name != null) topicNameToId.put(name.toLowerCase(Locale.ROOT), id);
                }
            }
            log.info("Loaded {} topics from database", topicSlugToId.size());

            // 2. Pre-seed hashes from questions table
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT description FROM questions")) {
                while (rs.next()) {
                    String desc = rs.getString("description");
                    if (desc != null) deduplicator.registerExistingHash(ContentDeduplicator.computeHash(desc));
                }
            }

            // 3. Pre-seed hashes from aptitude_questions table
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT question FROM aptitude_questions")) {
                while (rs.next()) {
                    String q = rs.getString("question");
                    if (q != null) deduplicator.registerExistingHash(ContentDeduplicator.computeHash(q));
                }
            }

            // 4. Pre-seed hashes from staged_questions table if any
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT content_hash FROM staged_questions")) {
                while (rs.next()) {
                    String hash = rs.getString("content_hash");
                    if (hash != null) deduplicator.registerExistingHash(hash);
                }
            }

            log.info("Deduplicator pre-seeded with {} existing content hashes", deduplicator.size());
        } catch (SQLException e) {
            throw new ByteForceException("Failed to initialize ContentImportService from database", e);
        }
    }

    /**
     * Executes the import of a list of staged candidate records into MySQL with batch transactions.
     */
    public void importCandidates(List<StagedQuestionRecord> candidates, ImportAuditReport report) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }

        final int batchSize = 100;
        int total = candidates.size();

        for (int i = 0; i < total; i += batchSize) {
            int end = Math.min(i + batchSize, total);
            List<StagedQuestionRecord> batch = candidates.subList(i, end);
            importBatch(batch, report);
        }
    }

    private void importBatch(List<StagedQuestionRecord> batch, ImportAuditReport report) {
        String insertStagedSql = """
                INSERT INTO staged_questions (batch_id, source_repo, source_path, license, author, attribution,
                                             category_or_subject, topic, difficulty, question_type, question_text,
                                             option_a, option_b, option_c, option_d, correct_answer, solution,
                                             status, rejection_reason, content_hash, target_table, target_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        String insertAptitudeSql = """
                INSERT INTO aptitude_questions (category, topic, difficulty, question, option_a, option_b, option_c, option_d,
                                               correct_answer, explanation, active, source_repo, source_url, license,
                                               author, attribution, review_status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        String insertQuestionSql = """
                INSERT INTO questions (topic_id, title, slug, description, difficulty, question_type, solution,
                                      source_repo, source_url, license, author, attribution, review_status,
                                      created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psStaged = conn.prepareStatement(insertStagedSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement psAptitude = conn.prepareStatement(insertAptitudeSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement psQuestion = conn.prepareStatement(insertQuestionSql, Statement.RETURN_GENERATED_KEYS)) {

                for (StagedQuestionRecord rec : batch) {
                    report.incrementRawCandidates();

                    // 1. Basic validation
                    String validationError = validateRecord(rec);
                    if (validationError != null) {
                        report.incrementInvalid();
                        rec.setStatus("REJECTED");
                        rec.setRejectionReason(validationError);
                        stageRecord(psStaged, rec);
                        continue;
                    }

                    // 2. License / Provenance Check
                    if (rec.getLicense() == null || (!rec.getLicense().equalsIgnoreCase("MIT") && !rec.getLicense().equalsIgnoreCase("Apache-2.0") && !rec.getLicense().toUpperCase().contains("APACHE") && !rec.getLicense().toUpperCase().contains("MIT"))) {
                        report.incrementProvenanceRejected();
                        rec.setStatus("PROVENANCE_REJECTED");
                        rec.setRejectionReason("Non-permissive or unverified license: " + rec.getLicense());
                        stageRecord(psStaged, rec);
                        continue;
                    }

                    // 3. Deduplication Check
                    String hash = rec.getContentHash();
                    if (hash == null || hash.isBlank()) {
                        hash = ContentDeduplicator.computeHash(rec.getQuestionText());
                        rec.setContentHash(hash);
                    }

                    if (!deduplicator.checkAndRegister(hash)) {
                        report.incrementDuplicatesRemoved();
                        rec.setStatus("DUPLICATE");
                        rec.setRejectionReason("Duplicate question content hash");
                        stageRecord(psStaged, rec);
                        continue;
                    }

                    report.incrementValidImportable();

                    // 4. Production insertion
                    Timestamp now = Timestamp.from(Instant.now());
                    long generatedId = 0;

                    if ("aptitude_questions".equalsIgnoreCase(rec.getTargetTable())) {
                        psAptitude.setString(1, rec.getCategoryOrSubject());
                        psAptitude.setString(2, rec.getTopicName() != null ? rec.getTopicName() : "General Aptitude");
                        psAptitude.setString(3, rec.getDifficulty() != null ? rec.getDifficulty().name() : "MEDIUM");
                        psAptitude.setString(4, rec.getQuestionText());
                        psAptitude.setString(5, rec.getOptionA() != null ? rec.getOptionA() : "");
                        psAptitude.setString(6, rec.getOptionB() != null ? rec.getOptionB() : "");
                        psAptitude.setString(7, rec.getOptionC() != null ? rec.getOptionC() : "");
                        psAptitude.setString(8, rec.getOptionD() != null ? rec.getOptionD() : "");
                        psAptitude.setString(9, rec.getCorrectAnswer() != null ? rec.getCorrectAnswer() : "A");
                        psAptitude.setString(10, rec.getSolution() != null ? rec.getSolution() : "");
                        psAptitude.setBoolean(11, true);
                        psAptitude.setString(12, rec.getSourceRepo());
                        psAptitude.setString(13, rec.getSourcePath());
                        psAptitude.setString(14, rec.getLicense());
                        psAptitude.setString(15, rec.getAuthor());
                        psAptitude.setString(16, rec.getAttribution());
                        psAptitude.setString(17, "PUBLISHED");
                        psAptitude.setTimestamp(18, now);
                        psAptitude.setTimestamp(19, now);

                        psAptitude.executeUpdate();
                        try (ResultSet rs = psAptitude.getGeneratedKeys()) {
                            if (rs.next()) generatedId = rs.getLong(1);
                        }
                        report.incrementImportedAptitude();
                        recordAptitudeCategory(rec, report);
                    } else {
                        // Insert into questions table
                        long topicId = resolveTopicId(rec);
                        String uniqueSlug = generateUniqueSlug(rec);
                        String title = truncate(rec.getTitle(), 240);

                        psQuestion.setLong(1, topicId);
                        psQuestion.setString(2, title);
                        psQuestion.setString(3, uniqueSlug);
                        psQuestion.setString(4, rec.getQuestionText());
                        psQuestion.setString(5, rec.getDifficulty() != null ? rec.getDifficulty().name() : "MEDIUM");
                        psQuestion.setString(6, rec.getQuestionType() != null ? rec.getQuestionType().name() : "CONCEPTUAL");
                        psQuestion.setString(7, rec.getSolution());
                        psQuestion.setString(8, rec.getSourceRepo());
                        psQuestion.setString(9, rec.getSourcePath());
                        psQuestion.setString(10, rec.getLicense());
                        psQuestion.setString(11, rec.getAuthor());
                        psQuestion.setString(12, rec.getAttribution());
                        psQuestion.setString(13, "PUBLISHED");
                        psQuestion.setTimestamp(14, now);
                        psQuestion.setTimestamp(15, now);

                        psQuestion.executeUpdate();
                        try (ResultSet rs = psQuestion.getGeneratedKeys()) {
                            if (rs.next()) generatedId = rs.getLong(1);
                        }
                        report.incrementImportedQuestions();
                        report.incrementCategory(rec.getCategoryOrSubject());
                    }

                    // 5. Stage as IMPORTED with generated id
                    rec.setStatus("IMPORTED");
                    rec.setTargetId(generatedId);
                    stageRecord(psStaged, rec);
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                log.error("Batch import failed and was rolled back", e);
                throw new ByteForceException("Batch transaction error during question import", e);
            }
        } catch (SQLException e) {
            log.error("Database connection error during batch import", e);
            throw new ByteForceException("Database error during question import", e);
        }
    }

    private void stageRecord(PreparedStatement ps, StagedQuestionRecord rec) throws SQLException {
        ps.setString(1, rec.getBatchId() != null ? rec.getBatchId() : "batch-1");
        ps.setString(2, rec.getSourceRepo());
        ps.setString(3, rec.getSourcePath());
        ps.setString(4, rec.getLicense());
        ps.setString(5, rec.getAuthor());
        ps.setString(6, rec.getAttribution());
        ps.setString(7, rec.getCategoryOrSubject() != null ? rec.getCategoryOrSubject() : "General");
        ps.setString(8, rec.getTopicName() != null ? rec.getTopicName() : "General");
        ps.setString(9, rec.getDifficulty() != null ? rec.getDifficulty().name() : "MEDIUM");
        ps.setString(10, rec.getQuestionType() != null ? rec.getQuestionType().name() : "CONCEPTUAL");
        ps.setString(11, rec.getQuestionText());
        ps.setString(12, rec.getOptionA());
        ps.setString(13, rec.getOptionB());
        ps.setString(14, rec.getOptionC());
        ps.setString(15, rec.getOptionD());
        ps.setString(16, rec.getCorrectAnswer());
        ps.setString(17, rec.getSolution());
        ps.setString(18, rec.getStatus());
        ps.setString(19, rec.getRejectionReason());
        ps.setString(20, rec.getContentHash() != null ? rec.getContentHash() : "");
        ps.setString(21, rec.getTargetTable());
        ps.setLong(22, rec.getTargetId());
        ps.executeUpdate();
    }

    private String validateRecord(StagedQuestionRecord rec) {
        if (rec.getQuestionText() == null || rec.getQuestionText().isBlank()) {
            return "Empty question text";
        }
        if (rec.getQuestionText().trim().length() < 5) {
            return "Question text too short";
        }
        if (rec.getSolution() == null || rec.getSolution().isBlank()) {
            return "Missing solution or explanation";
        }
        if ("aptitude_questions".equalsIgnoreCase(rec.getTargetTable())) {
            if (rec.getOptionA() == null || rec.getOptionA().isBlank() ||
                rec.getOptionB() == null || rec.getOptionB().isBlank() ||
                rec.getOptionC() == null || rec.getOptionC().isBlank() ||
                rec.getOptionD() == null || rec.getOptionD().isBlank()) {
                return "Incomplete MCQ options";
            }
            if (rec.getCorrectAnswer() == null || !rec.getCorrectAnswer().matches("[A-D]")) {
                return "Invalid correct answer letter: " + rec.getCorrectAnswer();
            }
        }
        return null;
    }

    private long resolveTopicId(StagedQuestionRecord rec) {
        if (rec.getTopicSlug() != null) {
            Long id = topicSlugToId.get(rec.getTopicSlug().toLowerCase(Locale.ROOT));
            if (id != null) return id;
        }
        if (rec.getTopicName() != null) {
            Long id = topicNameToId.get(rec.getTopicName().toLowerCase(Locale.ROOT));
            if (id != null) return id;
        }
        // Fallback matching
        String cat = (rec.getCategoryOrSubject() != null ? rec.getCategoryOrSubject() : "").toLowerCase(Locale.ROOT);
        if (cat.contains("network")) return topicSlugToId.getOrDefault("network-architecture-osi", 1L);
        if (cat.contains("dbms") || cat.contains("sql")) return topicSlugToId.getOrDefault("sql-databases", 3L);
        if (cat.contains("os") || cat.contains("operating")) return topicSlugToId.getOrDefault("os-concurrency", 5L);
        if (cat.contains("oop") || cat.contains("java")) return topicSlugToId.getOrDefault("oop-java", 4L);
        if (cat.contains("coa") || cat.contains("architecture")) return topicSlugToId.getOrDefault("computer-architecture-pipelining", 1L);
        if (cat.contains("toc") || cat.contains("computation")) return topicSlugToId.getOrDefault("automata-formal-languages", 1L);
        if (cat.contains("behavioral") || cat.contains("interview")) return topicSlugToId.getOrDefault("behavioral-hr-interview", 4L);
        if (cat.contains("system") || cat.contains("design")) return topicSlugToId.getOrDefault("system-design-scalability", 1L);

        return 1L; // default to topic 1 (DSA)
    }

    private String generateUniqueSlug(StagedQuestionRecord rec) {
        String base = rec.getSlug();
        if (base == null || base.isBlank()) {
            base = (rec.getTitle() != null ? rec.getTitle() : "question").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        }
        if (base.length() > 200) {
            base = base.substring(0, 200);
        }
        return base + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "Question";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen);
    }

    private void recordAptitudeCategory(StagedQuestionRecord rec, ImportAuditReport report) {
        String path = (rec.getSourcePath() != null ? rec.getSourcePath() : "").toLowerCase(Locale.ROOT);
        if (path.contains("01 quantitative")) report.incrementCategory("Quantitative Aptitude");
        else if (path.contains("02 logical")) report.incrementCategory("Logical Reasoning");
        else if (path.contains("03 verbal")) report.incrementCategory("Verbal Ability");
        else if (path.contains("04 data")) report.incrementCategory("Data Interpretation");
        else if (path.contains("05 abstract")) report.incrementCategory("Abstract Reasoning");
        else if (path.contains("06 technical")) report.incrementCategory("Technical Aptitude");
        else report.incrementCategory("Quantitative Aptitude");
    }

    public long queryCurrentQuestionCount() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM questions")) {
            if (rs.next()) return rs.getLong(1);
            return 0;
        } catch (SQLException e) {
            throw new ByteForceException("Error querying question count", e);
        }
    }

    public long queryCurrentAptitudeCount() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM aptitude_questions")) {
            if (rs.next()) return rs.getLong(1);
            return 0;
        } catch (SQLException e) {
            throw new ByteForceException("Error querying aptitude question count", e);
        }
    }
}
