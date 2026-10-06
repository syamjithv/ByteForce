package com.byteforce.repository;

import com.byteforce.domain.Company;
import com.byteforce.domain.CompanyInterviewCategory;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Company entities and their content relationships.
 */
public interface CompanyRepository {

    Optional<Company> findById(long id);

    Optional<Company> findBySlug(String slug);

    List<Company> findAll();

    List<Company> findAllActive();

    Company save(Company company);

    void update(Company company);

    void deleteById(long id);

    void setCompanyActive(long id, boolean active);

    // Content Relationships
    List<Long> findTopicIdsByCompanyId(long companyId);

    void linkTopic(long companyId, long topicId);

    void unlinkTopic(long companyId, long topicId);

    void setCompanyTopics(long companyId, List<Long> topicIds);

    List<Long> findQuestionIdsByCompanyId(long companyId);

    void linkQuestion(long companyId, long questionId);

    void unlinkQuestion(long companyId, long questionId);

    void setCompanyQuestions(long companyId, List<Long> questionIds);

    List<Long> findAptitudeQuestionIdsByCompanyId(long companyId);

    void linkAptitudeQuestion(long companyId, long aptitudeQuestionId);

    void unlinkAptitudeQuestion(long companyId, long aptitudeQuestionId);

    void setCompanyAptitudeQuestions(long companyId, List<Long> aptitudeQuestionIds);

    List<Long> findAssessmentIdsByCompanyId(long companyId);

    void linkAssessment(long companyId, long assessmentId);

    void unlinkAssessment(long companyId, long assessmentId);

    void setCompanyAssessments(long companyId, List<Long> assessmentIds);

    List<CompanyInterviewCategory> findInterviewCategoriesByCompanyId(long companyId);

    CompanyInterviewCategory saveInterviewCategory(CompanyInterviewCategory category);

    void deleteInterviewCategory(long categoryId);
}
