package com.byteforce.domain;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Summary view model for concept-level memory status.
 * Connects Learn, Remember, Brain Maps, and Practice.
 */
public final class ConceptMemoryStatus {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy")
            .withZone(ZoneId.of("UTC"));

    private final long conceptId;
    private final String conceptTitle;
    private final boolean enrolled;
    private final int totalItems;
    private final int reviewedItems;
    private final int dueItems;
    private final Instant nextDueAt;
    private final Instant lastReviewedAt;
    private final int totalReviews;
    private final int successfulReviews;
    private final double averageStability;
    private final double averageDifficulty;
    private final ReviewState dominantState;
    private final List<RelatedConceptView> relatedConcepts;

    public ConceptMemoryStatus(long conceptId,
                               String conceptTitle,
                               boolean enrolled,
                               int totalItems,
                               int reviewedItems,
                               int dueItems,
                               Instant nextDueAt,
                               Instant lastReviewedAt,
                               int totalReviews,
                               int successfulReviews,
                               double averageStability,
                               double averageDifficulty,
                               ReviewState dominantState,
                               List<RelatedConceptView> relatedConcepts) {
        this.conceptId = conceptId;
        this.conceptTitle = conceptTitle;
        this.enrolled = enrolled;
        this.totalItems = totalItems;
        this.reviewedItems = reviewedItems;
        this.dueItems = dueItems;
        this.nextDueAt = nextDueAt;
        this.lastReviewedAt = lastReviewedAt;
        this.totalReviews = totalReviews;
        this.successfulReviews = successfulReviews;
        this.averageStability = averageStability;
        this.averageDifficulty = averageDifficulty;
        this.dominantState = dominantState;
        this.relatedConcepts = relatedConcepts != null ? relatedConcepts : List.of();
    }

    public static ConceptMemoryStatus notEnrolled(long conceptId, String conceptTitle, int totalItems, List<RelatedConceptView> relatedConcepts) {
        return new ConceptMemoryStatus(
                conceptId,
                conceptTitle,
                false,
                totalItems,
                0,
                0,
                null,
                null,
                0,
                0,
                0.0,
                0.0,
                ReviewState.NEW,
                relatedConcepts
        );
    }

    public long getConceptId() {
        return conceptId;
    }

    public String getConceptTitle() {
        return conceptTitle;
    }

    public boolean isEnrolled() {
        return enrolled;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getReviewedItems() {
        return reviewedItems;
    }

    public int getDueItems() {
        return dueItems;
    }

    public Instant getNextDueAt() {
        return nextDueAt;
    }

    public Instant getLastReviewedAt() {
        return lastReviewedAt;
    }

    public int getTotalReviews() {
        return totalReviews;
    }

    public int getSuccessfulReviews() {
        return successfulReviews;
    }

    public double getAverageStability() {
        return averageStability;
    }

    public double getAverageDifficulty() {
        return averageDifficulty;
    }

    public ReviewState getDominantState() {
        return dominantState;
    }

    public List<RelatedConceptView> getRelatedConcepts() {
        return relatedConcepts;
    }

    public String getFormattedNextDue() {
        if (nextDueAt == null) {
            return "Not scheduled";
        }
        Instant now = Instant.now();
        if (!nextDueAt.isAfter(now)) {
            return "Due now";
        }
        long hours = java.time.Duration.between(now, nextDueAt).toHours();
        if (hours < 24) {
            return "Due today (" + hours + "h)";
        }
        long days = java.time.Duration.between(now, nextDueAt).toDays();
        if (days == 1) {
            return "Due tomorrow";
        }
        if (days < 30) {
            return "Due in " + days + " days";
        }
        return "Due " + DATE_FMT.format(nextDueAt);
    }

    public String getFormattedLastReviewed() {
        if (lastReviewedAt == null) {
            return "Never";
        }
        return DATE_FMT.format(lastReviewedAt);
    }
}
