package com.byteforce.web.controller;

import com.byteforce.domain.Concept;
import com.byteforce.domain.ConceptRelationshipType;
import com.byteforce.domain.RelatedConceptView;
import com.byteforce.domain.RememberCardView;
import com.byteforce.domain.RememberItem;
import com.byteforce.domain.RememberItemType;
import com.byteforce.domain.Subject;
import com.byteforce.domain.Topic;
import com.byteforce.service.BrainMapService;
import com.byteforce.service.LearnService;
import com.byteforce.service.RememberService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Unified Controller for the Remember knowledge layer (/remember).
 * Integrates:
 * 1. Quick Revision Cards: High-yield placement facts, common interview confusions, formulas, and tricks.
 * 2. Brain Maps: Visual concept knowledge network mapping prerequisites, successors, and related concepts.
 */
@Controller
@RequestMapping("/remember")
public class RememberController {

    private final RememberService rememberService;
    private final LearnService learnService;
    private final BrainMapService brainMapService;

    public RememberController(RememberService rememberService,
                              LearnService learnService,
                              BrainMapService brainMapService) {
        this.rememberService = Objects.requireNonNull(rememberService, "rememberService must not be null");
        this.learnService = Objects.requireNonNull(learnService, "learnService must not be null");
        this.brainMapService = Objects.requireNonNull(brainMapService, "brainMapService must not be null");
    }

    @GetMapping
    public String rememberIndex(
            @RequestParam(value = "view", required = false, defaultValue = "cards") String view,
            @RequestParam(value = "subject", required = false) String subjectFilter,
            @RequestParam(value = "type", required = false) String typeFilter,
            @RequestParam(value = "conceptId", required = false) Long conceptId,
            @RequestParam(value = "subjectId", required = false) String subjectId,
            Model model) {

        String activeView = "brain-maps".equalsIgnoreCase(view) ? "brain-maps" : "cards";
        model.addAttribute("activeView", activeView);

        List<Subject> subjects = learnService.getAllSubjects();
        model.addAttribute("subjects", subjects);

        if ("brain-maps".equals(activeView)) {
            populateBrainMapsModel(model, subjects, conceptId, subjectId);
        } else {
            populateCardsModel(model, subjects, subjectFilter, typeFilter);
        }

        return "remember/index";
    }

    private void populateCardsModel(Model model, List<Subject> subjects, String subjectFilter, String typeFilter) {
        RememberItemType selectedTypeEnum = null;
        if (typeFilter != null && !typeFilter.isBlank() && !"all".equalsIgnoreCase(typeFilter)) {
            try {
                selectedTypeEnum = RememberItemType.valueOf(typeFilter.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // fallback to all
            }
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

    private void populateBrainMapsModel(Model model, List<Subject> subjects, Long conceptId, String subjectId) {
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
        }
    }
}
