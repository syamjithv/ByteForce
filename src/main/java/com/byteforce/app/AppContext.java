package com.byteforce.app;

import com.byteforce.config.AppConfig;
import com.byteforce.persistence.DataSourceFactory;
import com.byteforce.persistence.DatabaseMigrator;
import com.byteforce.repository.ActivityRepository;
import com.byteforce.repository.AssessmentAnswerRepository;
import com.byteforce.repository.AssessmentAttemptRepository;
import com.byteforce.repository.AssessmentRepository;
import com.byteforce.repository.AttemptRepository;
import com.byteforce.repository.BookmarkRepository;
import com.byteforce.repository.ConceptRelationshipRepository;
import com.byteforce.repository.ConceptRepository;
import com.byteforce.repository.JdbcActivityRepository;
import com.byteforce.repository.JdbcAssessmentAnswerRepository;
import com.byteforce.repository.JdbcAssessmentAttemptRepository;
import com.byteforce.repository.JdbcAssessmentRepository;
import com.byteforce.repository.JdbcAttemptRepository;
import com.byteforce.repository.JdbcBookmarkRepository;
import com.byteforce.repository.JdbcConceptRelationshipRepository;
import com.byteforce.repository.JdbcConceptRepository;
import com.byteforce.repository.JdbcLearningResourceRepository;
import com.byteforce.repository.JdbcLearnRepository;
import com.byteforce.repository.JdbcQuestionRepository;
import com.byteforce.repository.JdbcRememberItemRepository;
import com.byteforce.repository.JdbcStudentProfileRepository;
import com.byteforce.repository.JdbcSubjectRepository;
import com.byteforce.repository.JdbcTopicRepository;
import com.byteforce.repository.JdbcUserRepository;
import com.byteforce.repository.LearnRepository;
import com.byteforce.repository.LearningResourceRepository;
import com.byteforce.repository.RememberItemRepository;
import com.byteforce.repository.SubjectRepository;
import com.byteforce.repository.QuestionRepository;
import com.byteforce.repository.StudentProfileRepository;
import com.byteforce.repository.TopicRepository;
import com.byteforce.repository.UserRepository;
import com.byteforce.security.BCryptPasswordHasher;
import com.byteforce.security.PasswordHasher;
import com.byteforce.security.UserSession;
import com.byteforce.service.ActivityService;
import com.byteforce.service.ActivityServiceImpl;
import com.byteforce.service.AssessmentService;
import com.byteforce.service.AssessmentServiceImpl;
import com.byteforce.service.AttemptService;
import com.byteforce.service.AttemptServiceImpl;
import com.byteforce.service.AuthService;
import com.byteforce.service.AuthServiceImpl;
import com.byteforce.service.BookmarkService;
import com.byteforce.service.BookmarkServiceImpl;
import com.byteforce.service.BrainMapService;
import com.byteforce.service.BrainMapServiceImpl;
import com.byteforce.service.LearnService;
import com.byteforce.service.LearnServiceImpl;
import com.byteforce.service.QuestionService;
import com.byteforce.service.QuestionServiceImpl;
import com.byteforce.service.RememberService;
import com.byteforce.service.RememberServiceImpl;
import com.byteforce.service.TopicService;
import com.byteforce.service.TopicServiceImpl;
import com.byteforce.service.TrackService;
import com.byteforce.service.TrackServiceImpl;
import com.byteforce.service.UserService;
import com.byteforce.service.UserServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Objects;

/**
 * Composition root for the ByteForce backend.
 * Wires DataSource -> Repositories -> Security/Session -> Services
 * using standard constructor injection without heavyweight reflection or DI frameworks.
 */
public class AppContext implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AppContext.class);

    private final DataSource dataSource;
    private final boolean managesDataSource;

    // Security & Session
    private final PasswordHasher passwordHasher;
    private final UserSession userSession;

    // Repositories
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final BookmarkRepository bookmarkRepository;
    private final AttemptRepository attemptRepository;
    private final ActivityRepository activityRepository;
    private final AssessmentAnswerRepository assessmentAnswerRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentAttemptRepository assessmentAttemptRepository;
    private final SubjectRepository subjectRepository;
    private final ConceptRepository conceptRepository;
    private final LearningResourceRepository learningResourceRepository;
    private final LearnRepository learnRepository;
    private final RememberItemRepository rememberItemRepository;
    private final ConceptRelationshipRepository conceptRelationshipRepository;
    private final com.byteforce.repository.AptitudeQuestionRepository aptitudeQuestionRepository;

    // Services
    private final ActivityService activityService;
    private final AuthService authService;
    private final UserService userService;
    private final TopicService topicService;
    private final QuestionService questionService;
    private final BookmarkService bookmarkService;
    private final AttemptService attemptService;
    private final AssessmentService assessmentService;
    private final LearnService learnService;
    private final RememberService rememberService;
    private final BrainMapService brainMapService;
    private final TrackService trackService;
    private final com.byteforce.service.AptitudeQuestionService aptitudeQuestionService;

    /**
     * Initializes the composition root with an existing DataSource.
     *
     * @param dataSource the database connection pool
     */
    public AppContext(DataSource dataSource) {
        this(dataSource, new BCryptPasswordHasher(), new UserSession(), false);
    }

    /**
     * Initializes the composition root with full control over security and session components.
     *
     * @param dataSource the database connection pool
     * @param passwordHasher the password hasher component
     * @param userSession the user session tracker
     * @param managesDataSource whether AppContext owns and should close the DataSource
     */
    public AppContext(DataSource dataSource, PasswordHasher passwordHasher, UserSession userSession, boolean managesDataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher must not be null");
        this.userSession = Objects.requireNonNull(userSession, "userSession must not be null");
        this.managesDataSource = managesDataSource;

        log.info("Wiring ByteForce repositories...");
        this.userRepository = new JdbcUserRepository(dataSource);
        this.studentProfileRepository = new JdbcStudentProfileRepository(dataSource);
        this.topicRepository = new JdbcTopicRepository(dataSource);
        this.questionRepository = new JdbcQuestionRepository(dataSource);
        this.bookmarkRepository = new JdbcBookmarkRepository(dataSource);
        this.attemptRepository = new JdbcAttemptRepository(dataSource);
        this.activityRepository = new JdbcActivityRepository(dataSource);
        this.assessmentAnswerRepository = new JdbcAssessmentAnswerRepository(dataSource);
        this.assessmentRepository = new JdbcAssessmentRepository(dataSource);
        this.assessmentAttemptRepository = new JdbcAssessmentAttemptRepository(dataSource);
        this.subjectRepository = new JdbcSubjectRepository(dataSource);
        this.learningResourceRepository = new JdbcLearningResourceRepository(dataSource);
        this.conceptRepository = new JdbcConceptRepository(dataSource, learningResourceRepository);
        this.learnRepository = new JdbcLearnRepository(subjectRepository, topicRepository, conceptRepository, learningResourceRepository);
        this.rememberItemRepository = new JdbcRememberItemRepository(dataSource);
        this.conceptRelationshipRepository = new JdbcConceptRelationshipRepository(dataSource);
        this.aptitudeQuestionRepository = new com.byteforce.repository.JdbcAptitudeQuestionRepository(dataSource);

        log.info("Wiring ByteForce services...");
        this.activityService = new ActivityServiceImpl(activityRepository);
        this.authService = new AuthServiceImpl(userRepository, passwordHasher, userSession, activityService);
        this.userService = new UserServiceImpl(userRepository, passwordHasher, studentProfileRepository, activityService);
        this.topicService = new TopicServiceImpl(topicRepository, questionRepository);
        this.questionService = new QuestionServiceImpl(questionRepository, topicRepository);
        this.bookmarkService = new BookmarkServiceImpl(bookmarkRepository, questionRepository, activityService);
        this.attemptService = new AttemptServiceImpl(attemptRepository, questionRepository, activityService);
        this.assessmentService = new AssessmentServiceImpl(assessmentRepository, assessmentAttemptRepository, assessmentAnswerRepository, questionRepository, activityService);
        this.learnService = new LearnServiceImpl(learnRepository, subjectRepository, topicRepository, conceptRepository, learningResourceRepository);
        this.rememberService = new RememberServiceImpl(rememberItemRepository, conceptRepository);
        this.brainMapService = new BrainMapServiceImpl(conceptRelationshipRepository, conceptRepository);
        this.trackService = new TrackServiceImpl(attemptService, assessmentService, activityService, bookmarkService, questionRepository, topicRepository);
        this.aptitudeQuestionService = new com.byteforce.service.AptitudeQuestionServiceImpl(aptitudeQuestionRepository);

        log.info("ByteForce AppContext initialized successfully.");
    }

    /**
     * Factory method to bootstrap AppContext from configuration:
     * creates HikariDataSource, executes Flyway migrations if enabled, and wires all components.
     *
     * @param config application configuration
     * @return fully initialized and wired AppContext
     */
    public static AppContext initialize(AppConfig config) {
        Objects.requireNonNull(config, "config must not be null");
        log.info("Initializing AppContext from AppConfig (DB: {})...", config.getDbUrl());

        DataSource dataSource = DataSourceFactory.createDataSource(config);

        if (config.isFlywayEnabled()) {
            DatabaseMigrator.migrate(dataSource, config);
        }

        return new AppContext(dataSource, new BCryptPasswordHasher(), new UserSession(), true);
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public PasswordHasher getPasswordHasher() {
        return passwordHasher;
    }

    public UserSession getUserSession() {
        return userSession;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public StudentProfileRepository getStudentProfileRepository() {
        return studentProfileRepository;
    }

    public TopicRepository getTopicRepository() {
        return topicRepository;
    }

    public QuestionRepository getQuestionRepository() {
        return questionRepository;
    }

    public BookmarkRepository getBookmarkRepository() {
        return bookmarkRepository;
    }

    public AttemptRepository getAttemptRepository() {
        return attemptRepository;
    }

    public ActivityRepository getActivityRepository() {
        return activityRepository;
    }

    public AssessmentAnswerRepository getAssessmentAnswerRepository() {
        return assessmentAnswerRepository;
    }

    public ActivityService getActivityService() {
        return activityService;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public UserService getUserService() {
        return userService;
    }

    public TopicService getTopicService() {
        return topicService;
    }

    public QuestionService getQuestionService() {
        return questionService;
    }

    public BookmarkService getBookmarkService() {
        return bookmarkService;
    }

    public AttemptService getAttemptService() {
        return attemptService;
    }

    public AssessmentRepository getAssessmentRepository() {
        return assessmentRepository;
    }

    public AssessmentAttemptRepository getAssessmentAttemptRepository() {
        return assessmentAttemptRepository;
    }

    public AssessmentService getAssessmentService() {
        return assessmentService;
    }

    public SubjectRepository getSubjectRepository() {
        return subjectRepository;
    }

    public ConceptRepository getConceptRepository() {
        return conceptRepository;
    }

    public LearningResourceRepository getLearningResourceRepository() {
        return learningResourceRepository;
    }

    public LearnRepository getLearnRepository() {
        return learnRepository;
    }

    public RememberItemRepository getRememberItemRepository() {
        return rememberItemRepository;
    }

    public ConceptRelationshipRepository getConceptRelationshipRepository() {
        return conceptRelationshipRepository;
    }

    public LearnService getLearnService() {
        return learnService;
    }

    public RememberService getRememberService() {
        return rememberService;
    }

    public BrainMapService getBrainMapService() {
        return brainMapService;
    }

    public TrackService getTrackService() {
        return trackService;
    }

    public com.byteforce.repository.AptitudeQuestionRepository getAptitudeQuestionRepository() {
        return aptitudeQuestionRepository;
    }

    public com.byteforce.service.AptitudeQuestionService getAptitudeQuestionService() {
        return aptitudeQuestionService;
    }

    @Override
    public void close() {
        if (managesDataSource) {
            log.info("Closing AppContext managed DataSource...");
            DataSourceFactory.close(dataSource);
        }
    }
}
