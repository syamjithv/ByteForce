package com.byteforce.repository;

import com.byteforce.domain.Concept;
import com.byteforce.domain.LearningResource;
import com.byteforce.domain.Subject;
import com.byteforce.domain.Topic;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for educational placement content (subjects, topics, concepts, resources).
 */
public interface LearnRepository {

    List<Subject> findAllSubjects();

    Optional<Subject> findSubjectById(String id);

    List<Topic> findTopicsBySubjectId(String subjectId);

    Optional<Topic> findTopicById(long topicId);

    List<Concept> findConceptsByTopicId(long topicId);

    Optional<Concept> findConceptById(long conceptId);

    List<LearningResource> findResourcesByConceptId(long conceptId);

    List<Concept> searchConcepts(String query);
}
