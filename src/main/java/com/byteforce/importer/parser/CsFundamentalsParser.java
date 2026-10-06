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
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for CS-Fundamentals by Manish Kumar (MIT License).
 * Extracts core CS placement questions across Computer Networks, DBMS, SQL,
 * Computer Organization & Architecture, Theory of Computation, Operating Systems, and OOP.
 */
public class CsFundamentalsParser {

    private static final Logger log = LoggerFactory.getLogger(CsFundamentalsParser.class);

    private static final String SOURCE_REPO = "https://github.com/manishkumar8312/CS-Fundamentals";
    private static final String LICENSE = "MIT";
    private static final String AUTHOR = "Manish Kumar";
    private static final String ATTRIBUTION = "CS Fundamentals by Manish Kumar (MIT License)";

    private static final Pattern HEADING_PATTERN = Pattern.compile("^##+\\s+(?:Chapter\\s+\\d+[:.]?\\s*|(?:\\d+\\.)*\\d+\\.?\\s*[:–-]?\\s*)?(.+)$");

    public List<StagedQuestionRecord> parseRepository(Path repoRoot, String batchId) {
        List<StagedQuestionRecord> records = new ArrayList<>();
        if (!Files.exists(repoRoot)) {
            log.warn("Repository root does not exist: {}", repoRoot);
            return records;
        }

        try {
            Files.walk(repoRoot)
                    .filter(p -> p.toString().endsWith(".md")
                            && !p.getFileName().toString().equalsIgnoreCase("README.md")
                            && !p.getFileName().toString().equalsIgnoreCase("syllabus.md")
                            && !p.getFileName().toString().equalsIgnoreCase("CODE_OF_CONDUCT.md")
                            && !p.getFileName().toString().equalsIgnoreCase("SECURITY.md")
                            && !p.toString().contains(".github"))
                    .forEach(filePath -> {
                        try {
                            records.addAll(parseChapterFile(filePath, repoRoot, batchId));
                        } catch (Exception e) {
                            log.error("Failed to parse CS Fundamentals chapter: " + filePath, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Error traversing CS Fundamentals: " + repoRoot, e);
        }

        log.info("CsFundamentalsParser extracted {} questions from {}", records.size(), repoRoot);
        return records;
    }

    private List<StagedQuestionRecord> parseChapterFile(Path filePath, Path repoRoot, String batchId) throws IOException {
        List<StagedQuestionRecord> list = new ArrayList<>();
        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        String relPath = repoRoot.relativize(filePath).toString().replace('\\', '/');

        // Determine Subject and Topic from path
        String subPath = relPath.toLowerCase(Locale.ROOT);
        String subject = "DSA";
        String topicSlug = "dsa";
        String topicName = "Data Structures & Algorithms";
        QuestionType qType = QuestionType.CONCEPTUAL;

        if (subPath.contains("computer network")) {
            subject = "Computer Networks";
            topicSlug = "network-architecture-osi";
            topicName = "Network Architecture & OSI Model";
            if (subPath.contains("routing") || subPath.contains("tcp") || subPath.contains("transport") || subPath.contains("ip")) {
                topicSlug = "tcp-ip-routing";
                topicName = "TCP/IP & Routing Protocols";
            }
        } else if (subPath.contains("dbms")) {
            subject = "DBMS";
            if (subPath.contains("sql")) {
                topicSlug = "sql-databases";
                topicName = "SQL & Database Queries";
                qType = QuestionType.SQL;
            } else if (subPath.contains("transaction") || subPath.contains("concurrency") || subPath.contains("recovery")) {
                topicSlug = "transactions-concurrency";
                topicName = "Transactions & Concurrency Control";
            } else {
                topicSlug = "sql-databases";
                topicName = "SQL & Database Queries";
            }
        } else if (subPath.contains("computer organization")) {
            subject = "COA";
            if (subPath.contains("memory") || subPath.contains("cache")) {
                topicSlug = "memory-hierarchy-cache";
                topicName = "Memory Hierarchy & Cache Mapping";
            } else {
                topicSlug = "computer-architecture-pipelining";
                topicName = "Computer Architecture & Pipelining";
            }
        } else if (subPath.contains("theory of computation") || subPath.contains("compiler")) {
            subject = "TOC";
            if (subPath.contains("turing") || subPath.contains("decidability") || subPath.contains("complexity")) {
                topicSlug = "turing-machines-decidability";
                topicName = "Turing Machines & Decidability";
            } else {
                topicSlug = "automata-formal-languages";
                topicName = "Automata & Formal Languages";
            }
        } else if (subPath.contains("object oriented")) {
            subject = "Java/OOP";
            topicSlug = "oop-java";
            topicName = "OOP & Java Concepts";
        } else if (subPath.contains("operating system")) {
            subject = "Operating Systems";
            topicSlug = "os-concurrency";
            topicName = "Operating Systems & Concurrency";
        } else if (subPath.contains("data structure") || subPath.contains("algorithm")) {
            subject = "DSA";
            if (subPath.contains("tree") || subPath.contains("graph") || subPath.contains("dp") || subPath.contains("dynamic")) {
                topicSlug = "trees-graphs-dp";
                topicName = "Trees, Graphs & Dynamic Programming";
            } else {
                topicSlug = "dsa";
                topicName = "Data Structures & Algorithms";
            }
        }

        // Split into sections by clean newline
        String cleanContent = content.replace("\r", "");
        String[] lines = cleanContent.split("\n");
        String currentSection = null;
        StringBuilder sectionBody = new StringBuilder();
        int sectionIndex = 0;

        for (String line : lines) {
            String trimmedLine = line.trim();
            Matcher m = HEADING_PATTERN.matcher(trimmedLine);
            if (m.matches()) {
                if (currentSection != null && !sectionBody.isEmpty()) {
                    StagedQuestionRecord rec = createRecord(currentSection, sectionBody.toString(), subject,
                            topicName, topicSlug, qType, relPath, batchId, ++sectionIndex);
                    if (rec != null) list.add(rec);
                    sectionBody.setLength(0);
                }
                currentSection = m.group(1).trim();
            } else {
                sectionBody.append(line).append("\n");
            }
        }

        if (currentSection != null && !sectionBody.isEmpty()) {
            StagedQuestionRecord rec = createRecord(currentSection, sectionBody.toString(), subject,
                    topicName, topicSlug, qType, relPath, batchId, ++sectionIndex);
            if (rec != null) list.add(rec);
        }

        return list;
    }

    private StagedQuestionRecord createRecord(String sectionTitle, String sectionContent, String subject,
                                              String topicName, String topicSlug, QuestionType qType,
                                              String relPath, String batchId, int index) {
        String body = sectionContent.replaceAll("```mermaid[\\s\\S]*?```", "").trim();
        if (body.length() < 60
                || sectionTitle.equalsIgnoreCase("Summary")
                || sectionTitle.equalsIgnoreCase("Summary Table")
                || sectionTitle.equalsIgnoreCase("Introduction")
                || sectionTitle.toLowerCase(Locale.ROOT).contains("syllabus")
                || sectionTitle.toLowerCase(Locale.ROOT).contains("table of contents")) {
            return null;
        }

        String questionPrompt;
        if (sectionTitle.endsWith("?")) {
            questionPrompt = sectionTitle;
        } else if (qType == QuestionType.SQL) {
            questionPrompt = "Explain SQL usage, syntax, and query optimization for: " + sectionTitle + ".";
        } else {
            questionPrompt = "Explain the concepts, core mechanisms, and practical applications of " + sectionTitle + ".";
        }

        Difficulty difficulty = index % 3 == 0 ? Difficulty.HARD : (index % 2 == 0 ? Difficulty.MEDIUM : Difficulty.EASY);

        StagedQuestionRecord rec = new StagedQuestionRecord();
        rec.setBatchId(batchId);
        rec.setSourceRepo(SOURCE_REPO);
        rec.setSourcePath(relPath);
        rec.setLicense(LICENSE);
        rec.setAuthor(AUTHOR);
        rec.setAttribution(ATTRIBUTION);
        rec.setCategoryOrSubject(subject);
        rec.setTopicName(topicName);
        rec.setTopicSlug(topicSlug);
        rec.setDifficulty(difficulty);
        rec.setQuestionType(qType);
        rec.setTargetTable("questions");
        rec.setTitle(sectionTitle);
        rec.setSlug((topicSlug + "-" + sectionTitle).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-"));
        rec.setQuestionText(questionPrompt);
        rec.setSolution(body);
        rec.setContentHash(ContentDeduplicator.computeHash(sectionTitle + " " + topicSlug));

        return rec;
    }
}
