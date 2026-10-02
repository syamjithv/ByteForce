package com.byteforce.web.controller;

import com.byteforce.domain.Concept;
import com.byteforce.domain.Subject;
import com.byteforce.domain.Topic;
import com.byteforce.service.LearnService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

/**
 * Controller for the Learn module: Subjects -> Topics -> Concepts -> Resources & Search.
 * Fully browsable by public visitors as well as authenticated students.
 */
@Controller
@RequestMapping("/learn")
public class LearnController {

    private final LearnService learnService;
    private final com.byteforce.service.RememberService rememberService;
    private final com.byteforce.service.BrainMapService brainMapService;

    public LearnController(LearnService learnService,
                           com.byteforce.service.RememberService rememberService,
                           com.byteforce.service.BrainMapService brainMapService) {
        this.learnService = learnService;
        this.rememberService = rememberService;
        this.brainMapService = brainMapService;
    }

    @GetMapping
    public String learnHome(Model model) {
        List<Subject> subjects = learnService.getAllSubjects();
        model.addAttribute("subjects", subjects);
        return "learn/index";
    }

    @GetMapping("/subject/{subjectId}")
    public String subjectDetail(@PathVariable("subjectId") String subjectId, Model model) {
        Subject subject = learnService.getSubjectById(subjectId)
                .orElseThrow(() -> new com.byteforce.exception.ResourceNotFoundException("Subject not found: " + subjectId));
        model.addAttribute("subject", subject);
        model.addAttribute("topics", subject.getTopics());
        return "learn/subject";
    }

    @GetMapping("/topic/{topicId}")
    public String topicConcepts(@PathVariable("topicId") long topicId, Model model) {
        Topic topic = learnService.getTopicById(topicId)
                .orElseThrow(() -> new com.byteforce.exception.ResourceNotFoundException("Topic not found with ID: " + topicId));
        List<Concept> concepts = learnService.getConceptsForTopic(topicId);
        model.addAttribute("topic", topic);
        model.addAttribute("concepts", concepts);
        return "learn/topic";
    }

    @GetMapping("/concept/{conceptId}")
    public String conceptDetail(@PathVariable("conceptId") long conceptId, Model model) {
        Concept concept = learnService.getConceptById(conceptId)
                .orElseThrow(() -> new com.byteforce.exception.ResourceNotFoundException("Concept not found with ID: " + conceptId));
        model.addAttribute("concept", concept);
        model.addAttribute("resources", concept.getResources());
        model.addAttribute("rememberItems", rememberService.getItemsForConcept(conceptId));
        model.addAttribute("relatedConcepts", brainMapService.getRelatedConceptsForConcept(conceptId));
        return "learn/concept";
    }

    @GetMapping("/search")
    public String searchConcepts(@RequestParam(value = "q", required = false, defaultValue = "") String query,
                                 Model model) {
        String trimmed = query.trim();
        List<Concept> results = trimmed.isEmpty() ? List.of() : learnService.searchConcepts(trimmed);
        model.addAttribute("query", trimmed);
        model.addAttribute("results", results);
        model.addAttribute("resultCount", results.size());
        return "learn/search";
    }
}
