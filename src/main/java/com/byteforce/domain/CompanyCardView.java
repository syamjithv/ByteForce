package com.byteforce.domain;

import java.util.List;
import java.util.Objects;

/**
 * View model representing a company card in the DataCamp-inspired student catalog (/companies).
 */
public class CompanyCardView {

    private final Company company;
    private final List<String> preparationPoints;
    private final int topicCount;
    private final int questionCount;
    private final int aptitudeCount;
    private final int assessmentCount;

    public CompanyCardView(Company company,
                           List<String> preparationPoints,
                           int topicCount,
                           int questionCount,
                           int aptitudeCount,
                           int assessmentCount) {
        this.company = Objects.requireNonNull(company, "company must not be null");
        this.preparationPoints = preparationPoints != null ? preparationPoints : List.of();
        this.topicCount = topicCount;
        this.questionCount = questionCount;
        this.aptitudeCount = aptitudeCount;
        this.assessmentCount = assessmentCount;
    }

    public Company getCompany() {
        return company;
    }

    public List<String> getPreparationPoints() {
        return preparationPoints;
    }

    public int getTopicCount() {
        return topicCount;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public int getAptitudeCount() {
        return aptitudeCount;
    }

    public int getAssessmentCount() {
        return assessmentCount;
    }

    public boolean hasCuratedContent() {
        return topicCount > 0 || questionCount > 0 || aptitudeCount > 0 || assessmentCount > 0;
    }
}
