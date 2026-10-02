package com.byteforce.service;

import com.byteforce.domain.AptitudeCategory;
import com.byteforce.domain.AptitudeQuestion;
import com.byteforce.domain.Difficulty;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for placement aptitude question operations.
 */
public interface AptitudeQuestionService {

    List<AptitudeQuestion> getAllQuestions();

    List<AptitudeQuestion> getQuestionsByCategory(AptitudeCategory category);

    List<AptitudeQuestion> getQuestionsByDifficulty(Difficulty difficulty);

    List<AptitudeQuestion> getQuestionsByCategoryAndDifficulty(AptitudeCategory category, Difficulty difficulty);

    Optional<AptitudeQuestion> getQuestionById(long id);

    AptitudeQuestion createQuestion(AptitudeCategory category,
                                    String topic,
                                    Difficulty difficulty,
                                    String question,
                                    String optionA,
                                    String optionB,
                                    String optionC,
                                    String optionD,
                                    String correctAnswer,
                                    String explanation);

    AptitudeQuestion updateQuestion(long id,
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
                                    boolean active);

    void deleteQuestion(long id);

    long getTotalCount();

    long getCountByCategory(AptitudeCategory category);

    default List<AptitudeQuestion> getAllAptitudeQuestions() {
        return getAllQuestions();
    }

    default List<AptitudeQuestion> getAptitudeQuestionsByCategory(AptitudeCategory category) {
        return getQuestionsByCategory(category);
    }

    default List<AptitudeQuestion> getAptitudeQuestionsByDifficulty(Difficulty difficulty) {
        return getQuestionsByDifficulty(difficulty);
    }

    default List<AptitudeQuestion> getAptitudeQuestionsByCategoryAndDifficulty(AptitudeCategory category, Difficulty difficulty) {
        return getQuestionsByCategoryAndDifficulty(category, difficulty);
    }

    default Optional<AptitudeQuestion> getAptitudeQuestionById(long id) {
        return getQuestionById(id);
    }

    default AptitudeQuestion createAptitudeQuestion(AptitudeCategory category,
                                                    String topic,
                                                    Difficulty difficulty,
                                                    String question,
                                                    String optionA,
                                                    String optionB,
                                                    String optionC,
                                                    String optionD,
                                                    String correctAnswer,
                                                    String explanation) {
        return createQuestion(category, topic, difficulty, question, optionA, optionB, optionC, optionD, correctAnswer, explanation);
    }

    default AptitudeQuestion updateAptitudeQuestion(long id,
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
                                                    boolean active) {
        return updateQuestion(id, category, topic, difficulty, question, optionA, optionB, optionC, optionD, correctAnswer, explanation, active);
    }

    default void deleteAptitudeQuestion(long id) {
        deleteQuestion(id);
    }

    default long getTotalAptitudeQuestionCount() {
        return getTotalCount();
    }

    default List<AptitudeQuestion> getAllActiveQuestions() {
        return getAllQuestions().stream().filter(AptitudeQuestion::isActive).toList();
    }

    default List<AptitudeQuestion> getQuestionsByTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            return getAllActiveQuestions();
        }
        return getAllQuestions().stream()
                .filter(AptitudeQuestion::isActive)
                .filter(q -> q.getTopic() != null && q.getTopic().equalsIgnoreCase(topic.trim()))
                .toList();
    }

    default long getQuestionCountByCategory(AptitudeCategory category) {
        return getCountByCategory(category);
    }
}
