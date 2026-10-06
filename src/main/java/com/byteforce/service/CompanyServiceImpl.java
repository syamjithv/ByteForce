package com.byteforce.service;

import com.byteforce.domain.AptitudeQuestion;
import com.byteforce.domain.Assessment;
import com.byteforce.domain.AssessmentAttempt;
import com.byteforce.domain.AttemptStatus;
import com.byteforce.domain.Company;
import com.byteforce.domain.CompanyCardView;
import com.byteforce.domain.CompanyInterviewCategory;
import com.byteforce.domain.CompanyPreparationDashboard;
import com.byteforce.domain.Question;
import com.byteforce.domain.QuestionAttempt;
import com.byteforce.domain.Topic;
import com.byteforce.exception.ResourceNotFoundException;
import com.byteforce.repository.AptitudeQuestionRepository;
import com.byteforce.repository.AssessmentRepository;
import com.byteforce.repository.CompanyRepository;
import com.byteforce.repository.QuestionRepository;
import com.byteforce.repository.TopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link CompanyService}.
 */
public class CompanyServiceImpl implements CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyServiceImpl.class);

    private final CompanyRepository companyRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final AptitudeQuestionRepository aptitudeQuestionRepository;
    private final AssessmentRepository assessmentRepository;
    private final AttemptService attemptService;
    private final AssessmentService assessmentService;

    public CompanyServiceImpl(CompanyRepository companyRepository,
                              TopicRepository topicRepository,
                              QuestionRepository questionRepository,
                              AptitudeQuestionRepository aptitudeQuestionRepository,
                              AssessmentRepository assessmentRepository,
                              AttemptService attemptService,
                              AssessmentService assessmentService) {
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository must not be null");
        this.topicRepository = Objects.requireNonNull(topicRepository, "topicRepository must not be null");
        this.questionRepository = Objects.requireNonNull(questionRepository, "questionRepository must not be null");
        this.aptitudeQuestionRepository = Objects.requireNonNull(aptitudeQuestionRepository, "aptitudeQuestionRepository must not be null");
        this.assessmentRepository = Objects.requireNonNull(assessmentRepository, "assessmentRepository must not be null");
        this.attemptService = Objects.requireNonNull(attemptService, "attemptService must not be null");
        this.assessmentService = Objects.requireNonNull(assessmentService, "assessmentService must not be null");
    }

    @Override
    public List<Company> getAllActiveCompanies() {
        return companyRepository.findAllActive();
    }

    @Override
    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    @Override
    public Optional<Company> getCompanyBySlug(String slug) {
        return companyRepository.findBySlug(slug);
    }

    @Override
    public Optional<Company> getCompanyById(long id) {
        return companyRepository.findById(id);
    }

    @Override
    public Company createCompany(Company company) {
        Objects.requireNonNull(company, "company must not be null");
        log.info("Creating new company: {}", company.getName());
        return companyRepository.save(company);
    }

    @Override
    public Company updateCompany(Company company) {
        Objects.requireNonNull(company, "company must not be null");
        log.info("Updating company id: {}", company.getId());
        companyRepository.update(company);
        return company;
    }

    @Override
    public void setCompanyActive(long id, boolean active) {
        companyRepository.setCompanyActive(id, active);
    }

    @Override
    public void deleteCompany(long id) {
        companyRepository.deleteById(id);
    }

    @Override
    public List<CompanyCardView> getCompanyCatalogCards() {
        List<Company> companies = companyRepository.findAllActive();
        List<CompanyCardView> cards = new ArrayList<>();

        for (Company comp : companies) {
            long compId = comp.getId();
            List<Long> topicIds = companyRepository.findTopicIdsByCompanyId(compId);
            List<Long> questionIds = companyRepository.findQuestionIdsByCompanyId(compId);
            List<Long> aptitudeIds = companyRepository.findAptitudeQuestionIdsByCompanyId(compId);
            List<Long> assessmentIds = companyRepository.findAssessmentIdsByCompanyId(compId);

            List<String> bullets = buildHonestBullets(topicIds.size(), questionIds.size(), aptitudeIds.size(), assessmentIds.size());
            cards.add(new CompanyCardView(comp, bullets, topicIds.size(), questionIds.size(), aptitudeIds.size(), assessmentIds.size()));
        }

        return cards;
    }

    private List<String> buildHonestBullets(int topicCount, int questionCount, int aptitudeCount, int assessmentCount) {
        List<String> bullets = new ArrayList<>(3);
        bullets.add("Technical concepts & CS foundations");
        bullets.add("Coding questions & targeted problem solving");
        bullets.add("Placement interview preparation & timed assessments");
        return Collections.unmodifiableList(bullets);
    }

    @Override
    public CompanyPreparationDashboard getCompanyPreparationDashboard(String slug, Optional<UUID> userIdOpt) {
        Company company = companyRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with slug: " + slug));

        long compId = company.getId();

        // 1. Fetch linked content
        List<Topic> linkedTopics = getLinkedTopics(compId);
        List<Question> linkedQuestions = getLinkedQuestions(compId);
        List<AptitudeQuestion> linkedAptitude = getLinkedAptitudeQuestions(compId);
        List<Assessment> linkedAssessments = getLinkedAssessments(compId);
        List<CompanyInterviewCategory> linkedInterview = getLinkedInterviewCategories(compId);

        // 2. Compute honest student progress (zero fake statistics)
        long qAttempted = 0;
        long qSolved = 0;
        long aAttempted = 0;
        long aSolved = 0;
        long testsCompleted = 0;
        boolean hasActivity = false;

        if (userIdOpt.isPresent()) {
            UUID userId = userIdOpt.get();

            // Coding questions progress
            if (!linkedQuestions.isEmpty()) {
                Set<Long> linkedQIds = linkedQuestions.stream().map(Question::getId).collect(Collectors.toSet());
                List<QuestionAttempt> attempts = attemptService.getAttemptsForUser(userId);
                Set<Long> attemptedQIds = new HashSet<>();
                Set<Long> solvedQIds = new HashSet<>();

                for (QuestionAttempt qa : attempts) {
                    if (linkedQIds.contains(qa.getQuestionId())) {
                        attemptedQIds.add(qa.getQuestionId());
                        if (qa.getStatus() == AttemptStatus.SOLVED) {
                            solvedQIds.add(qa.getQuestionId());
                        }
                    }
                }
                qAttempted = attemptedQIds.size();
                qSolved = solvedQIds.size();
            }

            // Assessments completed progress
            if (!linkedAssessments.isEmpty()) {
                Set<Long> linkedAssessmentIds = linkedAssessments.stream().map(Assessment::getId).collect(Collectors.toSet());
                List<AssessmentAttempt> completedAttempts = assessmentService.getAttemptsForUser(userId);
                testsCompleted = completedAttempts.stream()
                        .filter(aa -> linkedAssessmentIds.contains(aa.getAssessmentId()) && ("COMPLETED".equalsIgnoreCase(aa.getStatus()) || aa.getCompletedAt() != null))
                        .count();
            }

            hasActivity = (qAttempted > 0 || testsCompleted > 0);
        }

        return new CompanyPreparationDashboard(
                company,
                linkedTopics,
                linkedQuestions,
                linkedAptitude,
                linkedAssessments,
                linkedInterview,
                qAttempted,
                qSolved,
                aAttempted,
                aSolved,
                testsCompleted,
                hasActivity
        );
    }

    // ==========================================
    // Relationship Management
    // ==========================================
    @Override
    public List<Topic> getLinkedTopics(long companyId) {
        List<Long> ids = companyRepository.findTopicIdsByCompanyId(companyId);
        List<Topic> topics = new ArrayList<>();
        for (Long id : ids) {
            topicRepository.findById(id).ifPresent(topics::add);
        }
        return topics;
    }

    @Override
    public void setLinkedTopics(long companyId, List<Long> topicIds) {
        companyRepository.setCompanyTopics(companyId, topicIds);
    }

    @Override
    public List<Question> getLinkedQuestions(long companyId) {
        List<Long> ids = companyRepository.findQuestionIdsByCompanyId(companyId);
        List<Question> questions = new ArrayList<>();
        for (Long id : ids) {
            questionRepository.findById(id).ifPresent(questions::add);
        }
        return questions;
    }

    @Override
    public void setLinkedQuestions(long companyId, List<Long> questionIds) {
        companyRepository.setCompanyQuestions(companyId, questionIds);
    }

    @Override
    public List<AptitudeQuestion> getLinkedAptitudeQuestions(long companyId) {
        List<Long> ids = companyRepository.findAptitudeQuestionIdsByCompanyId(companyId);
        List<AptitudeQuestion> questions = new ArrayList<>();
        for (Long id : ids) {
            aptitudeQuestionRepository.findById(id).ifPresent(questions::add);
        }
        return questions;
    }

    @Override
    public void setLinkedAptitudeQuestions(long companyId, List<Long> aptitudeQuestionIds) {
        companyRepository.setCompanyAptitudeQuestions(companyId, aptitudeQuestionIds);
    }

    @Override
    public List<Assessment> getLinkedAssessments(long companyId) {
        List<Long> ids = companyRepository.findAssessmentIdsByCompanyId(companyId);
        List<Assessment> assessments = new ArrayList<>();
        for (Long id : ids) {
            assessmentRepository.findById(id).ifPresent(assessments::add);
        }
        return assessments;
    }

    @Override
    public void setLinkedAssessments(long companyId, List<Long> assessmentIds) {
        companyRepository.setCompanyAssessments(companyId, assessmentIds);
    }

    @Override
    public List<CompanyInterviewCategory> getLinkedInterviewCategories(long companyId) {
        return companyRepository.findInterviewCategoriesByCompanyId(companyId);
    }

    @Override
    public CompanyInterviewCategory addInterviewCategory(long companyId, String title, String categoryType, String description, int displayOrder) {
        CompanyInterviewCategory cat = CompanyInterviewCategory.create(companyId, title, categoryType, description, displayOrder);
        return companyRepository.saveInterviewCategory(cat);
    }

    @Override
    public void removeInterviewCategory(long categoryId) {
        companyRepository.deleteInterviewCategory(categoryId);
    }
}
