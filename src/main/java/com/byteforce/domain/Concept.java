package com.byteforce.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable domain entity representing a placement preparation concept
 * within a specific topic (e.g. "Array Traversal", "JOIN Operations", "Process States").
 */
public final class Concept {

    private final long id;
    private final long topicId;
    private final String topicName;
    private final String title;
    private final String shortExplanation;
    private final List<String> keyPoints;
    private final String example;
    private final List<LearningResource> resources;

    public Concept(long id,
                   long topicId,
                   String topicName,
                   String title,
                   String shortExplanation,
                   List<String> keyPoints,
                   String example,
                   List<LearningResource> resources) {
        this.id = id;
        this.topicId = topicId;
        this.topicName = topicName != null ? topicName.trim() : "";

        Objects.requireNonNull(title, "title must not be null");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        this.title = title.trim();

        Objects.requireNonNull(shortExplanation, "shortExplanation must not be null");
        this.shortExplanation = shortExplanation.trim();

        this.keyPoints = keyPoints != null ? Collections.unmodifiableList(new ArrayList<>(keyPoints)) : List.of();
        this.example = example != null ? example.trim() : "";
        this.resources = resources != null ? Collections.unmodifiableList(new ArrayList<>(resources)) : List.of();
    }

    private Concept(long id, long topicId, String topicName, String title, String shortExplanation,
                    List<String> keyPoints, String example, List<LearningResource> resources, boolean formEmpty) {
        this.id = id;
        this.topicId = topicId;
        this.topicName = topicName != null ? topicName.trim() : "";
        this.title = title != null ? title.trim() : "";
        this.shortExplanation = shortExplanation != null ? shortExplanation.trim() : "";
        this.keyPoints = keyPoints != null ? Collections.unmodifiableList(new ArrayList<>(keyPoints)) : List.of();
        this.example = example != null ? example.trim() : "";
        this.resources = resources != null ? Collections.unmodifiableList(new ArrayList<>(resources)) : List.of();
    }

    public static Concept empty(long topicId) {
        return new Concept(0, topicId, "", "", "", List.of(), "", List.of(), true);
    }

    public static Concept create(long id,
                                 long topicId,
                                 String topicName,
                                 String title,
                                 String shortExplanation,
                                 List<String> keyPoints,
                                 String example,
                                 List<LearningResource> resources) {
        return new Concept(id, topicId, topicName, title, shortExplanation, keyPoints, example, resources);
    }

    public long getId() {
        return id;
    }

    public long getTopicId() {
        return topicId;
    }

    public String getTopicName() {
        return topicName;
    }

    public String getTitle() {
        return title;
    }

    public String getShortExplanation() {
        return shortExplanation;
    }

    public List<String> getKeyPoints() {
        return keyPoints;
    }

    public String getExample() {
        return example;
    }

    public List<LearningResource> getResources() {
        return resources;
    }

    public Concept withId(long newId) {
        return new Concept(newId, this.topicId, this.topicName, this.title, this.shortExplanation, this.keyPoints, this.example, this.resources);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Concept concept = (Concept) o;
        return id == concept.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Concept{" +
                "id=" + id +
                ", topicId=" + topicId +
                ", topicName='" + topicName + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
