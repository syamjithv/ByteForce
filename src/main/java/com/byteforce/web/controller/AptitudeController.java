package com.byteforce.web.controller;

import com.byteforce.domain.AptitudeCategory;
import com.byteforce.domain.AptitudeQuestion;
import com.byteforce.service.AptitudeQuestionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Objects;

/**
 * Controller for the student-facing Aptitude preparation module (/aptitude).
 * Supports Quantitative Aptitude, Logical Reasoning, and Verbal Ability practice questions.
 */
@Controller
@RequestMapping("/aptitude")
public class AptitudeController {

    private final AptitudeQuestionService aptitudeQuestionService;

    public AptitudeController(AptitudeQuestionService aptitudeQuestionService) {
        this.aptitudeQuestionService = Objects.requireNonNull(aptitudeQuestionService, "aptitudeQuestionService must not be null");
    }

    @GetMapping
    public String aptitudeIndex(
            @RequestParam(value = "category", required = false) String categoryStr,
            @RequestParam(value = "topic", required = false) String topic,
            Model model) {

        AptitudeCategory selectedCategory = null;
        if (categoryStr != null && !categoryStr.isBlank() && !"all".equalsIgnoreCase(categoryStr)) {
            try {
                selectedCategory = AptitudeCategory.valueOf(categoryStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // fallback to all
            }
        }

        List<AptitudeQuestion> questions;
        if (selectedCategory != null) {
            questions = aptitudeQuestionService.getQuestionsByCategory(selectedCategory);
        } else if (topic != null && !topic.isBlank()) {
            questions = aptitudeQuestionService.getQuestionsByTopic(topic.trim());
        } else {
            questions = aptitudeQuestionService.getAllActiveQuestions();
        }

        long quantCount = aptitudeQuestionService.getQuestionCountByCategory(AptitudeCategory.QUANTITATIVE);
        long logicalCount = aptitudeQuestionService.getQuestionCountByCategory(AptitudeCategory.LOGICAL);
        long verbalCount = aptitudeQuestionService.getQuestionCountByCategory(AptitudeCategory.VERBAL);
        long totalCount = quantCount + logicalCount + verbalCount;

        model.addAttribute("questions", questions);
        model.addAttribute("categories", AptitudeCategory.values());
        model.addAttribute("selectedCategory", selectedCategory != null ? selectedCategory.name() : "all");
        model.addAttribute("selectedTopic", topic != null ? topic : "");
        model.addAttribute("quantCount", quantCount);
        model.addAttribute("logicalCount", logicalCount);
        model.addAttribute("verbalCount", verbalCount);
        model.addAttribute("totalCount", totalCount);

        return "aptitude/index";
    }
}
