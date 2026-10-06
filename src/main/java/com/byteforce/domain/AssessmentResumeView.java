package com.byteforce.domain;

/**
 * Presentation-friendly view model for resuming an incomplete assessment attempt.
 * E.g.:
 * Assessment in progress
 * DSA Fundamentals Mock
 * Question 17 of 30
 * [ Continue Assessment ]
 */
public record AssessmentResumeView(
        long attemptId,
        long assessmentId,
        String assessmentTitle,
        int answeredQuestions,
        int totalQuestions,
        int currentQuestionNumber
) {}
