package com.byteforce.domain;

/**
 * Ratings given by a learner when performing retrieval practice on a RememberItem.
 * Aligns with the 4-point rating scale of modern spaced-repetition engines (e.g., FSRS).
 */
public enum ReviewRating {
    AGAIN(1, "Again", "Complete recall failure or significant error; requires relearning."),
    HARD(2, "Hard", "Recalled with substantial effort or hesitation."),
    GOOD(3, "Good", "Successful recall with normal cognitive effort."),
    EASY(4, "Easy", "Effortless, instantaneous recall.");

    private final int value;
    private final String displayName;
    private final String description;

    ReviewRating(int value, String displayName, String description) {
        this.value = value;
        this.displayName = displayName;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static ReviewRating fromValue(int value) {
        for (ReviewRating r : values()) {
            if (r.value == value) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown review rating value: " + value);
    }
}
