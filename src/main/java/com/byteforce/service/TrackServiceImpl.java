package com.byteforce.service;

import com.byteforce.domain.Activity;
import com.byteforce.domain.Assessment;
import com.byteforce.domain.AssessmentAttempt;
import com.byteforce.domain.AssessmentAttemptSummary;
import com.byteforce.domain.AttemptStatus;
import com.byteforce.domain.Difficulty;
import com.byteforce.domain.DifficultyDistribution;
import com.byteforce.domain.Question;
import com.byteforce.domain.QuestionAttempt;
import com.byteforce.domain.StudentProgressSummary;
import com.byteforce.domain.Topic;
import com.byteforce.domain.TopicPerformance;
import com.byteforce.repository.QuestionRepository;
import com.byteforce.repository.TopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Production implementation of TrackService.
 * Aggregates real progress metrics, practice performance by topic,
 * assessment history, and user activity without inventing fake data.
 */
public class TrackServiceImpl implements TrackService {

    private static final Logger log = LoggerFactory.getLogger(TrackServiceImpl.class);

    private final AttemptService attemptService;
    private final AssessmentService assessmentService;
    private final ActivityService activityService;
    private final BookmarkService bookmarkService;
    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;

    public TrackServiceImpl(AttemptService attemptService,
                            AssessmentService assessmentService,
                            ActivityService activityService,
                            BookmarkService bookmarkService,
                            QuestionRepository questionRepository,
                            TopicRepository topicRepository) {
        this.attemptService = Objects.requireNonNull(attemptService, "attemptService must not be null");
        this.assessmentService = Objects.requireNonNull(assessmentService, "assessmentService must not be null");
        this.activityService = Objects.requireNonNull(activityService, "activityService must not be null");
        this.bookmarkService = Objects.requireNonNull(bookmarkService, "bookmarkService must not be null");
        this.questionRepository = Objects.requireNonNull(questionRepository, "questionRepository must not be null");
        this.topicRepository = Objects.requireNonNull(topicRepository, "topicRepository must not be null");
    }

    @Override
    public StudentProgressSummary getOverallProgress(UUID userId) {
        if (userId == null) {
            return StudentProgressSummary.empty(new UUID(0L, 0L));
        }

        List<QuestionAttempt> attempts = attemptService.getAttemptsForUser(userId);
        long totalAttempts = attempts.size();
        long solvedCount = attempts.stream().filter(a -> a.getStatus() == AttemptStatus.SOLVED).count();
        long failedCount = attempts.stream().filter(a -> a.getStatus() == AttemptStatus.FAILED).count();
        long skippedCount = attempts.stream().filter(a -> a.getStatus() == AttemptStatus.SKIPPED).count();

        long assessmentsCompleted = assessmentService.getAttemptsForUser(userId).stream()
                .filter(AssessmentAttempt::isCompleted)
                .count();

        long bookmarkedCount = bookmarkService.getBookmarkCount(userId);

        return StudentProgressSummary.calculate(
                userId,
                totalAttempts,
                solvedCount,
                failedCount,
                skippedCount,
                assessmentsCompleted,
                bookmarkedCount
        );
    }

    @Override
    public List<TopicPerformance> getTopicPerformances(UUID userId) {
        if (userId == null) {
            return List.of();
        }

        List<QuestionAttempt> attempts = attemptService.getAttemptsForUser(userId);
        if (attempts.isEmpty()) {
            return List.of();
        }

        // Cache questions and topics looked up for performance
        Map<Long, Question> questionMap = new HashMap<>();
        Map<Long, Topic> topicMap = new HashMap<>();

        // Group attempts by topic ID
        Map<Long, List<QuestionAttempt>> attemptsByTopic = new HashMap<>();

        for (QuestionAttempt attempt : attempts) {
            Question question = questionMap.computeIfAbsent(
                    attempt.getQuestionId(),
                    id -> questionRepository.findById(id).orElse(null)
            );

            if (question != null && question.getTopicId() > 0) {
                attemptsByTopic.computeIfAbsent(question.getTopicId(), k -> new ArrayList<>()).add(attempt);
            }
        }

        List<TopicPerformance> performances = new ArrayList<>();

        for (Map.Entry<Long, List<QuestionAttempt>> entry : attemptsByTopic.entrySet()) {
            long topicId = entry.getKey();
            List<QuestionAttempt> topicAttempts = entry.getValue();

            Topic topic = topicMap.computeIfAbsent(
                    topicId,
                    id -> topicRepository.findById(id).orElse(null)
            );
            String topicName = topic != null ? topic.getName() : "Topic #" + topicId;

            long total = topicAttempts.size();
            long solved = topicAttempts.stream().filter(a -> a.getStatus() == AttemptStatus.SOLVED).count();
            long failed = topicAttempts.stream().filter(a -> a.getStatus() == AttemptStatus.FAILED).count();
            long skipped = topicAttempts.stream().filter(a -> a.getStatus() == AttemptStatus.SKIPPED).count();

            performances.add(TopicPerformance.calculate(topicId, topicName, total, solved, failed, skipped));
        }

        // Sort alphabetically by topic name
        performances.sort(Comparator.comparing(TopicPerformance::topicName, String.CASE_INSENSITIVE_ORDER));
        return performances;
    }

    @Override
    public List<TopicPerformance> getWeakAreas(UUID userId) {
        if (userId == null) {
            return List.of();
        }

        List<TopicPerformance> topicPerformances = getTopicPerformances(userId);
        if (topicPerformances.isEmpty()) {
            return List.of();
        }

        long totalAttemptsAcrossTopics = topicPerformances.stream()
                .mapToLong(TopicPerformance::totalAttempts)
                .sum();

        // Require at least 3 total attempts across topics before diagnosing weak areas
        if (totalAttemptsAcrossTopics < 3) {
            return List.of();
        }

        // A topic appears if it has at least 2 attempts and accuracy is below 70%
        List<TopicPerformance> weakTopics = topicPerformances.stream()
                .filter(tp -> tp.totalAttempts() >= 2 && tp.accuracyPercentage() < 70.0)
                .sorted(Comparator.comparingDouble(TopicPerformance::accuracyPercentage))
                .toList();

        return weakTopics;
    }

    @Override
    public List<AssessmentAttemptSummary> getAssessmentHistory(UUID userId) {
        if (userId == null) {
            return List.of();
        }

        List<AssessmentAttempt> attempts = assessmentService.getAttemptsForUser(userId);
        if (attempts.isEmpty()) {
            return List.of();
        }

        Map<Long, Assessment> assessmentCache = new HashMap<>();
        List<AssessmentAttemptSummary> summaries = new ArrayList<>();

        for (AssessmentAttempt attempt : attempts) {
            if (!attempt.isCompleted()) {
                continue;
            }

            Assessment assessment = assessmentCache.computeIfAbsent(
                    attempt.getAssessmentId(),
                    id -> assessmentService.getAssessmentById(id).orElse(null)
            );

            String title = assessment != null ? assessment.getTitle() : "Assessment #" + attempt.getAssessmentId();
            int totalMarks = assessment != null ? assessment.getTotalMarks() : attempt.getScore();

            summaries.add(AssessmentAttemptSummary.of(
                    attempt.getId(),
                    attempt.getAssessmentId(),
                    title,
                    attempt.getScore(),
                    totalMarks,
                    attempt.getStatus(),
                    attempt.getCompletedAt()
            ));
        }

        // Sort descending by completion time (most recent first)
        summaries.sort((a, b) -> {
            if (a.completedAt() == null && b.completedAt() == null) return 0;
            if (a.completedAt() == null) return 1;
            if (b.completedAt() == null) return -1;
            return b.completedAt().compareTo(a.completedAt());
        });

        return summaries;
    }

    @Override
    public List<Activity> getRecentActivities(UUID userId, int limit) {
        if (userId == null) {
            return List.of();
        }
        int safeLimit = limit <= 0 ? 10 : limit;
        return activityService.getRecentActivities(userId, safeLimit);
    }

    @Override
    public DifficultyDistribution getDifficultyDistribution(UUID userId) {
        if (userId == null) {
            return DifficultyDistribution.empty();
        }

        List<QuestionAttempt> attempts = attemptService.getAttemptsForUser(userId);
        if (attempts.isEmpty()) {
            return DifficultyDistribution.empty();
        }

        Map<Long, Question> questionCache = new HashMap<>();
        long easyTotal = 0;
        long easySolved = 0;
        long mediumTotal = 0;
        long mediumSolved = 0;
        long hardTotal = 0;
        long hardSolved = 0;

        for (QuestionAttempt attempt : attempts) {
            Question question = questionCache.computeIfAbsent(
                    attempt.getQuestionId(),
                    id -> questionRepository.findById(id).orElse(null)
            );
            if (question == null) {
                continue;
            }

            boolean isSolved = (attempt.getStatus() == AttemptStatus.SOLVED);
            Difficulty diff = question.getDifficulty();

            if (diff == Difficulty.EASY) {
                easyTotal++;
                if (isSolved) easySolved++;
            } else if (diff == Difficulty.MEDIUM) {
                mediumTotal++;
                if (isSolved) mediumSolved++;
            } else if (diff == Difficulty.HARD) {
                hardTotal++;
                if (isSolved) hardSolved++;
            }
        }

        return new DifficultyDistribution(easyTotal, easySolved, mediumTotal, mediumSolved, hardTotal, hardSolved);
    }
}
