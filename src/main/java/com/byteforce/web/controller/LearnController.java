package com.byteforce.web.controller;

import com.byteforce.domain.Concept;
import com.byteforce.domain.Question;
import com.byteforce.domain.RememberItem;
import com.byteforce.domain.RememberItemType;
import com.byteforce.domain.Subject;
import com.byteforce.domain.Topic;
import com.byteforce.service.BrainMapService;
import com.byteforce.service.LearnService;
import com.byteforce.service.MemoryService;
import com.byteforce.service.QuestionService;
import com.byteforce.service.RememberService;
import com.byteforce.web.dto.ConceptPresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for the Learn module: Subjects -> Topics -> Concepts -> Resources & Search.
 * Fully browsable by public visitors as well as authenticated students.
 */
@Controller
@RequestMapping("/learn")
public class LearnController {

    private final LearnService learnService;
    private final RememberService rememberService;
    private final BrainMapService brainMapService;
    private final MemoryService memoryService;
    private final QuestionService questionService;
    private final com.byteforce.service.ConceptProgressService conceptProgressService;

    @Autowired
    public LearnController(LearnService learnService,
                           RememberService rememberService,
                           BrainMapService brainMapService,
                           @Autowired(required = false) MemoryService memoryService,
                           @Autowired(required = false) QuestionService questionService,
                           @Autowired(required = false) com.byteforce.service.ConceptProgressService conceptProgressService) {
        this.learnService = learnService;
        this.rememberService = rememberService;
        this.brainMapService = brainMapService;
        this.memoryService = memoryService;
        this.questionService = questionService;
        this.conceptProgressService = conceptProgressService;
    }

    public LearnController(LearnService learnService,
                           RememberService rememberService,
                           BrainMapService brainMapService,
                           MemoryService memoryService,
                           QuestionService questionService) {
        this(learnService, rememberService, brainMapService, memoryService, questionService, null);
    }

    public LearnController(LearnService learnService,
                           RememberService rememberService,
                           BrainMapService brainMapService,
                           MemoryService memoryService) {
        this(learnService, rememberService, brainMapService, memoryService, null, null);
    }

    public LearnController(LearnService learnService,
                           RememberService rememberService,
                           BrainMapService brainMapService) {
        this(learnService, rememberService, brainMapService, null, null, null);
    }

    @GetMapping
    public String learnHome(jakarta.servlet.http.HttpSession session, Model model) {
        // Exclude aptitude module from Learn subjects since Aptitude is a separate first-class module
        List<Subject> subjects = learnService.getAllSubjects().stream()
                .filter(s -> !"aptitude".equalsIgnoreCase(s.getId()))
                .toList();
        int totalTopics = subjects.stream().mapToInt(s -> s.getTopics().size()).sum();

        model.addAttribute("subjects", subjects);
        model.addAttribute("totalSubjects", subjects.size());
        model.addAttribute("totalTopics", totalTopics);

        Optional<com.byteforce.domain.User> userOpt = com.byteforce.web.util.WebSessionUtil.getCurrentUser(session);
        if (userOpt.isPresent() && conceptProgressService != null) {
            java.util.UUID userId = userOpt.get().getId();
            model.addAttribute("continueLearning", conceptProgressService.getContinueLearning(userId).orElse(null));
            model.addAttribute("recentlyViewed", conceptProgressService.getRecentlyViewed(userId, 6));
        }
        return "learn/index";
    }

    @GetMapping("/subject/{subjectId}")
    public String subjectDetail(@PathVariable("subjectId") String subjectId,
                                jakarta.servlet.http.HttpSession session,
                                Model model) {
        Subject subject = learnService.getSubjectById(subjectId)
                .orElseThrow(() -> new com.byteforce.exception.ResourceNotFoundException("Subject not found: " + subjectId));
        model.addAttribute("subject", subject);
        model.addAttribute("topics", subject.getTopics());

        Map<Long, List<Concept>> topicConceptsMap = new LinkedHashMap<>();
        int totalSubjectConcepts = 0;
        for (Topic t : subject.getTopics()) {
            List<Concept> concepts = learnService.getConceptsForTopic(t.getId());
            topicConceptsMap.put(t.getId(), concepts);
            totalSubjectConcepts += concepts.size();
        }
        model.addAttribute("topicConceptsMap", topicConceptsMap);
        model.addAttribute("totalSubjectConcepts", totalSubjectConcepts);

        Optional<com.byteforce.domain.User> userOpt = com.byteforce.web.util.WebSessionUtil.getCurrentUser(session);
        if (userOpt.isPresent() && conceptProgressService != null) {
            long completedCount = conceptProgressService.getCompletedCountForSubject(userOpt.get().getId(), subjectId);
            model.addAttribute("subjectCompletedCount", completedCount);
        }
        return "learn/subject";
    }

    @GetMapping("/topic/{topicId}")
    public String topicConcepts(@PathVariable("topicId") long topicId,
                                jakarta.servlet.http.HttpSession session,
                                Model model) {
        Topic topic = learnService.getTopicById(topicId)
                .orElseThrow(() -> new com.byteforce.exception.ResourceNotFoundException("Topic not found with ID: " + topicId));
        List<Concept> concepts = learnService.getConceptsForTopic(topicId);
        model.addAttribute("topic", topic);
        model.addAttribute("concepts", concepts);

        if (topic.getSubjectId() != null && !topic.getSubjectId().isBlank()) {
            learnService.getSubjectById(topic.getSubjectId()).ifPresent(s -> model.addAttribute("subject", s));
        }

        if (questionService != null) {
            List<Question> practiceQuestions = questionService.getQuestionsByTopic(topicId);
            model.addAttribute("practiceCount", practiceQuestions.size());
        }

        Optional<com.byteforce.domain.User> userOpt = com.byteforce.web.util.WebSessionUtil.getCurrentUser(session);
        if (userOpt.isPresent() && conceptProgressService != null) {
            long completedCount = conceptProgressService.getCompletedCountForTopic(userOpt.get().getId(), topicId);
            Map<Long, Boolean> completionMap = conceptProgressService.getCompletionMapForTopic(userOpt.get().getId(), topicId);
            model.addAttribute("topicCompletedCount", completedCount);
            model.addAttribute("conceptCompletionMap", completionMap);
        }

        return "learn/topic";
    }

    @GetMapping("/concept/{conceptId}")
    public String conceptDetail(@PathVariable("conceptId") long conceptId, jakarta.servlet.http.HttpSession session, Model model) {
        Concept concept = learnService.getConceptById(conceptId)
                .orElseThrow(() -> new com.byteforce.exception.ResourceNotFoundException("Concept not found with ID: " + conceptId));
        model.addAttribute("concept", concept);
        model.addAttribute("presentation", ConceptPresentation.from(concept));
        model.addAttribute("resources", concept.getResources());

        Topic topic = learnService.getTopicById(concept.getTopicId()).orElse(null);
        model.addAttribute("topic", topic);
        if (topic != null && topic.getSubjectId() != null && !topic.getSubjectId().isBlank()) {
            learnService.getSubjectById(topic.getSubjectId()).ifPresent(s -> model.addAttribute("subject", s));
        }

        // Sequential sibling concepts for previous/next navigation
        List<Concept> siblingConcepts = learnService.getConceptsForTopic(concept.getTopicId());
        Concept prevConcept = null;
        Concept nextConcept = null;
        int conceptIndex = 1;
        for (int i = 0; i < siblingConcepts.size(); i++) {
            if (siblingConcepts.get(i).getId() == conceptId) {
                conceptIndex = i + 1;
                if (i > 0) prevConcept = siblingConcepts.get(i - 1);
                if (i < siblingConcepts.size() - 1) nextConcept = siblingConcepts.get(i + 1);
                break;
            }
        }
        model.addAttribute("prevConcept", prevConcept);
        model.addAttribute("nextConcept", nextConcept);
        model.addAttribute("conceptIndex", conceptIndex);
        model.addAttribute("totalConceptsInTopic", siblingConcepts.size());

        // Remember items and categorized views
        List<RememberItem> allRemember = rememberService.getItemsForConcept(conceptId);
        model.addAttribute("rememberItems", allRemember);
        model.addAttribute("commonConfusions", allRemember.stream()
                .filter(i -> i.getType() == RememberItemType.COMMON_CONFUSION).toList());
        model.addAttribute("interviewReminders", allRemember.stream()
                .filter(i -> i.getType() == RememberItemType.INTERVIEW_REMINDER).toList());
        model.addAttribute("keyFacts", allRemember.stream()
                .filter(i -> i.getType() == RememberItemType.KEY_FACT || i.getType() == RememberItemType.FORMULA || i.getType() == RememberItemType.MEMORY_TRICK || i.getType() == RememberItemType.QUICK_EXAMPLE).toList());

        model.addAttribute("relatedConcepts", brainMapService.getRelatedConceptsForConcept(conceptId));

        if (questionService != null) {
            List<Question> practiceQuestions = questionService.getQuestionsByTopic(concept.getTopicId());
            model.addAttribute("practiceQuestions", practiceQuestions);
            model.addAttribute("practiceCount", practiceQuestions.size());
        }

        java.util.Optional<com.byteforce.domain.User> userOpt = com.byteforce.web.util.WebSessionUtil.getCurrentUser(session);
        if (userOpt.isPresent()) {
            java.util.UUID userId = userOpt.get().getId();
            if (conceptProgressService != null) {
                // Record view for learning history & continue learning
                conceptProgressService.recordConceptView(userId, conceptId);
                boolean isLearned = conceptProgressService.isConceptLearned(userId, conceptId);
                model.addAttribute("isLearned", isLearned);
            }
            if (memoryService != null) {
                com.byteforce.domain.ConceptMemoryStatus memStatus = memoryService.getConceptMemoryStatus(userId, conceptId);
                model.addAttribute("memoryStatus", memStatus);
            }
        }
        return "learn/concept";
    }

    @PostMapping("/concept/{conceptId}/toggle-learned")
    public String toggleLearned(@PathVariable("conceptId") long conceptId,
                                @RequestParam(value = "returnUrl", required = false) String returnUrl,
                                jakarta.servlet.http.HttpSession session) {
        java.util.Optional<com.byteforce.domain.User> userOpt = com.byteforce.web.util.WebSessionUtil.getCurrentUser(session);
        if (userOpt.isEmpty()) {
            return "redirect:/auth-required?returnUrl=/learn/concept/" + conceptId;
        }
        if (conceptProgressService != null) {
            boolean current = conceptProgressService.isConceptLearned(userOpt.get().getId(), conceptId);
            conceptProgressService.markConceptLearned(userOpt.get().getId(), conceptId, !current);
        }
        if (returnUrl != null && !returnUrl.isBlank()) {
            return "redirect:" + returnUrl;
        }
        return "redirect:/learn/concept/" + conceptId;
    }

    @GetMapping("/search")
    public String searchConcepts(@RequestParam(value = "q", required = false, defaultValue = "") String query,
                                 Model model) {
        String trimmed = query.trim();
        List<Concept> conceptResults = trimmed.isEmpty() ? List.of() : learnService.searchConcepts(trimmed);

        List<Subject> allSubjects = learnService.getAllSubjects().stream()
                .filter(s -> !"aptitude".equalsIgnoreCase(s.getId()))
                .toList();
        String lower = trimmed.toLowerCase(java.util.Locale.ROOT);

        List<Subject> subjectResults = trimmed.isEmpty() ? List.of() : allSubjects.stream()
                .filter(s -> s.getName().toLowerCase(java.util.Locale.ROOT).contains(lower)
                        || (s.getDescription() != null && s.getDescription().toLowerCase(java.util.Locale.ROOT).contains(lower)))
                .toList();

        List<Topic> topicResults = trimmed.isEmpty() ? List.of() : allSubjects.stream()
                .flatMap(s -> s.getTopics().stream())
                .filter(t -> t.getName().toLowerCase(java.util.Locale.ROOT).contains(lower)
                        || (t.getDescription() != null && t.getDescription().toLowerCase(java.util.Locale.ROOT).contains(lower)))
                .toList();

        int totalCount = subjectResults.size() + topicResults.size() + conceptResults.size();

        model.addAttribute("query", trimmed);
        model.addAttribute("matchingSubjects", subjectResults);
        model.addAttribute("matchingTopics", topicResults);
        model.addAttribute("matchingConcepts", conceptResults);
        model.addAttribute("results", conceptResults);
        model.addAttribute("resultCount", totalCount);
        return "learn/search";
    }
}

