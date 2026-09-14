package com.byteforce.app;

import com.byteforce.config.AppConfig;
import com.byteforce.persistence.DataSourceFactory;
import com.byteforce.persistence.DatabaseMigrator;
import com.byteforce.repository.ActivityRepository;
import com.byteforce.repository.AttemptRepository;
import com.byteforce.repository.BookmarkRepository;
import com.byteforce.repository.JdbcActivityRepository;
import com.byteforce.repository.JdbcAttemptRepository;
import com.byteforce.repository.JdbcBookmarkRepository;
import com.byteforce.repository.JdbcQuestionRepository;
import com.byteforce.repository.JdbcStudentProfileRepository;
import com.byteforce.repository.JdbcTopicRepository;
import com.byteforce.repository.JdbcUserRepository;
import com.byteforce.repository.QuestionRepository;
import com.byteforce.repository.StudentProfileRepository;
import com.byteforce.repository.TopicRepository;
import com.byteforce.repository.UserRepository;
import com.byteforce.security.BCryptPasswordHasher;
import com.byteforce.security.PasswordHasher;
import com.byteforce.security.UserSession;
import com.byteforce.service.ActivityService;
import com.byteforce.service.ActivityServiceImpl;
import com.byteforce.service.AttemptService;
import com.byteforce.service.AttemptServiceImpl;
import com.byteforce.service.AuthService;
import com.byteforce.service.AuthServiceImpl;
import com.byteforce.service.BookmarkService;
import com.byteforce.service.BookmarkServiceImpl;
import com.byteforce.service.QuestionService;
import com.byteforce.service.QuestionServiceImpl;
import com.byteforce.service.TopicService;
import com.byteforce.service.TopicServiceImpl;
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

    // Services
    private final ActivityService activityService;
    private final AuthService authService;
    private final UserService userService;
    private final TopicService topicService;
    private final QuestionService questionService;
    private final BookmarkService bookmarkService;
    private final AttemptService attemptService;

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

        log.info("Wiring ByteForce services...");
        this.activityService = new ActivityServiceImpl(activityRepository);
        this.authService = new AuthServiceImpl(userRepository, passwordHasher, userSession, activityService);
        this.userService = new UserServiceImpl(userRepository, passwordHasher, studentProfileRepository, activityService);
        this.topicService = new TopicServiceImpl(topicRepository, questionRepository);
        this.questionService = new QuestionServiceImpl(questionRepository, topicRepository);
        this.bookmarkService = new BookmarkServiceImpl(bookmarkRepository, questionRepository, activityService);
        this.attemptService = new AttemptServiceImpl(attemptRepository, questionRepository, activityService);

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

    @Override
    public void close() {
        if (managesDataSource) {
            log.info("Closing AppContext managed DataSource...");
            DataSourceFactory.close(dataSource);
        }
    }
}
