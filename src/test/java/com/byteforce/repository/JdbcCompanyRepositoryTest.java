package com.byteforce.repository;

import com.byteforce.domain.AptitudeCategory;
import com.byteforce.domain.AptitudeQuestion;
import com.byteforce.domain.Assessment;
import com.byteforce.domain.Company;
import com.byteforce.domain.CompanyInterviewCategory;
import com.byteforce.domain.Difficulty;
import com.byteforce.domain.Question;
import com.byteforce.domain.Topic;
import com.byteforce.persistence.DatabaseMigrator;
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

class JdbcCompanyRepositoryTest {

    private HikariDataSource dataSource;
    private JdbcCompanyRepository companyRepository;
    private JdbcTopicRepository topicRepository;
    private JdbcQuestionRepository questionRepository;
    private JdbcAptitudeQuestionRepository aptitudeQuestionRepository;
    private JdbcAssessmentRepository assessmentRepository;

    @BeforeEach
    void setUp() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("CompanyRepoTestPool-" + UUID.randomUUID());
        hikariConfig.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        hikariConfig.setUsername("sa");
        hikariConfig.setPassword("");
        hikariConfig.setMaximumPoolSize(5);

        dataSource = new HikariDataSource(hikariConfig);
        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");

        companyRepository = new JdbcCompanyRepository(dataSource);
        topicRepository = new JdbcTopicRepository(dataSource);
        questionRepository = new JdbcQuestionRepository(dataSource);
        aptitudeQuestionRepository = new JdbcAptitudeQuestionRepository(dataSource);
        assessmentRepository = new JdbcAssessmentRepository(dataSource);
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Test
    @DisplayName("Should find initial seeded companies like TCS and Microsoft")
    void shouldFindInitialSeededCompanies() {
        Optional<Company> tcsOpt = companyRepository.findBySlug("tcs");
        assertTrue(tcsOpt.isPresent());
        assertEquals("TCS", tcsOpt.get().getName());
        assertTrue(tcsOpt.get().isActive());

        Optional<Company> msOpt = companyRepository.findBySlug("microsoft");
        assertTrue(msOpt.isPresent());
        assertEquals("Microsoft", msOpt.get().getName());

        List<Company> allActive = companyRepository.findAllActive();
        assertTrue(allActive.size() >= 12);
    }

    @Test
    @DisplayName("Should save, find by id and find by slug for new company")
    void shouldSaveAndRetrieveCompany() {
        Company company = Company.create(
                "Test Corp",
                "test-corp",
                "/images/companies/test.svg",
                "https://testcorp.com",
                "Global technology services enterprise.",
                "Detailed overview of Test Corp preparation.",
                true
        );

        Company saved = companyRepository.save(company);
        assertNotNull(saved.getId());

        Optional<Company> byId = companyRepository.findById(saved.getId());
        assertTrue(byId.isPresent());
        assertEquals("Test Corp", byId.get().getName());
        assertEquals("test-corp", byId.get().getSlug());

        Optional<Company> bySlug = companyRepository.findBySlug("test-corp");
        assertTrue(bySlug.isPresent());
        assertEquals("Test Corp", bySlug.get().getName());
    }

    @Test
    @DisplayName("Should update company details and toggle active status")
    void shouldUpdateCompanyAndToggleActive() {
        Company company = Company.create(
                "Updatable Co",
                "updatable-co",
                "/images/companies/up.svg",
                "https://up.com",
                "Short info",
                "Full description",
                true
        );
        Company saved = companyRepository.save(company);

        Company toUpdate = new Company(
                saved.getId(),
                "Updated Co",
                saved.getSlug(),
                saved.getLogoPath(),
                saved.getWebsiteUrl(),
                "New short info",
                saved.getDescription(),
                saved.isActive(),
                saved.getLastReviewedAt(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
        companyRepository.update(toUpdate);

        Optional<Company> reloaded = companyRepository.findById(saved.getId());
        assertTrue(reloaded.isPresent());
        assertEquals("Updated Co", reloaded.get().getName());
        assertEquals("New short info", reloaded.get().getShortDescription());

        // Toggle active to false
        companyRepository.setCompanyActive(saved.getId(), false);
        reloaded = companyRepository.findById(saved.getId());
        assertTrue(reloaded.isPresent());
        assertFalse(reloaded.get().isActive());

        List<Company> activeList = companyRepository.findAllActive();
        assertFalse(activeList.stream().anyMatch(c -> c.getId().equals(saved.getId())));
    }

    @Test
    @DisplayName("Should manage relationships: topics, questions, aptitude questions, assessments")
    void shouldManageCompanyContentRelationships() {
        Optional<Company> tcsOpt = companyRepository.findBySlug("tcs");
        assertTrue(tcsOpt.isPresent());
        long companyId = tcsOpt.get().getId();

        // 1. Create referenced entities to satisfy foreign key constraints
        Topic t1 = topicRepository.save(Topic.create("Topic 1", "topic-1", "Desc 1", 1));
        Topic t2 = topicRepository.save(Topic.create("Topic 2", "topic-2", "Desc 2", 2));
        Topic t3 = topicRepository.save(Topic.create("Topic 3", "topic-3", "Desc 3", 3));

        Question q1 = questionRepository.save(Question.create(t1.getId(), "Question 1", "q-1", "Desc", Difficulty.EASY, ""));
        Question q2 = questionRepository.save(Question.create(t1.getId(), "Question 2", "q-2", "Desc", Difficulty.MEDIUM, ""));

        AptitudeQuestion aq1 = aptitudeQuestionRepository.save(AptitudeQuestion.create(
                AptitudeCategory.QUANTITATIVE, "Numbers", Difficulty.EASY,
                "What is 2+2?", "3", "4", "5", "6", "B", "2+2=4"));

        Assessment as1 = assessmentRepository.save(Assessment.create(
                "Mock 1", "Assessment description", 60, 100, Difficulty.MEDIUM, t1.getId()));

        // Topics
        companyRepository.setCompanyTopics(companyId, List.of(t1.getId(), t2.getId(), t3.getId()));
        List<Long> topicIds = companyRepository.findTopicIdsByCompanyId(companyId);
        assertEquals(3, topicIds.size());
        assertTrue(topicIds.contains(t1.getId()));

        companyRepository.unlinkTopic(companyId, t2.getId());
        topicIds = companyRepository.findTopicIdsByCompanyId(companyId);
        assertEquals(2, topicIds.size());
        assertFalse(topicIds.contains(t2.getId()));

        // Questions
        companyRepository.setCompanyQuestions(companyId, List.of(q1.getId(), q2.getId()));
        List<Long> questionIds = companyRepository.findQuestionIdsByCompanyId(companyId);
        assertEquals(2, questionIds.size());

        // Aptitude questions
        companyRepository.setCompanyAptitudeQuestions(companyId, List.of(aq1.getId()));
        List<Long> aptitudeIds = companyRepository.findAptitudeQuestionIdsByCompanyId(companyId);
        assertEquals(1, aptitudeIds.size());

        // Assessments
        companyRepository.setCompanyAssessments(companyId, List.of(as1.getId()));
        List<Long> assessmentIds = companyRepository.findAssessmentIdsByCompanyId(companyId);
        assertEquals(1, assessmentIds.size());
    }

    @Test
    @DisplayName("Should manage interview categories")
    void shouldManageInterviewCategories() {
        Optional<Company> tcsOpt = companyRepository.findBySlug("tcs");
        assertTrue(tcsOpt.isPresent());
        long companyId = tcsOpt.get().getId();

        CompanyInterviewCategory cat1 = CompanyInterviewCategory.create(
                companyId, "Technical Round 1", "TECHNICAL", "DSA and Core CS concepts", 1);
        CompanyInterviewCategory savedCat = companyRepository.saveInterviewCategory(cat1);
        assertNotNull(savedCat.getId());

        List<CompanyInterviewCategory> categories = companyRepository.findInterviewCategoriesByCompanyId(companyId);
        assertFalse(categories.isEmpty());
        assertTrue(categories.stream().anyMatch(c -> c.getTitle().equals("Technical Round 1")));

        companyRepository.deleteInterviewCategory(savedCat.getId());
        categories = companyRepository.findInterviewCategoriesByCompanyId(companyId);
        assertFalse(categories.stream().anyMatch(c -> c.getId().equals(savedCat.getId())));
    }
}
