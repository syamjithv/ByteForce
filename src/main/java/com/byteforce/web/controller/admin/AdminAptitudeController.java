package com.byteforce.web.controller.admin;

import com.byteforce.domain.AptitudeCategory;
import com.byteforce.domain.AptitudeQuestion;
import com.byteforce.domain.Difficulty;
import com.byteforce.exception.ValidationException;
import com.byteforce.service.AptitudeQuestionService;
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
import java.util.Optional;

/**
 * Controller for managing placement aptitude questions:
 * Quantitative Aptitude, Logical Reasoning, and Verbal Ability.
 */
@Controller
@RequestMapping("/admin/aptitude")
public class AdminAptitudeController {

    private static final Logger log = LoggerFactory.getLogger(AdminAptitudeController.class);

    private final AptitudeQuestionService aptitudeQuestionService;

    public AdminAptitudeController(AptitudeQuestionService aptitudeQuestionService) {
        this.aptitudeQuestionService = aptitudeQuestionService;
    }

    @GetMapping
    public String listAptitudeQuestions(@RequestParam(value = "category", required = false) AptitudeCategory category,
                                        @RequestParam(value = "difficulty", required = false) Difficulty difficulty,
                                        Model model) {
        List<AptitudeQuestion> questions;
        if (category != null && difficulty != null) {
            questions = aptitudeQuestionService.getAptitudeQuestionsByCategoryAndDifficulty(category, difficulty);
        } else if (category != null) {
            questions = aptitudeQuestionService.getAptitudeQuestionsByCategory(category);
        } else if (difficulty != null) {
            questions = aptitudeQuestionService.getAptitudeQuestionsByDifficulty(difficulty);
        } else {
            questions = aptitudeQuestionService.getAllAptitudeQuestions();
        }

        model.addAttribute("questions", questions);
        model.addAttribute("categories", AptitudeCategory.values());
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedDifficulty", difficulty);
        model.addAttribute("activeTab", "admin-aptitude");
        model.addAttribute("pageTitle", "Manage Aptitude Questions");
        return "admin/aptitude/list";
    }

    @GetMapping("/new")
    public String newAptitudeQuestionForm(@RequestParam(value = "category", required = false) AptitudeCategory category, Model model) {
        AptitudeQuestion question = AptitudeQuestion.create(
                category != null ? category : AptitudeCategory.QUANTITATIVE,
                "", Difficulty.EASY, "", "", "", "", "", "A", ""
        );
        model.addAttribute("question", question);
        model.addAttribute("categories", AptitudeCategory.values());
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("isNew", true);
        model.addAttribute("activeTab", "admin-aptitude");
        model.addAttribute("pageTitle", "Add Aptitude Question");
        return "admin/aptitude/form";
    }

    @GetMapping("/{id}/edit")
    public String editAptitudeQuestionForm(@PathVariable("id") long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<AptitudeQuestion> questionOpt = aptitudeQuestionService.getAptitudeQuestionById(id);
        if (questionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Aptitude question not found with ID: " + id);
            return "redirect:/admin/aptitude";
        }
        model.addAttribute("question", questionOpt.get());
        model.addAttribute("categories", AptitudeCategory.values());
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("isNew", false);
        model.addAttribute("activeTab", "admin-aptitude");
        model.addAttribute("pageTitle", "Edit Aptitude Question");
        return "admin/aptitude/form";
    }

    @PostMapping("/save")
    public String saveAptitudeQuestion(@RequestParam(value = "id", required = false, defaultValue = "0") long id,
                                       @RequestParam("category") AptitudeCategory category,
                                       @RequestParam("topic") String topic,
                                       @RequestParam("difficulty") Difficulty difficulty,
                                       @RequestParam("question") String question,
                                       @RequestParam("optionA") String optionA,
                                       @RequestParam("optionB") String optionB,
                                       @RequestParam(value = "optionC", required = false, defaultValue = "") String optionC,
                                       @RequestParam(value = "optionD", required = false, defaultValue = "") String optionD,
                                       @RequestParam("correctAnswer") String correctAnswer,
                                       @RequestParam(value = "explanation", required = false, defaultValue = "") String explanation,
                                       @RequestParam(value = "active", required = false, defaultValue = "true") boolean active,
                                       RedirectAttributes redirectAttributes) {
        try {
            if (topic == null || topic.isBlank()) {
                throw new ValidationException("Aptitude topic must not be blank.");
            }
            if (question == null || question.isBlank()) {
                throw new ValidationException("Question text must not be blank.");
            }
            if (optionA == null || optionA.isBlank() || optionB == null || optionB.isBlank()) {
                throw new ValidationException("Options A and B are required.");
            }
            if (correctAnswer == null || correctAnswer.isBlank()) {
                throw new ValidationException("Correct answer option must be specified (e.g. A, B, C, or D).");
            }

            if (id <= 0) {
                aptitudeQuestionService.createAptitudeQuestion(category, topic.trim(), difficulty, question.trim(),
                        optionA.trim(), optionB.trim(), optionC.trim(), optionD.trim(), correctAnswer.trim().toUpperCase(), explanation != null ? explanation.trim() : "");
                redirectAttributes.addFlashAttribute("successMessage", "Aptitude question created successfully.");
            } else {
                aptitudeQuestionService.updateAptitudeQuestion(id, category, topic.trim(), difficulty, question.trim(),
                        optionA.trim(), optionB.trim(), optionC.trim(), optionD.trim(), correctAnswer.trim().toUpperCase(), explanation != null ? explanation.trim() : "", active);
                redirectAttributes.addFlashAttribute("successMessage", "Aptitude question updated successfully.");
            }
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            if (id <= 0) {
                return "redirect:/admin/aptitude/new";
            } else {
                return "redirect:/admin/aptitude/" + id + "/edit";
            }
        } catch (Exception e) {
            log.error("Failed to save aptitude question", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save aptitude question: " + e.getMessage());
        }
        return "redirect:/admin/aptitude";
    }

    @PostMapping("/{id}/delete")
    public String deleteAptitudeQuestion(@PathVariable("id") long id, RedirectAttributes redirectAttributes) {
        try {
            aptitudeQuestionService.deleteAptitudeQuestion(id);
            redirectAttributes.addFlashAttribute("successMessage", "Aptitude question deleted successfully.");
        } catch (Exception e) {
            log.error("Failed to delete aptitude question {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete aptitude question: " + e.getMessage());
        }
        return "redirect:/admin/aptitude";
    }
}
