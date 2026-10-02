package com.byteforce.web.controller.admin;

import com.byteforce.domain.Assessment;
import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;
import com.byteforce.domain.Topic;
import com.byteforce.exception.ValidationException;
import com.byteforce.service.AssessmentService;
import com.byteforce.service.QuestionService;
import com.byteforce.service.TopicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for managing timed assessments and mock tests,
 * and configuring the questions assigned to each assessment.
 */
@Controller
@RequestMapping("/admin/assessments")
public class AdminAssessmentController {

    private static final Logger log = LoggerFactory.getLogger(AdminAssessmentController.class);

    private final AssessmentService assessmentService;
    private final QuestionService questionService;
    private final TopicService topicService;

    public AdminAssessmentController(AssessmentService assessmentService,
                                     QuestionService questionService,
                                     TopicService topicService) {
        this.assessmentService = assessmentService;
        this.questionService = questionService;
        this.topicService = topicService;
    }

    @GetMapping
    public String listAssessments(Model model) {
        List<Assessment> assessments = assessmentService.getAllAssessments();
        List<Topic> topics = topicService.getAllTopics();
        Map<Long, String> topicMap = topics.stream()
                .collect(Collectors.toMap(Topic::getId, Topic::getName, (a, b) -> a));

        model.addAttribute("assessments", assessments);
        model.addAttribute("topicMap", topicMap);
        model.addAttribute("activeTab", "admin-assessments");
        model.addAttribute("pageTitle", "Manage Assessments");
        return "admin/assessments/list";
    }

    @GetMapping("/new")
    public String newAssessmentForm(Model model) {
        Assessment assessment = Assessment.create("", "", 30, 30, Difficulty.MEDIUM, null);
        model.addAttribute("assessment", assessment);
        model.addAttribute("topics", topicService.getAllTopics());
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("isNew", true);
        model.addAttribute("activeTab", "admin-assessments");
        model.addAttribute("pageTitle", "Create Assessment");
        return "admin/assessments/form";
    }

    @GetMapping("/{id}/edit")
    public String editAssessmentForm(@PathVariable("id") long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Assessment> assessmentOpt = assessmentService.getAssessmentById(id);
        if (assessmentOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Assessment not found with ID: " + id);
            return "redirect:/admin/assessments";
        }
        model.addAttribute("assessment", assessmentOpt.get());
        model.addAttribute("topics", topicService.getAllTopics());
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("isNew", false);
        model.addAttribute("activeTab", "admin-assessments");
        model.addAttribute("pageTitle", "Edit Assessment — " + assessmentOpt.get().getTitle());
        return "admin/assessments/form";
    }

    @PostMapping("/save")
    public String saveAssessment(@RequestParam(value = "id", required = false, defaultValue = "0") long id,
                                 @RequestParam("title") String title,
                                 @RequestParam(value = "description", required = false, defaultValue = "") String description,
                                 @RequestParam("durationMinutes") int durationMinutes,
                                 @RequestParam("totalMarks") int totalMarks,
                                 @RequestParam("difficulty") Difficulty difficulty,
                                 @RequestParam(value = "topicId", required = false) Long topicId,
                                 @RequestParam(value = "active", required = false, defaultValue = "true") boolean active,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (title == null || title.isBlank()) {
                throw new ValidationException("Assessment title must not be blank.");
            }
            if (durationMinutes <= 0) {
                throw new ValidationException("Duration must be a positive number of minutes.");
            }
            if (totalMarks <= 0) {
                throw new ValidationException("Total marks must be a positive number.");
            }

            if (id <= 0) {
                Assessment created = assessmentService.createAssessment(title.trim(), description != null ? description.trim() : "", durationMinutes, totalMarks, difficulty, topicId);
                redirectAttributes.addFlashAttribute("successMessage", "Assessment created successfully! Now assign questions below.");
                return "redirect:/admin/assessments/" + created.getId() + "/questions";
            } else {
                assessmentService.updateAssessment(id, title.trim(), description != null ? description.trim() : "", durationMinutes, totalMarks, difficulty, topicId, active);
                redirectAttributes.addFlashAttribute("successMessage", "Assessment updated successfully.");
                return "redirect:/admin/assessments";
            }
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            if (id <= 0) {
                return "redirect:/admin/assessments/new";
            } else {
                return "redirect:/admin/assessments/" + id + "/edit";
            }
        } catch (Exception e) {
            log.error("Failed to save assessment", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save assessment: " + e.getMessage());
            return "redirect:/admin/assessments";
        }
    }

    @GetMapping("/{id}/questions")
    public String manageAssessmentQuestions(@PathVariable("id") long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Assessment> assessmentOpt = assessmentService.getAssessmentById(id);
        if (assessmentOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Assessment not found with ID: " + id);
            return "redirect:/admin/assessments";
        }

        Assessment assessment = assessmentOpt.get();
        List<Question> assignedQuestions = assessmentService.getQuestionsForAssessment(id);
        Map<Long, Integer> questionMarks = assessmentService.getQuestionMarks(id);

        Set<Long> assignedIds = assignedQuestions.stream().map(Question::getId).collect(Collectors.toSet());

        // Available questions from question bank to attach (not yet assigned)
        List<Question> availableQuestions = questionService.getAllQuestions().stream()
                .filter(q -> !assignedIds.contains(q.getId()))
                .collect(Collectors.toList());

        model.addAttribute("assessment", assessment);
        model.addAttribute("assignedQuestions", assignedQuestions);
        model.addAttribute("questionMarks", questionMarks);
        model.addAttribute("availableQuestions", availableQuestions);
        model.addAttribute("activeTab", "admin-assessments");
        model.addAttribute("pageTitle", "Manage Questions — " + assessment.getTitle());
        return "admin/assessments/questions";
    }

    @PostMapping("/{id}/questions/add")
    public String addQuestionToAssessment(@PathVariable("id") long id,
                                          @RequestParam("questionId") long questionId,
                                          @RequestParam(value = "marks", required = false, defaultValue = "10") int marks,
                                          @RequestParam(value = "questionOrder", required = false, defaultValue = "1") int questionOrder,
                                          RedirectAttributes redirectAttributes) {
        try {
            if (questionId <= 0) {
                throw new ValidationException("Please select a question from the question bank.");
            }
            if (marks <= 0) {
                throw new ValidationException("Marks must be positive.");
            }

            assessmentService.addQuestionToAssessment(id, questionId, marks, questionOrder);
            redirectAttributes.addFlashAttribute("successMessage", "Question attached to assessment successfully.");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to add question to assessment", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to assign question: " + e.getMessage());
        }
        return "redirect:/admin/assessments/" + id + "/questions";
    }

    @PostMapping("/{id}/questions/{questionId}/remove")
    public String removeQuestionFromAssessment(@PathVariable("id") long id,
                                               @PathVariable("questionId") long questionId,
                                               RedirectAttributes redirectAttributes) {
        try {
            assessmentService.removeQuestionFromAssessment(id, questionId);
            redirectAttributes.addFlashAttribute("successMessage", "Question removed from assessment.");
        } catch (Exception e) {
            log.error("Failed to remove question {} from assessment {}", questionId, id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to remove question: " + e.getMessage());
        }
        return "redirect:/admin/assessments/" + id + "/questions";
    }

    @PostMapping("/{id}/delete")
    public String deleteAssessment(@PathVariable("id") long id, RedirectAttributes redirectAttributes) {
        try {
            assessmentService.deleteAssessment(id);
            redirectAttributes.addFlashAttribute("successMessage", "Assessment deleted successfully.");
        } catch (Exception e) {
            log.error("Failed to delete assessment {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete assessment: " + e.getMessage());
        }
        return "redirect:/admin/assessments";
    }
}
