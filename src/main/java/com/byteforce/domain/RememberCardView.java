package com.byteforce.domain;

import java.util.Objects;

/**
 * Presentation view model combining a {@link RememberItem} with its parent
 * {@link Concept}, topic, and subject metadata for the /remember browser view.
 */
public final class RememberCardView {

    private final RememberItem item;
    private final Concept concept;
    private final String subjectId;
    private final String subjectName;
    private final String topicName;

    public RememberCardView(RememberItem item, Concept concept, String subjectId, String subjectName, String topicName) {
        this.item = Objects.requireNonNull(item, "item must not be null");
        this.concept = Objects.requireNonNull(concept, "concept must not be null");
        this.subjectId = subjectId != null ? subjectId : "";
        this.subjectName = subjectName != null ? subjectName : "";
        this.topicName = topicName != null ? topicName : "";
    }

    public RememberItem getItem() {
        return item;
    }

    public Concept getConcept() {
        return concept;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public String getTopicName() {
        return topicName;
    }

    public long getId() {
        return item.getId();
    }

    public RememberItemType getType() {
        return item.getType();
    }

    public String getContent() {
        return item.getContent();
    }

    public long getConceptId() {
        return concept.getId();
    }

    public String getConceptTitle() {
        return concept.getTitle();
    }
}
