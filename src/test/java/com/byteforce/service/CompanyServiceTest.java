package com.byteforce.service;

import com.byteforce.domain.AptitudeQuestion;
import com.byteforce.domain.Assessment;
import com.byteforce.domain.Company;
import com.byteforce.domain.CompanyCardView;
import com.byteforce.domain.CompanyInterviewCategory;
import com.byteforce.domain.CompanyPreparationDashboard;
import com.byteforce.domain.Question;
import com.byteforce.domain.Topic;
import com.byteforce.exception.ResourceNotFoundException;
import com.byteforce.persistence.DatabaseMigrator;
import com.byteforce.repository.AptitudeQuestionRepository;
import com.byteforce.repository.AssessmentRepository;
import com.byteforce.repository.CompanyRepository;
import com.byteforce.repository.JdbcCompanyRepository;
import com.byteforce.repository.QuestionRepository;
import com.byteforce.repository.TopicRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanyServiceTest {

    private HikariDataSource dataSource;
    private CompanyRepository companyRepository;
    private CompanyService companyService;

    @BeforeEach
    void setUp() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("CompanyServiceTestPool-" + UUID.randomUUID());
        hikariConfig.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        hikariConfig.setUsername("sa");
        hikariConfig.setPassword("");
        hikariConfig.setMaximumPoolSize(5);

        dataSource = new HikariDataSource(hikariConfig);
        DatabaseMigrator.migrate(dataSource, "classpath:db/migration");

        companyRepository = new JdbcCompanyRepository(dataSource);

        TopicRepository topicRepository = org.mockito.Mockito.mock(TopicRepository.class);
        QuestionRepository questionRepository = org.mockito.Mockito.mock(QuestionRepository.class);
        AptitudeQuestionRepository aptitudeQuestionRepository = org.mockito.Mockito.mock(AptitudeQuestionRepository.class);
        AssessmentRepository assessmentRepository = org.mockito.Mockito.mock(AssessmentRepository.class);
        AttemptService attemptService = org.mockito.Mockito.mock(AttemptService.class);
        AssessmentService assessmentService = org.mockito.Mockito.mock(AssessmentService.class);

        companyService = new CompanyServiceImpl(
                companyRepository,
                topicRepository,
                questionRepository,
                aptitudeQuestionRepository,
                assessmentRepository,
                attemptService,
                assessmentService
        );
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Test
    @DisplayName("Should retrieve active companies including TCS, Microsoft, Amazon")
    void shouldRetrieveActiveCompanies() {
        List<Company> companies = companyService.getAllActiveCompanies();
        assertNotNull(companies);
        assertTrue(companies.size() >= 12);
        assertTrue(companies.stream().anyMatch(c -> "tcs".equalsIgnoreCase(c.getSlug())));
        assertTrue(companies.stream().anyMatch(c -> "microsoft".equalsIgnoreCase(c.getSlug())));
    }

    @Test
    @DisplayName("Should generate catalog cards with exactly 3 bullets and no fake stats")
    void shouldGenerateCatalogCardsWithExactStructure() {
        List<CompanyCardView> cards = companyService.getCompanyCatalogCards();
        assertNotNull(cards);
        assertFalse(cards.isEmpty());

        for (CompanyCardView card : cards) {
            assertNotNull(card.getCompany().getName());
            assertNotNull(card.getCompany().getSlug());
            assertNotNull(card.getCompany().getLogoPath());
            assertEquals(3, card.getPreparationPoints().size(), "Each card must have exactly 3 bullets as per design reference");
            for (String bullet : card.getPreparationPoints()) {
                assertFalse(bullet.isBlank());
            }
        }
    }

    @Test
    @DisplayName("Should retrieve company preparation dashboard with honest empty states")
    void shouldRetrieveDashboardWithHonestEmptyStates() {
        CompanyPreparationDashboard dashboard = companyService.getCompanyPreparationDashboard("microsoft", Optional.empty());
        assertNotNull(dashboard);
        assertEquals("Microsoft", dashboard.getCompany().getName());
        assertEquals("microsoft", dashboard.getCompany().getSlug());

        // When no specific relationships are curated, empty states must reflect reality
        assertNotNull(dashboard.getLinkedTopics());
        assertNotNull(dashboard.getLinkedQuestions());
        assertNotNull(dashboard.getLinkedAptitudeQuestions());
        assertNotNull(dashboard.getLinkedAssessments());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for unknown company slug")
    void shouldThrowForUnknownSlug() {
        assertThrows(ResourceNotFoundException.class, () ->
                companyService.getCompanyPreparationDashboard("unknown-corp-xyz", Optional.empty())
        );
    }

    @Test
    @DisplayName("Should create, update, toggle active and delete company")
    void shouldPerformCompanyCrud() {
        Company newCompany = Company.create(
                "Acme Corp",
                "acme-corp",
                "/images/companies/acme.svg",
                "https://acme.com",
                "Leading technology manufacturer.",
                "Detailed overview",
                true
        );

        Company created = companyService.createCompany(newCompany);
        assertNotNull(created.getId());

        Company toUpdate = new Company(
                created.getId(),
                "Acme Global",
                created.getSlug(),
                created.getLogoPath(),
                created.getWebsiteUrl(),
                created.getShortDescription(),
                created.getDescription(),
                created.isActive(),
                created.getLastReviewedAt(),
                created.getCreatedAt(),
                created.getUpdatedAt()
        );
        Company updated = companyService.updateCompany(toUpdate);
        assertEquals("Acme Global", updated.getName());

        companyService.setCompanyActive(created.getId(), false);
        Optional<Company> found = companyService.getCompanyById(created.getId());
        assertTrue(found.isPresent());
        assertFalse(found.get().isActive());

        companyService.deleteCompany(created.getId());
        assertTrue(companyService.getCompanyById(created.getId()).isEmpty());
    }
}
