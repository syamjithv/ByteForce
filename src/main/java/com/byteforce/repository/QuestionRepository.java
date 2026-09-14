package com.byteforce.repository;

import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Question domain entities.
 */
public interface QuestionRepository {

    Optional<Question> findById(long id);

    Optional<Question> findBySlug(String slug);

    List<Question> findAll();

    List<Question> findByTopicId(long topicId);

    List<Question> findByDifficulty(Difficulty difficulty);

    List<Question> findByTopicIdAndDifficulty(long topicId, Difficulty difficulty);

    List<Question> search(String keyword);

    Question save(Question question);

    boolean deleteById(long id);

    boolean existsById(long id);

    boolean existsBySlug(String slug);

    long count();

    long countByTopicId(long topicId);

    long countByDifficulty(Difficulty difficulty);
}
