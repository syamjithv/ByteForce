package com.byteforce.domain;

import java.util.Map;
import java.util.Objects;

/**
 * Presentation view model combining a student's personal review state with the
 * underlying RememberItem content, concept, topic, and scheduler rating previews.
 */
public final class MemoryReviewItemView {

    private final UserRememberReview review;
    private final RememberItem rememberItem;
    private final Concept concept;
    private final Topic topic;
    private final Subject subject;
    private final Map<ReviewRating, SchedulingResult> ratingPreviews;
    private final double currentRetrievability;

    public MemoryReviewItemView(UserRememberReview review,
                                RememberItem rememberItem,
                                Concept concept,
                                Topic topic,
                                Subject subject,
                                Map<ReviewRating, SchedulingResult> ratingPreviews,
                                double currentRetrievability) {
        this.review = review;
        this.rememberItem = Objects.requireNonNull(rememberItem, "rememberItem must not be null");
        this.concept = Objects.requireNonNull(concept, "concept must not be null");
        this.topic = topic;
        this.subject = subject;
        this.ratingPreviews = ratingPreviews != null ? ratingPreviews : Map.of();
        this.currentRetrievability = currentRetrievability;
    }

    public UserRememberReview getReview() {
        return review;
    }

    public RememberItem getRememberItem() {
        return rememberItem;
    }

    public Concept getConcept() {
        return concept;
    }

    public Topic getTopic() {
        return topic;
    }

    public Subject getSubject() {
        return subject;
    }

    public Map<ReviewRating, SchedulingResult> getRatingPreviews() {
        return ratingPreviews;
    }

    public double getCurrentRetrievability() {
        return currentRetrievability;
    }

    public String getFormattedRetrievability() {
        if (review == null || review.getReviewCount() == 0) {
            return "New";
        }
        return Math.round(currentRetrievability * 100) + "%";
    }

    public String getTopicName() {
        if (topic != null && topic.getName() != null) {
            return topic.getName();
        }
        return concept.getTopicName() != null ? concept.getTopicName() : "Core Computer Science";
    }

    public String getSubjectName() {
        if (subject != null && subject.getName() != null) {
            return subject.getName();
        }
        return "Computer Science";
    }
}
