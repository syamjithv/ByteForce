package com.byteforce.admin;

import com.byteforce.ByteForceWebApplication;
import com.byteforce.domain.AptitudeCategory;
import com.byteforce.domain.AptitudeQuestion;
import com.byteforce.domain.Assessment;
import com.byteforce.domain.Concept;
import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;
import com.byteforce.domain.QuestionType;
import com.byteforce.domain.Role;
import com.byteforce.domain.Subject;
import com.byteforce.domain.Topic;
import com.byteforce.domain.User;
import com.byteforce.service.AptitudeQuestionService;
import com.byteforce.service.AssessmentService;
import com.byteforce.service.LearnService;
import com.byteforce.service.QuestionService;
import com.byteforce.service.TopicService;
import com.byteforce.web.util.WebSessionUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Comprehensive verification of ByteForce Admin Panel implementation:
 * 1. Role-based backend security for /admin/** routes.
 * 2. Unauthenticated redirection to /login with returnUrl.
 * 3. STUDENT role denied with HTTP 403 Forbidden.
 * 4. ADMIN role granted access with HTTP 200 OK.
 * 5. Real database persistence across all Admin modules and reflection in student views.
 */
@SpringBootTest(classes = ByteForceWebApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSecurityAndCrudTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LearnService learnService;

    @Autowired
    private TopicService topicService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private AptitudeQuestionService aptitudeQuestionService;

    private MockHttpSession createStudentSession() {
        MockHttpSession session = new MockHttpSession();
        User student = User.create("student_test@byteforce.com", "$2a$12$testHash", "Test Student", Role.STUDENT);
        WebSessionUtil.setCurrentUser(session, student);
        return session;
    }

    private MockHttpSession createAdminSession() {
        MockHttpSession session = new MockHttpSession();
        User admin = User.create("admin_test@byteforce.com", "$2a$12$testHash", "Test Administrator", Role.ADMIN);
        WebSessionUtil.setCurrentUser(session, admin);
        return session;
    }

    // =========================================================================
    // 1. SECURITY & AUTHORIZATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Security 1: Unauthenticated request to /admin is redirected to /login")
    void unauthenticatedUserRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login?returnUrl=")));

        mockMvc.perform(get("/admin/subjects"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login?returnUrl=")));

        mockMvc.perform(get("/admin/questions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login?returnUrl=")));
    }

    @Test
    @DisplayName("Security 2: Authenticated STUDENT is denied access to /admin with HTTP 403 Forbidden")
    void studentDeniedAccessToAdmin() throws Exception {
        MockHttpSession studentSession = createStudentSession();

        mockMvc.perform(get("/admin").session(studentSession))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/subjects").session(studentSession))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/questions").session(studentSession))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/assessments").session(studentSession))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/aptitude").session(studentSession))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security 3: Authenticated ADMIN is granted access to all /admin management routes")
    void adminGrantedAccessToAdminRoutes() throws Exception {
        MockHttpSession adminSession = createAdminSession();

        mockMvc.perform(get("/admin").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("subjectCount"))
                .andExpect(model().attributeExists("questionCount"));

        mockMvc.perform(get("/admin/subjects").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/subjects/list"));

        mockMvc.perform(get("/admin/topics").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/topics/list"));

        mockMvc.perform(get("/admin/concepts").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/concepts/list"));

        mockMvc.perform(get("/admin/resources").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/resources/list"));

        mockMvc.perform(get("/admin/questions").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/questions/list"));

        mockMvc.perform(get("/admin/assessments").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/assessments/list"));

        mockMvc.perform(get("/admin/remember").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/remember/list"));

        mockMvc.perform(get("/admin/brain-maps").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/brain-maps/list"));

        mockMvc.perform(get("/admin/aptitude").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/aptitude/list"));
    }

    // =========================================================================
    // 2. END-TO-END CRUD PERSISTENCE & STUDENT REFLECTION
    // =========================================================================

    @Test
    @DisplayName("CRUD 1: Subject added by admin persists and appears in student Learn catalog")
    void adminAddSubjectAppearsInStudentLearn() throws Exception {
        MockHttpSession adminSession = createAdminSession();

        String testSubjectId = "cloud-computing-" + System.currentTimeMillis();

        mockMvc.perform(post("/admin/subjects/save")
                        .session(adminSession)
                        .param("id", testSubjectId)
                        .param("name", "Cloud Computing & DevOps")
                        .param("description", "Microservices, containerization, and AWS architecture.")
                        .param("displayOrder", "99")
                        .param("isNew", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/admin/subjects"));

        // Verify subject persisted in database and returned by service
        Optional<Subject> subjectOpt = learnService.getSubjectById(testSubjectId);
        assertTrue(subjectOpt.isPresent(), "Subject must be persisted in database");
        assertEquals("Cloud Computing & DevOps", subjectOpt.get().getName());

        // Verify public/student /learn page shows this new subject
        mockMvc.perform(get("/learn"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cloud Computing")));

        // Cleanup
        learnService.deleteSubject(testSubjectId);
        assertFalse(learnService.getSubjectById(testSubjectId).isPresent());
    }

    @Test
    @DisplayName("CRUD 2: Concept added by admin persists and appears on student Learn topic page")
    void adminAddConceptAppearsInStudentTopicView() throws Exception {
        MockHttpSession adminSession = createAdminSession();

        // Use existing seeded topic "arrays-two-pointers"
        Optional<Topic> topicOpt = topicService.getTopicBySlug("arrays-two-pointers");
        assertTrue(topicOpt.isPresent(), "Arrays topic must exist");
        long topicId = topicOpt.get().getId();

        long testConceptId = 88888L;
        mockMvc.perform(post("/admin/concepts/save")
                        .session(adminSession)
                        .param("id", String.valueOf(testConceptId))
                        .param("topicId", String.valueOf(topicId))
                        .param("title", "Kadane Algorithm Dynamic Range")
                        .param("shortExplanation", "Maximum subarray sum in linear O(N) runtime.")
                        .param("keyPointsText", "Single pass O(N) complexity\nMaintains max_so_far and current_max")
                        .param("example", "int maxSoFar = nums[0];"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/admin/concepts"));

        // Verify concept in service
        Optional<Concept> conceptOpt = learnService.getConceptById(testConceptId);
        assertTrue(conceptOpt.isPresent());
        assertEquals("Kadane Algorithm Dynamic Range", conceptOpt.get().getTitle());

        // Verify student topic page /learn/topic/{id} renders the new concept
        mockMvc.perform(get("/learn/topic/" + topicId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kadane Algorithm Dynamic Range")));

        // Cleanup
        learnService.deleteConcept(testConceptId);
    }

    @Test
    @DisplayName("CRUD 3: Question added by admin persists and is immediately usable in Practice")
    void adminAddQuestionAppearsInPractice() throws Exception {
        MockHttpSession adminSession = createAdminSession();

        Optional<Topic> topicOpt = topicService.getTopicBySlug("arrays-two-pointers");
        assertTrue(topicOpt.isPresent());
        long topicId = topicOpt.get().getId();

        String testSlug = "trapping-rain-water-" + System.currentTimeMillis();

        mockMvc.perform(post("/admin/questions/save")
                        .session(adminSession)
                        .param("id", "0")
                        .param("topicId", String.valueOf(topicId))
                        .param("title", "Trapping Rain Water Placement Problem")
                        .param("slug", testSlug)
                        .param("difficulty", "HARD")
                        .param("questionType", "CODING")
                        .param("description", "Given n non-negative integers representing an elevation map...")
                        .param("solution", "public int trap(int[] height) { return 0; }"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/admin/questions"));

        // Verify question exists in service
        Optional<Question> questionOpt = questionService.getQuestionBySlug(testSlug);
        assertTrue(questionOpt.isPresent());
        Question savedQ = questionOpt.get();
        assertEquals("Trapping Rain Water Placement Problem", savedQ.getTitle());

        // Verify student practice page /practice?topicId= loads the question
        mockMvc.perform(get("/practice").param("topicId", String.valueOf(topicId)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Trapping Rain Water Placement Problem")));

        // Cleanup
        questionService.deleteQuestion(savedQ.getId());
    }

    @Test
    @DisplayName("CRUD 4: Assessment created and questions attached by admin can be started by students")
    void adminCreateAssessmentAndAssignQuestions() throws Exception {
        MockHttpSession adminSession = createAdminSession();

        List<Question> existingQuestions = questionService.getAllQuestions();
        assertFalse(existingQuestions.isEmpty(), "Question bank should not be empty");
        Question testQ = existingQuestions.get(0);

        // 1. Create Assessment
        Assessment created = assessmentService.createAssessment(
                "Automated Diagnostic Test " + System.currentTimeMillis(),
                "Evaluation test description",
                30,
                30,
                Difficulty.MEDIUM,
                null
        );
        assertTrue(created.getId() > 0);

        // 2. Assign Question
        mockMvc.perform(post("/admin/assessments/" + created.getId() + "/questions/add")
                        .session(adminSession)
                        .param("questionId", String.valueOf(testQ.getId()))
                        .param("marks", "15")
                        .param("questionOrder", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/admin/assessments/" + created.getId() + "/questions"));

        // 3. Verify student can access assessment questions
        List<Question> assigned = assessmentService.getQuestionsForAssessment(created.getId());
        assertEquals(1, assigned.size());
        assertEquals(testQ.getId(), assigned.get(0).getId());

        // 4. Verify student Assessments catalog lists the assessment
        mockMvc.perform(get("/assessments"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(created.getTitle())));

        // Cleanup
        assessmentService.deleteAssessment(created.getId());
    }

    @Test
    @DisplayName("CRUD 5: Aptitude question added by admin persists to database and is retrieved by category")
    void adminAddAptitudeQuestionPersistence() throws Exception {
        MockHttpSession adminSession = createAdminSession();

        mockMvc.perform(post("/admin/aptitude/save")
                        .session(adminSession)
                        .param("id", "0")
                        .param("category", "QUANTITATIVE")
                        .param("topic", "Speed & Distance")
                        .param("difficulty", "MEDIUM")
                        .param("question", "A train travels at 60 km/h. How long does it take to travel 150 km?")
                        .param("optionA", "2 hours")
                        .param("optionB", "2.5 hours")
                        .param("optionC", "3 hours")
                        .param("optionD", "3.5 hours")
                        .param("correctAnswer", "B")
                        .param("explanation", "Time = Distance / Speed = 150 / 60 = 2.5 hours.")
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/admin/aptitude"));

        // Verify retrieval via service
        List<AptitudeQuestion> quants = aptitudeQuestionService.getAptitudeQuestionsByCategory(AptitudeCategory.QUANTITATIVE);
        assertNotNull(quants);
        boolean found = quants.stream().anyMatch(q -> q.getTopic().equals("Speed & Distance"));
        assertTrue(found, "Newly added aptitude question must be retrievable from database");
    }
}
