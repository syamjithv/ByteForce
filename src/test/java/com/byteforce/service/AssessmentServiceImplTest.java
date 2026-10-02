package com.byteforce.service;

import com.byteforce.domain.ActivityType;
import com.byteforce.domain.Assessment;
import com.byteforce.domain.AssessmentAnswer;
import com.byteforce.domain.AssessmentAttempt;
import com.byteforce.domain.AttemptStatus;
import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;
import com.byteforce.domain.QuestionType;
import com.byteforce.exception.ResourceNotFoundException;
import com.byteforce.exception.ValidationException;
import com.byteforce.repository.AssessmentAnswerRepository;
import com.byteforce.repository.AssessmentAttemptRepository;
import com.byteforce.repository.AssessmentRepository;
import com.byteforce.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceImplTest {

    @Mock
    private AssessmentRepository assessmentRepository;
    @Mock
    private AssessmentAttemptRepository assessmentAttemptRepository;
    @Mock
    private AssessmentAnswerRepository assessmentAnswerRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private ActivityService activityService;

    private AssessmentServiceImpl assessmentService;

    private final UUID userId = UUID.randomUUID();
    private Assessment sampleAssessment;

    @BeforeEach
    void setUp() {
        assessmentService = new AssessmentServiceImpl(
                assessmentRepository,
                assessmentAttemptRepository,
                assessmentAnswerRepository,
                questionRepository,
                activityService
        );

        sampleAssessment = new Assessment(
                1L,
                "Java Placement Mock",
                "Core Java, OOP, Collections",
                60,
                100,
                true,
                Difficulty.MEDIUM,
                2L,
                Instant.now()
        );
    }

    @Test
    @DisplayName("getAvailableAssessments should return active assessments from repository")
    void shouldReturnAvailableAssessments() {
        when(assessmentRepository.findAllActive()).thenReturn(List.of(sampleAssessment));

        List<Assessment> list = assessmentService.getAvailableAssessments();

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Java Placement Mock", list.get(0).getTitle());
        verify(assessmentRepository).findAllActive();
    }

    @Test
    @DisplayName("getAssessmentById should validate id and return assessment")
    void shouldGetAssessmentById() {
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(sampleAssessment));

        Optional<Assessment> opt = assessmentService.getAssessmentById(1L);
        assertTrue(opt.isPresent());
        assertEquals("Java Placement Mock", opt.get().getTitle());

        Optional<Assessment> invalid = assessmentService.getAssessmentById(-1L);
        assertFalse(invalid.isPresent());
    }

    @Test
    @DisplayName("startAssessment should create attempt and log activity")
    void shouldStartAssessmentSuccessfully() {
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(sampleAssessment));
        when(assessmentAttemptRepository.save(any(AssessmentAttempt.class))).thenAnswer(inv -> {
            AssessmentAttempt att = inv.getArgument(0);
            return att.withId(10L);
        });

        AssessmentAttempt attempt = assessmentService.startAssessment(userId, 1L);

        assertNotNull(attempt);
        assertEquals(10L, attempt.getId());
        assertEquals(userId, attempt.getUserId());
        assertEquals(1L, attempt.getAssessmentId());
        assertEquals("IN_PROGRESS", attempt.getStatus());

        verify(activityService).recordActivity(eq(userId), eq(ActivityType.ATTEMPTED_QUESTION), anyString());
    }

    @Test
    @DisplayName("startAssessment should throw on invalid ID or non-existent assessment")
    void shouldThrowWhenStartingInvalidAssessment() {
        assertThrows(ValidationException.class, () -> assessmentService.startAssessment(userId, 0));

        when(assessmentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> assessmentService.startAssessment(userId, 99L));
    }

    @Test
    @DisplayName("submitAssessment should evaluate MCQ, conceptual, and skipped answers, award marks, and update attempt")
    void shouldSubmitAndEvaluateAssessmentAnswers() {
        AssessmentAttempt activeAttempt = AssessmentAttempt.start(1L, userId).withId(100L);
        when(assessmentAttemptRepository.findById(100L)).thenReturn(Optional.of(activeAttempt));

        Question q1 = new Question(
                10L, 1L, "MCQ Question", "mcq-q",
                "What is 2+2?\nA) 3\nB) 4\nC) 5\nD) 6",
                Difficulty.EASY, QuestionType.MCQ, "B) 4",
                Instant.now(), Instant.now()
        );

        Question q2 = new Question(
                20L, 1L, "Conceptual Question", "conc-q",
                "Explain OOP.",
                Difficulty.MEDIUM, QuestionType.CONCEPTUAL, "Encapsulation, Inheritance, Polymorphism",
                Instant.now(), Instant.now()
        );

        Question q3 = new Question(
                30L, 1L, "Skipped Question", "skip-q",
                "SQL Query",
                Difficulty.HARD, QuestionType.SQL, "SELECT * FROM users",
                Instant.now(), Instant.now()
        );

        when(assessmentRepository.findQuestionsByAssessmentId(1L)).thenReturn(List.of(q1, q2, q3));
        when(assessmentRepository.getQuestionMarks(1L)).thenReturn(Map.of(
                10L, 20,
                20L, 30,
                30L, 50
        ));

        // User answers: q1 correct ('B'), q2 attempted ('Encapsulation and inheritance'), q3 null (skipped)
        Map<Long, String> userAnswers = Map.of(
                10L, "B) 4",
                20L, "Encapsulation, Inheritance, Polymorphism"
        );

        AssessmentAttempt completed = assessmentService.submitAssessment(100L, userAnswers);

        assertNotNull(completed);
        assertEquals(50, completed.getScore()); // 20 for q1 + 30 for exact q2 = 50
        assertEquals("COMPLETED", completed.getStatus());

        verify(assessmentAttemptRepository).updateScoreAndStatus(eq(100L), eq(50), eq("COMPLETED"), any(Instant.class));
        verify(assessmentAnswerRepository, org.mockito.Mockito.times(3)).save(any(AssessmentAnswer.class));
        verify(activityService).recordActivity(eq(userId), eq(ActivityType.SOLVED_QUESTION), anyString());
    }

    @Test
    @DisplayName("submitAssessment on already completed attempt should return existing attempt without re-submitting")
    void shouldReturnCompletedAttemptIfAlreadySubmitted() {
        AssessmentAttempt completedAttempt = AssessmentAttempt.start(1L, userId).withId(100L).complete(80);
        when(assessmentAttemptRepository.findById(100L)).thenReturn(Optional.of(completedAttempt));

        AssessmentAttempt result = assessmentService.submitAssessment(100L, Map.of());

        assertEquals(80, result.getScore());
        assertEquals("COMPLETED", result.getStatus());
        verify(assessmentAttemptRepository, org.mockito.Mockito.never())
                .updateScoreAndStatus(any(Long.class), any(Integer.class), anyString(), any(Instant.class));
    }
}
