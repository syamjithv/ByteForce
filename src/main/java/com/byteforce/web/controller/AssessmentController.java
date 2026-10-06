package com.byteforce.web.controller;

import com.byteforce.domain.Assessment;
import com.byteforce.domain.AssessmentAnswer;
import com.byteforce.domain.AssessmentAttempt;
import com.byteforce.domain.AttemptStatus;
import com.byteforce.domain.Question;
import com.byteforce.domain.QuestionType;
import com.byteforce.domain.User;
import com.byteforce.service.AssessmentService;
import com.byteforce.service.QuestionService;
import com.byteforce.web.util.WebSessionUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for the Assessment module:
 * Assessment Catalog -> Briefing -> Timed Mock Test -> Results -> Answer Review.
 */
@Controller
@RequestMapping("/assessments")
public class AssessmentController {

    private static final Logger log = LoggerFactory.getLogger(AssessmentController.class);

    private final AssessmentService assessmentService;
    private final QuestionService questionService;

    public AssessmentController(AssessmentService assessmentService,
                                QuestionService questionService) {
        this.assessmentService = assessmentService;
        this.questionService = questionService;
    }

    @GetMapping
    public String assessmentsCatalog(Model model, HttpSession session) {
        List<Assessment> assessments = assessmentService.getAvailableAssessments();
        model.addAttribute("assessments", assessments);

        // Fetch question counts per assessment
        Map<Long, Integer> questionCounts = new HashMap<>();
        for (Assessment a : assessments) {
            questionCounts.put(a.getId(), assessmentService.getQuestionsForAssessment(a.getId()).size());
        }
        model.addAttribute("questionCounts", questionCounts);

        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);
        if (userOpt.isPresent()) {
            model.addAttribute("activeResumeAttempt", assessmentService.getActiveResumeAttempt(userOpt.get().getId()).orElse(null));
        }

        return "assessments/index";
    }

    @GetMapping("/briefing/{id}")
    public String assessmentBriefing(@PathVariable("id") long id, Model model) {
        Optional<Assessment> assessmentOpt = assessmentService.getAssessmentById(id);
        if (assessmentOpt.isEmpty()) {
            return "redirect:/assessments";
        }

        Assessment assessment = assessmentOpt.get();
        List<Question> questions = assessmentService.getQuestionsForAssessment(id);

        model.addAttribute("assessment", assessment);
        model.addAttribute("questionCount", questions.size());
        return "assessments/briefing";
    }

    @GetMapping("/test/{id}")
    public String startOrResumeTest(@PathVariable("id") long id,
                                    Model model,
                                    HttpSession session) {
        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);
        if (userOpt.isEmpty()) {
            return "redirect:/auth-required?returnUrl=/assessments/briefing/" + id + "&feature=taking timed assessments";
        }

        User user = userOpt.get();
        Optional<Assessment> assessmentOpt = assessmentService.getAssessmentById(id);
        if (assessmentOpt.isEmpty()) {
            return "redirect:/assessments";
        }

        Assessment assessment = assessmentOpt.get();
        List<Question> questions = assessmentService.getQuestionsForAssessment(id);
        Map<Long, Integer> marksMap = assessmentService.getQuestionMarks(id);

        // Start attempt using core service
        AssessmentAttempt attempt = assessmentService.startAssessment(user.getId(), id);

        model.addAttribute("assessment", assessment);
        model.addAttribute("attempt", attempt);
        model.addAttribute("questions", questions);
        model.addAttribute("marksMap", marksMap);
        model.addAttribute("durationMinutes", assessment.getDurationMinutes());

        return "assessments/test";
    }

    @PostMapping("/submit/{attemptId}")
    public String submitTest(@PathVariable("attemptId") long attemptId,
                             HttpServletRequest request,
                             HttpSession session) {
        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);
        if (userOpt.isEmpty()) {
            return "redirect:/auth-required?returnUrl=/assessments";
        }

        // Collect answers from request parameters: answer_123 -> value
        Map<Long, String> submittedAnswers = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            if (name.startsWith("answer_")) {
                try {
                    long questionId = Long.parseLong(name.substring("answer_".length()));
                    String answer = request.getParameter(name);
                    if (answer != null && !answer.isBlank()) {
                        submittedAnswers.put(questionId, answer.trim());
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        try {
            AssessmentAttempt completedAttempt = assessmentService.submitAssessment(attemptId, submittedAnswers);
            log.info("Submitted assessment attempt {}: score={}", attemptId, completedAttempt.getScore());
            return "redirect:/assessments/result/" + attemptId;
        } catch (Exception e) {
            log.error("Failed to submit assessment attempt {}", attemptId, e);
            return "redirect:/assessments";
        }
    }

    @GetMapping("/result/{attemptId}")
    public String testResult(@PathVariable("attemptId") long attemptId,
                             Model model,
                             HttpSession session) {
        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);
        if (userOpt.isEmpty()) {
            return "redirect:/auth-required?returnUrl=/assessments";
        }

        Optional<AssessmentAttempt> attemptOpt = assessmentService.getAttemptById(attemptId);
        if (attemptOpt.isEmpty()) {
            return "redirect:/assessments";
        }

        AssessmentAttempt attempt = attemptOpt.get();
        Optional<Assessment> assessmentOpt = assessmentService.getAssessmentById(attempt.getAssessmentId());
        if (assessmentOpt.isEmpty()) {
            return "redirect:/assessments";
        }

        Assessment assessment = assessmentOpt.get();
        List<AssessmentAnswer> answers = assessmentService.getAnswersForAttempt(attemptId);

        int totalQuestions = answers.size();
        long correctCount = answers.stream().filter(a -> a.getStatus() == AttemptStatus.SOLVED).count();
        long failedCount = answers.stream().filter(a -> a.getStatus() == AttemptStatus.FAILED).count();
        long skippedCount = answers.stream().filter(a -> a.getStatus() == AttemptStatus.SKIPPED).count();

        double percentage = (assessment.getTotalMarks() > 0)
                ? ((double) attempt.getScore() / assessment.getTotalMarks()) * 100.0
                : 0.0;

        model.addAttribute("attempt", attempt);
        model.addAttribute("assessment", assessment);
        model.addAttribute("totalQuestions", totalQuestions);
        model.addAttribute("correctCount", correctCount);
        model.addAttribute("failedCount", failedCount);
        model.addAttribute("skippedCount", skippedCount);
        model.addAttribute("percentage", String.format("%.1f", percentage));

        return "assessments/result";
    }

    @GetMapping("/review/{attemptId}")
    public String reviewAnswers(@PathVariable("attemptId") long attemptId,
                                Model model,
                                HttpSession session) {
        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);
        if (userOpt.isEmpty()) {
            return "redirect:/auth-required?returnUrl=/assessments";
        }

        Optional<AssessmentAttempt> attemptOpt = assessmentService.getAttemptById(attemptId);
        if (attemptOpt.isEmpty()) {
            return "redirect:/assessments";
        }

        AssessmentAttempt attempt = attemptOpt.get();
        Optional<Assessment> assessmentOpt = assessmentService.getAssessmentById(attempt.getAssessmentId());
        if (assessmentOpt.isEmpty()) {
            return "redirect:/assessments";
        }

        Assessment assessment = assessmentOpt.get();
        List<AssessmentAnswer> answers = assessmentService.getAnswersForAttempt(attemptId);
        Map<Long, Integer> marksMap = assessmentService.getQuestionMarks(assessment.getId());

        // Associate question entity with each answer
        List<ReviewItem> reviewItems = new ArrayList<>();
        for (AssessmentAnswer ans : answers) {
            Optional<Question> questionOpt = questionService.getQuestionById(ans.getQuestionId());
            if (questionOpt.isPresent()) {
                Question q = questionOpt.get();
                int maxMarks = marksMap.getOrDefault(q.getId(), 1);
                reviewItems.add(new ReviewItem(q, ans, maxMarks));
            }
        }

        model.addAttribute("attempt", attempt);
        model.addAttribute("assessment", assessment);
        model.addAttribute("reviewItems", reviewItems);
        model.addAttribute("totalQuestions", reviewItems.size());

        return "assessments/review";
    }

    public record ReviewItem(Question question, AssessmentAnswer answer, int maxMarks) {}
}
