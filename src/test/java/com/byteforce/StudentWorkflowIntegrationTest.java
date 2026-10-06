package com.byteforce;

import com.byteforce.app.AppContext;
import com.byteforce.domain.Activity;
import com.byteforce.domain.ActivityType;
import com.byteforce.domain.AttemptStatus;
import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;
import com.byteforce.domain.StudentProfile;
import com.byteforce.domain.Topic;
import com.byteforce.domain.User;
import com.byteforce.domain.UserProgress;
import com.byteforce.persistence.DatabaseMigrator;
import com.byteforce.service.ActivityService;
import com.byteforce.service.AttemptService;
import com.byteforce.service.AuthService;
import com.byteforce.service.BookmarkService;
import com.byteforce.service.QuestionService;
import com.byteforce.service.TopicService;
import com.byteforce.service.UserService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end integration test verifying the complete student lifecycle workflow:
 * Registration -> Login -> Profile Update -> Topic & Question Management ->
 * Attempt Tracking (in-progress, failed, solved) -> Bookmarking -> Progress Calculation ->
 * Activity Logging -> Final Database State Verification.
 */
class StudentWorkflowIntegrationTest {

    private HikariDataSource dataSource;
    private AppContext appContext;

    private AuthService authService;
    private UserService userService;
    private TopicService topicService;
    private QuestionService questionService;
    private AttemptService attemptService;
    private BookmarkService bookmarkService;
    private ActivityService activityService;

    @BeforeEach
    void setUp() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(5);

        dataSource = new HikariDataSource(config);
        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");

        appContext = new AppContext(dataSource);

        authService = appContext.getAuthService();
        userService = appContext.getUserService();
        topicService = appContext.getTopicService();
        questionService = appContext.getQuestionService();
        attemptService = appContext.getAttemptService();
        bookmarkService = appContext.getBookmarkService();
        activityService = appContext.getActivityService();
    }

    @AfterEach
    void tearDown() {
        if (appContext != null) {
            appContext.close();
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Test
    @DisplayName("Complete end-to-end student workflow integration test")
    void executeCompleteStudentWorkflow() {
        // Step 1: Register a student
        String email = "alice.developer@byteforce.com";
        String rawPassword = "StrongPassword2026!";
        String initialName = "Alice Developer";

        User student = authService.register(email, rawPassword, initialName);
        assertNotNull(student);
        assertNotNull(student.getId());
        assertEquals("alice.developer@byteforce.com", student.getEmail());
        assertEquals(initialName, student.getFullName());

        // Step 2: Authenticate / Login
        User loggedInUser = authService.login(email, rawPassword);
        assertNotNull(loggedInUser);
        assertEquals(student.getId(), loggedInUser.getId());
        assertTrue(appContext.getUserSession().isAuthenticated());
        assertEquals(student.getId(), appContext.getUserSession().getCurrentUser().get().getId());

        // Step 3: Update student profile
        String updatedFullName = "Alice W. Developer";
        String phone = "+1-555-0144";
        String college = "MIT Computer Science";
        Integer graduationYear = 2026;

        StudentProfile profile = userService.updateStudentProfile(
                student.getId(), updatedFullName, phone, college, graduationYear);

        assertNotNull(profile);
        assertEquals(updatedFullName, profile.getFullName());
        assertEquals(phone, profile.getPhone());
        assertEquals(college, profile.getCollege());
        assertEquals(graduationYear, profile.getGraduationYear());

        // Verify profile retrieval
        Optional<StudentProfile> retrievedProfile = userService.getStudentProfile(student.getId());
        assertTrue(retrievedProfile.isPresent());
        assertEquals(college, retrievedProfile.get().getCollege());

        // Step 4: Create and find a topic
        Topic topic = topicService.createTopic(
                "Algorithms", "algorithms", "Core data structures and algorithms", 1);
        assertNotNull(topic);
        assertTrue(topic.getId() > 0);

        Optional<Topic> foundTopic = topicService.getTopicBySlug("algorithms");
        assertTrue(foundTopic.isPresent());
        assertEquals("Algorithms", foundTopic.get().getName());

        // Step 5: Create and find a question
        Question question = questionService.createQuestion(
                topic.getId(),
                "Binary Search",
                "binary-search",
                "Given a sorted array and target, return index or -1.",
                Difficulty.EASY,
                "def binary_search(nums, target): ..."
        );
        assertNotNull(question);
        assertTrue(question.getId() > 0);

        Optional<Question> foundQuestion = questionService.getQuestionBySlug("binary-search");
        assertTrue(foundQuestion.isPresent());
        assertEquals("Binary Search", foundQuestion.get().getTitle());

        // Step 6: Attempt the question (initial in-progress attempt)
        attemptService.recordAttempt(
                student.getId(), question.getId(), AttemptStatus.ATTEMPTED, "def binary_search(nums, target): pass", 12);

        // Step 7: Record a failed attempt
        attemptService.recordAttempt(
                student.getId(), question.getId(), AttemptStatus.FAILED, "def binary_search(nums, target): return 0", 15);

        assertFalse(attemptService.hasUserSolved(student.getId(), question.getId()));

        // Step 8: Record a solved attempt
        attemptService.recordAttempt(
                student.getId(), question.getId(), AttemptStatus.SOLVED,
                "def binary_search(nums, target):\n    low, high = 0, len(nums)-1\n    ...", 5);

        assertTrue(attemptService.hasUserSolved(student.getId(), question.getId()));

        // Step 9: Bookmark the question
        bookmarkService.addBookmark(student.getId(), question.getId(), "Re-solve before technical interview");
        assertTrue(bookmarkService.isBookmarked(student.getId(), question.getId()));

        // Step 10: Query progress
        UserProgress progress = attemptService.getProgressSummary(student.getId());
        assertNotNull(progress);
        assertEquals(student.getId(), progress.userId());
        assertEquals(3L, progress.totalAttempts());
        assertEquals(1L, progress.solvedQuestions());
        assertEquals(33.33, progress.solveRatePercentage(), 0.01);
        assertEquals(1L, progress.solvedByDifficulty().get(Difficulty.EASY));
        assertEquals(1L, progress.solvedByTopic().get("Algorithms"));

        // Step 11: Query recent activity
        List<Activity> activities = activityService.getRecentActivities(student.getId(), 20);
        assertFalse(activities.isEmpty());

        // Verify activities contain registration, login, profile updated, attempted, solved, and bookmark
        List<ActivityType> types = activities.stream().map(Activity::getActivityType).toList();
        assertTrue(types.contains(ActivityType.REGISTRATION), "Should record REGISTRATION");
        assertTrue(types.contains(ActivityType.LOGIN), "Should record LOGIN");
        assertTrue(types.contains(ActivityType.PROFILE_UPDATED), "Should record PROFILE_UPDATED");
        assertTrue(types.contains(ActivityType.ATTEMPTED_QUESTION), "Should record ATTEMPTED_QUESTION");
        assertTrue(types.contains(ActivityType.SOLVED_QUESTION), "Should record SOLVED_QUESTION");
        assertTrue(types.contains(ActivityType.BOOKMARK_ADDED), "Should record BOOKMARK_ADDED");

        // Step 12: Verify the expected persisted state in database
        assertEquals(1L, appContext.getUserRepository().findByEmail(email).stream().count());
        assertTrue(appContext.getStudentProfileRepository().findByUserId(student.getId()).isPresent());
        assertEquals(1, appContext.getBookmarkRepository().findByUserId(student.getId()).size());
        assertEquals(3, appContext.getAttemptRepository().findByUserId(student.getId()).size());
        assertTrue(appContext.getActivityRepository().countByUserId(student.getId()) >= 6);

        // Verify session logout
        authService.logout();
        assertFalse(appContext.getUserSession().isAuthenticated());
    }
}
