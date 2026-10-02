package com.byteforce.repository;

import com.byteforce.domain.AptitudeCategory;
import com.byteforce.domain.AptitudeQuestion;
import com.byteforce.domain.Difficulty;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link AptitudeQuestion} persistence operations.
 */
public interface AptitudeQuestionRepository {

    Optional<AptitudeQuestion> findById(long id);

    List<AptitudeQuestion> findAll();

    List<AptitudeQuestion> findByCategory(AptitudeCategory category);

    List<AptitudeQuestion> findByDifficulty(Difficulty difficulty);

    List<AptitudeQuestion> findByCategoryAndDifficulty(AptitudeCategory category, Difficulty difficulty);

    AptitudeQuestion save(AptitudeQuestion question);

    boolean deleteById(long id);

    boolean existsById(long id);

    long count();

    long countByCategory(AptitudeCategory category);
}
