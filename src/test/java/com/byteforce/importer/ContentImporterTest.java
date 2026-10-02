package com.byteforce.importer;

import com.byteforce.domain.Difficulty;
import com.byteforce.domain.QuestionType;
import com.byteforce.importer.model.ImportAuditReport;
import com.byteforce.importer.model.StagedQuestionRecord;
import com.byteforce.importer.parser.AptitudeHubParser;
import com.byteforce.importer.parser.CsFundamentalsParser;
import com.byteforce.importer.parser.OsMasteryParser;
import com.byteforce.importer.parser.TechInterviewHandbookParser;
import com.byteforce.importer.service.ContentImportService;
import com.byteforce.importer.util.ContentDeduplicator;
import com.byteforce.persistence.DatabaseMigrator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentImporterTest {

    private HikariDataSource dataSource;
    private ContentImportService importService;

    @BeforeEach
    void setUp() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("ImporterTestPool-" + UUID.randomUUID());
        hikariConfig.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        hikariConfig.setUsername("sa");
        hikariConfig.setPassword("");
        hikariConfig.setMaximumPoolSize(3);
        dataSource = new HikariDataSource(hikariConfig);

        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO topics (id, subject_id, name, slug, description, display_order) VALUES (1, 'data-structures', 'General DSA', 'dsa', 'DSA', 1)");
            stmt.execute("INSERT INTO topics (id, subject_id, name, slug, description, display_order) VALUES (5, 'operating-systems', 'Operating Systems & Concurrency', 'os-concurrency', 'OS', 2)");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to seed test topics", e);
        }
        importService = new ContentImportService(dataSource);
        importService.initialize();
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Test
    @DisplayName("ContentDeduplicator should normalize text and detect duplicate hashes")
    void shouldDeduplicateNormalizedText() {
        ContentDeduplicator deduplicator = new ContentDeduplicator();

        String q1 = "What is the average time complexity of QuickSort?";
        String q2 = "what is the average time complexity of quicksort???";
        String q3 = "What is the worst case time complexity of QuickSort?";

        String hash1 = ContentDeduplicator.computeHash(q1);
        String hash2 = ContentDeduplicator.computeHash(q2);
        String hash3 = ContentDeduplicator.computeHash(q3);

        assertEquals(hash1, hash2, "Case, punctuation, and whitespace variations must yield identical hashes");
        assertFalse(hash1.equals(hash3), "Distinct questions must yield different hashes");

        assertTrue(deduplicator.checkAndRegister(hash1), "First occurrence should be accepted");
        assertFalse(deduplicator.checkAndRegister(hash2), "Duplicate hash must be rejected");
        assertTrue(deduplicator.checkAndRegister(hash3), "Distinct question should be accepted");
    }

    @Test
    @DisplayName("AptitudeHubParser should parse questions with derived options and solutions")
    void shouldParseAptitudeHubQuestions(@org.junit.jupiter.api.io.TempDir Path tempDir) throws IOException {
        Path repoRoot = tempDir.resolve("CSE-Aptitude-Test-Practice-Hub");
        Path quantDir = repoRoot.resolve("01 Quantitative Aptitude (Numerical Ability)/01 Basic");
        Files.createDirectories(quantDir);

        String sampleContent = """
                # 100 Quantitative Aptitude Practice Questions
                
                ## Percentages
                1. **Question**: What is 25% of 400?  
                   **Solution**:  
                   25% of 400 = (25/100) * 400 = 100.  
                   **Answer**: 100
                
                2. **Question**: Find 10% of 1500.  
                   **Solution**:  
                   10% of 1500 = 150.  
                   **Answer**: 150
                """;

        Files.writeString(quantDir.resolve("README.md"), sampleContent);

        AptitudeHubParser parser = new AptitudeHubParser();
        List<StagedQuestionRecord> records = parser.parseRepository(repoRoot, "test-batch");

        assertEquals(2, records.size());
        StagedQuestionRecord rec1 = records.get(0);
        assertEquals("What is 25% of 400?", rec1.getQuestionText());
        assertNotNull(rec1.getOptionA());
        assertNotNull(rec1.getOptionB());
        assertNotNull(rec1.getOptionC());
        assertNotNull(rec1.getOptionD());
        assertTrue(rec1.getCorrectAnswer().matches("[A-D]"));
        assertEquals("Percentages", rec1.getTopicName());
        assertEquals("QUANTITATIVE", rec1.getCategoryOrSubject());
    }

    @Test
    @DisplayName("ContentImportService should import candidate records, reject duplicates, and record provenance")
    void shouldImportCandidatesAndRecordProvenance() throws SQLException {
        ImportAuditReport report = new ImportAuditReport();

        StagedQuestionRecord r1 = new StagedQuestionRecord();
        r1.setBatchId("batch-test");
        r1.setSourceRepo("https://github.com/rohanmistry231/CSE-Aptitude-Test-Practice-Hub");
        r1.setLicense("MIT");
        r1.setAuthor("Rohan Mistry");
        r1.setAttribution("CSE Aptitude Test Practice Hub (MIT)");
        r1.setCategoryOrSubject("QUANTITATIVE");
        r1.setTopicName("Percentages");
        r1.setDifficulty(Difficulty.EASY);
        r1.setQuestionType(QuestionType.APTITUDE);
        r1.setTargetTable("aptitude_questions");
        r1.setQuestionText("What is 20% of 500?");
        r1.setOptionA("80");
        r1.setOptionB("100");
        r1.setOptionC("120");
        r1.setOptionD("150");
        r1.setCorrectAnswer("B");
        r1.setSolution("20% of 500 = 0.2 * 500 = 100");
        r1.setContentHash(ContentDeduplicator.computeHash(r1.getQuestionText()));

        // Duplicate of r1
        StagedQuestionRecord r2 = new StagedQuestionRecord();
        r2.setBatchId("batch-test");
        r2.setSourceRepo("https://github.com/rohanmistry231/CSE-Aptitude-Test-Practice-Hub");
        r2.setLicense("MIT");
        r2.setTargetTable("aptitude_questions");
        r2.setCategoryOrSubject("QUANTITATIVE");
        r2.setQuestionText("what is 20% of 500???");
        r2.setOptionA("80"); r2.setOptionB("100"); r2.setOptionC("120"); r2.setOptionD("150");
        r2.setCorrectAnswer("B");
        r2.setSolution("20% of 500 = 100");
        r2.setContentHash(ContentDeduplicator.computeHash(r2.getQuestionText()));

        // Core CS Question
        StagedQuestionRecord r3 = new StagedQuestionRecord();
        r3.setBatchId("batch-test");
        r3.setSourceRepo("https://github.com/kadirisaikumar3/Operating-Systems-Mastery");
        r3.setLicense("MIT");
        r3.setAuthor("Saikumar Kadiri");
        r3.setAttribution("Operating Systems Mastery (MIT)");
        r3.setCategoryOrSubject("Operating Systems");
        r3.setTopicSlug("os-concurrency");
        r3.setTopicName("Operating Systems & Concurrency");
        r3.setDifficulty(Difficulty.MEDIUM);
        r3.setQuestionType(QuestionType.CONCEPTUAL);
        r3.setTargetTable("questions");
        r3.setTitle("What is the Convoy Effect?");
        r3.setSlug("what-is-convoy-effect");
        r3.setQuestionText("Explain the Convoy Effect in FCFS CPU scheduling.");
        r3.setSolution("The Convoy Effect occurs when a CPU-bound process holds the CPU while multiple I/O-bound processes wait in the ready queue.");
        r3.setContentHash(ContentDeduplicator.computeHash(r3.getQuestionText()));

        importService.importCandidates(List.of(r1, r2, r3), report);

        assertEquals(3, report.getRawCandidates());
        assertEquals(2, report.getValidImportable());
        assertEquals(1, report.getDuplicatesRemoved());
        assertEquals(1, report.getTotalImportedAptitude());
        assertEquals(1, report.getTotalImportedQuestions());

        // Verify database state
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT question, source_repo, review_status FROM aptitude_questions WHERE question LIKE '%20% of 500%'")) {
                assertTrue(rs.next());
                assertEquals("What is 20% of 500?", rs.getString("question"));
                assertEquals("https://github.com/rohanmistry231/CSE-Aptitude-Test-Practice-Hub", rs.getString("source_repo"));
                assertEquals("PUBLISHED", rs.getString("review_status"));
            }

            try (ResultSet rs = stmt.executeQuery("SELECT title, source_repo, review_status FROM questions WHERE slug LIKE 'what-is-convoy-effect%'")) {
                assertTrue(rs.next());
                assertEquals("What is the Convoy Effect?", rs.getString("title"));
                assertEquals("https://github.com/kadirisaikumar3/Operating-Systems-Mastery", rs.getString("source_repo"));
                assertEquals("PUBLISHED", rs.getString("review_status"));
            }

            try (ResultSet rs = stmt.executeQuery("SELECT status, rejection_reason FROM staged_questions WHERE status = 'DUPLICATE'")) {
                assertTrue(rs.next());
                assertEquals("DUPLICATE", rs.getString("status"));
                assertEquals("Duplicate question content hash", rs.getString("rejection_reason"));
            }
        }
    }

    @Test
    @DisplayName("CsFundamentalsParser should extract questions from CS-Fundamentals if present")
    void shouldParseCsFundamentalsQuestions() {
        Path repoPath = java.nio.file.Paths.get("scratch/sources/CS-Fundamentals");
        if (Files.exists(repoPath)) {
            CsFundamentalsParser parser = new CsFundamentalsParser();
            List<StagedQuestionRecord> records = parser.parseRepository(repoPath, "test-batch");
            assertNotNull(records);
            assertTrue(records.size() > 50, "Expected at least 50 core CS questions from CS-Fundamentals, got: " + records.size());
            StagedQuestionRecord sample = records.get(0);
            assertNotNull(sample.getQuestionText());
            assertNotNull(sample.getSolution());
            assertNotNull(sample.getTopicSlug());
            assertEquals("MIT", sample.getLicense());
        }
    }
}
