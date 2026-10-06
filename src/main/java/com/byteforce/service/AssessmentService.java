package com.byteforce.service;

import com.byteforce.domain.Assessment;
import com.byteforce.domain.AssessmentAnswer;
import com.byteforce.domain.AssessmentAttempt;
import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for Assessment and Mock Test business operations.
 */
public interface AssessmentService {

    List<Assessment> getAvailableAssessments();

    Optional<Assessment> getAssessmentById(long id);

    List<Question> getQuestionsForAssessment(long assessmentId);

    Map<Long, Integer> getQuestionMarks(long assessmentId);

    AssessmentAttempt startAssessment(UUID userId, long assessmentId);

    AssessmentAttempt submitAssessment(long attemptId, Map<Long, String> submittedAnswers);

    Optional<AssessmentAttempt> getAttemptById(long attemptId);

    List<AssessmentAttempt> getAttemptsForUser(UUID userId);

    Optional<com.byteforce.domain.AssessmentResumeView> getActiveResumeAttempt(UUID userId);

    Optional<AssessmentAttempt> getActiveAttemptForUserAndAssessment(UUID userId, long assessmentId);

    List<AssessmentAnswer> getAnswersForAttempt(long attemptId);

    Assessment createAssessment(String title, String description, int durationMinutes, int totalMarks, Difficulty difficulty, Long topicId);

    Assessment updateAssessment(long id, String title, String description, int durationMinutes, int totalMarks, Difficulty difficulty, Long topicId, boolean active);

    List<Assessment> getAllAssessments();

    void addQuestionToAssessment(long assessmentId, long questionId, int marks, int questionOrder);

    void removeQuestionFromAssessment(long assessmentId, long questionId);

    void deleteAssessment(long id);

    long getTotalAssessmentCount();
}
