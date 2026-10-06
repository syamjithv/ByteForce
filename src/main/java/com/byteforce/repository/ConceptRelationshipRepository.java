package com.byteforce.repository;

import com.byteforce.domain.ConceptRelationship;
import com.byteforce.domain.ConceptRelationshipType;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link ConceptRelationship} persistence operations.
 */
public interface ConceptRelationshipRepository {

    List<ConceptRelationship> findBySourceConceptId(long conceptId);

    List<ConceptRelationship> findByTargetConceptId(long conceptId);

    List<ConceptRelationship> findByConceptId(long conceptId);

    List<ConceptRelationship> findAll();

    Optional<ConceptRelationship> findById(long id);

    ConceptRelationship save(ConceptRelationship relationship);

    boolean exists(long sourceConceptId, long targetConceptId, ConceptRelationshipType type);

    boolean deleteById(long id);

    long count();
}
