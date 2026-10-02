package com.byteforce.web.controller.admin;

import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;
import com.byteforce.domain.QuestionType;
import com.byteforce.domain.Topic;
import com.byteforce.exception.ValidationException;
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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Controller for managing the ByteForce question bank:
 * Coding challenges, MCQs, SQL problems, and Conceptual interview questions.
 */
@Controller
@RequestMapping("/admin/questions")
public class AdminQuestionController {

    private static final Logger log = LoggerFactory.getLogger(AdminQuestionController.class);

    private final QuestionService questionService;
    private final TopicService topicService;

    public AdminQuestionController(QuestionService questionService, TopicService topicService) {
        this.questionService = questionService;
        this.topicService = topicService;
    }

    @GetMapping
    public String listQuestions(@RequestParam(value = "topicId", required = false) Long topicId,
                                @RequestParam(value = "difficulty", required = false) Difficulty difficulty,
                                @RequestParam(value = "questionType", required = false) QuestionType questionType,
                                @RequestParam(value = "q", required = false) String searchKeyword,
                                Model model) {
        List<Question> questions;

        if (searchKeyword != null && !searchKeyword.isBlank()) {
            questions = questionService.searchQuestions(searchKeyword.trim());
        } else if (topicId != null && topicId > 0 && difficulty != null) {
            questions = questionService.getQuestionsByTopicAndDifficulty(topicId, difficulty);
        } else if (topicId != null && topicId > 0) {
            questions = questionService.getQuestionsByTopic(topicId);
        } else if (difficulty != null) {
            questions = questionService.getQuestionsByDifficulty(difficulty);
        } else if (questionType != null) {
            questions = questionService.getQuestionsByType(questionType);
        } else {
            questions = questionService.getAllQuestions();
        }

        // Map topic names for display in table
        List<Topic> topics = topicService.getAllTopics();
        Map<Long, Topic> topicMap = topics.stream()
                .collect(Collectors.toMap(Topic::getId, Function.identity(), (a, b) -> a));

        model.addAttribute("questions", questions);
        model.addAttribute("topics", topics);
        model.addAttribute("topicMap", topicMap);
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("questionTypes", QuestionType.values());

        model.addAttribute("selectedTopicId", topicId);
        model.addAttribute("selectedDifficulty", difficulty);
        model.addAttribute("selectedQuestionType", questionType);
        model.addAttribute("searchKeyword", searchKeyword != null ? searchKeyword.trim() : "");

        model.addAttribute("activeTab", "admin-questions");
        model.addAttribute("pageTitle", "Manage Questions");
        return "admin/questions/list";
    }

    @GetMapping("/new")
    public String newQuestionForm(@RequestParam(value = "topicId", required = false) Long topicId, Model model) {
        Question question = Question.create(topicId != null ? topicId : 0L, "", "", "", Difficulty.EASY, QuestionType.CODING, "");
        model.addAttribute("question", question);
        model.addAttribute("topics", topicService.getAllTopics());
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("questionTypes", QuestionType.values());
        model.addAttribute("isNew", true);
        model.addAttribute("activeTab", "admin-questions");
        model.addAttribute("pageTitle", "Add New Question");
        return "admin/questions/form";
    }

    @GetMapping("/{id}/edit")
    public String editQuestionForm(@PathVariable("id") long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Question> questionOpt = questionService.getQuestionById(id);
        if (questionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Question not found with ID: " + id);
            return "redirect:/admin/questions";
        }
        model.addAttribute("question", questionOpt.get());
        model.addAttribute("topics", topicService.getAllTopics());
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("questionTypes", QuestionType.values());
        model.addAttribute("isNew", false);
        model.addAttribute("activeTab", "admin-questions");
        model.addAttribute("pageTitle", "Edit Question — " + questionOpt.get().getTitle());
        return "admin/questions/form";
    }

    @PostMapping("/save")
    public String saveQuestion(@RequestParam(value = "id", required = false, defaultValue = "0") long id,
                               @RequestParam("topicId") long topicId,
                               @RequestParam("title") String title,
                               @RequestParam("slug") String slug,
                               @RequestParam("description") String description,
                               @RequestParam("difficulty") Difficulty difficulty,
                               @RequestParam("questionType") QuestionType questionType,
                               @RequestParam(value = "solution", required = false, defaultValue = "") String solution,
                               RedirectAttributes redirectAttributes) {
        try {
            if (topicId <= 0) {
                throw new ValidationException("Please select an associated Topic.");
            }
            if (title == null || title.isBlank()) {
                throw new ValidationException("Question title must not be blank.");
            }
            if (slug == null || slug.isBlank()) {
                throw new ValidationException("Question slug must not be blank.");
            }
            if (description == null || description.isBlank()) {
                throw new ValidationException("Question description / prompt must not be blank.");
            }

            if (id <= 0) {
                questionService.createQuestion(topicId, title.trim(), slug.trim().toLowerCase(), description.trim(), difficulty, questionType, solution != null ? solution.trim() : "");
                redirectAttributes.addFlashAttribute("successMessage", "Question '" + title.trim() + "' created successfully.");
            } else {
                questionService.updateQuestion(id, topicId, title.trim(), slug.trim().toLowerCase(), description.trim(), difficulty, questionType, solution != null ? solution.trim() : "");
                redirectAttributes.addFlashAttribute("successMessage", "Question '" + title.trim() + "' updated successfully.");
            }
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            if (id <= 0) {
                return "redirect:/admin/questions/new" + (topicId > 0 ? "?topicId=" + topicId : "");
            } else {
                return "redirect:/admin/questions/" + id + "/edit";
            }
        } catch (Exception e) {
            log.error("Failed to save question", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save question: " + e.getMessage());
        }
        return "redirect:/admin/questions";
    }

    @PostMapping("/{id}/delete")
    public String deleteQuestion(@PathVariable("id") long id, RedirectAttributes redirectAttributes) {
        try {
            questionService.deleteQuestion(id);
            redirectAttributes.addFlashAttribute("successMessage", "Question deleted successfully.");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to delete question {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete question: " + e.getMessage());
        }
        return "redirect:/admin/questions";
    }
}
