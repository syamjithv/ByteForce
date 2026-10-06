package com.byteforce.web.controller;

import com.byteforce.domain.Concept;
import com.byteforce.domain.ConceptMemoryStatus;
import com.byteforce.domain.ConceptRelationshipType;
import com.byteforce.domain.MemoryAnalytics;
import com.byteforce.domain.MemoryReviewItemView;
import com.byteforce.domain.RelatedConceptView;
import com.byteforce.domain.RememberCardView;
import com.byteforce.domain.RememberItem;
import com.byteforce.domain.RememberItemType;
import com.byteforce.domain.ReviewRating;
import com.byteforce.domain.Subject;
import com.byteforce.domain.Topic;
import com.byteforce.domain.User;
import com.byteforce.domain.UserRememberReview;
import com.byteforce.domain.brainmap.BrainMapGraph;
import com.byteforce.exception.ResourceNotFoundException;
import com.byteforce.service.BrainMapService;
import com.byteforce.service.LearnService;
import com.byteforce.service.MemoryService;
import com.byteforce.service.RememberService;
import com.byteforce.web.util.WebSessionUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Unified Controller for ByteForce Memory & Remember knowledge layer (/remember).
 *
 * Integrates:
 * 1. ByteForce Memory: Evidence-informed spaced repetition revision schedule, FSRS engine, retrieval-first testing.
 * 2. Revision Cards: High-yield facts, common interview traps, formulas, and tricks.
 * 3. Brain Maps: Visual concept knowledge network.
 */
@Controller
@RequestMapping("/remember")
public class RememberController {

    private final RememberService rememberService;
    private final LearnService learnService;
    private final BrainMapService brainMapService;
    private final MemoryService memoryService;

    @org.springframework.beans.factory.annotation.Autowired
    public RememberController(RememberService rememberService,
                              LearnService learnService,
                              BrainMapService brainMapService,
                              MemoryService memoryService) {
        this.rememberService = Objects.requireNonNull(rememberService, "rememberService must not be null");
        this.learnService = Objects.requireNonNull(learnService, "learnService must not be null");
        this.brainMapService = Objects.requireNonNull(brainMapService, "brainMapService must not be null");
        this.memoryService = memoryService;
    }

    public RememberController(RememberService rememberService,
                              LearnService learnService,
                              BrainMapService brainMapService) {
        this(rememberService, learnService, brainMapService, null);
    }

    @GetMapping
    public String rememberIndex(
            @RequestParam(value = "view", required = false) String view,
            @RequestParam(value = "subject", required = false) String subjectFilter,
            @RequestParam(value = "type", required = false) String typeFilter,
            @RequestParam(value = "conceptId", required = false) Long conceptId,
            @RequestParam(value = "subjectId", required = false) String subjectId,
            HttpSession session,
            Model model) {

        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);

        String activeView;
        if ("brain-maps".equalsIgnoreCase(view)) {
            activeView = "brain-maps";
        } else if ("cards".equalsIgnoreCase(view)) {
            activeView = "cards";
        } else if ("memory".equalsIgnoreCase(view)) {
            activeView = "memory";
        } else if (subjectFilter != null || typeFilter != null) {
            activeView = "cards";
        } else if (userOpt.isPresent()) {
            activeView = "memory";
        } else {
            // For unauthenticated guest browsing /remember without explicit view, preserve cards browsing
            activeView = "cards";
        }
        model.addAttribute("activeView", activeView);

        List<Subject> subjects = learnService.getAllSubjects();
        model.addAttribute("subjects", subjects);

        if ("brain-maps".equals(activeView)) {
            UUID userId = userOpt.map(User::getId).orElse(null);
            populateBrainMapsModel(model, subjects, conceptId, subjectId, userId);
        } else if ("cards".equals(activeView)) {
            populateCardsModel(model, subjects, subjectFilter, typeFilter);
        } else {
            populateMemoryDashboardModel(model, subjects, userOpt.orElse(null));
        }

        return "remember/index";
    }

    private void populateMemoryDashboardModel(Model model, List<Subject> subjects, User currentUser) {
        if (currentUser != null && memoryService != null) {
            UUID userId = currentUser.getId();
            List<MemoryReviewItemView> dueQueue = memoryService.getDueQueue(userId, 8);
            MemoryAnalytics analytics = memoryService.getMemoryAnalytics(userId);
            List<MemoryReviewItemView> recentReviewed = memoryService.getRecentlyReviewed(userId, 5);
            List<MemoryReviewItemView> weakItems = memoryService.getWeakItems(userId, 5);

            model.addAttribute("dueQueue", dueQueue);
            model.addAttribute("analytics", analytics);
            model.addAttribute("recentReviewed", recentReviewed);
            model.addAttribute("recentlyReviewed", recentReviewed);
            model.addAttribute("weakItems", weakItems);
            model.addAttribute("dueCount", dueQueue.size());
        } else {
            model.addAttribute("dueQueue", List.of());
            model.addAttribute("analytics", MemoryAnalytics.empty());
            model.addAttribute("recentReviewed", List.of());
            model.addAttribute("recentlyReviewed", List.of());
            model.addAttribute("weakItems", List.of());
            model.addAttribute("dueCount", 0);
        }

        // Add curriculum preview for Brain Maps section on Remember Home
        Map<String, List<Concept>> conceptsBySubject = new LinkedHashMap<>();
        for (Subject s : subjects) {
            List<Concept> subConcepts = new ArrayList<>();
            if (s.getTopics() != null) {
                for (Topic t : s.getTopics()) {
                    subConcepts.addAll(learnService.getConceptsForTopic(t.getId()));
                }
            }
            if (!subConcepts.isEmpty()) {
                conceptsBySubject.put(s.getName(), subConcepts);
            }
        }
        model.addAttribute("conceptsBySubject", conceptsBySubject);
    }

    private void populateCardsModel(Model model, List<Subject> subjects, String subjectFilter, String typeFilter) {
        RememberItemType selectedTypeEnum = null;
        if (typeFilter != null && !typeFilter.isBlank() && !"all".equalsIgnoreCase(typeFilter)) {
            try {
                selectedTypeEnum = RememberItemType.valueOf(typeFilter.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        List<RememberItem> rawItems = (selectedTypeEnum != null)
                ? rememberService.getItemsByType(selectedTypeEnum)
                : rememberService.getAllItems();

        Map<Long, Concept> conceptCache = new HashMap<>();
        Map<Long, Topic> topicCache = new HashMap<>();
        Map<String, Subject> subjectCache = new HashMap<>();
        for (Subject s : subjects) {
            subjectCache.put(s.getId(), s);
            if (s.getTopics() != null) {
                for (Topic t : s.getTopics()) {
                    topicCache.put(t.getId(), t);
                }
            }
        }

        String activeSubject = (subjectFilter != null && !subjectFilter.isBlank() && !"all".equalsIgnoreCase(subjectFilter))
                ? subjectFilter.trim().toLowerCase()
                : null;

        List<RememberCardView> cardViews = new ArrayList<>();
        for (RememberItem item : rawItems) {
            Concept concept = conceptCache.computeIfAbsent(item.getConceptId(), id ->
                    learnService.getConceptById(id).orElse(null)
            );
            if (concept == null) {
                continue;
            }

            Topic topic = topicCache.computeIfAbsent(concept.getTopicId(), id ->
                    learnService.getTopicById(id).orElse(null)
            );

            String subId = (topic != null && topic.getSubjectId() != null) ? topic.getSubjectId() : "";
            if (activeSubject != null && !activeSubject.equalsIgnoreCase(subId)) {
                continue;
            }

            Subject subject = subjectCache.computeIfAbsent(subId, id ->
                    learnService.getSubjectById(id).orElse(null)
            );

            String subjectName = subject != null ? subject.getName() : "Computer Science";
            String topicName = topic != null ? topic.getName() : concept.getTopicName();

            cardViews.add(new RememberCardView(item, concept, subId, subjectName, topicName));
        }

        model.addAttribute("items", cardViews);
        model.addAttribute("types", RememberItemType.values());
        model.addAttribute("selectedSubject", activeSubject != null ? activeSubject : "all");
        model.addAttribute("selectedType", selectedTypeEnum != null ? selectedTypeEnum.name() : "all");
        model.addAttribute("totalCount", cardViews.size());
    }

    private void populateBrainMapsModel(Model model, List<Subject> subjects, Long conceptId, String subjectId, UUID currentUserId) {
        Map<String, Map<Topic, List<Concept>>> curriculumHierarchy = new LinkedHashMap<>();
        Concept defaultConcept = null;

        for (Subject s : subjects) {
            Map<Topic, List<Concept>> topicConcepts = new LinkedHashMap<>();
            if (s.getTopics() != null) {
                for (Topic t : s.getTopics()) {
                    List<Concept> concepts = learnService.getConceptsForTopic(t.getId());
                    if (!concepts.isEmpty()) {
                        topicConcepts.put(t, concepts);
                        if (defaultConcept == null) {
                            defaultConcept = concepts.getFirst();
                        }
                    }
                }
            }
            if (!topicConcepts.isEmpty()) {
                curriculumHierarchy.put(s.getId(), topicConcepts);
            }
        }
        if (learnService.getConceptById(1001L).isPresent()) {
            defaultConcept = learnService.getConceptById(1001L).get();
        }
        model.addAttribute("curriculumHierarchy", curriculumHierarchy);

        Concept activeConcept = null;
        if (conceptId != null && conceptId > 0) {
            activeConcept = learnService.getConceptById(conceptId).orElse(null);
        }

        if (activeConcept == null && subjectId != null && !subjectId.isBlank()) {
            Map<Topic, List<Concept>> topicMap = curriculumHierarchy.get(subjectId);
            if (topicMap != null && !topicMap.isEmpty()) {
                List<Concept> firstTopicConcepts = topicMap.values().iterator().next();
                if (!firstTopicConcepts.isEmpty()) {
                    activeConcept = firstTopicConcepts.getFirst();
                }
            }
        }

        if (activeConcept == null) {
            activeConcept = defaultConcept;
        }

        if (activeConcept != null) {
            model.addAttribute("selectedConcept", activeConcept);

            Topic activeTopic = learnService.getTopicById(activeConcept.getTopicId()).orElse(null);
            model.addAttribute("selectedTopic", activeTopic);

            String activeSubId = activeTopic != null ? activeTopic.getSubjectId() : null;
            Subject activeSubject = activeSubId != null ? learnService.getSubjectById(activeSubId).orElse(null) : null;
            model.addAttribute("selectedSubject", activeSubject);

            List<RelatedConceptView> related = brainMapService.getRelatedConceptsForConcept(activeConcept.getId());
            model.addAttribute("relatedConcepts", related);

            // Build visual BrainMapGraph
            BrainMapGraph graph = brainMapService.getBrainMapGraph(activeConcept.getId(), currentUserId);
            model.addAttribute("graph", graph);

            List<RelatedConceptView> prerequisites = new ArrayList<>();
            List<RelatedConceptView> successors = new ArrayList<>();
            List<RelatedConceptView> relatedNodes = new ArrayList<>();
            List<RelatedConceptView> confusedNodes = new ArrayList<>();

            for (RelatedConceptView relView : related) {
                ConceptRelationshipType relType = relView.getType();
                if (relType == ConceptRelationshipType.PREREQUISITE) {
                    if (relView.isOutgoing()) {
                        prerequisites.add(relView);
                    } else {
                        successors.add(relView);
                    }
                } else if (relType == ConceptRelationshipType.CHILD) {
                    if (relView.isOutgoing()) {
                        successors.add(relView);
                    } else {
                        prerequisites.add(relView);
                    }
                } else if (relType == ConceptRelationshipType.PARENT) {
                    if (relView.isOutgoing()) {
                        prerequisites.add(relView);
                    } else {
                        successors.add(relView);
                    }
                } else if (relType == ConceptRelationshipType.COMMONLY_CONFUSED) {
                    confusedNodes.add(relView);
                } else {
                    relatedNodes.add(relView);
                }
            }

            model.addAttribute("prerequisites", prerequisites);
            model.addAttribute("successors", successors);
            model.addAttribute("relatedNodes", relatedNodes);
            model.addAttribute("confusedNodes", confusedNodes);
        } else {
            model.addAttribute("selectedConcept", null);
            model.addAttribute("relatedConcepts", List.of());
            model.addAttribute("graph", null);
        }
    }

    // ==========================================
    // Retrieval Practice Review Session Routes
    // ==========================================

    @GetMapping("/review")
    public String reviewSession(
            @RequestParam(value = "itemId", required = false) Long itemId,
            HttpSession session,
            Model model) {

        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);
        if (userOpt.isEmpty()) {
            return "redirect:/auth-required?returnUrl=/remember/review&feature=performing retrieval practice reviews";
        }

        if (memoryService == null) {
            model.addAttribute("queueComplete", true);
            return "remember/review";
        }

        UUID userId = userOpt.get().getId();
        Optional<MemoryReviewItemView> itemOpt;
        if (itemId != null && itemId > 0) {
            itemOpt = memoryService.getReviewItem(userId, itemId);
        } else {
            itemOpt = memoryService.getNextDueItem(userId);
        }

        if (itemOpt.isEmpty()) {
            model.addAttribute("queueComplete", true);
            model.addAttribute("analytics", memoryService.getMemoryAnalytics(userId));
            return "remember/review";
        }

        MemoryReviewItemView reviewItem = itemOpt.get();
        model.addAttribute("queueComplete", false);
        model.addAttribute("item", reviewItem);
        model.addAttribute("previews", reviewItem.getRatingPreviews());
        model.addAttribute("totalDue", memoryService.getDueQueue(userId, 50).size());

        return "remember/review";
    }

    @PostMapping("/review")
    public String submitReviewRating(
            @RequestParam("itemId") long itemId,
            @RequestParam("rating") String ratingStr,
            HttpSession session,
            RedirectAttributes ra) {

        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        ReviewRating rating;
        try {
            rating = ReviewRating.valueOf(ratingStr.trim().toUpperCase());
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Invalid review rating.");
            return "redirect:/remember/review";
        }

        if (memoryService != null) {
            UserRememberReview review = memoryService.recordReview(userOpt.get().getId(), itemId, rating);
            ra.addFlashAttribute("feedbackRating", rating.getDisplayName());
            ra.addFlashAttribute("feedbackNextState", review.getState().getDisplayName());
        }

        return "redirect:/remember/review";
    }

    // ==========================================
    // Concept Memory Detail & Enrollment Routes
    // ==========================================

    @GetMapping("/concept/{conceptId}")
    public String conceptMemoryDetail(
            @PathVariable("conceptId") long conceptId,
            HttpSession session,
            Model model) {

        Concept concept = learnService.getConceptById(conceptId)
                .orElseThrow(() -> new ResourceNotFoundException("Concept not found with ID: " + conceptId));

        Topic topic = learnService.getTopicById(concept.getTopicId()).orElse(null);
        Subject subject = (topic != null && topic.getSubjectId() != null)
                ? learnService.getSubjectById(topic.getSubjectId()).orElse(null)
                : null;

        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);
        ConceptMemoryStatus memoryStatus;
        if (userOpt.isPresent() && memoryService != null) {
            memoryStatus = memoryService.getConceptMemoryStatus(userOpt.get().getId(), conceptId);
        } else {
            List<RelatedConceptView> related = brainMapService.getRelatedConceptsForConcept(conceptId);
            memoryStatus = ConceptMemoryStatus.notEnrolled(conceptId, concept.getTitle(), 0, related);
        }

        List<RememberItem> items = rememberService.getItemsForConcept(conceptId);

        model.addAttribute("concept", concept);
        model.addAttribute("topic", topic);
        model.addAttribute("subject", subject);
        model.addAttribute("memoryStatus", memoryStatus);
        model.addAttribute("rememberItems", items);
        model.addAttribute("items", items);

        return "remember/concept-detail";
    }

    @PostMapping("/concept/{conceptId}/enroll")
    public String enrollConcept(
            @PathVariable("conceptId") long conceptId,
            HttpSession session,
            RedirectAttributes ra) {

        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);
        if (userOpt.isEmpty()) {
            return "redirect:/auth-required?returnUrl=/remember/concept/" + conceptId + "&feature=scheduling concept revision";
        }

        if (memoryService != null) {
            int enrolled = memoryService.enrollConcept(userOpt.get().getId(), conceptId);
            ra.addFlashAttribute("successMessage", "Enrolled " + enrolled + " items into your personal revision schedule.");
        }

        return "redirect:/remember/concept/" + conceptId;
    }
}
