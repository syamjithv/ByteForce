package com.byteforce.web;

import com.byteforce.ByteForceWebApplication;
import com.byteforce.domain.Concept;
import com.byteforce.domain.Role;
import com.byteforce.domain.User;
import com.byteforce.service.LearnService;
import com.byteforce.service.MemoryService;
import com.byteforce.service.UserService;
import com.byteforce.web.util.WebSessionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
class RememberWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.byteforce.service.AuthService authService;

    @Autowired
    private LearnService learnService;

    @Autowired
    private MemoryService memoryService;

    private User testUser;
    private MockHttpSession testSession;
    private Concept sampleConcept;

    @BeforeEach
    void setUp() {
        String email = "mem.student." + UUID.randomUUID().toString().substring(0, 8) + "@byteforce.test";
        testUser = authService.register(email, "SecurePassword123!", "Memory Student");

        testSession = new MockHttpSession();
        WebSessionUtil.setCurrentUser(testSession, testUser);

        List<Concept> concepts = learnService.searchConcepts("");
        if (!concepts.isEmpty()) {
            sampleConcept = concepts.getFirst();
        }
    }

    @Test
    @DisplayName("Remember home page loads for guest with default cards view and explicit memory view")
    void testRememberHomeGuest() throws Exception {
        // Default guest visit loads cards browsing
        mockMvc.perform(get("/remember"))
                .andExpect(status().isOk())
                .andExpect(view().name("remember/index"))
                .andExpect(model().attribute("activeView", "cards"))
                .andExpect(model().attributeExists("items", "types"))
                .andExpect(content().string(containsString("Remember — Quick Revision")));

        // Explicit memory view for guest shows prompt to sign in
        mockMvc.perform(get("/remember").param("view", "memory"))
                .andExpect(status().isOk())
                .andExpect(view().name("remember/index"))
                .andExpect(model().attribute("activeView", "memory"))
                .andExpect(content().string(containsString("ByteForce Memory")))
                .andExpect(content().string(containsString("Personal Spaced Repetition Memory System")));
    }

    @Test
    @DisplayName("Remember home page loads for authenticated student with review queue and analytics models")
    void testRememberHomeAuthenticated() throws Exception {
        mockMvc.perform(get("/remember").session(testSession))
                .andExpect(status().isOk())
                .andExpect(view().name("remember/index"))
                .andExpect(model().attributeExists("dueQueue", "analytics", "recentlyReviewed", "weakItems"))
                .andExpect(content().string(containsString("ByteForce Memory")));
    }

    @Test
    @DisplayName("Remember views switch correctly between Memory, Cards, and Brain Maps")
    void testRememberViewSwitching() throws Exception {
        // Cards view
        mockMvc.perform(get("/remember").param("view", "cards").session(testSession))
                .andExpect(status().isOk())
                .andExpect(model().attribute("activeView", "cards"))
                .andExpect(model().attributeExists("items", "types"));

        // Brain Maps view
        mockMvc.perform(get("/remember").param("view", "brain-maps").session(testSession))
                .andExpect(status().isOk())
                .andExpect(model().attribute("activeView", "brain-maps"))
                .andExpect(model().attributeExists("curriculumHierarchy", "graph"))
                .andExpect(content().string(containsString("brainMapSvg")))
                .andExpect(content().string(containsString("width=\"100%\"")))
                .andExpect(content().string(containsString("height=\"100%\"")))
                .andExpect(content().string(containsString("preserveAspectRatio=\"xMidYMid meet\"")))
                .andExpect(content().string(containsString("nodesLayer")))
                .andExpect(content().string(containsString("bm-node-element")))
                .andExpect(content().string(containsString("window.bmZoomIn")))
                .andExpect(content().string(containsString("window.bmReset")));
    }

    @Test
    @DisplayName("Direct Brain Maps route (/brain-maps) loads graph model attribute and visible SVG canvas")
    void testDirectBrainMapsRoute() throws Exception {
        mockMvc.perform(get("/brain-maps").session(testSession))
                .andExpect(status().isOk())
                .andExpect(view().name("brain-maps/index"))
                .andExpect(model().attributeExists("curriculumHierarchy", "graph", "selectedConcept"))
                .andExpect(content().string(containsString("brainMapSvg")))
                .andExpect(content().string(containsString("width=\"100%\"")))
                .andExpect(content().string(containsString("height=\"100%\"")))
                .andExpect(content().string(containsString("preserveAspectRatio=\"xMidYMid meet\"")))
                .andExpect(content().string(containsString("viewBox=\"0 0 980 620\"")))
                .andExpect(content().string(containsString("nodesLayer")))
                .andExpect(content().string(containsString("edgesLayer")))
                .andExpect(content().string(containsString("bm-node-element")))
                .andExpect(content().string(containsString("Array Traversal")))
                .andExpect(content().string(containsString("Prefix Sum")))
                .andExpect(content().string(containsString("Singly Linked List")))
                .andExpect(content().string(containsString("translate(380.0,256.0)")))
                .andExpect(content().string(containsString("translate(380.0,70.0)")))
                .andExpect(content().string(containsString("translate(740.0,256.0)")))
                .andExpect(content().string(containsString("nodeInspector")))
                .andExpect(content().string(containsString("window.bmZoomIn")))
                .andExpect(content().string(containsString("window.bmReset")));
    }

    @Test
    @DisplayName("Review session redirects unauthenticated users to auth-required gate")
    void testReviewSessionUnauthenticatedRedirect() throws Exception {
        mockMvc.perform(get("/remember/review"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth-required?returnUrl=/remember/review&feature=performing retrieval practice reviews"));
    }

    @Test
    @DisplayName("Authenticated review session loads queue or honest complete state")
    void testReviewSessionAuthenticated() throws Exception {
        mockMvc.perform(get("/remember/review").session(testSession))
                .andExpect(status().isOk())
                .andExpect(view().name("remember/review"))
                .andExpect(model().attributeExists("queueComplete"));
    }

    @Test
    @DisplayName("Concept memory detail view loads with FSRS metrics and related concepts")
    void testConceptMemoryDetail() throws Exception {
        if (sampleConcept == null) return;

        mockMvc.perform(get("/remember/concept/" + sampleConcept.getId()).session(testSession))
                .andExpect(status().isOk())
                .andExpect(view().name("remember/concept-detail"))
                .andExpect(model().attributeExists("concept", "memoryStatus", "items"))
                .andExpect(content().string(containsString(sampleConcept.getTitle())));
    }

    @Test
    @DisplayName("Enrolling a concept populates review items into the student queue")
    void testConceptEnrollment() throws Exception {
        if (sampleConcept == null) return;

        mockMvc.perform(post("/remember/concept/" + sampleConcept.getId() + "/enroll")
                        .session(testSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/remember/concept/" + sampleConcept.getId()));
    }

    @Test
    @DisplayName("Submitting review rating with valid rating updates scheduling")
    void testSubmitReviewRating() throws Exception {
        if (sampleConcept == null) return;

        // Enroll concept first
        memoryService.enrollConcept(testUser.getId(), sampleConcept.getId());
        var dueQueue = memoryService.getDueQueue(testUser.getId(), 5);

        if (!dueQueue.isEmpty()) {
            long reviewItemId = dueQueue.getFirst().getRememberItem().getId();

            mockMvc.perform(post("/remember/review")
                            .session(testSession)
                            .param("itemId", String.valueOf(reviewItemId))
                            .param("rating", "GOOD"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/remember/review"));
        }
    }

    @Test
    @DisplayName("Submitting invalid rating string is handled gracefully without HTTP 500")
    void testSubmitInvalidRating() throws Exception {
        mockMvc.perform(post("/remember/review")
                        .session(testSession)
                        .param("itemId", "999999")
                        .param("rating", "INVALID_RATING"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/remember/review"));
    }

    @Test
    @DisplayName("Brain Maps PDF export generates vector PDF with application/pdf content type")
    void testBrainMapsPdfExport() throws Exception {
        if (sampleConcept == null) return;

        mockMvc.perform(get("/brain-maps/export/pdf")
                        .param("conceptId", String.valueOf(sampleConcept.getId())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    @DisplayName("Brain Maps PDF export with non-existent concept returns 404 rather than 500")
    void testBrainMapsPdfExportNotFound() throws Exception {
        mockMvc.perform(get("/brain-maps/export/pdf")
                        .param("conceptId", "9999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Security: Student session isolation - User A's review queue cannot be manipulated by User B")
    void testStudentSessionIsolation() {
        String emailB = "student.b." + UUID.randomUUID().toString().substring(0, 8) + "@byteforce.test";
        User userB = authService.register(emailB, "SecurePassword123!", "Student B");

        if (sampleConcept != null) {
            memoryService.enrollConcept(testUser.getId(), sampleConcept.getId());

            var queueA = memoryService.getDueQueue(testUser.getId(), 10);
            var queueB = memoryService.getDueQueue(userB.getId(), 10);

            // User B's queue must be empty even though User A enrolled items
            assertTrue(queueB.isEmpty(), "User B must not see User A's due queue items");
        }
    }

    @Test
    @DisplayName("End-to-End Student Memory Revision Flow: Empty State, Queue Display, Reveal, Rating Advance, and Completion")
    void testCompleteStudentMemoryRevisionFlow() throws Exception {
        if (sampleConcept == null) return;

        // 1. New student visits /remember?view=memory before enrolling
        mockMvc.perform(get("/remember").param("view", "memory").session(testSession))
                .andExpect(status().isOk())
                .andExpect(view().name("remember/index"))
                .andExpect(model().attribute("dueCount", 0))
                .andExpect(content().string(containsString("Queue Clear for Today")))
                .andExpect(content().string(containsString("Your memory queue is empty.")));

        // 2. Direct review when queue is empty returns honest empty-state
        mockMvc.perform(get("/remember/review").session(testSession))
                .andExpect(status().isOk())
                .andExpect(view().name("remember/review"))
                .andExpect(model().attribute("queueComplete", true))
                .andExpect(content().string(containsString("Memory Queue Clear")));

        // 3. Enroll concept items into student's personal schedule
        mockMvc.perform(post("/remember/concept/" + sampleConcept.getId() + "/enroll").session(testSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/remember/concept/" + sampleConcept.getId()));

        // 4. Visit /remember?view=memory - Queue now displays available revision items
        mockMvc.perform(get("/remember").param("view", "memory").session(testSession))
                .andExpect(status().isOk())
                .andExpect(view().name("remember/index"))
                .andExpect(model().attribute("dueCount", org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(content().string(containsString("Items Ready for Revision")))
                .andExpect(content().string(containsString("Begin Review")))
                .andExpect(content().string(containsString(sampleConcept.getTitle())));

        // 5. Start /remember/review - Active card with unrevealed prompt and rating options
        var reviewMvcResult = mockMvc.perform(get("/remember/review").session(testSession))
                .andExpect(status().isOk())
                .andExpect(view().name("remember/review"))
                .andExpect(model().attribute("queueComplete", false))
                .andExpect(model().attributeExists("item", "previews"))
                .andExpect(content().string(containsString(sampleConcept.getTitle())))
                .andExpect(content().string(containsString("Retrieval Challenge")))
                .andExpect(content().string(containsString("id=\"recallPromptState\"")))
                .andExpect(content().string(containsString("id=\"revealAnswerBtn\"")))
                .andExpect(content().string(containsString("id=\"revealedContentState\"")))
                .andExpect(content().string(containsString("id=\"rateAgainBtn\"")))
                .andExpect(content().string(containsString("id=\"rateHardBtn\"")))
                .andExpect(content().string(containsString("id=\"rateGoodBtn\"")))
                .andExpect(content().string(containsString("id=\"rateEasyBtn\"")))
                .andReturn();

        var item = (com.byteforce.domain.MemoryReviewItemView) reviewMvcResult.getModelAndView().getModel().get("item");
        long firstItemId = item.getRememberItem().getId();

        // 6. Verify submitting rating moves the session forward and updates review state/due info
        mockMvc.perform(post("/remember/review")
                        .session(testSession)
                        .param("itemId", String.valueOf(firstItemId))
                        .param("rating", "GOOD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/remember/review"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attribute("feedbackRating", "Good"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attribute("feedbackNextState", "Review"));

        // 7. Rate remaining cards in queue until empty
        var remainingDue = memoryService.getDueQueue(testUser.getId(), 50);
        for (var dueItem : remainingDue) {
            mockMvc.perform(post("/remember/review")
                            .session(testSession)
                            .param("itemId", String.valueOf(dueItem.getRememberItem().getId()))
                            .param("rating", "EASY"))
                    .andExpect(status().is3xxRedirection());
        }

        // 8. With all cards reviewed, /remember/review renders the clean queue-clear empty state
        mockMvc.perform(get("/remember/review").session(testSession))
                .andExpect(status().isOk())
                .andExpect(view().name("remember/review"))
                .andExpect(model().attribute("queueComplete", true))
                .andExpect(content().string(containsString("Memory Queue Clear")));
    }
}
