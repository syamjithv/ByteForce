package com.byteforce.web.controller.admin;

import com.byteforce.domain.RememberItem;
import com.byteforce.domain.RememberItemType;
import com.byteforce.exception.ValidationException;
import com.byteforce.service.LearnService;
import com.byteforce.service.RememberService;
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
 * Controller for managing Remember quick-revision content.
 */
@Controller
@RequestMapping("/admin/remember")
public class AdminRememberController {

    private static final Logger log = LoggerFactory.getLogger(AdminRememberController.class);

    private final RememberService rememberService;
    private final LearnService learnService;

    public AdminRememberController(RememberService rememberService, LearnService learnService) {
        this.rememberService = rememberService;
        this.learnService = learnService;
    }

    @GetMapping
    public String listRememberItems(@RequestParam(value = "conceptId", required = false) Long conceptId,
                                    @RequestParam(value = "type", required = false) RememberItemType type,
                                    Model model) {
        List<RememberItem> items;
        if (conceptId != null && conceptId > 0) {
            items = rememberService.getItemsForConcept(conceptId);
            model.addAttribute("selectedConceptId", conceptId);
        } else if (type != null) {
            items = rememberService.getItemsByType(type);
            model.addAttribute("selectedConceptId", null);
        } else {
            items = rememberService.getAllItems();
            model.addAttribute("selectedConceptId", null);
        }

        model.addAttribute("items", items);
        model.addAttribute("concepts", learnService.getAllConcepts());
        model.addAttribute("itemTypes", RememberItemType.values());
        model.addAttribute("selectedType", type);
        model.addAttribute("activeTab", "admin-remember");
        model.addAttribute("pageTitle", "Manage Remember Items");
        return "admin/remember/list";
    }

    @GetMapping("/new")
    public String newItemForm(@RequestParam(value = "conceptId", required = false) Long conceptId, Model model) {
        RememberItem item = RememberItem.empty(conceptId != null ? conceptId : 0L);
        model.addAttribute("item", item);
        model.addAttribute("concepts", learnService.getAllConcepts());
        model.addAttribute("itemTypes", RememberItemType.values());
        model.addAttribute("isNew", true);
        model.addAttribute("activeTab", "admin-remember");
        model.addAttribute("pageTitle", "Add Remember Item");
        return "admin/remember/form";
    }

    @GetMapping("/{id}/edit")
    public String editItemForm(@PathVariable("id") long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<RememberItem> itemOpt = rememberService.getItemById(id);
        if (itemOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Remember item not found with ID: " + id);
            return "redirect:/admin/remember";
        }
        model.addAttribute("item", itemOpt.get());
        model.addAttribute("concepts", learnService.getAllConcepts());
        model.addAttribute("itemTypes", RememberItemType.values());
        model.addAttribute("isNew", false);
        model.addAttribute("activeTab", "admin-remember");
        model.addAttribute("pageTitle", "Edit Remember Item");
        return "admin/remember/form";
    }

    @PostMapping("/save")
    public String saveItem(@RequestParam(value = "id", required = false, defaultValue = "0") long id,
                           @RequestParam("conceptId") long conceptId,
                           @RequestParam("type") RememberItemType type,
                           @RequestParam("content") String content,
                           @RequestParam(value = "displayOrder", required = false, defaultValue = "0") int displayOrder,
                           @RequestParam(value = "active", required = false, defaultValue = "true") boolean active,
                           RedirectAttributes redirectAttributes) {
        try {
            if (conceptId <= 0) {
                throw new ValidationException("Please select an associated Concept.");
            }
            if (content == null || content.isBlank()) {
                throw new ValidationException("Content must not be blank.");
            }

            if (id <= 0) {
                rememberService.createItem(conceptId, type, content, displayOrder);
                redirectAttributes.addFlashAttribute("successMessage", "Remember item created successfully.");
            } else {
                rememberService.updateItem(id, type, content, displayOrder, active);
                redirectAttributes.addFlashAttribute("successMessage", "Remember item updated successfully.");
            }
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            if (id <= 0) {
                return "redirect:/admin/remember/new" + (conceptId > 0 ? "?conceptId=" + conceptId : "");
            } else {
                return "redirect:/admin/remember/" + id + "/edit";
            }
        } catch (Exception e) {
            log.error("Failed to save remember item", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to save remember item: " + e.getMessage());
        }
        return "redirect:/admin/remember";
    }

    @PostMapping("/{id}/delete")
    public String deleteItem(@PathVariable("id") long id, RedirectAttributes redirectAttributes) {
        try {
            rememberService.deleteItem(id);
            redirectAttributes.addFlashAttribute("successMessage", "Remember item deleted successfully.");
        } catch (Exception e) {
            log.error("Failed to delete remember item {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete remember item: " + e.getMessage());
        }
        return "redirect:/admin/remember";
    }
}
