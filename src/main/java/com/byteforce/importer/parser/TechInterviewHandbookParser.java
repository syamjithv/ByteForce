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
 * Parser for Tech Interview Handbook by Yangshun Tay (MIT License).
 * Extracts Behavioral/HR interview questions and algorithmic coding/conceptual questions.
 */
public class TechInterviewHandbookParser {

    private static final Logger log = LoggerFactory.getLogger(TechInterviewHandbookParser.class);

    private static final String SOURCE_REPO = "https://github.com/yangshun/tech-interview-handbook";
    private static final String LICENSE = "MIT";
    private static final String AUTHOR = "Yangshun Tay";
    private static final String ATTRIBUTION = "Tech Interview Handbook by Yangshun Tay (MIT License)";

    public List<StagedQuestionRecord> parseRepository(Path repoRoot, String batchId) {
        List<StagedQuestionRecord> records = new ArrayList<>();
        if (!Files.exists(repoRoot)) {
            log.warn("Repository root does not exist: {}", repoRoot);
            return records;
        }

        Path behavioralPath = repoRoot.resolve("apps/website/contents/behavioral-interview-questions.md");
        if (Files.exists(behavioralPath)) {
            try {
                records.addAll(parseBehavioralQuestions(behavioralPath, repoRoot, batchId));
            } catch (Exception e) {
                log.error("Failed to parse behavioral questions", e);
            }
        }

        Path algoDir = repoRoot.resolve("apps/website/contents/algorithms");
        if (Files.exists(algoDir)) {
            try {
                Files.list(algoDir)
                        .filter(p -> p.toString().endsWith(".md") && !p.getFileName().toString().startsWith("_"))
                        .forEach(filePath -> {
                            try {
                                records.addAll(parseAlgorithmFile(filePath, repoRoot, batchId));
                            } catch (Exception e) {
                                log.error("Failed to parse algorithm file: " + filePath, e);
                            }
                        });
            } catch (IOException e) {
                log.error("Failed to traverse algorithm directory: " + algoDir, e);
            }
        }

        log.info("TechInterviewHandbookParser extracted {} questions from {}", records.size(), repoRoot);
        return records;
    }

    private List<StagedQuestionRecord> parseBehavioralQuestions(Path filePath, Path repoRoot, String batchId) throws IOException {
        List<StagedQuestionRecord> list = new ArrayList<>();
        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        String relPath = repoRoot.relativize(filePath).toString().replace('\\', '/');

        // Match numbered questions e.g. "1. Tell me about a time when you had a conflict with a co-worker."
        Pattern p = Pattern.compile("(?m)^\\d+\\.\\s+([^\\n?]+(?:\\?|\\.))");
        Matcher m = p.matcher(content);

        int count = 0;
        while (m.find()) {
            String questionTitle = m.group(1).trim();
            if (questionTitle.length() < 10) continue;

            count++;
            StagedQuestionRecord rec = new StagedQuestionRecord();
            rec.setBatchId(batchId);
            rec.setSourceRepo(SOURCE_REPO);
            rec.setSourcePath(relPath);
            rec.setLicense(LICENSE);
            rec.setAuthor(AUTHOR);
            rec.setAttribution(ATTRIBUTION);
            rec.setCategoryOrSubject("Behavioral/HR");
            rec.setTopicName("Behavioral & HR Interview");
            rec.setTopicSlug("behavioral-hr-interview");
            rec.setDifficulty(Difficulty.MEDIUM);
            rec.setQuestionType(QuestionType.CONCEPTUAL);
            rec.setTargetTable("questions");
            rec.setTitle("Behavioral Interview: " + questionTitle);
            rec.setSlug(("behavioral-" + count + "-" + questionTitle).toLowerCase().replaceAll("[^a-z0-9]+", "-"));
            rec.setQuestionText("How would you answer this behavioral interview question using the STAR framework?\n\n\"" + questionTitle + "\"");
            rec.setSolution("Use the STAR framework to structure your answer effectively:\n\n" +
                    "- **Situation:** Briefly set the context, team size, project scope, and specific technical background.\n" +
                    "- **Task:** Clearly explain your personal role, expectations, and the key conflict or challenge faced.\n" +
                    "- **Action:** Describe the specific technical and interpersonal actions YOU took (collaborative communication, root cause analysis, data-driven decisions).\n" +
                    "- **Result:** Quantify the positive outcome (e.g., delivered on schedule, improved test coverage by 25%, maintained strong stakeholder relationship), and share what you learned.");
            rec.setContentHash(ContentDeduplicator.computeHash(questionTitle));

            list.add(rec);
        }

        return list;
    }

    private List<StagedQuestionRecord> parseAlgorithmFile(Path filePath, Path repoRoot, String batchId) throws IOException {
        List<StagedQuestionRecord> list = new ArrayList<>();
        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        String relPath = repoRoot.relativize(filePath).toString().replace('\\', '/');
        String topicName = filePath.getFileName().toString().replace(".md", "");

        // Match sections under ## Corner Cases, ## Things to look out for, ## Essential Questions
        Pattern p = Pattern.compile("(?m)^##\\s+([^#\\n]+)\\s*\\n+([\\s\\S]*?)(?=(?:^##\\s+)|\\Z)");
        Matcher m = p.matcher(content);

        int count = 0;
        while (m.find()) {
            String sectionTitle = m.group(1).trim();
            String sectionBody = m.group(2).trim();

            if (sectionBody.length() < 40 || sectionTitle.equalsIgnoreCase("Introduction") || sectionTitle.equalsIgnoreCase("Learning Resources")) {
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
            rec.setCategoryOrSubject("DSA");
            rec.setTopicName("Data Structures & Algorithms");
            rec.setTopicSlug("dsa");
            rec.setDifficulty(Difficulty.MEDIUM);
            rec.setQuestionType(QuestionType.CONCEPTUAL);
            rec.setTargetTable("questions");
            rec.setTitle("Algorithm Concept: " + topicName + " - " + sectionTitle);
            rec.setSlug(("algo-" + topicName + "-" + sectionTitle + "-" + count).toLowerCase().replaceAll("[^a-z0-9]+", "-"));
            rec.setQuestionText("Explain key algorithmic considerations and interview patterns for: " + topicName + " (" + sectionTitle + ").");
            rec.setSolution(sectionBody);
            rec.setContentHash(ContentDeduplicator.computeHash(topicName + " " + sectionTitle));

            list.add(rec);
        }

        return list;
    }
}
