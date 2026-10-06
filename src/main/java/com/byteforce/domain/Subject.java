package com.byteforce.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable domain entity representing a high-level academic/placement subject
 * (e.g. "Data Structures", "DBMS", "Operating Systems") that contains multiple topics.
 */
public final class Subject {

    private final String id;
    private final String name;
    private final String description;
    private final int displayOrder;
    private final List<Topic> topics;

    public Subject(String id, String name, String description, int displayOrder, List<Topic> topics) {
        Objects.requireNonNull(id, "id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        this.id = id.trim().toLowerCase();

        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.name = name.trim();

        this.description = description != null ? description.trim() : "";
        this.displayOrder = displayOrder;
        this.topics = topics != null ? Collections.unmodifiableList(new ArrayList<>(topics)) : List.of();
    }

    private Subject(String id, String name, String description, int displayOrder, List<Topic> topics, boolean formEmpty) {
        this.id = id != null ? id.trim().toLowerCase() : "";
        this.name = name != null ? name.trim() : "";
        this.description = description != null ? description.trim() : "";
        this.displayOrder = displayOrder;
        this.topics = topics != null ? Collections.unmodifiableList(new ArrayList<>(topics)) : List.of();
    }

    public static Subject empty() {
        return new Subject("", "", "", 0, List.of(), true);
    }

    public static Subject create(String id, String name, String description, int displayOrder, List<Topic> topics) {
        return new Subject(id, name, description, displayOrder, topics);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public List<Topic> getTopics() {
        return topics;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subject subject = (Subject) o;
        return id.equals(subject.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Subject{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", topicsCount=" + topics.size() +
                '}';
    }
}
