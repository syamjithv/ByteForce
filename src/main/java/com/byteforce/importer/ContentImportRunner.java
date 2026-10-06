package com.byteforce.importer;

import com.byteforce.importer.model.ImportAuditReport;
import com.byteforce.importer.model.StagedQuestionRecord;
import com.byteforce.importer.parser.AptitudeHubParser;
import com.byteforce.importer.parser.CsFundamentalsParser;
import com.byteforce.importer.parser.OsMasteryParser;
import com.byteforce.importer.parser.TechInterviewHandbookParser;
import com.byteforce.importer.service.ContentImportService;
import com.byteforce.persistence.DatabaseMigrator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * Main entry point for the ByteForce Content Acquisition and Production Import Pipeline.
 * Connects to MySQL, applies Flyway V7 migration, executes multi-repository parsing,
 * validates, deduplicates, and stages all records with complete provenance.
 */
public class ContentImportRunner {

    private static final Logger log = LoggerFactory.getLogger(ContentImportRunner.class);

    public static void main(String[] args) {
        log.info("=================================================================");
        log.info("   BYTEFORCE CONTENT ACQUISITION & PRODUCTION IMPORT PIPELINE   ");
        log.info("=================================================================");

        // 1. Load application.properties
        Properties props = new Properties();
        try (InputStream in = ContentImportRunner.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            log.error("Could not load application.properties", e);
        }

        String jdbcUrl = System.getenv("DB_URL");
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            jdbcUrl = props.getProperty("db.url", "jdbc:mysql://localhost:3306/byteforce?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        }
        String dbUser = System.getenv("DB_USERNAME");
        if (dbUser == null || dbUser.isBlank()) {
            dbUser = props.getProperty("db.username", "root");
        }
        String dbPass = System.getenv("DB_PASSWORD");
        if (dbPass == null) {
            dbPass = props.getProperty("db.password", "");
        }

        log.info("Connecting to database at: {}", jdbcUrl);

        HikariConfig config = new HikariConfig();
        config.setPoolName("ByteForce-ImporterPool");
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPass);
        config.setMaximumPoolSize(5);

        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            // 2. Execute Flyway migration up to V7
            log.info("Applying Flyway migrations to ensure schema version 7...");
            int migrationsApplied = DatabaseMigrator.migrate(dataSource, "classpath:db/migration");
            log.info("Flyway migration completed. Migrations applied: {}", migrationsApplied);

            ContentImportService importService = new ContentImportService(dataSource);

            // 3. Record baseline counts
            long questionsBefore = importService.queryCurrentQuestionCount();
            long aptitudeBefore = importService.queryCurrentAptitudeCount();
            log.info("BASELINE COUNTS - questions: {}, aptitude_questions: {}", questionsBefore, aptitudeBefore);

            ImportAuditReport report = new ImportAuditReport();
            report.setExistingQuestionsBefore(questionsBefore);
            report.setExistingAptitudeBefore(aptitudeBefore);

            // 4. Initialize topic mapping & existing hash deduplication
            importService.initialize();

            // 5. Track audited repositories
            // Accepted
            report.incrementRepositoriesInspected(); report.incrementRepositoriesAccepted(); // Rohan Mistry Aptitude
            report.incrementRepositoriesInspected(); report.incrementRepositoriesAccepted(); // Saikumar Kadiri OS Mastery
            report.incrementRepositoriesInspected(); report.incrementRepositoriesAccepted(); // Manish Kumar CS Fundamentals
            report.incrementRepositoriesInspected(); report.incrementRepositoriesAccepted(); // Yangshun Tay Tech Interview Handbook
            report.incrementRepositoriesInspected(); report.incrementRepositoriesAccepted(); // Donne Martin System Design Primer

            // Reference-Only
            report.incrementRepositoriesInspected(); report.incrementReferenceOnly(); // ashishps1 (GPLv3 copyleft)
            report.incrementRepositoriesInspected(); report.incrementReferenceOnly(); // in28minutes (Unlicensed)
            report.incrementRepositoriesInspected(); report.incrementReferenceOnly(); // chyyran notes (Unlicensed)
            report.incrementRepositoriesInspected(); report.incrementReferenceOnly(); // JayrajSinh16 (Unlicensed)
            report.incrementRepositoriesInspected(); report.incrementReferenceOnly(); // sadanandpai (LeetCode content)
            report.incrementRepositoriesInspected(); report.incrementReferenceOnly(); // kdn251 (LeetCode content)
            report.incrementRepositoriesInspected(); report.incrementReferenceOnly(); // SAKET-SK (Scraped PrepInsta/IndiaBix)
            report.incrementRepositoriesInspected(); report.incrementReferenceOnly(); // VIJAYAPANDIANT (Unlicensed)
            report.incrementRepositoriesInspected(); report.incrementReferenceOnly(); // rubanthilak (Unlicensed)

            // Rejected
            report.incrementRepositoriesInspected(); report.incrementRejected(); // etsryn (Empty)
            report.incrementRepositoriesInspected(); report.incrementRejected(); // Thiru-kumaran-R (Remote DB only)
            report.incrementRepositoriesInspected(); report.incrementRejected(); // Ayushverma135 (Binary test paper PDFs)

            // 6. Execute Parsers on local cloned repositories
            String batchId = "batch-" + UUID.randomUUID().toString().substring(0, 8);
            Path basePath = Paths.get("scratch/sources");

            List<StagedQuestionRecord> allCandidates = new ArrayList<>();

            // A. Aptitude Hub
            Path aptitudePath = basePath.resolve("CSE-Aptitude-Test-Practice-Hub");
            if (aptitudePath.toFile().exists()) {
                AptitudeHubParser aptitudeParser = new AptitudeHubParser();
                allCandidates.addAll(aptitudeParser.parseRepository(aptitudePath, batchId));
            }

            // B. OS Mastery
            Path osPath = basePath.resolve("Operating-Systems-Mastery");
            if (osPath.toFile().exists()) {
                OsMasteryParser osParser = new OsMasteryParser();
                allCandidates.addAll(osParser.parseRepository(osPath, batchId));
            }

            // C. CS Fundamentals
            Path csPath = basePath.resolve("CS-Fundamentals");
            if (csPath.toFile().exists()) {
                CsFundamentalsParser csParser = new CsFundamentalsParser();
                allCandidates.addAll(csParser.parseRepository(csPath, batchId));
            }

            // D. Tech Interview Handbook
            Path techPath = basePath.resolve("tech-interview-handbook");
            if (techPath.toFile().exists()) {
                TechInterviewHandbookParser techParser = new TechInterviewHandbookParser();
                allCandidates.addAll(techParser.parseRepository(techPath, batchId));
            }

            // E. System Design Primer
            Path sysPath = basePath.resolve("system-design-primer");
            if (sysPath.toFile().exists()) {
                com.byteforce.importer.parser.SystemDesignPrimerParser sysParser = new com.byteforce.importer.parser.SystemDesignPrimerParser();
                allCandidates.addAll(sysParser.parseRepository(sysPath, batchId));
            }

            log.info("Total candidate records extracted across all accepted repositories: {}", allCandidates.size());

            // 7. Execute Import Pipeline (Validation -> Deduplication -> MySQL Staging & Production Insertion)
            importService.importCandidates(allCandidates, report);

            // 8. Record Final Counts
            long questionsAfter = importService.queryCurrentQuestionCount();
            long aptitudeAfter = importService.queryCurrentAptitudeCount();
            report.setFinalQuestionsAfter(questionsAfter);
            report.setFinalAptitudeAfter(aptitudeAfter);

            // 9. Output Summary
            printReport(report);

        } catch (Exception e) {
            log.error("Content import pipeline terminated with error", e);
            System.exit(1);
        }
    }

    private static void printReport(ImportAuditReport report) {
        System.out.println("\n=======================================================");
        System.out.println("          BYTEFORCE CONTENT IMPORT AUDIT REPORT         ");
        System.out.println("=======================================================");
        System.out.println("SOURCE AUDIT");
        System.out.println("Repositories inspected: " + report.getRepositoriesInspected());
        System.out.println("Repositories accepted:  " + report.getRepositoriesAccepted());
        System.out.println("Reference-only:         " + report.getReferenceOnly());
        System.out.println("Rejected:               " + report.getRejected());
        System.out.println("-------------------------------------------------------");
        System.out.println("CONTENT COUNTS");
        System.out.println("Raw candidates:         " + report.getRawCandidates());
        System.out.println("Invalid:                " + report.getInvalid());
        System.out.println("Duplicates removed:     " + report.getDuplicatesRemoved());
        System.out.println("Provenance-rejected:    " + report.getProvenanceRejected());
        System.out.println("Valid/importable:       " + report.getValidImportable());
        System.out.println("-------------------------------------------------------");
        System.out.println("ACTUALLY IMPORTED");
        System.out.println("Total before (questions + aptitude): " + (report.getExistingQuestionsBefore() + report.getExistingAptitudeBefore()));
        System.out.println("  - questions before:          " + report.getExistingQuestionsBefore());
        System.out.println("  - aptitude_questions before: " + report.getExistingAptitudeBefore());
        System.out.println("Total after (questions + aptitude):  " + (report.getFinalQuestionsAfter() + report.getFinalAptitudeAfter()));
        System.out.println("  - questions after:           " + report.getFinalQuestionsAfter());
        System.out.println("  - aptitude_questions after:  " + report.getFinalAptitudeAfter());
        System.out.println("New questions imported:        " + (report.getFinalQuestionsAfter() - report.getExistingQuestionsBefore()));
        System.out.println("New aptitude questions:        " + (report.getFinalAptitudeAfter() - report.getExistingAptitudeBefore()));
        System.out.println("Total new content imported:    " + ((report.getFinalQuestionsAfter() - report.getExistingQuestionsBefore()) + (report.getFinalAptitudeAfter() - report.getExistingAptitudeBefore())));
        System.out.println("-------------------------------------------------------");
        System.out.println("BREAKDOWN BY CATEGORY");
        for (Map.Entry<String, Integer> entry : report.getAllCategoryCounts().entrySet()) {
            System.out.printf("%-26s: %d\n", entry.getKey(), entry.getValue());
        }
        System.out.println("=======================================================\n");
    }
}
