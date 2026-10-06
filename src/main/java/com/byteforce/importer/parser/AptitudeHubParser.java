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
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for the MIT-licensed CSE-Aptitude-Test-Practice-Hub by Rohan Mistry.
 * Extracts Quantitative Aptitude, Logical Reasoning, Verbal Ability, Data Interpretation,
 * Abstract Reasoning, and Technical Aptitude questions with complete mathematical/logical solutions.
 */
public class AptitudeHubParser {

    private static final Logger log = LoggerFactory.getLogger(AptitudeHubParser.class);

    private static final String SOURCE_REPO = "https://github.com/rohanmistry231/CSE-Aptitude-Test-Practice-Hub";
    private static final String LICENSE = "MIT";
    private static final String AUTHOR = "Rohan Mistry";
    private static final String ATTRIBUTION = "CSE Aptitude Test Practice Hub by Rohan Mistry (MIT License)";

    private static final Pattern SECTION_PATTERN = Pattern.compile("(?m)^##\\s+(.+)$");
    private static final Pattern QUESTION_PATTERN = Pattern.compile("(?m)^\\d+\\.\\s+\\*\\*Question\\*\\*:\\s*(.*?)\\s*\\n\\s*\\*\\*Solution\\*\\*:\\s*\\n?([\\s\\S]*?)\\s*\\*\\*Answer\\*\\*:\\s*(.*)$");

    public List<StagedQuestionRecord> parseRepository(Path repoRoot, String batchId) {
        List<StagedQuestionRecord> records = new ArrayList<>();
        if (!Files.exists(repoRoot)) {
            log.warn("Repository root does not exist: {}", repoRoot);
            return records;
        }

        try {
            Files.walk(repoRoot)
                    .filter(p -> p.toString().endsWith("README.md") && !p.equals(repoRoot.resolve("README.md")))
                    .forEach(filePath -> {
                        try {
                            records.addAll(parseFile(filePath, repoRoot, batchId));
                        } catch (Exception e) {
                            log.error("Failed to parse file: " + filePath, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Error traversing repository files in " + repoRoot, e);
        }

        log.info("AptitudeHubParser extracted {} questions from {}", records.size(), repoRoot);
        return records;
    }

    public List<StagedQuestionRecord> parseFile(Path filePath, Path repoRoot, String batchId) throws IOException {
        List<StagedQuestionRecord> results = new ArrayList<>();
        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        String relPath = repoRoot.relativize(filePath).toString().replace('\\', '/');

        // Determine category and difficulty from directory hierarchy
        String category = "QUANTITATIVE";
        String catFolder = relPath.toLowerCase();
        if (catFolder.contains("02 logical") || catFolder.contains("logical reasoning")) {
            category = "LOGICAL";
        } else if (catFolder.contains("03 verbal") || catFolder.contains("verbal ability")) {
            category = "VERBAL";
        } else if (catFolder.contains("04 data interpretation") || catFolder.contains("data interpretation")) {
            category = "QUANTITATIVE";
        } else if (catFolder.contains("05 abstract") || catFolder.contains("abstract reasoning")) {
            category = "LOGICAL";
        } else if (catFolder.contains("06 technical") || catFolder.contains("technical aptitude")) {
            category = "LOGICAL";
        }

        Difficulty difficulty = Difficulty.MEDIUM;
        if (catFolder.contains("01 basic") || catFolder.contains("basic")) {
            difficulty = Difficulty.EASY;
        } else if (catFolder.contains("03 advance") || catFolder.contains("advance") || catFolder.contains("hard")) {
            difficulty = Difficulty.HARD;
        }

        // Split by sections to capture subtopics
        String[] lines = content.split("\n");
        String currentTopic = "General Aptitude";
        StringBuilder sectionBuffer = new StringBuilder();

        for (String line : lines) {
            Matcher sm = SECTION_PATTERN.matcher(line);
            if (sm.matches()) {
                if (!sectionBuffer.isEmpty()) {
                    results.addAll(extractQuestions(sectionBuffer.toString(), currentTopic, category, difficulty, relPath, batchId));
                    sectionBuffer.setLength(0);
                }
                currentTopic = sm.group(1).trim();
                // Clean section topic title e.g. "Programming Fundamentals (1–20)" -> "Programming Fundamentals"
                currentTopic = currentTopic.replaceAll("\\s*\\([^)]*\\)", "").trim();
            } else {
                sectionBuffer.append(line).append("\n");
            }
        }
        if (!sectionBuffer.isEmpty()) {
            results.addAll(extractQuestions(sectionBuffer.toString(), currentTopic, category, difficulty, relPath, batchId));
        }

        return results;
    }

    private List<StagedQuestionRecord> extractQuestions(String block, String topic, String category,
                                                        Difficulty difficulty, String relPath, String batchId) {
        List<StagedQuestionRecord> list = new ArrayList<>();
        Matcher m = QUESTION_PATTERN.matcher(block);

        int qIndex = 0;
        while (m.find()) {
            qIndex++;
            String rawQuestion = m.group(1).trim();
            String solution = m.group(2).trim();
            String rawAnswer = m.group(3).trim();

            if (rawQuestion.isBlank() || rawAnswer.isBlank()) {
                continue;
            }

            StagedQuestionRecord record = new StagedQuestionRecord();
            record.setBatchId(batchId);
            record.setSourceRepo(SOURCE_REPO);
            record.setSourcePath(relPath);
            record.setLicense(LICENSE);
            record.setAuthor(AUTHOR);
            record.setAttribution(ATTRIBUTION);
            record.setCategoryOrSubject(category);
            record.setTopicName(topic);
            record.setTopicSlug(topic.toLowerCase().replaceAll("[^a-z0-9]+", "-"));
            record.setDifficulty(difficulty);
            record.setQuestionType(QuestionType.APTITUDE);
            record.setTargetTable("aptitude_questions");
            record.setSolution(solution);

            // Check if options are already embedded in the question string: e.g. "A) ... B) ... C) ... D) ..."
            Pattern optionPattern = Pattern.compile("(?i)A\\)\\s*(.*?)\\s*B\\)\\s*(.*?)\\s*C\\)\\s*(.*?)\\s*D\\)\\s*(.*)");
            Matcher optMatcher = optionPattern.matcher(rawQuestion);

            String optA = "", optB = "", optC = "", optD = "";
            String correctAnswerLetter = "A";
            String cleanQuestion = rawQuestion;

            if (optMatcher.find()) {
                optA = optMatcher.group(1).trim();
                optB = optMatcher.group(2).trim();
                optC = optMatcher.group(3).trim();
                optD = optMatcher.group(4).trim();
                cleanQuestion = rawQuestion.substring(0, optMatcher.start()).trim();
                if (cleanQuestion.endsWith(":")) {
                    cleanQuestion = cleanQuestion.substring(0, cleanQuestion.length() - 1).trim();
                }

                // Determine correct answer letter from rawAnswer e.g. "B) Honest" or "B"
                String cleanAns = rawAnswer.trim().toUpperCase();
                if (cleanAns.startsWith("A")) correctAnswerLetter = "A";
                else if (cleanAns.startsWith("B")) correctAnswerLetter = "B";
                else if (cleanAns.startsWith("C")) correctAnswerLetter = "C";
                else if (cleanAns.startsWith("D")) correctAnswerLetter = "D";
                else {
                    // Match against option texts
                    if (optA.equalsIgnoreCase(cleanAns)) correctAnswerLetter = "A";
                    else if (optB.equalsIgnoreCase(cleanAns)) correctAnswerLetter = "B";
                    else if (optC.equalsIgnoreCase(cleanAns)) correctAnswerLetter = "C";
                    else if (optD.equalsIgnoreCase(cleanAns)) correctAnswerLetter = "D";
                    else correctAnswerLetter = "A";
                }
            } else {
                // Generate realistic distractors based on the actual answer
                String cleanAns = rawAnswer.replaceAll("(?i)^answer\\s*:\\s*", "").trim();
                OptionSet optionSet = generateDistractors(cleanAns, qIndex);
                optA = optionSet.optionA;
                optB = optionSet.optionB;
                optC = optionSet.optionC;
                optD = optionSet.optionD;
                correctAnswerLetter = optionSet.correctAnswerLetter;
            }

            record.setQuestionText(cleanQuestion);
            record.setOptionA(optA);
            record.setOptionB(optB);
            record.setOptionC(optC);
            record.setOptionD(optD);
            record.setCorrectAnswer(correctAnswerLetter);
            record.setContentHash(ContentDeduplicator.computeHash(cleanQuestion));

            list.add(record);
        }

        return list;
    }

    private static class OptionSet {
        String optionA;
        String optionB;
        String optionC;
        String optionD;
        String correctAnswerLetter;
    }

    private OptionSet generateDistractors(String trueAnswer, int seedIndex) {
        OptionSet set = new OptionSet();
        List<String> options = new ArrayList<>();
        options.add(trueAnswer);

        // Try numeric distractor generation
        String numStr = trueAnswer.replaceAll("[^0-9.-]", "").trim();
        boolean numeric = false;
        double val = 0;
        if (!numStr.isEmpty() && !numStr.equals("-") && !numStr.equals(".")) {
            try {
                val = Double.parseDouble(numStr);
                numeric = true;
            } catch (NumberFormatException ignored) {}
        }

        if (numeric && val != 0) {
            String suffix = trueAnswer.replaceAll("^[0-9.\\-\\s]+", "").trim();
            if (!suffix.isEmpty()) suffix = " " + suffix;

            boolean isInt = (val == Math.floor(val));
            double d1 = val > 0 ? (isInt ? Math.round(val * 0.8) : val * 0.8) : val - 2;
            double d2 = val > 0 ? (isInt ? Math.round(val * 1.2) : val * 1.2) : val + 2;
            double d3 = val > 0 ? (isInt ? Math.round(val * 1.5) : val * 1.5) : val + 5;

            if (d1 == val) d1 = val - 1;
            if (d2 == val || d2 == d1) d2 = val + 1;
            if (d3 == val || d3 == d2 || d3 == d1) d3 = val + 3;

            options.add(formatNum(d1, isInt) + suffix);
            options.add(formatNum(d2, isInt) + suffix);
            options.add(formatNum(d3, isInt) + suffix);
        } else {
            // Textual / Symbolic distractors
            if (trueAnswer.equalsIgnoreCase("true")) {
                options.add("False");
                options.add("Cannot be determined");
                options.add("None of the above");
            } else if (trueAnswer.equalsIgnoreCase("false")) {
                options.add("True");
                options.add("Cannot be determined");
                options.add("Both true and false");
            } else {
                options.add("None of these");
                options.add("Cannot be determined");
                options.add("Insufficient data");
            }
        }

        // Deterministic rotation based on seedIndex so correct answer is distributed across A, B, C, D
        int correctPos = seedIndex % 4;
        String correctVal = options.get(0);
        String other1 = options.get(1);
        String other2 = options.get(2);
        String other3 = options.get(3);

        List<String> slotList = new ArrayList<>(List.of(other1, other2, other3));
        slotList.add(correctPos, correctVal);

        set.optionA = slotList.get(0);
        set.optionB = slotList.get(1);
        set.optionC = slotList.get(2);
        set.optionD = slotList.get(3);
        set.correctAnswerLetter = switch (correctPos) {
            case 0 -> "A";
            case 1 -> "B";
            case 2 -> "C";
            default -> "D";
        };

        return set;
    }

    private String formatNum(double d, boolean isInt) {
        if (isInt) {
            return String.valueOf((long) d);
        } else {
            return String.format(java.util.Locale.ROOT, "%.2f", d);
        }
    }
}
