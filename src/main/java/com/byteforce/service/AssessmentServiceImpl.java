package com.byteforce.service;

import com.byteforce.domain.ActivityType;
import com.byteforce.domain.Assessment;
import com.byteforce.domain.AssessmentAnswer;
import com.byteforce.domain.AssessmentAttempt;
import com.byteforce.domain.AttemptStatus;
import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;
import com.byteforce.domain.QuestionType;
import com.byteforce.exception.ByteForceException;
import com.byteforce.exception.ResourceNotFoundException;
import com.byteforce.exception.ValidationException;
import com.byteforce.repository.AssessmentAnswerRepository;
import com.byteforce.repository.AssessmentAttemptRepository;
import com.byteforce.repository.AssessmentRepository;
import com.byteforce.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Production implementation of {@link AssessmentService}.
 */
public class AssessmentServiceImpl implements AssessmentService {

    private static final Logger log = LoggerFactory.getLogger(AssessmentServiceImpl.class);

    private final AssessmentRepository assessmentRepository;
    private final AssessmentAttemptRepository assessmentAttemptRepository;
    private final AssessmentAnswerRepository assessmentAnswerRepository;
    private final QuestionRepository questionRepository;
    private final ActivityService activityService;

    public AssessmentServiceImpl(AssessmentRepository assessmentRepository,
                                 AssessmentAttemptRepository assessmentAttemptRepository,
                                 AssessmentAnswerRepository assessmentAnswerRepository,
                                 QuestionRepository questionRepository,
                                 ActivityService activityService) {
        this.assessmentRepository = Objects.requireNonNull(assessmentRepository, "assessmentRepository must not be null");
        this.assessmentAttemptRepository = Objects.requireNonNull(assessmentAttemptRepository, "assessmentAttemptRepository must not be null");
        this.assessmentAnswerRepository = Objects.requireNonNull(assessmentAnswerRepository, "assessmentAnswerRepository must not be null");
        this.questionRepository = questionRepository;
        this.activityService = activityService;
    }

    @Override
    public List<Assessment> getAvailableAssessments() {
        return assessmentRepository.findAllActive();
    }

    @Override
    public Optional<Assessment> getAssessmentById(long id) {
        if (id <= 0) return Optional.empty();
        return assessmentRepository.findById(id);
    }

    @Override
    public List<Question> getQuestionsForAssessment(long assessmentId) {
        if (assessmentId <= 0) return List.of();
        return assessmentRepository.findQuestionsByAssessmentId(assessmentId);
    }

    @Override
    public Map<Long, Integer> getQuestionMarks(long assessmentId) {
        if (assessmentId <= 0) return Map.of();
        return assessmentRepository.getQuestionMarks(assessmentId);
    }

    @Override
    public AssessmentAttempt startAssessment(UUID userId, long assessmentId) {
        Objects.requireNonNull(userId, "userId must not be null");
        if (assessmentId <= 0) {
            throw new ValidationException("Invalid assessment ID: " + assessmentId);
        }

        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found with ID: " + assessmentId));

        if (!assessment.isActive()) {
            throw new ValidationException("Assessment is not currently active.");
        }

        AssessmentAttempt attempt = AssessmentAttempt.start(assessmentId, userId);
        AssessmentAttempt saved = assessmentAttemptRepository.save(attempt);
        log.info("Started assessment attempt {} for user {} on assessment {}", saved.getId(), userId, assessmentId);

        if (activityService != null) {
            try {
                activityService.recordActivity(userId, ActivityType.ATTEMPTED_QUESTION, "Started assessment: " + assessment.getTitle());
            } catch (Exception e) {
                log.warn("Failed to record activity for assessment start", e);
            }
        }

        return saved;
    }

    @Override
    public AssessmentAttempt submitAssessment(long attemptId, Map<Long, String> submittedAnswers) {
        if (attemptId <= 0) {
            throw new ValidationException("Invalid attempt ID: " + attemptId);
        }

        AssessmentAttempt attempt = assessmentAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment attempt not found with ID: " + attemptId));

        if (attempt.isCompleted()) {
            log.warn("Attempt {} is already completed.", attemptId);
            return attempt;
        }

        List<Question> questions = assessmentRepository.findQuestionsByAssessmentId(attempt.getAssessmentId());
        Map<Long, Integer> marksMap = assessmentRepository.getQuestionMarks(attempt.getAssessmentId());

        int totalScore = 0;
        Instant now = Instant.now();

        for (Question q : questions) {
            String answerText = submittedAnswers != null ? submittedAnswers.get(q.getId()) : null;
            int qMarks = marksMap.getOrDefault(q.getId(), 1);

            AttemptStatus status;
            int marksAwarded = 0;

            if (answerText == null || answerText.trim().isBlank()) {
                status = AttemptStatus.SKIPPED;
                answerText = null;
            } else {
                answerText = answerText.trim();
                status = evaluateAnswer(q, answerText);
                if (status == AttemptStatus.SOLVED) {
                    marksAwarded = qMarks;
                    totalScore += marksAwarded;
                }
            }

            AssessmentAnswer answer = new AssessmentAnswer(
                    0, attemptId, q.getId(), answerText, marksAwarded, status,
                    status != AttemptStatus.SKIPPED ? now : null
            );
            assessmentAnswerRepository.save(answer);
        }

        assessmentAttemptRepository.updateScoreAndStatus(attemptId, totalScore, "COMPLETED", now);
        log.info("Completed assessment attempt {} with score {}", attemptId, totalScore);

        if (activityService != null) {
            try {
                activityService.recordActivity(attempt.getUserId(), ActivityType.SOLVED_QUESTION,
                        "Completed assessment ID " + attempt.getAssessmentId() + " with score " + totalScore);
            } catch (Exception e) {
                log.warn("Failed to record activity for assessment completion", e);
            }
        }

        return attempt.complete(totalScore);
    }

    @Override
    public Optional<AssessmentAttempt> getAttemptById(long attemptId) {
        if (attemptId <= 0) return Optional.empty();
        return assessmentAttemptRepository.findById(attemptId);
    }

    @Override
    public List<AssessmentAttempt> getAttemptsForUser(UUID userId) {
        if (userId == null) return List.of();
        return assessmentAttemptRepository.findByUserId(userId);
    }

    @Override
    public List<AssessmentAnswer> getAnswersForAttempt(long attemptId) {
        if (attemptId <= 0) return List.of();
        return assessmentAnswerRepository.findByAttemptId(attemptId);
    }

    @Override
    public Assessment createAssessment(String title, String description, int durationMinutes, int totalMarks, Difficulty difficulty, Long topicId) {
        Assessment assessment = Assessment.create(title, description, durationMinutes, totalMarks, difficulty, topicId);
        return assessmentRepository.save(assessment);
    }

    @Override
    public Assessment updateAssessment(long id, String title, String description, int durationMinutes, int totalMarks, Difficulty difficulty, Long topicId, boolean active) {
        Assessment existing = assessmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found with ID: " + id));

        Assessment updated = existing
                .withTitle(title)
                .withDescription(description)
                .withDurationMinutes(durationMinutes)
                .withTotalMarks(totalMarks)
                .withDifficulty(difficulty)
                .withTopicId(topicId)
                .withActive(active);

        Assessment saved = assessmentRepository.save(updated);
        log.info("Updated assessment with ID {}", saved.getId());
        return saved;
    }

    @Override
    public List<Assessment> getAllAssessments() {
        return assessmentRepository.findAll();
    }

    @Override
    public void addQuestionToAssessment(long assessmentId, long questionId, int marks, int questionOrder) {
        if (assessmentId <= 0 || questionId <= 0) {
            throw new ValidationException("assessmentId and questionId must be positive");
        }
        assessmentRepository.addQuestionToAssessment(assessmentId, questionId, marks, questionOrder);
    }

    @Override
    public void removeQuestionFromAssessment(long assessmentId, long questionId) {
        if (assessmentId <= 0 || questionId <= 0) {
            throw new ValidationException("assessmentId and questionId must be positive");
        }
        assessmentRepository.removeQuestionFromAssessment(assessmentId, questionId);
    }

    @Override
    public void deleteAssessment(long id) {
        Assessment existing = assessmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found with ID: " + id));
        assessmentRepository.deleteById(id);
        log.info("Deleted assessment with ID {}", id);
    }

    @Override
    public long getTotalAssessmentCount() {
        return assessmentRepository.count();
    }

    private AttemptStatus evaluateAnswer(Question q, String studentAnswer) {
        String solution = q.getSolution() != null ? q.getSolution().trim() : "";
        QuestionType type = q.getQuestionType();

        if (type == QuestionType.MCQ) {
            return isMcqCorrect(studentAnswer, solution) ? AttemptStatus.SOLVED : AttemptStatus.FAILED;
        } else if (type == QuestionType.CONCEPTUAL) {
            if (!solution.isBlank() && studentAnswer.equalsIgnoreCase(solution)) {
                return AttemptStatus.SOLVED;
            }
            return AttemptStatus.ATTEMPTED;
        } else {
            return AttemptStatus.ATTEMPTED;
        }
    }

    private boolean isMcqCorrect(String selected, String expected) {
        if (selected == null || expected == null || expected.isBlank()) {
            return false;
        }
        String s = selected.trim().toLowerCase();
        String e = expected.trim().toLowerCase();

        if (s.equals(e)) return true;
        if (e.length() == 1 && (s.startsWith(e + ")") || s.startsWith(e + "."))) return true;
        if (s.contains(e) || e.contains(s)) return true;

        if (s.length() >= 2 && (s.charAt(1) == ')' || s.charAt(1) == '.')) {
            String letter = s.substring(0, 1).trim();
            if (letter.equalsIgnoreCase(e)) return true;
            String textAfterLetter = s.substring(2).trim();
            if (textAfterLetter.equalsIgnoreCase(e) || textAfterLetter.contains(e)) return true;
        }
        return false;
    }
}
