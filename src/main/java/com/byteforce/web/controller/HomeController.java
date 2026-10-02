package com.byteforce.web.controller;

import com.byteforce.service.AssessmentService;
import com.byteforce.service.LearnService;
import com.byteforce.service.QuestionService;
import com.byteforce.service.TopicService;
import com.byteforce.web.util.WebSessionUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Public Landing Page controller and Authentication-Required flow handler.
 */
@Controller
public class HomeController {

    private final LearnService learnService;
    private final TopicService topicService;
    private final QuestionService questionService;
    private final AssessmentService assessmentService;

    public HomeController(LearnService learnService,
                          TopicService topicService,
                          QuestionService questionService,
                          AssessmentService assessmentService) {
        this.learnService = learnService;
        this.topicService = topicService;
        this.questionService = questionService;
        this.assessmentService = assessmentService;
    }

    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        model.addAttribute("isAuthenticated", WebSessionUtil.isAuthenticated(session));
        model.addAttribute("subjects", learnService.getAllSubjects());
        model.addAttribute("topics", topicService.getAllTopics());
        model.addAttribute("totalQuestions", questionService.getTotalQuestionCount());
        model.addAttribute("assessments", assessmentService.getAvailableAssessments());
        return "index";
    }

    @GetMapping("/auth-required")
    public String authRequired(@RequestParam(value = "returnUrl", required = false, defaultValue = "/dashboard") String returnUrl,
                               @RequestParam(value = "feature", required = false, defaultValue = "this protected feature") String feature,
                               Model model) {
        model.addAttribute("returnUrl", returnUrl);
        model.addAttribute("feature", feature);
        return "auth-required";
    }
}
