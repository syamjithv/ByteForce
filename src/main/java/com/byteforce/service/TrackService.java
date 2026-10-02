package com.byteforce.service;

import com.byteforce.domain.Activity;
import com.byteforce.domain.AssessmentAttemptSummary;
import com.byteforce.domain.DifficultyDistribution;
import com.byteforce.domain.StudentProgressSummary;
import com.byteforce.domain.TopicPerformance;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for aggregating student preparation progress,
 * topic performance, weak areas, assessment history, and activity timeline.
 */
public interface TrackService {

    /**
     * Retrieves overall preparation metrics for the student.
     *
     * @param userId student user ID
     * @return summary of questions attempted, solved, failed, skipped, accuracy, assessments, and bookmarks
     */
    StudentProgressSummary getOverallProgress(UUID userId);

    /**
     * Retrieves practice problem performance grouped by topic.
     * Only returns topics for which real attempt data exists.
     *
     * @param userId student user ID
     * @return list of topic performances
     */
    List<TopicPerformance> getTopicPerformances(UUID userId);

    /**
     * Identifies areas where the student requires improvement based on practice attempt accuracy.
     * Returns an empty list if there is insufficient attempt data.
     *
     * @param userId student user ID
     * @return list of weak topic performances, or empty if insufficient data
     */
    List<TopicPerformance> getWeakAreas(UUID userId);

    /**
     * Retrieves recent completed assessment attempt summaries for the student.
     *
     * @param userId student user ID
     * @return list of assessment attempts with scores and timestamps
     */
    List<AssessmentAttemptSummary> getAssessmentHistory(UUID userId);

    /**
     * Retrieves the recent activity log for the student.
     *
     * @param userId student user ID
     * @param limit maximum number of activities to return
     * @return list of recent activities
     */
    List<Activity> getRecentActivities(UUID userId, int limit);

    /**
     * Retrieves the breakdown of question attempts and solves categorized by difficulty.
     *
     * @param userId student user ID
     * @return difficulty distribution metrics
     */
    DifficultyDistribution getDifficultyDistribution(UUID userId);
}
