package com.byteforce.web.controller;

import com.byteforce.domain.Company;
import com.byteforce.domain.CompanyCardView;
import com.byteforce.domain.CompanyPreparationDashboard;
import com.byteforce.domain.User;
import com.byteforce.exception.ResourceNotFoundException;
import com.byteforce.service.CompanyService;
import com.byteforce.web.util.WebSessionUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Controller for the student-facing Company Practice module:
 * /companies (Company Catalog) and /companies/{slug} (Company Preparation Dashboard).
 */
@Controller
@RequestMapping("/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = Objects.requireNonNull(companyService, "companyService must not be null");
    }

    @GetMapping
    public String companyCatalog(Model model, HttpSession session) {
        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);
        if (userOpt.isEmpty()) {
            return "redirect:/auth-required?returnUrl=/companies&feature=company-specific placement preparation";
        }

        List<CompanyCardView> cards = companyService.getCompanyCatalogCards();
        model.addAttribute("companyCards", cards);
        model.addAttribute("activeTab", "companies");
        return "companies/index";
    }

    @GetMapping("/{slug}")
    public String companyDashboard(@PathVariable("slug") String slug, Model model, HttpSession session) {
        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);
        if (userOpt.isEmpty()) {
            return "redirect:/auth-required?returnUrl=/companies/" + slug + "&feature=company-specific placement preparation";
        }

        Optional<Company> companyOpt = companyService.getCompanyBySlug(slug);
        if (companyOpt.isEmpty() || !companyOpt.get().isActive()) {
            throw new ResourceNotFoundException("Company preparation not found for: " + slug);
        }

        CompanyPreparationDashboard dashboard = companyService.getCompanyPreparationDashboard(slug, userOpt.map(User::getId));
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("company", dashboard.getCompany());
        model.addAttribute("activeTab", "companies");
        return "companies/dashboard";
    }
}
