package com.byteforce.web.controller;

import com.byteforce.domain.Concept;
import com.byteforce.domain.ConceptRelationshipType;
import com.byteforce.domain.RelatedConceptView;
import com.byteforce.domain.Subject;
import com.byteforce.domain.Topic;
import com.byteforce.domain.User;
import com.byteforce.domain.brainmap.BrainMapGraph;
import com.byteforce.exception.ResourceNotFoundException;
import com.byteforce.service.BrainMapService;
import com.byteforce.service.LearnService;
import com.byteforce.web.util.WebSessionUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for Brain Maps explorer (/brain-maps).
 * Renders the visual concept knowledge network with zoom/pan and downloadable vector PDF.
 */
@Controller
@RequestMapping("/brain-maps")
public class BrainMapController {

    private final BrainMapService brainMapService;
    private final LearnService learnService;

    public BrainMapController(BrainMapService brainMapService, LearnService learnService) {
        this.brainMapService = brainMapService;
        this.learnService = learnService;
    }

    @GetMapping
    public String brainMapIndex(
            @RequestParam(value = "conceptId", required = false) Long conceptId,
            @RequestParam(value = "subjectId", required = false) String subjectId,
            HttpSession session,
            Model model) {

        List<Subject> subjects = learnService.getAllSubjects();
        model.addAttribute("subjects", subjects);

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

            // Build real interactive BrainMapGraph
            UUID userId = WebSessionUtil.getCurrentUser(session).map(User::getId).orElse(null);
            BrainMapGraph graph = brainMapService.getBrainMapGraph(activeConcept.getId(), userId);
            model.addAttribute("graph", graph);

            List<RelatedConceptView> prerequisites = new ArrayList<>();
            List<RelatedConceptView> successors = new ArrayList<>();
            List<RelatedConceptView> relatedNodes = new ArrayList<>();
            List<RelatedConceptView> confusedNodes = new ArrayList<>();

            for (RelatedConceptView view : related) {
                ConceptRelationshipType type = view.getType();
                if (type == ConceptRelationshipType.PREREQUISITE) {
                    if (view.isOutgoing()) {
                        prerequisites.add(view);
                    } else {
                        successors.add(view);
                    }
                } else if (type == ConceptRelationshipType.CHILD) {
                    if (view.isOutgoing()) {
                        successors.add(view);
                    } else {
                        prerequisites.add(view);
                    }
                } else if (type == ConceptRelationshipType.PARENT) {
                    if (view.isOutgoing()) {
                        prerequisites.add(view);
                    } else {
                        successors.add(view);
                    }
                } else if (type == ConceptRelationshipType.COMMONLY_CONFUSED) {
                    confusedNodes.add(view);
                } else {
                    relatedNodes.add(view);
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

        return "brain-maps/index";
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportBrainMapPdf(@RequestParam("conceptId") long conceptId) {
        Concept concept = learnService.getConceptById(conceptId)
                .orElseThrow(() -> new ResourceNotFoundException("Concept not found with ID: " + conceptId));

        byte[] pdf = brainMapService.exportBrainMapPdf(conceptId);
        String filename = "brain-map-" + (concept.getTitle().replaceAll("[^a-zA-Z0-9.-]", "_").toLowerCase()) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
