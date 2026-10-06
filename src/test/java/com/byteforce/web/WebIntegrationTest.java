package com.byteforce.web;

import com.byteforce.ByteForceWebApplication;
import com.byteforce.domain.Role;
import com.byteforce.domain.User;
import com.byteforce.web.util.WebSessionUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
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
class WebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.byteforce.service.AuthService authService;

    @Autowired
    private com.byteforce.service.AssessmentService assessmentService;

    @Test
    @DisplayName("Journey 1: Public Home page loads with hero, brand identity, and core pillars")
    void testPublicHomePage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("Practice. Prepare.")))
                .andExpect(content().string(containsString("Get Placed.")))
                .andExpect(content().string(containsString("Learn")))
                .andExpect(content().string(containsString("Practice")))
                .andExpect(content().string(containsString("Assess")))
                .andExpect(content().string(containsString("Track")))
                .andExpect(content().string(containsString("Interview")))
                .andExpect(content().string(containsString("Create Free Account")));
    }

    @Test
    @DisplayName("Journey 8: Logged-out visitor trying to access protected feature receives polite auth-required page")
    void testAuthRequiredGate() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth-required?returnUrl=/dashboard&feature=the student dashboard"));

        mockMvc.perform(get("/auth-required").param("returnUrl", "/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth-required"))
                .andExpect(content().string(containsString("You're almost there.")))
                .andExpect(content().string(containsString("Create Account")))
                .andExpect(content().string(containsString("Sign In")))
                .andExpect(content().string(containsString("href=\"/\"")))
                .andExpect(content().string(containsString("Continue browsing without signing in")));
    }

    @Test
    @DisplayName("Journey 2 & 9: Registration flow - validation errors and successful creation")
    void testRegistrationFlow() throws Exception {
        // Validation error: Passwords do not match
        mockMvc.perform(post("/register")
                        .param("fullName", "Test Student")
                        .param("email", "test.student@example.com")
                        .param("password", "SecurePass@123")
                        .param("confirmPassword", "DifferentPass@123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(content().string(containsString("Passwords do not match")));

        // Successful registration
        String uniqueEmail = "student." + System.currentTimeMillis() + "@byteforce.com";
        mockMvc.perform(post("/register")
                        .param("fullName", "Jane Doe")
                        .param("email", uniqueEmail)
                        .param("password", "Pass#12345")
                        .param("confirmPassword", "Pass#12345"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?welcome=true"));
    }

    @Test
    @DisplayName("Journey 3 & 10: Login flow - error handling and successful login")
    void testLoginFlow() throws Exception {
        // Register an account first
        String email = "candidate." + System.currentTimeMillis() + "@byteforce.com";
        mockMvc.perform(post("/register")
                .param("fullName", "Candidate Test")
                .param("email", email)
                .param("password", "ValidPass#123")
                .param("confirmPassword", "ValidPass#123"));

        // Test invalid login
        mockMvc.perform(post("/login")
                        .param("email", email)
                        .param("password", "WrongPassword!"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(containsString("Invalid email or password")));

        // Test valid login
        mockMvc.perform(post("/login")
                        .param("email", email)
                        .param("password", "ValidPass#123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("Journey 4: Learn module - browsing subjects, topics, and concepts")
    void testLearnFlow() throws Exception {
        mockMvc.perform(get("/learn"))
                .andExpect(status().isOk())
                .andExpect(view().name("learn/index"))
                .andExpect(content().string(containsString("Placement Learning Curriculum")));

        // Search in learn
        mockMvc.perform(get("/learn/search").param("q", "Array"))
                .andExpect(status().isOk())
                .andExpect(view().name("learn/search"))
                .andExpect(content().string(containsString("Search Results")));
    }

    @Test
    @DisplayName("Journey 5: Practice module - catalog view and session solver")
    void testPracticeFlow() throws Exception {
        // Public practice catalog renders exactly once (no duplicate bug)
        org.springframework.test.web.servlet.MvcResult publicResult = mockMvc.perform(get("/practice"))
                .andExpect(status().isOk())
                .andExpect(view().name("practice/index"))
                .andExpect(content().string(containsString("Practice Questions")))
                .andReturn();

        String publicHtml = publicResult.getResponse().getContentAsString();
        int countPublic = org.springframework.util.StringUtils.countOccurrencesOf(publicHtml, "class=\"practice-catalog-root\"");
        org.junit.jupiter.api.Assertions.assertEquals(1, countPublic, "Practice catalog must render exactly once in public view");

        // Authenticated practice catalog renders exactly once (no duplicate bug)
        MockHttpSession session = new MockHttpSession();
        User testUser = User.create("test.student@byteforce.com", "hash", "Test Student", Role.STUDENT);
        WebSessionUtil.setCurrentUser(session, testUser);

        org.springframework.test.web.servlet.MvcResult authResult = mockMvc.perform(get("/practice").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("practice/index"))
                .andReturn();

        String authHtml = authResult.getResponse().getContentAsString();
        int countAuth = org.springframework.util.StringUtils.countOccurrencesOf(authHtml, "class=\"practice-catalog-root\"");
        org.junit.jupiter.api.Assertions.assertEquals(1, countAuth, "Practice catalog must render exactly once in authenticated view");

        // Unauthenticated access to practice session redirects to auth-required
        mockMvc.perform(get("/practice/session"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth-required?returnUrl=/practice/session&feature=practicing questions and recording progress"));

        // Authenticated practice session
        mockMvc.perform(get("/practice/session").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("practice/session"))
                .andExpect(content().string(containsString("Your Answer")));
    }

    @Test
    @DisplayName("Journey 6: Assessment module - catalog and briefing")
    void testAssessmentFlow() throws Exception {
        mockMvc.perform(get("/assessments"))
                .andExpect(status().isOk())
                .andExpect(view().name("assessments/index"))
                .andExpect(content().string(containsString("Placement Mock Assessments")));
    }

    @Test
    @DisplayName("Journey 7: Track analytics module for logged-in user")
    void testTrackModule() throws Exception {
        MockHttpSession session = new MockHttpSession();
        User testUser = User.create("track.user@byteforce.com", "hash", "Track User", Role.STUDENT);
        WebSessionUtil.setCurrentUser(session, testUser);

        mockMvc.perform(get("/track").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("track/index"))
                .andExpect(content().string(containsString("Placement Preparation Progress")))
                .andExpect(content().string(containsString("Overall Preparation Metrics")));
    }

    @Test
    @DisplayName("Journey 7b: Track module renders assessment history with formatted date without SpEL errors")
    void testTrackModuleWithAssessmentHistory() throws Exception {
        String email = "track.student." + System.currentTimeMillis() + "@byteforce.com";
        User student = authService.register(email, "SecurePassword123!", "Track Student");

        java.util.List<com.byteforce.domain.Assessment> assessments = assessmentService.getAvailableAssessments();
        if (!assessments.isEmpty()) {
            com.byteforce.domain.Assessment assessment = assessments.getFirst();
            com.byteforce.domain.AssessmentAttempt attempt = assessmentService.startAssessment(student.getId(), assessment.getId());
            assessmentService.submitAssessment(attempt.getId(), java.util.Map.of());
        }

        MockHttpSession session = new MockHttpSession();
        WebSessionUtil.setCurrentUser(session, student);

        mockMvc.perform(get("/track").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("track/index"))
                .andExpect(content().string(containsString("Placement Preparation Progress")))
                .andExpect(content().string(containsString("Overall Preparation Metrics")))
                .andExpect(content().string(containsString("Technical Placement Readiness Mock Test 1")));
    }

    @Test
    @DisplayName("Journey 11: Logout flow invalidates session and redirects to public home")
    void testLogoutFlow() throws Exception {
        MockHttpSession session = new MockHttpSession();
        User testUser = User.create("logout.user@byteforce.com", "hash", "Logout User", Role.STUDENT);
        WebSessionUtil.setCurrentUser(session, testUser);

        mockMvc.perform(get("/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?loggedOut=true"));
    }

    @Test
    @DisplayName("Interview module is publicly accessible")
    void testInterviewModule() throws Exception {
        mockMvc.perform(get("/interview"))
                .andExpect(status().isOk())
                .andExpect(view().name("interview/index"))
                .andExpect(content().string(containsString("Technical & HR Interview Preparation")));
    }
}
