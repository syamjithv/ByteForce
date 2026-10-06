package com.byteforce.repository;

import com.byteforce.domain.Concept;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link Concept} persistence operations.
 */
public interface ConceptRepository {

    Optional<Concept> findById(long id);

    List<Concept> findByTopicId(long topicId);

    List<Concept> findAll();

    List<Concept> search(String query);

    Concept save(Concept concept);

    boolean existsById(long id);

    boolean deleteById(long id);

    long count();
}
