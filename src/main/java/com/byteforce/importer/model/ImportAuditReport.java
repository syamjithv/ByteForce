package com.byteforce.importer.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Detailed report capturing content acquisition, validation, deduplication,
 * and import statistics across repositories and topic domains.
 */
public class ImportAuditReport {

    private final AtomicInteger repositoriesInspected = new AtomicInteger(0);
    private final AtomicInteger repositoriesAccepted = new AtomicInteger(0);
    private final AtomicInteger referenceOnly = new AtomicInteger(0);
    private final AtomicInteger rejected = new AtomicInteger(0);

    private final AtomicInteger rawCandidates = new AtomicInteger(0);
    private final AtomicInteger invalid = new AtomicInteger(0);
    private final AtomicInteger duplicatesRemoved = new AtomicInteger(0);
    private final AtomicInteger provenanceRejected = new AtomicInteger(0);
    private final AtomicInteger validImportable = new AtomicInteger(0);

    private final AtomicInteger totalImportedQuestions = new AtomicInteger(0);
    private final AtomicInteger totalImportedAptitude = new AtomicInteger(0);

    private long existingQuestionsBefore = 0;
    private long existingAptitudeBefore = 0;
    private long finalQuestionsAfter = 0;
    private long finalAptitudeAfter = 0;

    private final Map<String, AtomicInteger> categoryCounts = new LinkedHashMap<>();

    public ImportAuditReport() {
        String[] categories = {
                "DSA", "Java/OOP", "DBMS/SQL", "Operating Systems",
                "Computer Networks", "COA", "TOC", "Software Engineering",
                "Quantitative Aptitude", "Logical Reasoning", "Verbal Ability",
                "Data Interpretation", "Abstract Reasoning", "Technical Aptitude",
                "Behavioral/HR", "System Design"
        };
        for (String cat : categories) {
            categoryCounts.put(cat, new AtomicInteger(0));
        }
    }

    public void incrementCategory(String category) {
        categoryCounts.computeIfAbsent(category, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public int getCategoryCount(String category) {
        AtomicInteger count = categoryCounts.get(category);
        return count != null ? count.get() : 0;
    }

    public Map<String, Integer> getAllCategoryCounts() {
        Map<String, Integer> map = new LinkedHashMap<>();
        categoryCounts.forEach((k, v) -> map.put(k, v.get()));
        return Collections.unmodifiableMap(map);
    }

    public int incrementRepositoriesInspected() { return repositoriesInspected.incrementAndGet(); }
    public int incrementRepositoriesAccepted() { return repositoriesAccepted.incrementAndGet(); }
    public int incrementReferenceOnly() { return referenceOnly.incrementAndGet(); }
    public int incrementRejected() { return rejected.incrementAndGet(); }

    public int incrementRawCandidates() { return rawCandidates.incrementAndGet(); }
    public int incrementInvalid() { return invalid.incrementAndGet(); }
    public int incrementDuplicatesRemoved() { return duplicatesRemoved.incrementAndGet(); }
    public int incrementProvenanceRejected() { return provenanceRejected.incrementAndGet(); }
    public int incrementValidImportable() { return validImportable.incrementAndGet(); }

    public int incrementImportedQuestions() { return totalImportedQuestions.incrementAndGet(); }
    public int incrementImportedAptitude() { return totalImportedAptitude.incrementAndGet(); }

    public int getRepositoriesInspected() { return repositoriesInspected.get(); }
    public int getRepositoriesAccepted() { return repositoriesAccepted.get(); }
    public int getReferenceOnly() { return referenceOnly.get(); }
    public int getRejected() { return rejected.get(); }

    public int getRawCandidates() { return rawCandidates.get(); }
    public int getInvalid() { return invalid.get(); }
    public int getDuplicatesRemoved() { return duplicatesRemoved.get(); }
    public int getProvenanceRejected() { return provenanceRejected.get(); }
    public int getValidImportable() { return validImportable.get(); }

    public int getTotalImportedQuestions() { return totalImportedQuestions.get(); }
    public int getTotalImportedAptitude() { return totalImportedAptitude.get(); }
    public int getTotalImported() { return totalImportedQuestions.get() + totalImportedAptitude.get(); }

    public long getExistingQuestionsBefore() { return existingQuestionsBefore; }
    public void setExistingQuestionsBefore(long existingQuestionsBefore) { this.existingQuestionsBefore = existingQuestionsBefore; }

    public long getExistingAptitudeBefore() { return existingAptitudeBefore; }
    public void setExistingAptitudeBefore(long existingAptitudeBefore) { this.existingAptitudeBefore = existingAptitudeBefore; }

    public long getFinalQuestionsAfter() { return finalQuestionsAfter; }
    public void setFinalQuestionsAfter(long finalQuestionsAfter) { this.finalQuestionsAfter = finalQuestionsAfter; }

    public long getFinalAptitudeAfter() { return finalAptitudeAfter; }
    public void setFinalAptitudeAfter(long finalAptitudeAfter) { this.finalAptitudeAfter = finalAptitudeAfter; }
}
