package com.byteforce.web.controller;

import com.byteforce.domain.AttemptStatus;
import com.byteforce.domain.Question;
import com.byteforce.domain.QuestionAttempt;
import com.byteforce.domain.Topic;
import com.byteforce.domain.User;
import com.byteforce.service.AttemptService;
import com.byteforce.service.QuestionService;
import com.byteforce.service.TopicService;
import com.byteforce.web.util.WebSessionUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Controller for the enhanced Interview Preparation module (/interview).
 * Provides interactive preparation tracks for:
 * 1. Technical Interview Preparation (DSA, DBMS, OS, Networks, OOP/Java)
 * 2. HR & Behavioral Interview Masterclass (STAR Method, standard behavioral scenarios)
 * 3. Company Interview Round Blueprints
 * 4. Curated Placement Interview Question Cards with solutions
 * 5. Real Student Readiness Stats from database attempts (with honest empty states)
 */
@Controller
@RequestMapping("/interview")
public class InterviewController {

    private final QuestionService questionService;
    private final TopicService topicService;
    private final AttemptService attemptService;

    public InterviewController(QuestionService questionService,
                               TopicService topicService,
                               AttemptService attemptService) {
        this.questionService = Objects.requireNonNull(questionService, "questionService must not be null");
        this.topicService = Objects.requireNonNull(topicService, "topicService must not be null");
        this.attemptService = Objects.requireNonNull(attemptService, "attemptService must not be null");
    }

    @GetMapping
    public String interviewHome(Model model, HttpSession session) {
        // Fetch real practice stats for current user
        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);
        long solvedCount = 0;
        long totalAttempts = 0;

        if (userOpt.isPresent()) {
            List<QuestionAttempt> attempts = attemptService.getAttemptsForUser(userOpt.get().getId());
            totalAttempts = attempts.size();
            solvedCount = attempts.stream().filter(a -> a.getStatus() == AttemptStatus.SOLVED).count();
        }

        // Fetch curated interview questions from question bank
        List<Question> interviewQuestions = questionService.getAllQuestions().stream()
                .limit(6)
                .toList();

        // Fetch core topics for technical prep
        List<Topic> topics = topicService.getAllTopics();

        model.addAttribute("solvedCount", solvedCount);
        model.addAttribute("totalAttempts", totalAttempts);
        model.addAttribute("hasAttempts", totalAttempts > 0);
        model.addAttribute("interviewQuestions", interviewQuestions);
        model.addAttribute("topics", topics);

        return "interview/index";
    }
}
