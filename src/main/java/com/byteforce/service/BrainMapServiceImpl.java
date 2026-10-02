package com.byteforce.service;

import com.byteforce.domain.Concept;
import com.byteforce.domain.ConceptRelationship;
import com.byteforce.domain.ConceptRelationshipType;
import com.byteforce.domain.RelatedConceptView;
import com.byteforce.exception.ResourceNotFoundException;
import com.byteforce.exception.ValidationException;
import com.byteforce.repository.ConceptRelationshipRepository;
import com.byteforce.repository.ConceptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Production implementation of {@link BrainMapService}.
 */
public class BrainMapServiceImpl implements BrainMapService {

    private static final Logger log = LoggerFactory.getLogger(BrainMapServiceImpl.class);

    private final ConceptRelationshipRepository relationshipRepository;
    private final ConceptRepository conceptRepository;

    public BrainMapServiceImpl(ConceptRelationshipRepository relationshipRepository,
                               ConceptRepository conceptRepository) {
        this.relationshipRepository = Objects.requireNonNull(relationshipRepository, "relationshipRepository must not be null");
        this.conceptRepository = Objects.requireNonNull(conceptRepository, "conceptRepository must not be null");
    }

    @Override
    public List<RelatedConceptView> getRelatedConceptsForConcept(long conceptId) {
        if (conceptId <= 0) {
            return List.of();
        }

        List<RelatedConceptView> views = new ArrayList<>();

        // 1. Outgoing edges (this concept -> other concept)
        List<ConceptRelationship> outgoing = relationshipRepository.findBySourceConceptId(conceptId);
        for (ConceptRelationship rel : outgoing) {
            Optional<Concept> targetOpt = conceptRepository.findById(rel.getTargetConceptId());
            targetOpt.ifPresent(target -> views.add(new RelatedConceptView(rel, target, true)));
        }

        // 2. Incoming edges (other concept -> this concept)
        List<ConceptRelationship> incoming = relationshipRepository.findByTargetConceptId(conceptId);
        for (ConceptRelationship rel : incoming) {
            Optional<Concept> sourceOpt = conceptRepository.findById(rel.getSourceConceptId());
            sourceOpt.ifPresent(source -> views.add(new RelatedConceptView(rel, source, false)));
        }

        return views;
    }

    @Override
    public List<ConceptRelationship> getAllRelationships() {
        return relationshipRepository.findAll();
    }

    @Override
    public ConceptRelationship createRelationship(long sourceConceptId,
                                                  long targetConceptId,
                                                  ConceptRelationshipType type,
                                                  String description,
                                                  int displayOrder) {
        if (sourceConceptId <= 0 || targetConceptId <= 0) {
            throw new ValidationException("Source and target concept IDs must be valid positive numbers.");
        }
        if (sourceConceptId == targetConceptId) {
            throw new ValidationException("Cannot create a relationship from a concept to itself.");
        }
        if (type == null) {
            throw new ValidationException("Relationship type must not be null.");
        }

        if (!conceptRepository.existsById(sourceConceptId)) {
            throw new ResourceNotFoundException("Source concept not found with ID: " + sourceConceptId);
        }
        if (!conceptRepository.existsById(targetConceptId)) {
            throw new ResourceNotFoundException("Target concept not found with ID: " + targetConceptId);
        }

        ConceptRelationship rel = ConceptRelationship.create(sourceConceptId, targetConceptId, type, description, displayOrder);
        ConceptRelationship saved = relationshipRepository.save(rel);
        log.info("Created concept relationship ID {} ({} -> {} as {})", saved.getId(), sourceConceptId, targetConceptId, type);
        return saved;
    }

    @Override
    public ConceptRelationship updateRelationship(long id,
                                                  long sourceConceptId,
                                                  long targetConceptId,
                                                  ConceptRelationshipType type,
                                                  String description,
                                                  int displayOrder) {
        if (sourceConceptId <= 0 || targetConceptId <= 0) {
            throw new ValidationException("Source and target concept IDs must be valid positive numbers.");
        }
        if (sourceConceptId == targetConceptId) {
            throw new ValidationException("Cannot create a relationship from a concept to itself.");
        }
        if (type == null) {
            throw new ValidationException("Relationship type must not be null.");
        }

        if (!conceptRepository.existsById(sourceConceptId)) {
            throw new ResourceNotFoundException("Source concept not found with ID: " + sourceConceptId);
        }
        if (!conceptRepository.existsById(targetConceptId)) {
            throw new ResourceNotFoundException("Target concept not found with ID: " + targetConceptId);
        }

        ConceptRelationship existing = relationshipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concept relationship not found with ID: " + id));

        ConceptRelationship updated = existing
                .withSourceConceptId(sourceConceptId)
                .withTargetConceptId(targetConceptId)
                .withRelationshipType(type)
                .withDescription(description != null ? description.trim() : "")
                .withDisplayOrder(displayOrder);

        ConceptRelationship saved = relationshipRepository.save(updated);
        log.info("Updated concept relationship ID {}", saved.getId());
        return saved;
    }

    @Override
    public Optional<ConceptRelationship> getRelationshipById(long id) {
        if (id <= 0) {
            return Optional.empty();
        }
        return relationshipRepository.findById(id);
    }

    @Override
    public void deleteRelationship(long id) {
        if (!relationshipRepository.deleteById(id)) {
            throw new ResourceNotFoundException("Concept relationship not found with ID: " + id);
        }
        log.info("Deleted concept relationship ID {}", id);
    }

    @Override
    public long getTotalRelationshipCount() {
        return relationshipRepository.count();
    }
}
