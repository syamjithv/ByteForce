package com.byteforce.web.controller.admin;

import com.byteforce.domain.AptitudeQuestion;
import com.byteforce.domain.Assessment;
import com.byteforce.domain.Company;
import com.byteforce.domain.CompanyInterviewCategory;
import com.byteforce.domain.Question;
import com.byteforce.domain.Topic;
import com.byteforce.exception.ResourceNotFoundException;
import com.byteforce.service.AptitudeQuestionService;
import com.byteforce.service.AssessmentService;
import com.byteforce.service.CompanyService;
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
import java.util.Objects;
import java.util.Optional;

/**
 * Admin controller for managing Companies and Company-Content relationships (/admin/companies).
 */
@Controller
@RequestMapping("/admin/companies")
public class AdminCompanyController {

    private static final Logger log = LoggerFactory.getLogger(AdminCompanyController.class);

    private final CompanyService companyService;
    private final TopicService topicService;
    private final QuestionService questionService;
    private final AptitudeQuestionService aptitudeQuestionService;
    private final AssessmentService assessmentService;

    public AdminCompanyController(CompanyService companyService,
                                  TopicService topicService,
                                  QuestionService questionService,
                                  AptitudeQuestionService aptitudeQuestionService,
                                  AssessmentService assessmentService) {
        this.companyService = Objects.requireNonNull(companyService, "companyService must not be null");
        this.topicService = Objects.requireNonNull(topicService, "topicService must not be null");
        this.questionService = Objects.requireNonNull(questionService, "questionService must not be null");
        this.aptitudeQuestionService = Objects.requireNonNull(aptitudeQuestionService, "aptitudeQuestionService must not be null");
        this.assessmentService = Objects.requireNonNull(assessmentService, "assessmentService must not be null");
    }

    @GetMapping
    public String listCompanies(Model model) {
        List<Company> companies = companyService.getAllCompanies();
        model.addAttribute("companies", companies);
        model.addAttribute("activeTab", "admin-companies");
        return "admin/companies/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("company", Company.create("", "", "", "", "", "", true));
        model.addAttribute("isNew", true);
        model.addAttribute("activeTab", "admin-companies");
        return "admin/companies/form";
    }

    @PostMapping({"", "/new"})
    public String createCompany(@RequestParam("name") String name,
                                @RequestParam("slug") String slug,
                                @RequestParam(value = "logoPath", required = false) String logoPath,
                                @RequestParam(value = "websiteUrl", required = false) String websiteUrl,
                                @RequestParam(value = "shortDescription", required = false) String shortDescription,
                                @RequestParam(value = "description", required = false) String description,
                                @RequestParam(value = "active", defaultValue = "false") boolean active,
                                RedirectAttributes redirectAttributes) {
        try {
            if (name == null || name.isBlank()) {
                redirectAttributes.addFlashAttribute("flashError", "Company name is required.");
                return "redirect:/admin/companies/new";
            }
            if (slug == null || slug.isBlank()) {
                slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
            }
            Company company = Company.create(name.trim(), slug.trim(), logoPath, websiteUrl, shortDescription, description, active);
            companyService.createCompany(company);
            redirectAttributes.addFlashAttribute("flashSuccess", "Company created successfully.");
            return "redirect:/admin/companies";
        } catch (Exception e) {
            log.error("Failed to create company", e);
            redirectAttributes.addFlashAttribute("flashError", "Failed to create company: " + e.getMessage());
            return "redirect:/admin/companies/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") long id, Model model) {
        Company company = companyService.getCompanyById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + id));
        model.addAttribute("company", company);
        model.addAttribute("isNew", false);
        model.addAttribute("activeTab", "admin-companies");
        return "admin/companies/form";
    }

    @PostMapping({"/{id}", "/{id}/edit"})
    public String updateCompany(@PathVariable("id") long id,
                                @RequestParam("name") String name,
                                @RequestParam("slug") String slug,
                                @RequestParam(value = "logoPath", required = false) String logoPath,
                                @RequestParam(value = "websiteUrl", required = false) String websiteUrl,
                                @RequestParam(value = "shortDescription", required = false) String shortDescription,
                                @RequestParam(value = "description", required = false) String description,
                                @RequestParam(value = "active", defaultValue = "false") boolean active,
                                RedirectAttributes redirectAttributes) {
        try {
            Company existing = companyService.getCompanyById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + id));
            Company updated = new Company(id, name.trim(), slug.trim(), logoPath, websiteUrl, shortDescription, description,
                    active, existing.getLastReviewedAt(), existing.getCreatedAt(), existing.getUpdatedAt());
            companyService.updateCompany(updated);
            redirectAttributes.addFlashAttribute("flashSuccess", "Company updated successfully.");
            return "redirect:/admin/companies";
        } catch (Exception e) {
            log.error("Failed to update company {}", id, e);
            redirectAttributes.addFlashAttribute("flashError", "Failed to update company: " + e.getMessage());
            return "redirect:/admin/companies/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable("id") long id, RedirectAttributes redirectAttributes) {
        Optional<Company> compOpt = companyService.getCompanyById(id);
        if (compOpt.isPresent()) {
            boolean newStatus = !compOpt.get().isActive();
            companyService.setCompanyActive(id, newStatus);
            redirectAttributes.addFlashAttribute("flashSuccess", "Company status updated to " + (newStatus ? "Active" : "Inactive"));
        }
        return "redirect:/admin/companies";
    }

    @PostMapping("/{id}/delete")
    public String deleteCompany(@PathVariable("id") long id, RedirectAttributes redirectAttributes) {
        try {
            companyService.deleteCompany(id);
            redirectAttributes.addFlashAttribute("flashSuccess", "Company deleted successfully.");
        } catch (Exception e) {
            log.error("Failed to delete company {}", id, e);
            redirectAttributes.addFlashAttribute("flashError", "Failed to delete company: " + e.getMessage());
        }
        return "redirect:/admin/companies";
    }

    @GetMapping("/{id}/relationships")
    public String manageRelationships(@PathVariable("id") long id, Model model) {
        Company company = companyService.getCompanyById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + id));

        List<Topic> allTopics = topicService.getAllTopics();
        List<Question> allQuestions = questionService.getAllQuestions();
        List<AptitudeQuestion> allAptitude = aptitudeQuestionService.getAllQuestions();
        List<Assessment> allAssessments = assessmentService.getAllAssessments();

        List<Topic> linkedTopics = companyService.getLinkedTopics(id);
        List<Question> linkedQuestions = companyService.getLinkedQuestions(id);
        List<AptitudeQuestion> linkedAptitude = companyService.getLinkedAptitudeQuestions(id);
        List<Assessment> linkedAssessments = companyService.getLinkedAssessments(id);
        List<CompanyInterviewCategory> linkedInterview = companyService.getLinkedInterviewCategories(id);

        model.addAttribute("company", company);
        model.addAttribute("allTopics", allTopics);
        model.addAttribute("allQuestions", allQuestions);
        model.addAttribute("allAptitude", allAptitude);
        model.addAttribute("allAssessments", allAssessments);

        model.addAttribute("linkedTopics", linkedTopics);
        model.addAttribute("linkedQuestions", linkedQuestions);
        model.addAttribute("linkedAptitude", linkedAptitude);
        model.addAttribute("linkedAssessments", linkedAssessments);
        model.addAttribute("linkedInterview", linkedInterview);

        model.addAttribute("activeTab", "admin-companies");
        return "admin/companies/relationships";
    }

    @PostMapping({"/{id}/relationships", "/{id}/relationships/topics"})
    public String saveRelationships(@PathVariable("id") long id,
                                    @RequestParam(value = "topicIds", required = false) List<Long> topicIds,
                                    @RequestParam(value = "questionIds", required = false) List<Long> questionIds,
                                    @RequestParam(value = "aptitudeIds", required = false) List<Long> aptitudeIds,
                                    @RequestParam(value = "assessmentIds", required = false) List<Long> assessmentIds,
                                    RedirectAttributes redirectAttributes) {
        try {
            companyService.setLinkedTopics(id, topicIds);
            companyService.setLinkedQuestions(id, questionIds);
            companyService.setLinkedAptitudeQuestions(id, aptitudeIds);
            companyService.setLinkedAssessments(id, assessmentIds);

            redirectAttributes.addFlashAttribute("flashSuccess", "Content relationships updated successfully.");
        } catch (Exception e) {
            log.error("Failed to update relationships for company {}", id, e);
            redirectAttributes.addFlashAttribute("flashError", "Failed to update relationships: " + e.getMessage());
        }
        return "redirect:/admin/companies/" + id + "/relationships";
    }

    @PostMapping("/{id}/interview-categories/add")
    public String addInterviewCategory(@PathVariable("id") long id,
                                       @RequestParam("title") String title,
                                       @RequestParam(value = "categoryType", defaultValue = "TECHNICAL") String categoryType,
                                       @RequestParam(value = "description", required = false) String description,
                                       @RequestParam(value = "displayOrder", defaultValue = "0") int displayOrder,
                                       RedirectAttributes redirectAttributes) {
        try {
            companyService.addInterviewCategory(id, title.trim(), categoryType, description, displayOrder);
            redirectAttributes.addFlashAttribute("flashSuccess", "Interview round added successfully.");
        } catch (Exception e) {
            log.error("Failed to add interview category for company {}", id, e);
            redirectAttributes.addFlashAttribute("flashError", "Failed to add interview round: " + e.getMessage());
        }
        return "redirect:/admin/companies/" + id + "/relationships";
    }

    @PostMapping("/{id}/interview-categories/{catId}/delete")
    public String deleteInterviewCategory(@PathVariable("id") long id,
                                          @PathVariable("catId") long catId,
                                          RedirectAttributes redirectAttributes) {
        try {
            companyService.removeInterviewCategory(catId);
            redirectAttributes.addFlashAttribute("flashSuccess", "Interview round deleted successfully.");
        } catch (Exception e) {
            log.error("Failed to delete interview category {}", catId, e);
            redirectAttributes.addFlashAttribute("flashError", "Failed to delete interview round: " + e.getMessage());
        }
        return "redirect:/admin/companies/" + id + "/relationships";
    }
}
