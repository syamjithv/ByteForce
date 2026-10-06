package com.byteforce.web.controller.admin;

import com.byteforce.domain.Concept;
import com.byteforce.domain.LearningResource;
import com.byteforce.domain.ResourceType;
import com.byteforce.domain.Subject;
import com.byteforce.domain.Topic;
import com.byteforce.exception.ValidationException;
import com.byteforce.service.LearnService;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for managing educational placement content:
 * Subjects, Topics, Concepts, and Learning Resources.
 */
@Controller
@RequestMapping("/admin")
public class AdminLearnController {

    private static final Logger log = LoggerFactory.getLogger(AdminLearnController.class);

    private final LearnService learnService;
    private final TopicService topicService;

    public AdminLearnController(LearnService learnService, TopicService topicService) {
        this.learnService = learnService;
        this.topicService = topicService;
    }

    // =========================================================================
    // SUBJECTS
    // =========================================================================

    @GetMapping("/subjects")
    public String listSubjects(Model model) {
        List<Subject> subjects = learnService.getAllSubjects();
        model.addAttribute("subjects", subjects);
        model.addAttribute("activeTab", "admin-subjects");
        model.addAttribute("pageTitle", "Manage Subjects");
        return "admin/subjects/list";
    }

    @GetMapping("/subjects/new")
    public String newSubjectForm(Model model) {
        model.addAttribute("subject", Subject.empty());
        model.addAttribute("isNew", true);
        model.addAttribute("activeTab", "admin-subjects");
        model.addAttribute("pageTitle", "Add New Subject");
        return "admin/subjects/form";
    }

    @GetMapping("/subjects/{id}/edit")
    public String editSubjectForm(@PathVariable("id") String id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Subject> subjectOpt = learnService.getSubjectById(id);
        if (subjectOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Subject not found: " + id);
            return "redirect:/admin/subjects";
        }
        model.addAttribute("subject", subjectOpt.get());
        model.addAttribute("isNew", false);
        model.addAttribute("activeTab", "admin-subjects");
        model.addAttribute("pageTitle", "Edit Subject — " + subjectOpt.get().getName());
        return "admin/subjects/form";
    }

    @PostMapping("/subjects/save")
    public String saveSubject(@RequestParam("id") String id,
                              @RequestParam("name") String name,
                              @RequestParam(value = "description", required = false, defaultValue = "") String description,
                              @RequestParam(value = "displayOrder", required = false, defaultValue = "0") int displayOrder,
                              @RequestParam(value = "isNew", required = false, defaultValue = "false") boolean isNew,
                              RedirectAttributes redirectAttributes) {
        try {
            if (id == null || id.isBlank()) {
                throw new ValidationException("Subject ID must not be blank.");
            }
            if (name == null || name.isBlank()) {
                throw new ValidationException("Subject name must not be blank.");
            }

            Subject subject = new Subject(id.trim().toLowerCase(), name.trim(), description != null ? description.trim() : "", displayOrder, List.of());
            learnService.saveSubject(subject);
            redirectAttributes.addFlashAttribute("successMessage", "Subject '" + subject.getName() + "' saved successfully.");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            if (isNew) {
                return "redirect:/admin/subjects/new";
            } else {
                return "redirect:/admin/subjects/" + id + "/edit";
            }
        } catch (Exception e) {
            log.error("Failed to save subject", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save subject: " + e.getMessage());
        }
        return "redirect:/admin/subjects";
    }

    @PostMapping("/subjects/{id}/delete")
    public String deleteSubject(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            learnService.deleteSubject(id);
            redirectAttributes.addFlashAttribute("successMessage", "Subject deleted successfully.");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to delete subject {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete subject: " + e.getMessage());
        }
        return "redirect:/admin/subjects";
    }

    // =========================================================================
    // TOPICS
    // =========================================================================

    @GetMapping("/topics")
    public String listTopics(@RequestParam(value = "subjectId", required = false) String subjectId, Model model) {
        List<Topic> topics;
        if (subjectId != null && !subjectId.isBlank()) {
            topics = topicService.getTopicsForSubject(subjectId);
            model.addAttribute("selectedSubjectId", subjectId);
        } else {
            topics = topicService.getAllTopics();
            model.addAttribute("selectedSubjectId", "");
        }
        model.addAttribute("topics", topics);
        model.addAttribute("subjects", learnService.getAllSubjects());
        model.addAttribute("activeTab", "admin-topics");
        model.addAttribute("pageTitle", "Manage Topics");
        return "admin/topics/list";
    }

    @GetMapping("/topics/new")
    public String newTopicForm(@RequestParam(value = "subjectId", required = false) String subjectId, Model model) {
        Topic topic = Topic.empty(subjectId);
        model.addAttribute("topic", topic);
        model.addAttribute("subjects", learnService.getAllSubjects());
        model.addAttribute("isNew", true);
        model.addAttribute("activeTab", "admin-topics");
        model.addAttribute("pageTitle", "Add New Topic");
        return "admin/topics/form";
    }

    @GetMapping("/topics/{id}/edit")
    public String editTopicForm(@PathVariable("id") long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Topic> topicOpt = topicService.getTopicById(id);
        if (topicOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Topic not found with ID: " + id);
            return "redirect:/admin/topics";
        }
        model.addAttribute("topic", topicOpt.get());
        model.addAttribute("subjects", learnService.getAllSubjects());
        model.addAttribute("isNew", false);
        model.addAttribute("activeTab", "admin-topics");
        model.addAttribute("pageTitle", "Edit Topic — " + topicOpt.get().getName());
        return "admin/topics/form";
    }

    @PostMapping("/topics/save")
    public String saveTopic(@RequestParam(value = "id", required = false, defaultValue = "0") long id,
                            @RequestParam("subjectId") String subjectId,
                            @RequestParam("name") String name,
                            @RequestParam("slug") String slug,
                            @RequestParam(value = "description", required = false, defaultValue = "") String description,
                            @RequestParam(value = "displayOrder", required = false, defaultValue = "0") int displayOrder,
                            RedirectAttributes redirectAttributes) {
        try {
            if (id <= 0) {
                topicService.createTopic(subjectId, name, slug, description, displayOrder);
                redirectAttributes.addFlashAttribute("successMessage", "Topic '" + name + "' created successfully.");
            } else {
                topicService.updateTopic(id, subjectId, name, slug, description, displayOrder);
                redirectAttributes.addFlashAttribute("successMessage", "Topic '" + name + "' updated successfully.");
            }
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            if (id <= 0) {
                return "redirect:/admin/topics/new" + (subjectId != null && !subjectId.isBlank() ? "?subjectId=" + subjectId : "");
            } else {
                return "redirect:/admin/topics/" + id + "/edit";
            }
        } catch (Exception e) {
            log.error("Failed to save topic", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save topic: " + e.getMessage());
        }
        return "redirect:/admin/topics";
    }

    @PostMapping("/topics/{id}/delete")
    public String deleteTopic(@PathVariable("id") long id, RedirectAttributes redirectAttributes) {
        try {
            topicService.deleteTopic(id);
            redirectAttributes.addFlashAttribute("successMessage", "Topic deleted successfully.");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to delete topic {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete topic: " + e.getMessage());
        }
        return "redirect:/admin/topics";
    }

    // =========================================================================
    // CONCEPTS
    // =========================================================================

    @GetMapping("/concepts")
    public String listConcepts(@RequestParam(value = "topicId", required = false) Long topicId, Model model) {
        List<Concept> concepts;
        if (topicId != null && topicId > 0) {
            concepts = learnService.getConceptsForTopic(topicId);
            model.addAttribute("selectedTopicId", topicId);
        } else {
            concepts = learnService.getAllConcepts();
            model.addAttribute("selectedTopicId", null);
        }
        model.addAttribute("concepts", concepts);
        model.addAttribute("topics", topicService.getAllTopics());
        model.addAttribute("activeTab", "admin-concepts");
        model.addAttribute("pageTitle", "Manage Concepts");
        return "admin/concepts/list";
    }

    @GetMapping("/concepts/new")
    public String newConceptForm(@RequestParam(value = "topicId", required = false) Long topicId, Model model) {
        Concept concept = Concept.empty(topicId != null ? topicId : 0L);
        model.addAttribute("concept", concept);
        model.addAttribute("keyPointsText", "");
        model.addAttribute("topics", topicService.getAllTopics());
        model.addAttribute("isNew", true);
        model.addAttribute("activeTab", "admin-concepts");
        model.addAttribute("pageTitle", "Add New Concept");
        return "admin/concepts/form";
    }

    @GetMapping("/concepts/{id}/edit")
    public String editConceptForm(@PathVariable("id") long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Concept> conceptOpt = learnService.getConceptById(id);
        if (conceptOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Concept not found with ID: " + id);
            return "redirect:/admin/concepts";
        }
        Concept concept = conceptOpt.get();
        String keyPointsText = String.join("\n", concept.getKeyPoints());
        model.addAttribute("concept", concept);
        model.addAttribute("keyPointsText", keyPointsText);
        model.addAttribute("topics", topicService.getAllTopics());
        model.addAttribute("isNew", false);
        model.addAttribute("activeTab", "admin-concepts");
        model.addAttribute("pageTitle", "Edit Concept — " + concept.getTitle());
        return "admin/concepts/form";
    }

    @PostMapping("/concepts/save")
    public String saveConcept(@RequestParam(value = "id", required = false, defaultValue = "0") long id,
                              @RequestParam("topicId") long topicId,
                              @RequestParam("title") String title,
                              @RequestParam(value = "shortExplanation", required = false, defaultValue = "") String shortExplanation,
                              @RequestParam(value = "keyPointsText", required = false, defaultValue = "") String keyPointsText,
                              @RequestParam(value = "example", required = false, defaultValue = "") String example,
                              RedirectAttributes redirectAttributes) {
        try {
            if (topicId <= 0) {
                throw new ValidationException("Please select an associated Topic.");
            }
            if (title == null || title.isBlank()) {
                throw new ValidationException("Concept title must not be blank.");
            }

            Optional<Topic> topicOpt = topicService.getTopicById(topicId);
            String topicName = topicOpt.map(Topic::getName).orElse("");

            List<String> keyPoints = Arrays.stream(keyPointsText.split("\\r?\\n"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());

            // If ID is 0, generate an ID based on topicId or current timestamp to avoid collisions
            long conceptId = id;
            if (conceptId <= 0) {
                conceptId = System.currentTimeMillis() % 1_000_000_000L;
            }

            Concept concept = Concept.create(conceptId, topicId, topicName, title.trim(),
                    shortExplanation != null ? shortExplanation.trim() : "",
                    keyPoints,
                    example != null ? example.trim() : "",
                    List.of());

            learnService.saveConcept(concept);
            redirectAttributes.addFlashAttribute("successMessage", "Concept '" + concept.getTitle() + "' saved successfully.");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            if (id <= 0) {
                return "redirect:/admin/concepts/new" + (topicId > 0 ? "?topicId=" + topicId : "");
            } else {
                return "redirect:/admin/concepts/" + id + "/edit";
            }
        } catch (Exception e) {
            log.error("Failed to save concept", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save concept: " + e.getMessage());
        }
        return "redirect:/admin/concepts";
    }

    @PostMapping("/concepts/{id}/delete")
    public String deleteConcept(@PathVariable("id") long id, RedirectAttributes redirectAttributes) {
        try {
            learnService.deleteConcept(id);
            redirectAttributes.addFlashAttribute("successMessage", "Concept deleted successfully.");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to delete concept {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete concept: " + e.getMessage());
        }
        return "redirect:/admin/concepts";
    }

    // =========================================================================
    // LEARNING RESOURCES
    // =========================================================================

    @GetMapping("/resources")
    public String listResources(@RequestParam(value = "conceptId", required = false) Long conceptId, Model model) {
        List<LearningResource> resources;
        if (conceptId != null && conceptId > 0) {
            resources = learnService.getResourcesForConcept(conceptId);
            model.addAttribute("selectedConceptId", conceptId);
        } else {
            resources = learnService.getAllResources();
            model.addAttribute("selectedConceptId", null);
        }
        model.addAttribute("resources", resources);
        model.addAttribute("concepts", learnService.getAllConcepts());
        model.addAttribute("activeTab", "admin-resources");
        model.addAttribute("pageTitle", "Manage Learning Resources");
        return "admin/resources/list";
    }

    @GetMapping("/resources/new")
    public String newResourceForm(@RequestParam(value = "conceptId", required = false) Long conceptId, Model model) {
        LearningResource resource = LearningResource.empty(conceptId != null ? conceptId : 0L);
        model.addAttribute("resource", resource);
        model.addAttribute("concepts", learnService.getAllConcepts());
        model.addAttribute("resourceTypes", ResourceType.values());
        model.addAttribute("isNew", true);
        model.addAttribute("activeTab", "admin-resources");
        model.addAttribute("pageTitle", "Add New Learning Resource");
        return "admin/resources/form";
    }

    @GetMapping("/resources/{id}/edit")
    public String editResourceForm(@PathVariable("id") long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<LearningResource> resourceOpt = learnService.getResourceById(id);
        if (resourceOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Resource not found with ID: " + id);
            return "redirect:/admin/resources";
        }
        model.addAttribute("resource", resourceOpt.get());
        model.addAttribute("concepts", learnService.getAllConcepts());
        model.addAttribute("resourceTypes", ResourceType.values());
        model.addAttribute("isNew", false);
        model.addAttribute("activeTab", "admin-resources");
        model.addAttribute("pageTitle", "Edit Resource — " + resourceOpt.get().getTitle());
        return "admin/resources/form";
    }

    @PostMapping("/resources/save")
    public String saveResource(@RequestParam(value = "id", required = false, defaultValue = "0") long id,
                               @RequestParam("conceptId") long conceptId,
                               @RequestParam("title") String title,
                               @RequestParam("type") ResourceType type,
                               @RequestParam("url") String url,
                               @RequestParam(value = "description", required = false, defaultValue = "") String description,
                               RedirectAttributes redirectAttributes) {
        try {
            if (conceptId <= 0) {
                throw new ValidationException("Please select an associated Concept.");
            }
            if (title == null || title.isBlank()) {
                throw new ValidationException("Resource title must not be blank.");
            }
            if (url == null || url.isBlank()) {
                throw new ValidationException("Resource URL must not be blank.");
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                throw new ValidationException("Resource URL must start with http:// or https://");
            }

            LearningResource resource = LearningResource.create(id, conceptId, title.trim(), type, url.trim(), description != null ? description.trim() : "");
            learnService.saveResource(resource);
            redirectAttributes.addFlashAttribute("successMessage", "Learning resource '" + resource.getTitle() + "' saved successfully.");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            if (id <= 0) {
                return "redirect:/admin/resources/new" + (conceptId > 0 ? "?conceptId=" + conceptId : "");
            } else {
                return "redirect:/admin/resources/" + id + "/edit";
            }
        } catch (Exception e) {
            log.error("Failed to save resource", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save resource: " + e.getMessage());
        }
        return "redirect:/admin/resources";
    }

    @PostMapping("/resources/{id}/delete")
    public String deleteResource(@PathVariable("id") long id, RedirectAttributes redirectAttributes) {
        try {
            learnService.deleteResource(id);
            redirectAttributes.addFlashAttribute("successMessage", "Resource deleted successfully.");
        } catch (Exception e) {
            log.error("Failed to delete resource {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete resource: " + e.getMessage());
        }
        return "redirect:/admin/resources";
    }
}
