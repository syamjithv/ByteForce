package com.byteforce.service;

import com.byteforce.domain.ConceptRelationship;
import com.byteforce.domain.ConceptRelationshipType;
import com.byteforce.domain.RelatedConceptView;

import java.util.List;

/**
 * Service interface for Brain Map concept relationship network operations.
 */
public interface BrainMapService {

    List<RelatedConceptView> getRelatedConceptsForConcept(long conceptId);

    List<ConceptRelationship> getAllRelationships();

    ConceptRelationship createRelationship(long sourceConceptId,
                                           long targetConceptId,
                                           ConceptRelationshipType type,
                                           String description,
                                           int displayOrder);

    ConceptRelationship updateRelationship(long id,
                                           long sourceConceptId,
                                           long targetConceptId,
                                           ConceptRelationshipType type,
                                           String description,
                                           int displayOrder);

    java.util.Optional<ConceptRelationship> getRelationshipById(long id);

    void deleteRelationship(long id);

    long getTotalRelationshipCount();

    com.byteforce.domain.brainmap.BrainMapGraph getBrainMapGraph(long centerConceptId, java.util.UUID currentUserId);

    byte[] exportBrainMapPdf(long centerConceptId);
}
