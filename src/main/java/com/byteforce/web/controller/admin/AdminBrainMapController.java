package com.byteforce.web.controller.admin;

import com.byteforce.domain.Concept;
import com.byteforce.domain.ConceptRelationship;
import com.byteforce.domain.ConceptRelationshipType;
import com.byteforce.exception.ValidationException;
import com.byteforce.service.BrainMapService;
import com.byteforce.service.LearnService;
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
 * Controller for managing Brain Map knowledge graph relationships between concepts.
 */
@Controller
@RequestMapping("/admin/brain-maps")
public class AdminBrainMapController {

    private static final Logger log = LoggerFactory.getLogger(AdminBrainMapController.class);

    private final BrainMapService brainMapService;
    private final LearnService learnService;

    public AdminBrainMapController(BrainMapService brainMapService, LearnService learnService) {
        this.brainMapService = brainMapService;
        this.learnService = learnService;
    }

    @GetMapping
    public String listRelationships(Model model) {
        List<ConceptRelationship> relationships = brainMapService.getAllRelationships();
        List<Concept> concepts = learnService.getAllConcepts();
        Map<Long, Concept> conceptMap = concepts.stream()
                .collect(Collectors.toMap(Concept::getId, Function.identity(), (a, b) -> a));

        model.addAttribute("relationships", relationships);
        model.addAttribute("conceptMap", conceptMap);
        model.addAttribute("activeTab", "admin-brain-maps");
        model.addAttribute("pageTitle", "Manage Brain Map Relationships");
        return "admin/brain-maps/list";
    }

    @GetMapping("/new")
    public String newRelationshipForm(Model model) {
        ConceptRelationship relationship = ConceptRelationship.create(0L, 0L, ConceptRelationshipType.PREREQUISITE, "", 0);
        model.addAttribute("relationship", relationship);
        model.addAttribute("concepts", learnService.getAllConcepts());
        model.addAttribute("relationshipTypes", ConceptRelationshipType.values());
        model.addAttribute("isNew", true);
        model.addAttribute("activeTab", "admin-brain-maps");
        model.addAttribute("pageTitle", "Add Concept Relationship");
        return "admin/brain-maps/form";
    }

    @GetMapping("/{id}/edit")
    public String editRelationshipForm(@PathVariable("id") long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<ConceptRelationship> relOpt = brainMapService.getRelationshipById(id);
        if (relOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Concept relationship not found with ID: " + id);
            return "redirect:/admin/brain-maps";
        }
        model.addAttribute("relationship", relOpt.get());
        model.addAttribute("concepts", learnService.getAllConcepts());
        model.addAttribute("relationshipTypes", ConceptRelationshipType.values());
        model.addAttribute("isNew", false);
        model.addAttribute("activeTab", "admin-brain-maps");
        model.addAttribute("pageTitle", "Edit Concept Relationship");
        return "admin/brain-maps/form";
    }

    @PostMapping("/save")
    public String saveRelationship(@RequestParam(value = "id", required = false, defaultValue = "0") long id,
                                   @RequestParam("sourceConceptId") long sourceConceptId,
                                   @RequestParam("targetConceptId") long targetConceptId,
                                   @RequestParam("type") ConceptRelationshipType type,
                                   @RequestParam(value = "description", required = false, defaultValue = "") String description,
                                   @RequestParam(value = "displayOrder", required = false, defaultValue = "0") int displayOrder,
                                   RedirectAttributes redirectAttributes) {
        try {
            if (sourceConceptId <= 0 || targetConceptId <= 0) {
                throw new ValidationException("Please select both source and target concepts.");
            }
            if (sourceConceptId == targetConceptId) {
                throw new ValidationException("Source and target concepts cannot be the same. Self-referencing relationships are invalid.");
            }

            if (id <= 0) {
                brainMapService.createRelationship(sourceConceptId, targetConceptId, type, description, displayOrder);
                redirectAttributes.addFlashAttribute("successMessage", "Concept relationship created successfully.");
            } else {
                brainMapService.updateRelationship(id, sourceConceptId, targetConceptId, type, description, displayOrder);
                redirectAttributes.addFlashAttribute("successMessage", "Concept relationship updated successfully.");
            }
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            if (id <= 0) {
                return "redirect:/admin/brain-maps/new";
            } else {
                return "redirect:/admin/brain-maps/" + id + "/edit";
            }
        } catch (Exception e) {
            log.error("Failed to save concept relationship", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save concept relationship: " + e.getMessage());
        }
        return "redirect:/admin/brain-maps";
    }

    @PostMapping("/{id}/delete")
    public String deleteRelationship(@PathVariable("id") long id, RedirectAttributes redirectAttributes) {
        try {
            brainMapService.deleteRelationship(id);
            redirectAttributes.addFlashAttribute("successMessage", "Concept relationship deleted successfully.");
        } catch (Exception e) {
            log.error("Failed to delete concept relationship {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete concept relationship: " + e.getMessage());
        }
        return "redirect:/admin/brain-maps";
    }
}
