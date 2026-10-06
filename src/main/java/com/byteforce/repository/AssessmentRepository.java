package com.byteforce.repository;

import com.byteforce.domain.Assessment;
import com.byteforce.domain.Question;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository interface for Assessment domain entities and assessment-question associations.
 */
public interface AssessmentRepository {

    Assessment save(Assessment assessment);

    Optional<Assessment> findById(long id);

    List<Assessment> findAllActive();

    List<Assessment> findAll();

    List<Question> findQuestionsByAssessmentId(long assessmentId);

    Map<Long, Integer> getQuestionMarks(long assessmentId);

    void addQuestionToAssessment(long assessmentId, long questionId, int marks, int questionOrder);

    void removeQuestionFromAssessment(long assessmentId, long questionId);

    void clearQuestionsFromAssessment(long assessmentId);

    boolean deleteById(long id);

    long count();
}
