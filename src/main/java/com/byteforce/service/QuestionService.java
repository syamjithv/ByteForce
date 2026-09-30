package com.byteforce.service;

import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;
import com.byteforce.domain.QuestionType;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for Question business operations.
 */
public interface QuestionService {

    Question createQuestion(long topicId, String title, String slug, String description, Difficulty difficulty, String solution);

    Question createQuestion(long topicId, String title, String slug, String description, Difficulty difficulty, QuestionType questionType, String solution);

    Question updateQuestion(long id, long topicId, String title, String slug, String description, Difficulty difficulty, String solution);

    Question updateQuestion(long id, long topicId, String title, String slug, String description, Difficulty difficulty, QuestionType questionType, String solution);

    Optional<Question> getQuestionById(long id);

    Optional<Question> getQuestionBySlug(String slug);

    List<Question> getAllQuestions();

    List<Question> getQuestionsByTopic(long topicId);

    List<Question> getQuestionsByDifficulty(Difficulty difficulty);

    List<Question> getQuestionsByType(QuestionType questionType);

    List<Question> getQuestionsByTopicAndDifficulty(long topicId, Difficulty difficulty);

    List<Question> searchQuestions(String keyword);

    void deleteQuestion(long id);

    long getTotalQuestionCount();
}
