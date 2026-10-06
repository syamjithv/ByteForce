package com.byteforce.importer.parser;

import com.byteforce.domain.Difficulty;
import com.byteforce.domain.QuestionType;
import com.byteforce.importer.model.StagedQuestionRecord;
import com.byteforce.importer.util.ContentDeduplicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for System Design Primer by Donne Martin (Apache-2.0 / CC-BY-SA-4.0).
 * Extracts System Design interview problems, scalability architectures, and case studies.
 */
public class SystemDesignPrimerParser {

    private static final Logger log = LoggerFactory.getLogger(SystemDesignPrimerParser.class);

    private static final String SOURCE_REPO = "https://github.com/donnemartin/system-design-primer";
    private static final String LICENSE = "Apache-2.0";
    private static final String AUTHOR = "Donne Martin";
    private static final String ATTRIBUTION = "System Design Primer by Donne Martin (Apache-2.0 / CC-BY-SA-4.0)";

    public List<StagedQuestionRecord> parseRepository(Path repoRoot, String batchId) {
        List<StagedQuestionRecord> records = new ArrayList<>();
        if (!Files.exists(repoRoot)) {
            log.warn("Repository root does not exist: {}", repoRoot);
            return records;
        }

        // 1. Parse Case Studies from solutions/system_design
        Path solutionsDir = repoRoot.resolve("solutions/system_design");
        if (Files.exists(solutionsDir)) {
            try {
                Files.list(solutionsDir).filter(Files::isDirectory).forEach(dir -> {
                    Path readme = dir.resolve("README.md");
                    if (Files.exists(readme)) {
                        try {
                            StagedQuestionRecord rec = parseCaseStudy(readme, repoRoot, dir.getFileName().toString(), batchId);
                            if (rec != null) records.add(rec);
                        } catch (Exception e) {
                            log.error("Failed to parse case study: " + readme, e);
                        }
                    }
                });
            } catch (IOException e) {
                log.error("Error listing solutions directory", e);
            }
        }

        // 2. Parse core architectural sections from root README.md
        Path rootReadme = repoRoot.resolve("README.md");
        if (Files.exists(rootReadme)) {
            try {
                records.addAll(parseCoreTopics(rootReadme, repoRoot, batchId));
            } catch (Exception e) {
                log.error("Failed to parse system design core topics", e);
            }
        }

        log.info("SystemDesignPrimerParser extracted {} questions from {}", records.size(), repoRoot);
        return records;
    }

    private StagedQuestionRecord parseCaseStudy(Path readmePath, Path repoRoot, String systemName, String batchId) throws IOException {
        String content = Files.readString(readmePath, StandardCharsets.UTF_8);
        String relPath = repoRoot.relativize(readmePath).toString().replace('\\', '/');

        String title = "Design " + capitalize(systemName);
        String prompt = "Design a scalable, highly available system for: " + capitalize(systemName) +
                ". Address functional requirements, non-functional requirements, data modeling, high-level architecture, and bottlenecks.";

        StagedQuestionRecord rec = new StagedQuestionRecord();
        rec.setBatchId(batchId);
        rec.setSourceRepo(SOURCE_REPO);
        rec.setSourcePath(relPath);
        rec.setLicense(LICENSE);
        rec.setAuthor(AUTHOR);
        rec.setAttribution(ATTRIBUTION);
        rec.setCategoryOrSubject("System Design");
        rec.setTopicName("System Design & Scalability");
        rec.setTopicSlug("system-design-scalability");
        rec.setDifficulty(Difficulty.HARD);
        rec.setQuestionType(QuestionType.CONCEPTUAL);
        rec.setTargetTable("questions");
        rec.setTitle(title);
        rec.setSlug(("sys-design-" + systemName).toLowerCase().replaceAll("[^a-z0-9]+", "-"));
        rec.setQuestionText(prompt);
        rec.setSolution(content.replaceAll("```mermaid[\\s\\S]*?```", "").trim());
        rec.setContentHash(ContentDeduplicator.computeHash(title + " system design"));

        return rec;
    }

    private List<StagedQuestionRecord> parseCoreTopics(Path readmePath, Path repoRoot, String batchId) throws IOException {
        List<StagedQuestionRecord> list = new ArrayList<>();
        String content = Files.readString(readmePath, StandardCharsets.UTF_8);
        String relPath = repoRoot.relativize(readmePath).toString().replace('\\', '/');

        // Match ## Section Title e.g. ## Caching, ## Asynchronism, ## Load balancer
        Pattern p = Pattern.compile("(?m)^##\\s+([A-Za-z\\s/-]+)\\s*\\n+([\\s\\S]*?)(?=(?:^##\\s+)|\\Z)");
        Matcher m = p.matcher(content);

        int count = 0;
        while (m.find()) {
            String topicTitle = m.group(1).trim();
            String body = m.group(2).trim();

            if (topicTitle.equalsIgnoreCase("Table of Contents") || topicTitle.equalsIgnoreCase("Contributing") ||
                topicTitle.equalsIgnoreCase("Index of system design topics") || body.length() < 100) {
                continue;
            }

            count++;
            StagedQuestionRecord rec = new StagedQuestionRecord();
            rec.setBatchId(batchId);
            rec.setSourceRepo(SOURCE_REPO);
            rec.setSourcePath(relPath);
            rec.setLicense(LICENSE);
            rec.setAuthor(AUTHOR);
            rec.setAttribution(ATTRIBUTION);
            rec.setCategoryOrSubject("System Design");
            rec.setTopicName("System Design & Scalability");
            rec.setTopicSlug("system-design-scalability");
            rec.setDifficulty(Difficulty.MEDIUM);
            rec.setQuestionType(QuestionType.CONCEPTUAL);
            rec.setTargetTable("questions");
            rec.setTitle("System Architecture: " + topicTitle);
            rec.setSlug(("arch-" + count + "-" + topicTitle).toLowerCase().replaceAll("[^a-z0-9]+", "-"));
            rec.setQuestionText("Explain the role, trade-offs, and implementation strategies of " + topicTitle + " in high-scale distributed systems.");
            rec.setSolution(body.replaceAll("```mermaid[\\s\\S]*?```", "").trim());
            rec.setContentHash(ContentDeduplicator.computeHash(topicTitle + " scalability"));

            list.add(rec);
        }

        return list;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1).replace('_', ' ');
    }
}
