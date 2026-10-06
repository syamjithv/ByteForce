package com.byteforce.service;

import com.byteforce.domain.Concept;
import com.byteforce.domain.LearningResource;
import com.byteforce.domain.Subject;
import com.byteforce.domain.Topic;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for ByteForce placement learning module.
 * Provides hierarchical access to Subjects -> Topics -> Concepts -> Resources.
 */
public interface LearnService {

    List<Subject> getAllSubjects();

    Optional<Subject> getSubjectById(String subjectId);

    List<Topic> getTopicsForSubject(String subjectId);

    Optional<Topic> getTopicById(long topicId);

    List<Concept> getConceptsForTopic(long topicId);

    Optional<Concept> getConceptById(long conceptId);

    List<LearningResource> getResourcesForConcept(long conceptId);

    List<Concept> searchConcepts(String query);

    List<Concept> getAllConcepts();

    Subject saveSubject(Subject subject);

    void deleteSubject(String subjectId);

    Concept saveConcept(Concept concept);

    void deleteConcept(long conceptId);

    List<LearningResource> getAllResources();

    Optional<LearningResource> getResourceById(long resourceId);

    LearningResource saveResource(LearningResource resource);

    void deleteResource(long resourceId);

    long getTotalSubjectCount();

    long getTotalConceptCount();

    long getTotalResourceCount();
}
