package com.byteforce.web;

import com.byteforce.ByteForceWebApplication;
import com.byteforce.domain.Company;
import com.byteforce.domain.Role;
import com.byteforce.domain.User;
import com.byteforce.service.CompanyService;
import com.byteforce.web.util.WebSessionUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(classes = ByteForceWebApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompanyWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyService companyService;

    private MockHttpSession createStudentSession() {
        MockHttpSession session = new MockHttpSession();
        User student = User.create("student_company@byteforce.com", "$2a$12$testHash", "Student Company", Role.STUDENT);
        WebSessionUtil.setCurrentUser(session, student);
        return session;
    }

    private MockHttpSession createAdminSession() {
        MockHttpSession session = new MockHttpSession();
        User admin = User.create("admin_company@byteforce.com", "$2a$12$testHash", "Admin Company", Role.ADMIN);
        WebSessionUtil.setCurrentUser(session, admin);
        return session;
    }

    // =========================================================================
    // 1. AUTHENTICATION & ACCESS CONTROL
    // =========================================================================

    @Test
    @DisplayName("Unauthenticated visitor accessing /companies is redirected to polite auth-required page")
    void unauthenticatedUserRedirectedFromCompanies() throws Exception {
        mockMvc.perform(get("/companies"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/auth-required?returnUrl=/companies")));
    }

    @Test
    @DisplayName("Unauthenticated visitor accessing /companies/tcs is redirected to auth-required page")
    void unauthenticatedUserRedirectedFromCompanyDashboard() throws Exception {
        mockMvc.perform(get("/companies/tcs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/auth-required?returnUrl=/companies/tcs")));
    }

    @Test
    @DisplayName("Unauthenticated user accessing /admin/companies is redirected to /login")
    void unauthenticatedUserRedirectedFromAdminCompanies() throws Exception {
        mockMvc.perform(get("/admin/companies"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login?returnUrl=")));
    }

    @Test
    @DisplayName("Authenticated STUDENT is denied access to /admin/companies with HTTP 403")
    void studentDeniedAccessToAdminCompanies() throws Exception {
        MockHttpSession session = createStudentSession();
        mockMvc.perform(get("/admin/companies").session(session))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 2. STUDENT COMPANY CATALOG (/companies)
    // =========================================================================

    @Test
    @DisplayName("Student accessing /companies sees Company Catalog with reference hierarchy and seeded companies")
    void studentViewsCompanyCatalog() throws Exception {
        MockHttpSession session = createStudentSession();

        mockMvc.perform(get("/companies").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("companies/index"))
                .andExpect(model().attributeExists("companyCards"))
                .andExpect(content().string(containsString("Prepare by Company")))
                .andExpect(content().string(containsString("Focus your preparation around the companies you're targeting.")))
                .andExpect(content().string(containsString("TCS")))
                .andExpect(content().string(containsString("Microsoft")))
                .andExpect(content().string(containsString("Amazon")))
                .andExpect(content().string(containsString("Google")))
                .andExpect(content().string(containsString("View preparation")))
                .andExpect(content().string(containsString("View Preparation")));
    }

    @Test
    @DisplayName("Inactive companies are NOT displayed in the student company catalog")
    void inactiveCompanyNotDisplayedInCatalog() throws Exception {
        // Create an inactive company
        Company hidden = Company.create(
                "Hidden Enterprise",
                "hidden-enterprise-" + System.currentTimeMillis(),
                "/images/companies/hidden.svg",
                "https://hidden.com",
                "Hidden description",
                "Hidden details",
                false
        );
        Company saved = companyService.createCompany(hidden);

        MockHttpSession session = createStudentSession();
        mockMvc.perform(get("/companies").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Hidden Enterprise"))));

        // Attempting to access dashboard of inactive company returns 404
        mockMvc.perform(get("/companies/" + saved.getSlug()).session(session))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // 3. STUDENT COMPANY DASHBOARD (/companies/{slug})
    // =========================================================================

    @Test
    @DisplayName("Student accessing /companies/tcs sees company preparation dashboard")
    void studentViewsTcsDashboard() throws Exception {
        MockHttpSession session = createStudentSession();

        mockMvc.perform(get("/companies/tcs").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("companies/dashboard"))
                .andExpect(model().attributeExists("dashboard"))
                .andExpect(model().attributeExists("company"))
                .andExpect(content().string(containsString("TCS")))
                .andExpect(content().string(containsString("Overview")))
                .andExpect(content().string(containsString("Technical")))
                .andExpect(content().string(containsString("Practice")))
                .andExpect(content().string(containsString("Aptitude")))
                .andExpect(content().string(containsString("Assessments")))
                .andExpect(content().string(containsString("Interview")));
    }

    // =========================================================================
    // 4. ADMIN COMPANY MANAGEMENT (/admin/companies)
    // =========================================================================

    @Test
    @DisplayName("Admin accessing /admin/companies sees company management dashboard")
    void adminViewsCompanyList() throws Exception {
        MockHttpSession session = createAdminSession();

        mockMvc.perform(get("/admin/companies").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/companies/list"))
                .andExpect(model().attributeExists("companies"))
                .andExpect(content().string(containsString("Company Management")))
                .andExpect(content().string(containsString("TCS")))
                .andExpect(content().string(containsString("Microsoft")));
    }

    @Test
    @DisplayName("Admin can open create company form")
    void adminOpensCreateCompanyForm() throws Exception {
        MockHttpSession session = createAdminSession();

        mockMvc.perform(get("/admin/companies/new").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/companies/form"))
                .andExpect(model().attributeExists("company"));
    }

    @Test
    @DisplayName("Admin can create a new company and toggle its status")
    void adminCreatesAndTogglesCompany() throws Exception {
        MockHttpSession session = createAdminSession();
        String slug = "admin-test-" + System.currentTimeMillis();

        mockMvc.perform(post("/admin/companies")
                        .session(session)
                        .param("name", "Admin Test Co")
                        .param("slug", slug)
                        .param("logoPath", "/images/companies/tcs.svg")
                        .param("websiteUrl", "https://admintest.com")
                        .param("shortDescription", "Admin test company")
                        .param("description", "Admin test detailed description")
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/companies"));

        Optional<Company> createdOpt = companyService.getCompanyBySlug(slug);
        assertTrue(createdOpt.isPresent());
        assertTrue(createdOpt.get().isActive());

        // Toggle active
        mockMvc.perform(post("/admin/companies/" + createdOpt.get().getId() + "/toggle-active")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/companies"));

        Optional<Company> toggledOpt = companyService.getCompanyById(createdOpt.get().getId());
        assertTrue(toggledOpt.isPresent());
        assertFalse(toggledOpt.get().isActive());
    }

    @Test
    @DisplayName("Admin can view relationships and link content to company")
    void adminManagesRelationships() throws Exception {
        MockHttpSession session = createAdminSession();
        Optional<Company> tcsOpt = companyService.getCompanyBySlug("tcs");
        assertTrue(tcsOpt.isPresent());
        long tcsId = tcsOpt.get().getId();

        mockMvc.perform(get("/admin/companies/" + tcsId + "/relationships").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/companies/relationships"))
                .andExpect(model().attributeExists("company"))
                .andExpect(model().attributeExists("allTopics"));

        mockMvc.perform(post("/admin/companies/" + tcsId + "/relationships/topics")
                        .session(session)
                        .param("topicIds", "1", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/companies/" + tcsId + "/relationships"));
    }
}
