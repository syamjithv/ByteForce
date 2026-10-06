package com.byteforce.domain;

import java.util.List;
import java.util.Objects;

/**
 * View model representing the dedicated Company Preparation Dashboard (/companies/{slug}).
 * Acts as a curated lens over Learn, Practice, Aptitude, Assess, Interview, and Track.
 */
public class CompanyPreparationDashboard {

    private final Company company;
    private final List<Topic> linkedTopics;
    private final List<Question> linkedQuestions;
    private final List<AptitudeQuestion> linkedAptitudeQuestions;
    private final List<Assessment> linkedAssessments;
    private final List<CompanyInterviewCategory> linkedInterviewCategories;

    // Student Progress metrics (honest stats only)
    private final long questionsAttempted;
    private final long questionsSolved;
    private final long aptitudeAttempted;
    private final long aptitudeSolved;
    private final long assessmentsCompleted;
    private final boolean hasActivity;

    public CompanyPreparationDashboard(Company company,
                                       List<Topic> linkedTopics,
                                       List<Question> linkedQuestions,
                                       List<AptitudeQuestion> linkedAptitudeQuestions,
                                       List<Assessment> linkedAssessments,
                                       List<CompanyInterviewCategory> linkedInterviewCategories,
                                       long questionsAttempted,
                                       long questionsSolved,
                                       long aptitudeAttempted,
                                       long aptitudeSolved,
                                       long assessmentsCompleted,
                                       boolean hasActivity) {
        this.company = Objects.requireNonNull(company, "company must not be null");
        this.linkedTopics = linkedTopics != null ? linkedTopics : List.of();
        this.linkedQuestions = linkedQuestions != null ? linkedQuestions : List.of();
        this.linkedAptitudeQuestions = linkedAptitudeQuestions != null ? linkedAptitudeQuestions : List.of();
        this.linkedAssessments = linkedAssessments != null ? linkedAssessments : List.of();
        this.linkedInterviewCategories = linkedInterviewCategories != null ? linkedInterviewCategories : List.of();
        this.questionsAttempted = questionsAttempted;
        this.questionsSolved = questionsSolved;
        this.aptitudeAttempted = aptitudeAttempted;
        this.aptitudeSolved = aptitudeSolved;
        this.assessmentsCompleted = assessmentsCompleted;
        this.hasActivity = hasActivity;
    }

    public Company getCompany() {
        return company;
    }

    public List<Topic> getLinkedTopics() {
        return linkedTopics;
    }

    public List<Question> getLinkedQuestions() {
        return linkedQuestions;
    }

    public List<AptitudeQuestion> getLinkedAptitudeQuestions() {
        return linkedAptitudeQuestions;
    }

    public List<Assessment> getLinkedAssessments() {
        return linkedAssessments;
    }

    public List<CompanyInterviewCategory> getLinkedInterviewCategories() {
        return linkedInterviewCategories;
    }

    public long getQuestionsAttempted() {
        return questionsAttempted;
    }

    public long getQuestionsSolved() {
        return questionsSolved;
    }

    public long getAptitudeAttempted() {
        return aptitudeAttempted;
    }

    public long getAptitudeSolved() {
        return aptitudeSolved;
    }

    public long getAssessmentsCompleted() {
        return assessmentsCompleted;
    }

    public boolean isHasActivity() {
        return hasActivity;
    }

    public boolean hasTopics() {
        return !linkedTopics.isEmpty();
    }

    public boolean hasQuestions() {
        return !linkedQuestions.isEmpty();
    }

    public boolean hasAptitude() {
        return !linkedAptitudeQuestions.isEmpty();
    }

    public boolean hasAssessments() {
        return !linkedAssessments.isEmpty();
    }

    public boolean hasInterview() {
        return !linkedInterviewCategories.isEmpty();
    }
}
