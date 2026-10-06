package com.byteforce.repository;

import com.byteforce.domain.LearningResource;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link LearningResource} persistence operations.
 */
public interface LearningResourceRepository {

    List<LearningResource> findByConceptId(long conceptId);

    List<LearningResource> findAll();

    Optional<LearningResource> findById(long id);

    LearningResource save(LearningResource resource);

    void saveForConcept(long conceptId, LearningResource resource, int displayOrder);

    boolean deleteById(long id);

    void deleteByConceptId(long conceptId);

    long count();
}
