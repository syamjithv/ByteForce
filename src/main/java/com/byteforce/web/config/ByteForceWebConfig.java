package com.byteforce.web.config;

import com.byteforce.app.AppContext;
import com.byteforce.config.AppConfig;
import com.byteforce.service.ActivityService;
import com.byteforce.service.AssessmentService;
import com.byteforce.service.AttemptService;
import com.byteforce.service.AuthService;
import com.byteforce.service.BookmarkService;
import com.byteforce.service.LearnService;
import com.byteforce.service.QuestionService;
import com.byteforce.service.TopicService;
import com.byteforce.service.TrackService;
import com.byteforce.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Web configuration that integrates the existing ByteForce composition root
 * without rewriting or modifying backend service implementations.
 */
@Configuration
public class ByteForceWebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(ByteForceWebConfig.class);

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("Registering AdminAuthInterceptor for /admin/** routes...");
        registry.addInterceptor(new com.byteforce.web.interceptor.AdminAuthInterceptor())
                .addPathPatterns("/admin/**");
    }

    @Bean
    public AppConfig appConfig(org.springframework.core.env.Environment env) {
        log.info("Loading existing ByteForce AppConfig...");
        String resourcePath = AppConfig.DEFAULT_PROPERTIES_FILE;
        if (env.matchesProfiles("test") || "true".equalsIgnoreCase(System.getProperty("byteforce.test"))) {
            resourcePath = "/application-test.properties";
            log.info("Test profile detected. Using test configuration: {}", resourcePath);
        }
        return AppConfig.load(resourcePath, System::getenv);
    }

    @Bean(destroyMethod = "close")
    public AppContext appContext(AppConfig appConfig) {
        log.info("Initializing ByteForce AppContext composition root for Web tier...");
        return AppContext.initialize(appConfig);
    }

    @Bean
    public AuthService authService(AppContext appContext) {
        return appContext.getAuthService();
    }

    @Bean
    public com.byteforce.security.PasswordHasher passwordHasher(AppContext appContext) {
        return appContext.getPasswordHasher();
    }

    @Bean
    public UserService userService(AppContext appContext) {
        return appContext.getUserService();
    }

    @Bean
    public TopicService topicService(AppContext appContext) {
        return appContext.getTopicService();
    }

    @Bean
    public QuestionService questionService(AppContext appContext) {
        return appContext.getQuestionService();
    }

    @Bean
    public AttemptService attemptService(AppContext appContext) {
        return appContext.getAttemptService();
    }

    @Bean
    public BookmarkService bookmarkService(AppContext appContext) {
        return appContext.getBookmarkService();
    }

    @Bean
    public AssessmentService assessmentService(AppContext appContext) {
        return appContext.getAssessmentService();
    }

    @Bean
    public com.byteforce.repository.TopicRepository topicRepository(AppContext appContext) {
        return appContext.getTopicRepository();
    }

    @Bean
    public com.byteforce.repository.QuestionRepository questionRepository(AppContext appContext) {
        return appContext.getQuestionRepository();
    }

    @Bean
    public com.byteforce.repository.UserRepository userRepository(AppContext appContext) {
        return appContext.getUserRepository();
    }

    @Bean
    public com.byteforce.repository.StudentProfileRepository studentProfileRepository(AppContext appContext) {
        return appContext.getStudentProfileRepository();
    }

    @Bean
    public com.byteforce.repository.SubjectRepository subjectRepository(AppContext appContext) {
        return appContext.getSubjectRepository();
    }

    @Bean
    public com.byteforce.repository.ConceptRepository conceptRepository(AppContext appContext) {
        return appContext.getConceptRepository();
    }

    @Bean
    public com.byteforce.repository.LearningResourceRepository learningResourceRepository(AppContext appContext) {
        return appContext.getLearningResourceRepository();
    }

    @Bean
    public com.byteforce.repository.LearnRepository learnRepository(AppContext appContext) {
        return appContext.getLearnRepository();
    }

    @Bean
    public com.byteforce.repository.RememberItemRepository rememberItemRepository(AppContext appContext) {
        return appContext.getRememberItemRepository();
    }

    @Bean
    public com.byteforce.repository.ConceptRelationshipRepository conceptRelationshipRepository(AppContext appContext) {
        return appContext.getConceptRelationshipRepository();
    }

    @Bean
    public LearnService learnService(AppContext appContext) {
        return appContext.getLearnService();
    }

    @Bean
    public com.byteforce.service.RememberService rememberService(AppContext appContext) {
        return appContext.getRememberService();
    }

    @Bean
    public com.byteforce.service.BrainMapService brainMapService(AppContext appContext) {
        return appContext.getBrainMapService();
    }

    @Bean
    public TrackService trackService(AppContext appContext) {
        return appContext.getTrackService();
    }

    @Bean
    public ActivityService activityService(AppContext appContext) {
        return appContext.getActivityService();
    }

    @Bean
    public com.byteforce.repository.AssessmentRepository assessmentRepository(AppContext appContext) {
        return appContext.getAssessmentRepository();
    }

    @Bean
    public com.byteforce.repository.AptitudeQuestionRepository aptitudeQuestionRepository(AppContext appContext) {
        return appContext.getAptitudeQuestionRepository();
    }

    @Bean
    public com.byteforce.service.AptitudeQuestionService aptitudeQuestionService(AppContext appContext) {
        return appContext.getAptitudeQuestionService();
    }

    @Bean
    public com.byteforce.repository.UserRememberReviewRepository userRememberReviewRepository(AppContext appContext) {
        return appContext.getUserRememberReviewRepository();
    }

    @Bean
    public com.byteforce.service.memory.SpacedRepetitionScheduler spacedRepetitionScheduler(AppContext appContext) {
        return appContext.getSpacedRepetitionScheduler();
    }

    @Bean
    public com.byteforce.service.MemoryService memoryService(AppContext appContext) {
        return appContext.getMemoryService();
    }

    @Bean
    public com.byteforce.repository.CompanyRepository companyRepository(AppContext appContext) {
        return appContext.getCompanyRepository();
    }

    @Bean
    public com.byteforce.service.CompanyService companyService(AppContext appContext) {
        return appContext.getCompanyService();
    }

    @Bean
    public com.byteforce.repository.UserConceptProgressRepository userConceptProgressRepository(AppContext appContext) {
        return appContext.getUserConceptProgressRepository();
    }

    @Bean
    public com.byteforce.service.ConceptProgressService conceptProgressService(AppContext appContext) {
        return appContext.getConceptProgressService();
    }
}
