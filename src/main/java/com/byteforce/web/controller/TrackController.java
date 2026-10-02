package com.byteforce.web.controller;

import com.byteforce.domain.Activity;
import com.byteforce.domain.AssessmentAttemptSummary;
import com.byteforce.domain.DifficultyDistribution;
import com.byteforce.domain.StudentProgressSummary;
import com.byteforce.domain.TopicPerformance;
import com.byteforce.domain.User;
import com.byteforce.service.TrackService;
import com.byteforce.web.util.WebSessionUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Optional;

/**
 * Controller for the Track & Progress analytics dashboard (/track).
 * Aggregates overall preparation metrics, topic-level performance bars,
 * difficulty distribution, weak diagnostic areas, assessment score progression,
 * and activity timeline with strictly honest empty states.
 */
@Controller
@RequestMapping("/track")
public class TrackController {

    private final TrackService trackService;

    public TrackController(TrackService trackService) {
        this.trackService = trackService;
    }

    @GetMapping
    public String trackDashboard(Model model, HttpSession session) {
        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);
        if (userOpt.isEmpty()) {
            return "redirect:/auth-required?returnUrl=/track&feature=tracking preparation analytics and progress";
        }

        User user = userOpt.get();
        StudentProgressSummary overall = trackService.getOverallProgress(user.getId());
        List<TopicPerformance> topicPerformances = trackService.getTopicPerformances(user.getId());
        List<TopicPerformance> weakAreas = trackService.getWeakAreas(user.getId());
        List<AssessmentAttemptSummary> assessmentHistory = trackService.getAssessmentHistory(user.getId());
        List<Activity> activities = trackService.getRecentActivities(user.getId(), 15);
        DifficultyDistribution difficultyDistribution = trackService.getDifficultyDistribution(user.getId());

        model.addAttribute("overall", overall);
        model.addAttribute("topicPerformances", topicPerformances);
        model.addAttribute("weakAreas", weakAreas);
        model.addAttribute("assessmentHistory", assessmentHistory);
        model.addAttribute("activities", activities);
        model.addAttribute("difficultyDistribution", difficultyDistribution);

        boolean hasActivity = (overall.totalAttempts() > 0 || overall.assessmentsCompleted() > 0);
        model.addAttribute("hasActivity", hasActivity);

        // Placement Readiness Index (0 - 100%)
        int readinessScore = 0;
        if (hasActivity) {
            double accuracyFactor = overall.accuracyPercentage() * 0.5; // up to 50 pts
            double topicFactor = Math.min(topicPerformances.size() * 6.0, 30.0); // up to 30 pts for 5 topics
            double testFactor = Math.min(overall.assessmentsCompleted() * 10.0, 20.0); // up to 20 pts for 2 tests
            readinessScore = (int) Math.round(accuracyFactor + topicFactor + testFactor);
            readinessScore = Math.min(readinessScore, 100);
        }
        model.addAttribute("readinessScore", readinessScore);

        return "track/index";
    }
}
