package com.byteforce.domain;

/**
 * Memory state of a specific item for an individual learner.
 */
public enum ReviewState {
    /**
     * Item has not been reviewed yet by this learner.
     */
    NEW("New"),

    /**
     * Item is in the initial learning phase (short-term consolidation).
     */
    LEARNING("Learning"),

    /**
     * Item has graduated to long-term spaced repetition review.
     */
    REVIEW("Review"),

    /**
     * Item was previously in review but lapsed (forgotten) and is being relearned.
     */
    RELEARNING("Relearning");

    private final String displayName;

    ReviewState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
