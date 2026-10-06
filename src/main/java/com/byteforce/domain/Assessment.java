package com.byteforce.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable domain entity representing a timed assessment / mock test.
 */
public final class Assessment {

    private final long id;
    private final String title;
    private final String description;
    private final int durationMinutes;
    private final int totalMarks;
    private final boolean isActive;
    private final Difficulty difficulty;
    private final Long topicId;
    private final Instant createdAt;

    public Assessment(long id, String title, String description, int durationMinutes,
                      int totalMarks, boolean isActive, Difficulty difficulty,
                      Long topicId, Instant createdAt) {
        this.id = id;

        Objects.requireNonNull(title, "title must not be null");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        this.title = title.trim();

        this.description = description;

        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("durationMinutes must be positive");
        }
        this.durationMinutes = durationMinutes;

        if (totalMarks <= 0) {
            throw new IllegalArgumentException("totalMarks must be positive");
        }
        this.totalMarks = totalMarks;

        this.isActive = isActive;
        this.difficulty = difficulty != null ? difficulty : Difficulty.MEDIUM;
        this.topicId = topicId;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    private Assessment(long id, String title, String description, int durationMinutes,
                       int totalMarks, boolean isActive, Difficulty difficulty,
                       Long topicId, Instant createdAt, boolean formEmpty) {
        this.id = id;
        this.title = title != null ? title.trim() : "";
        this.description = description != null ? description : "";
        this.durationMinutes = durationMinutes > 0 ? durationMinutes : 30;
        this.totalMarks = totalMarks > 0 ? totalMarks : 30;
        this.isActive = isActive;
        this.difficulty = difficulty != null ? difficulty : Difficulty.MEDIUM;
        this.topicId = topicId;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static Assessment empty() {
        return new Assessment(0, "", "", 30, 30, true, Difficulty.MEDIUM, null, Instant.now(), true);
    }

    public static Assessment create(String title, String description, int durationMinutes,
                                    int totalMarks, Difficulty difficulty, Long topicId) {
        return new Assessment(0, title, description, durationMinutes, totalMarks, true, difficulty, topicId, Instant.now());
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public int getTotalMarks() {
        return totalMarks;
    }

    public boolean isActive() {
        return isActive;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public Long getTopicId() {
        return topicId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Assessment withId(long newId) {
        return new Assessment(newId, this.title, this.description, this.durationMinutes,
                this.totalMarks, this.isActive, this.difficulty, this.topicId, this.createdAt);
    }

    public Assessment withTitle(String newTitle) {
        return new Assessment(this.id, newTitle, this.description, this.durationMinutes, this.totalMarks, this.isActive, this.difficulty, this.topicId, this.createdAt);
    }

    public Assessment withDescription(String newDescription) {
        return new Assessment(this.id, this.title, newDescription, this.durationMinutes, this.totalMarks, this.isActive, this.difficulty, this.topicId, this.createdAt);
    }

    public Assessment withDurationMinutes(int newDurationMinutes) {
        return new Assessment(this.id, this.title, this.description, newDurationMinutes, this.totalMarks, this.isActive, this.difficulty, this.topicId, this.createdAt);
    }

    public Assessment withTotalMarks(int newTotalMarks) {
        return new Assessment(this.id, this.title, this.description, this.durationMinutes, newTotalMarks, this.isActive, this.difficulty, this.topicId, this.createdAt);
    }

    public Assessment withActive(boolean newIsActive) {
        return new Assessment(this.id, this.title, this.description, this.durationMinutes, this.totalMarks, newIsActive, this.difficulty, this.topicId, this.createdAt);
    }

    public Assessment withDifficulty(Difficulty newDifficulty) {
        return new Assessment(this.id, this.title, this.description, this.durationMinutes, this.totalMarks, this.isActive, newDifficulty, this.topicId, this.createdAt);
    }

    public Assessment withTopicId(Long newTopicId) {
        return new Assessment(this.id, this.title, this.description, this.durationMinutes, this.totalMarks, this.isActive, this.difficulty, newTopicId, this.createdAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Assessment that = (Assessment) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return "Assessment{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", durationMinutes=" + durationMinutes +
                ", totalMarks=" + totalMarks +
                ", difficulty=" + difficulty +
                ", topicId=" + topicId +
                ", isActive=" + isActive +
                '}';
    }
}
