package com.byteforce.repository;

import com.byteforce.domain.Concept;
import com.byteforce.domain.LearningResource;
import com.byteforce.domain.Subject;
import com.byteforce.domain.Topic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Production JDBC-backed implementation of {@link LearnRepository}.
 * Coordinates Subject, Topic, Concept, and LearningResource repositories.
 */
public class JdbcLearnRepository implements LearnRepository {

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final ConceptRepository conceptRepository;
    private final LearningResourceRepository learningResourceRepository;

    public JdbcLearnRepository(SubjectRepository subjectRepository,
                               TopicRepository topicRepository,
                               ConceptRepository conceptRepository,
                               LearningResourceRepository learningResourceRepository) {
        this.subjectRepository = Objects.requireNonNull(subjectRepository, "subjectRepository must not be null");
        this.topicRepository = Objects.requireNonNull(topicRepository, "topicRepository must not be null");
        this.conceptRepository = Objects.requireNonNull(conceptRepository, "conceptRepository must not be null");
        this.learningResourceRepository = Objects.requireNonNull(learningResourceRepository, "learningResourceRepository must not be null");
    }

    @Override
    public List<Subject> findAllSubjects() {
        List<Subject> baseSubjects = subjectRepository.findAll();
        List<Subject> enriched = new ArrayList<>(baseSubjects.size());
        for (Subject s : baseSubjects) {
            List<Topic> topics = topicRepository.findBySubjectId(s.getId());
            enriched.add(Subject.create(s.getId(), s.getName(), s.getDescription(), s.getDisplayOrder(), topics));
        }
        return enriched;
    }

    @Override
    public Optional<Subject> findSubjectById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return subjectRepository.findById(id).map(s -> {
            List<Topic> topics = topicRepository.findBySubjectId(s.getId());
            return Subject.create(s.getId(), s.getName(), s.getDescription(), s.getDisplayOrder(), topics);
        });
    }

    @Override
    public List<Topic> findTopicsBySubjectId(String subjectId) {
        if (subjectId == null || subjectId.isBlank()) {
            return List.of();
        }
        return topicRepository.findBySubjectId(subjectId);
    }

    @Override
    public Optional<Topic> findTopicById(long topicId) {
        if (topicId <= 0) {
            return Optional.empty();
        }
        return topicRepository.findById(topicId);
    }

    @Override
    public List<Concept> findConceptsByTopicId(long topicId) {
        if (topicId <= 0) {
            return List.of();
        }
        return conceptRepository.findByTopicId(topicId);
    }

    @Override
    public Optional<Concept> findConceptById(long conceptId) {
        if (conceptId <= 0) {
            return Optional.empty();
        }
        return conceptRepository.findById(conceptId);
    }

    @Override
    public List<LearningResource> findResourcesByConceptId(long conceptId) {
        if (conceptId <= 0) {
            return List.of();
        }
        return learningResourceRepository.findByConceptId(conceptId);
    }

    @Override
    public List<Concept> searchConcepts(String query) {
        return conceptRepository.search(query);
    }
}
