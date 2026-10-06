package com.byteforce.web.controller;

import com.byteforce.domain.Activity;
import com.byteforce.domain.StudentProgressSummary;
import com.byteforce.domain.User;
import com.byteforce.service.AssessmentService;
import com.byteforce.service.TrackService;
import com.byteforce.web.util.WebSessionUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the authenticated Student Dashboard.
 */
@Controller
public class DashboardController {

    private final TrackService trackService;
    private final AssessmentService assessmentService;
    private final com.byteforce.service.ConceptProgressService conceptProgressService;

    @org.springframework.beans.factory.annotation.Autowired
    public DashboardController(TrackService trackService,
                               AssessmentService assessmentService,
                               @org.springframework.beans.factory.annotation.Autowired(required = false) com.byteforce.service.ConceptProgressService conceptProgressService) {
        this.trackService = trackService;
        this.assessmentService = assessmentService;
        this.conceptProgressService = conceptProgressService;
    }

    public DashboardController(TrackService trackService, AssessmentService assessmentService) {
        this(trackService, assessmentService, null);
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(value = "welcome", required = false) Boolean welcome,
                            Model model,
                            HttpSession session) {
        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);
        if (userOpt.isEmpty()) {
            return "redirect:/auth-required?returnUrl=/dashboard&feature=the student dashboard";
        }

        User user = userOpt.get();
        model.addAttribute("user", user);
        model.addAttribute("welcome", Boolean.TRUE.equals(welcome));

        // Time-based greeting
        LocalTime now = LocalTime.now();
        String greeting = (now.getHour() < 12) ? "Good morning" :
                (now.getHour() < 17) ? "Good afternoon" : "Good evening";
        model.addAttribute("greeting", greeting);

        // Fetch real analytics data
        StudentProgressSummary progress = trackService.getOverallProgress(user.getId());
        model.addAttribute("progress", progress);

        List<Activity> recentActivities = trackService.getRecentActivities(user.getId(), 5);
        model.addAttribute("recentActivities", recentActivities);

        // Determine if user has activity yet
        boolean isNewStudent = (progress.totalAttempts() == 0 && progress.assessmentsCompleted() == 0);
        model.addAttribute("isNewStudent", isNewStudent);

        // Available mock assessments count
        model.addAttribute("availableAssessments", assessmentService.getAvailableAssessments());

        // Assessment in progress resume card
        model.addAttribute("activeResumeAttempt", assessmentService.getActiveResumeAttempt(user.getId()).orElse(null));

        // Continue Learning & Recently Viewed
        if (conceptProgressService != null) {
            model.addAttribute("continueLearning", conceptProgressService.getContinueLearning(user.getId()).orElse(null));
            model.addAttribute("recentlyViewedConcepts", conceptProgressService.getRecentlyViewed(user.getId(), 5));
        } else {
            model.addAttribute("continueLearning", null);
            model.addAttribute("recentlyViewedConcepts", List.of());
        }

        return "dashboard";
    }
}
