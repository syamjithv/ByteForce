package com.byteforce.domain;

/**
 * Discriminator representing the category of an aptitude preparation question.
 */
public enum AptitudeCategory {
    QUANTITATIVE("Quantitative Aptitude"),
    LOGICAL("Logical Reasoning"),
    VERBAL("Verbal Ability");

    private final String displayName;

    AptitudeCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
