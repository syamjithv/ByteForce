package com.byteforce.service;

import com.byteforce.domain.Concept;
import com.byteforce.domain.LearningResource;
import com.byteforce.domain.Subject;
import com.byteforce.domain.Topic;
import com.byteforce.exception.ResourceNotFoundException;
import com.byteforce.exception.ValidationException;
import com.byteforce.repository.ConceptRepository;
import com.byteforce.repository.LearnRepository;
import com.byteforce.repository.LearningResourceRepository;
import com.byteforce.repository.SubjectRepository;
import com.byteforce.repository.TopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Production implementation of {@link LearnService}.
 */
public class LearnServiceImpl implements LearnService {

    private static final Logger log = LoggerFactory.getLogger(LearnServiceImpl.class);

    private final LearnRepository learnRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final ConceptRepository conceptRepository;
    private final LearningResourceRepository learningResourceRepository;

    public LearnServiceImpl(LearnRepository learnRepository,
                            SubjectRepository subjectRepository,
                            TopicRepository topicRepository,
                            ConceptRepository conceptRepository,
                            LearningResourceRepository learningResourceRepository) {
        this.learnRepository = Objects.requireNonNull(learnRepository, "learnRepository must not be null");
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.conceptRepository = conceptRepository;
        this.learningResourceRepository = learningResourceRepository;
    }

    public LearnServiceImpl(LearnRepository learnRepository) {
        this(learnRepository, null, null, null, null);
    }

    @Override
    public List<Subject> getAllSubjects() {
        log.debug("Retrieving all placement learning subjects");
        return learnRepository.findAllSubjects();
    }

    @Override
    public Optional<Subject> getSubjectById(String subjectId) {
        if (subjectId == null || subjectId.isBlank()) {
            return Optional.empty();
        }
        return learnRepository.findSubjectById(subjectId);
    }

    @Override
    public List<Topic> getTopicsForSubject(String subjectId) {
        if (subjectId == null || subjectId.isBlank()) {
            return List.of();
        }
        return learnRepository.findTopicsBySubjectId(subjectId);
    }

    @Override
    public Optional<Topic> getTopicById(long topicId) {
        if (topicId <= 0) {
            return Optional.empty();
        }
        return learnRepository.findTopicById(topicId);
    }

    @Override
    public List<Concept> getConceptsForTopic(long topicId) {
        if (topicId <= 0) {
            return List.of();
        }
        log.debug("Retrieving concepts for topic ID {}", topicId);
        return learnRepository.findConceptsByTopicId(topicId);
    }

    @Override
    public Optional<Concept> getConceptById(long conceptId) {
        if (conceptId <= 0) {
            return Optional.empty();
        }
        return learnRepository.findConceptById(conceptId);
    }

    @Override
    public List<LearningResource> getResourcesForConcept(long conceptId) {
        if (conceptId <= 0) {
            return List.of();
        }
        return learnRepository.findResourcesByConceptId(conceptId);
    }

    @Override
    public List<Concept> searchConcepts(String query) {
        return learnRepository.searchConcepts(query);
    }

    @Override
    public List<Concept> getAllConcepts() {
        if (conceptRepository != null) {
            return conceptRepository.findAll();
        }
        return List.of();
    }

    @Override
    public Subject saveSubject(Subject subject) {
        if (subject == null) {
            throw new ValidationException("Subject must not be null");
        }
        if (subject.getId() == null || subject.getId().isBlank()) {
            throw new ValidationException("Subject ID must not be blank");
        }
        if (subject.getName() == null || subject.getName().isBlank()) {
            throw new ValidationException("Subject name must not be blank");
        }
        if (subjectRepository != null) {
            Subject saved = subjectRepository.save(subject);
            log.info("Saved subject: {} ({})", saved.getName(), saved.getId());
            return saved;
        }
        throw new UnsupportedOperationException("SubjectRepository not configured");
    }

    @Override
    public void deleteSubject(String subjectId) {
        if (subjectId == null || subjectId.isBlank()) {
            throw new ValidationException("Subject ID must not be blank");
        }
        List<Topic> topics = getTopicsForSubject(subjectId);
        if (!topics.isEmpty()) {
            throw new ValidationException("Cannot delete subject '" + subjectId + "' because it has " + topics.size() + " associated topics. Delete or reassign topics first.");
        }
        if (subjectRepository != null) {
            subjectRepository.deleteById(subjectId);
            log.info("Deleted subject: {}", subjectId);
        }
    }

    @Override
    public Concept saveConcept(Concept concept) {
        if (concept == null) {
            throw new ValidationException("Concept must not be null");
        }
        if (concept.getTitle() == null || concept.getTitle().isBlank()) {
            throw new ValidationException("Concept title must not be blank");
        }
        if (conceptRepository != null) {
            Concept saved = conceptRepository.save(concept);
            log.info("Saved concept: {} ({})", saved.getTitle(), saved.getId());
            return saved;
        }
        throw new UnsupportedOperationException("ConceptRepository not configured");
    }

    @Override
    public void deleteConcept(long conceptId) {
        if (conceptId <= 0) {
            throw new ValidationException("Invalid concept ID: " + conceptId);
        }
        if (conceptRepository != null) {
            if (!conceptRepository.existsById(conceptId)) {
                throw new ResourceNotFoundException("Concept not found with ID: " + conceptId);
            }
            if (learningResourceRepository != null) {
                learningResourceRepository.deleteByConceptId(conceptId);
            }
            conceptRepository.deleteById(conceptId);
            log.info("Deleted concept ID {}", conceptId);
        }
    }

    @Override
    public List<LearningResource> getAllResources() {
        if (learningResourceRepository != null) {
            return learningResourceRepository.findAll();
        }
        return List.of();
    }

    @Override
    public Optional<LearningResource> getResourceById(long resourceId) {
        if (resourceId <= 0) {
            return Optional.empty();
        }
        if (learningResourceRepository != null) {
            return learningResourceRepository.findById(resourceId);
        }
        return Optional.empty();
    }

    @Override
    public LearningResource saveResource(LearningResource resource) {
        if (resource == null) {
            throw new ValidationException("Resource must not be null");
        }
        if (learningResourceRepository != null) {
            LearningResource saved = learningResourceRepository.save(resource);
            log.info("Saved learning resource: {} ({})", saved.getTitle(), saved.getId());
            return saved;
        }
        throw new UnsupportedOperationException("LearningResourceRepository not configured");
    }

    @Override
    public void deleteResource(long resourceId) {
        if (resourceId <= 0) {
            throw new ValidationException("Invalid resource ID: " + resourceId);
        }
        if (learningResourceRepository != null) {
            learningResourceRepository.deleteById(resourceId);
            log.info("Deleted resource ID {}", resourceId);
        }
    }

    @Override
    public long getTotalSubjectCount() {
        if (subjectRepository != null) {
            return subjectRepository.count();
        }
        return getAllSubjects().size();
    }

    @Override
    public long getTotalConceptCount() {
        if (conceptRepository != null) {
            return conceptRepository.count();
        }
        return 0;
    }

    @Override
    public long getTotalResourceCount() {
        if (learningResourceRepository != null) {
            return learningResourceRepository.count();
        }
        return 0;
    }
}
