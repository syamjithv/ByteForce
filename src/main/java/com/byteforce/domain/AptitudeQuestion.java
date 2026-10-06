package com.byteforce.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable domain entity representing a placement aptitude practice/test question
 * (Quantitative Aptitude, Logical Reasoning, or Verbal Ability).
 */
public final class AptitudeQuestion {

    private final long id;
    private final AptitudeCategory category;
    private final String topic;
    private final Difficulty difficulty;
    private final String question;
    private final String optionA;
    private final String optionB;
    private final String optionC;
    private final String optionD;
    private final String correctAnswer;
    private final String explanation;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    public AptitudeQuestion(long id,
                            AptitudeCategory category,
                            String topic,
                            Difficulty difficulty,
                            String question,
                            String optionA,
                            String optionB,
                            String optionC,
                            String optionD,
                            String correctAnswer,
                            String explanation,
                            boolean active,
                            Instant createdAt,
                            Instant updatedAt) {
        this.id = id;
        this.category = Objects.requireNonNull(category, "category must not be null");

        Objects.requireNonNull(topic, "topic must not be null");
        if (topic.isBlank()) {
            throw new IllegalArgumentException("topic must not be blank");
        }
        this.topic = topic.trim();

        this.difficulty = Objects.requireNonNull(difficulty, "difficulty must not be null");

        Objects.requireNonNull(question, "question must not be null");
        if (question.isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }
        this.question = question.trim();

        Objects.requireNonNull(optionA, "optionA must not be null");
        this.optionA = optionA.trim();

        Objects.requireNonNull(optionB, "optionB must not be null");
        this.optionB = optionB.trim();

        Objects.requireNonNull(optionC, "optionC must not be null");
        this.optionC = optionC.trim();

        Objects.requireNonNull(optionD, "optionD must not be null");
        this.optionD = optionD.trim();

        Objects.requireNonNull(correctAnswer, "correctAnswer must not be null");
        if (correctAnswer.isBlank()) {
            throw new IllegalArgumentException("correctAnswer must not be blank");
        }
        this.correctAnswer = correctAnswer.trim().toUpperCase();

        this.explanation = explanation != null ? explanation.trim() : "";
        this.active = active;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    private AptitudeQuestion(long id,
                             AptitudeCategory category,
                             String topic,
                             Difficulty difficulty,
                             String question,
                             String optionA,
                             String optionB,
                             String optionC,
                             String optionD,
                             String correctAnswer,
                             String explanation,
                             boolean active,
                             Instant createdAt,
                             Instant updatedAt,
                             boolean formEmpty) {
        this.id = id;
        this.category = category != null ? category : AptitudeCategory.QUANTITATIVE;
        this.topic = topic != null ? topic.trim() : "";
        this.difficulty = difficulty != null ? difficulty : Difficulty.EASY;
        this.question = question != null ? question.trim() : "";
        this.optionA = optionA != null ? optionA.trim() : "";
        this.optionB = optionB != null ? optionB.trim() : "";
        this.optionC = optionC != null ? optionC.trim() : "";
        this.optionD = optionD != null ? optionD.trim() : "";
        this.correctAnswer = correctAnswer != null ? correctAnswer : "A";
        this.explanation = explanation != null ? explanation.trim() : "";
        this.active = active;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static AptitudeQuestion empty(AptitudeCategory category) {
        return new AptitudeQuestion(0, category != null ? category : AptitudeCategory.QUANTITATIVE,
                "", Difficulty.EASY, "", "", "", "", "", "A", "", true, Instant.now(), Instant.now(), true);
    }

    public static AptitudeQuestion create(AptitudeCategory category,
                                          String topic,
                                          Difficulty difficulty,
                                          String question,
                                          String optionA,
                                          String optionB,
                                          String optionC,
                                          String optionD,
                                          String correctAnswer,
                                          String explanation) {
        Instant now = Instant.now();
        return new AptitudeQuestion(0, category, topic, difficulty, question, optionA, optionB, optionC, optionD, correctAnswer, explanation, true, now, now);
    }

    public long getId() {
        return id;
    }

    public AptitudeCategory getCategory() {
        return category;
    }

    public String getTopic() {
        return topic;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public String getQuestion() {
        return question;
    }

    public String getOptionA() {
        return optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public AptitudeQuestion withId(long newId) {
        return new AptitudeQuestion(newId, this.category, this.topic, this.difficulty, this.question,
                this.optionA, this.optionB, this.optionC, this.optionD, this.correctAnswer, this.explanation,
                this.active, this.createdAt, this.updatedAt);
    }

    public AptitudeQuestion withDetails(AptitudeCategory category, String topic, Difficulty difficulty,
                                        String question, String optionA, String optionB, String optionC,
                                        String optionD, String correctAnswer, String explanation, boolean active) {
        return new AptitudeQuestion(this.id, category, topic, difficulty, question, optionA, optionB,
                optionC, optionD, correctAnswer, explanation, active, this.createdAt, Instant.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AptitudeQuestion that = (AptitudeQuestion) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return "AptitudeQuestion{" +
                "id=" + id +
                ", category=" + category +
                ", topic='" + topic + '\'' +
                ", difficulty=" + difficulty +
                ", question='" + question + '\'' +
                ", correctAnswer='" + correctAnswer + '\'' +
                ", active=" + active +
                '}';
    }
}
