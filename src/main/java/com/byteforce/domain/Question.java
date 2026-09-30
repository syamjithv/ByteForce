package com.byteforce.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable domain entity representing a placement preparation question
 * (coding, MCQ, SQL, or conceptual).
 */
public final class Question {

    private final long id;
    private final long topicId;
    private final String title;
    private final String slug;
    private final String description;
    private final Difficulty difficulty;
    private final QuestionType questionType;
    private final String solution;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Question(long id, long topicId, String title, String slug, String description,
                    Difficulty difficulty, QuestionType questionType, String solution,
                    Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.topicId = topicId;

        Objects.requireNonNull(title, "title must not be null");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        this.title = title.trim();

        Objects.requireNonNull(slug, "slug must not be null");
        if (slug.isBlank()) {
            throw new IllegalArgumentException("slug must not be blank");
        }
        this.slug = slug.trim().toLowerCase();

        Objects.requireNonNull(description, "description must not be null");
        if (description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        this.description = description.trim();

        this.difficulty = Objects.requireNonNull(difficulty, "difficulty must not be null");
        this.questionType = Objects.requireNonNull(questionType, "questionType must not be null");
        this.solution = solution;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    /**
     * Backward-compatible constructor defaulting to {@link QuestionType#CODING}.
     */
    public Question(long id, long topicId, String title, String slug, String description,
                    Difficulty difficulty, String solution, Instant createdAt, Instant updatedAt) {
        this(id, topicId, title, slug, description, difficulty, QuestionType.CODING, solution, createdAt, updatedAt);
    }

    /**
     * Factory for creating a new question with a specific question type (id 0 indicates unsaved).
     */
    public static Question create(long topicId, String title, String slug, String description,
                                  Difficulty difficulty, QuestionType questionType, String solution) {
        Instant now = Instant.now();
        return new Question(0, topicId, title, slug, description, difficulty, questionType, solution, now, now);
    }

    /**
     * Factory for creating a new coding question (id 0 indicates unsaved).
     */
    public static Question create(long topicId, String title, String slug, String description,
                                  Difficulty difficulty, String solution) {
        return create(topicId, title, slug, description, difficulty, QuestionType.CODING, solution);
    }

    public long getId() {
        return id;
    }

    public long getTopicId() {
        return topicId;
    }

    public String getTitle() {
        return title;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public String getSolution() {
        return solution;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Returns a copy with the database-assigned id.
     */
    public Question withId(long newId) {
        return new Question(newId, this.topicId, this.title, this.slug, this.description,
                this.difficulty, this.questionType, this.solution, this.createdAt, this.updatedAt);
    }

    public Question withTitle(String newTitle) {
        return new Question(this.id, this.topicId, newTitle, this.slug, this.description,
                this.difficulty, this.questionType, this.solution, this.createdAt, Instant.now());
    }

    public Question withDescription(String newDescription) {
        return new Question(this.id, this.topicId, this.title, this.slug, newDescription,
                this.difficulty, this.questionType, this.solution, this.createdAt, Instant.now());
    }

    public Question withDifficulty(Difficulty newDifficulty) {
        return new Question(this.id, this.topicId, this.title, this.slug, this.description,
                newDifficulty, this.questionType, this.solution, this.createdAt, Instant.now());
    }

    public Question withQuestionType(QuestionType newQuestionType) {
        return new Question(this.id, this.topicId, this.title, this.slug, this.description,
                this.difficulty, newQuestionType, this.solution, this.createdAt, Instant.now());
    }

    public Question withSolution(String newSolution) {
        return new Question(this.id, this.topicId, this.title, this.slug, this.description,
                this.difficulty, this.questionType, newSolution, this.createdAt, Instant.now());
    }

    public Question withTopicId(long newTopicId) {
        return new Question(this.id, newTopicId, this.title, this.slug, this.description,
                this.difficulty, this.questionType, this.solution, this.createdAt, Instant.now());
    }

    public Question withSlug(String newSlug) {
        return new Question(this.id, this.topicId, this.title, newSlug, this.description,
                this.difficulty, this.questionType, this.solution, this.createdAt, Instant.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Question question = (Question) o;
        return id == question.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return "Question{" +
                "id=" + id +
                ", topicId=" + topicId +
                ", title='" + title + '\'' +
                ", difficulty=" + difficulty +
                ", questionType=" + questionType +
                '}';
    }
}
