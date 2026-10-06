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
 * Parser for Operating-Systems-Mastery by Saikumar Kadiri (MIT License).
 * Extracts OS conceptual questions, solved numerical scheduling/memory problems,
 * and multiple-choice questions.
 */
public class OsMasteryParser {

    private static final Logger log = LoggerFactory.getLogger(OsMasteryParser.class);

    private static final String SOURCE_REPO = "https://github.com/kadirisaikumar3/Operating-Systems-Mastery";
    private static final String LICENSE = "MIT";
    private static final String AUTHOR = "Saikumar Kadiri";
    private static final String ATTRIBUTION = "Operating Systems Mastery by Saikumar Kadiri (MIT License)";

    public List<StagedQuestionRecord> parseRepository(Path repoRoot, String batchId) {
        List<StagedQuestionRecord> records = new ArrayList<>();
        if (!Files.exists(repoRoot)) {
            log.warn("Repository root does not exist: {}", repoRoot);
            return records;
        }

        try {
            Files.walk(repoRoot)
                    .filter(p -> p.toString().endsWith(".md"))
                    .forEach(filePath -> {
                        String name = filePath.getFileName().toString();
                        try {
                            if (name.equals("05-Practice-Problems.md")) {
                                records.addAll(parsePracticeProblems(filePath, repoRoot, batchId));
                            } else if (name.equals("11-FAQs.md")) {
                                records.addAll(parseFaqs(filePath, repoRoot, batchId));
                            } else if (name.equals("04-Solved-Examples.md")) {
                                records.addAll(parseSolvedExamples(filePath, repoRoot, batchId));
                            }
                        } catch (Exception e) {
                            log.error("Failed to parse OS Mastery file: " + filePath, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Error traversing OS Mastery directory: " + repoRoot, e);
        }

        log.info("OsMasteryParser extracted {} questions from {}", records.size(), repoRoot);
        return records;
    }

    private List<StagedQuestionRecord> parsePracticeProblems(Path filePath, Path repoRoot, String batchId) throws IOException {
        List<StagedQuestionRecord> list = new ArrayList<>();
        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        String relPath = repoRoot.relativize(filePath).toString().replace('\\', '/');
        String chapter = filePath.getParent().getFileName().toString();

        // Extract MCQs: ### Q[0-9]+ \n Question \n A. ... \n B. ... \n C. ... \n D. ... \n ✅ Answer: [A-D]
        Pattern mcqPattern = Pattern.compile("(?m)###\\s+Q\\d+\\s*\\n+([^\\n]+)\\s*\\n+A\\.\\s*([^\\n]+)\\s*\\n+B\\.\\s*([^\\n]+)\\s*\\n+C\\.\\s*([^\\n]+)\\s*\\n+D\\.\\s*([^\\n]+)\\s*\\n+✅\\s*Answer:\\s*([A-D])");
        Matcher m = mcqPattern.matcher(content);

        int count = 0;
        while (m.find()) {
            count++;
            String qText = m.group(1).trim();
            String optA = m.group(2).trim();
            String optB = m.group(3).trim();
            String optC = m.group(4).trim();
            String optD = m.group(5).trim();
            String ans = m.group(6).trim();

            String fullDescription = qText + "\n\nA) " + optA + "\nB) " + optB + "\nC) " + optC + "\nD) " + optD;
            String solutionText = ans + ") " + switch (ans) {
                case "A" -> optA;
                case "B" -> optB;
                case "C" -> optC;
                default -> optD;
            };

            StagedQuestionRecord record = new StagedQuestionRecord();
            record.setBatchId(batchId);
            record.setSourceRepo(SOURCE_REPO);
            record.setSourcePath(relPath);
            record.setLicense(LICENSE);
            record.setAuthor(AUTHOR);
            record.setAttribution(ATTRIBUTION);
            record.setCategoryOrSubject("Operating Systems");
            record.setTopicName("Operating Systems & Concurrency");
            record.setTopicSlug("os-concurrency");
            record.setDifficulty(Difficulty.MEDIUM);
            record.setQuestionType(QuestionType.MCQ);
            record.setTargetTable("questions");
            record.setTitle("OS " + cleanChapter(chapter) + " MCQ " + count);
            record.setSlug(("os-mcq-" + cleanChapter(chapter) + "-" + count).toLowerCase().replaceAll("[^a-z0-9]+", "-"));
            record.setQuestionText(fullDescription);
            record.setOptionA(optA);
            record.setOptionB(optB);
            record.setOptionC(optC);
            record.setOptionD(optD);
            record.setCorrectAnswer(ans);
            record.setSolution(solutionText);
            record.setContentHash(ContentDeduplicator.computeHash(qText));

            list.add(record);
        }

        return list;
    }

    private List<StagedQuestionRecord> parseFaqs(Path filePath, Path repoRoot, String batchId) throws IOException {
        List<StagedQuestionRecord> list = new ArrayList<>();
        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        String relPath = repoRoot.relativize(filePath).toString().replace('\\', '/');
        String chapter = filePath.getParent().getFileName().toString();

        // FAQs format: ## Question \n Answer text until next ## or ---
        Pattern faqPattern = Pattern.compile("(?m)^##\\s+([^#\\n]+)\\s*\\n+([\\s\\S]*?)(?=(?:^##\\s+)|\\Z)");
        Matcher m = faqPattern.matcher(content);

        int count = 0;
        while (m.find()) {
            String question = m.group(1).trim();
            String answer = m.group(2).replaceAll("---", "").trim();

            if (question.isBlank() || answer.isBlank() || answer.length() < 10) {
                continue;
            }
            if (question.toLowerCase().contains("frequently asked questions") || question.startsWith("?")) {
                continue;
            }

            count++;
            StagedQuestionRecord record = new StagedQuestionRecord();
            record.setBatchId(batchId);
            record.setSourceRepo(SOURCE_REPO);
            record.setSourcePath(relPath);
            record.setLicense(LICENSE);
            record.setAuthor(AUTHOR);
            record.setAttribution(ATTRIBUTION);
            record.setCategoryOrSubject("Operating Systems");
            record.setTopicName("Operating Systems & Concurrency");
            record.setTopicSlug("os-concurrency");
            record.setDifficulty(Difficulty.EASY);
            record.setQuestionType(QuestionType.CONCEPTUAL);
            record.setTargetTable("questions");
            record.setTitle("OS Concept: " + question);
            record.setSlug(("os-faq-" + cleanChapter(chapter) + "-" + count).toLowerCase().replaceAll("[^a-z0-9]+", "-"));
            record.setQuestionText(question);
            record.setSolution(answer);
            record.setContentHash(ContentDeduplicator.computeHash(question));

            list.add(record);
        }

        return list;
    }

    private List<StagedQuestionRecord> parseSolvedExamples(Path filePath, Path repoRoot, String batchId) throws IOException {
        List<StagedQuestionRecord> list = new ArrayList<>();
        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        String relPath = repoRoot.relativize(filePath).toString().replace('\\', '/');
        String chapter = filePath.getParent().getFileName().toString();

        // Solved examples format: # Example N – ...
        Pattern exPattern = Pattern.compile("(?m)^#\\s+Example\\s+\\d+\\s*[–-]\\s*([^\\n]+)\\s*\\n+([\\s\\S]*?)(?=(?:^#\\s+Example)|\\Z)");
        Matcher m = exPattern.matcher(content);

        int count = 0;
        while (m.find()) {
            count++;
            String title = m.group(1).trim();
            String body = m.group(2).trim();

            if (body.length() < 30) continue;

            StagedQuestionRecord record = new StagedQuestionRecord();
            record.setBatchId(batchId);
            record.setSourceRepo(SOURCE_REPO);
            record.setSourcePath(relPath);
            record.setLicense(LICENSE);
            record.setAuthor(AUTHOR);
            record.setAttribution(ATTRIBUTION);
            record.setCategoryOrSubject("Operating Systems");
            record.setTopicName("Operating Systems & Concurrency");
            record.setTopicSlug("os-concurrency");
            record.setDifficulty(Difficulty.HARD);
            record.setQuestionType(QuestionType.CONCEPTUAL);
            record.setTargetTable("questions");
            record.setTitle("OS Problem: " + title);
            record.setSlug(("os-problem-" + cleanChapter(chapter) + "-" + count).toLowerCase().replaceAll("[^a-z0-9]+", "-"));
            record.setQuestionText("Solve the following Operating Systems problem on " + title + ":\n\n" + body);
            record.setSolution(body);
            record.setContentHash(ContentDeduplicator.computeHash(title + " " + cleanChapter(chapter)));

            list.add(record);
        }

        return list;
    }

    private String cleanChapter(String folder) {
        return folder.replaceAll("^\\d+-", "").replaceAll("-", " ").trim();
    }
}
