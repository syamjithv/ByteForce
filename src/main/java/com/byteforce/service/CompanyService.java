package com.byteforce.service;

import com.byteforce.domain.AptitudeQuestion;
import com.byteforce.domain.Assessment;
import com.byteforce.domain.Company;
import com.byteforce.domain.CompanyCardView;
import com.byteforce.domain.CompanyInterviewCategory;
import com.byteforce.domain.CompanyPreparationDashboard;
import com.byteforce.domain.Question;
import com.byteforce.domain.Topic;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for Company practice preparation, catalog views, and relationship curation.
 */
public interface CompanyService {

    List<Company> getAllActiveCompanies();

    List<Company> getAllCompanies();

    Optional<Company> getCompanyBySlug(String slug);

    Optional<Company> getCompanyById(long id);

    Company createCompany(Company company);

    Company updateCompany(Company company);

    void setCompanyActive(long id, boolean active);

    void deleteCompany(long id);

    List<CompanyCardView> getCompanyCatalogCards();

    CompanyPreparationDashboard getCompanyPreparationDashboard(String slug, Optional<java.util.UUID> userIdOpt);

    // Relationship Management
    List<Topic> getLinkedTopics(long companyId);

    void setLinkedTopics(long companyId, List<Long> topicIds);

    List<Question> getLinkedQuestions(long companyId);

    void setLinkedQuestions(long companyId, List<Long> questionIds);

    List<AptitudeQuestion> getLinkedAptitudeQuestions(long companyId);

    void setLinkedAptitudeQuestions(long companyId, List<Long> aptitudeQuestionIds);

    List<Assessment> getLinkedAssessments(long companyId);

    void setLinkedAssessments(long companyId, List<Long> assessmentIds);

    List<CompanyInterviewCategory> getLinkedInterviewCategories(long companyId);

    CompanyInterviewCategory addInterviewCategory(long companyId, String title, String categoryType, String description, int displayOrder);

    void removeInterviewCategory(long categoryId);
}
