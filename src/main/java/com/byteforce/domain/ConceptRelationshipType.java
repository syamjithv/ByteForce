package com.byteforce.domain;

/**
 * Enumeration of concept relationship types representing edges in the Brain Map.
 */
public enum ConceptRelationshipType {
    PREREQUISITE("Prerequisite", "Required foundational knowledge"),
    RELATED("Related", "Conceptually linked or complementary"),
    PARENT("Parent", "Broader foundational parent concept"),
    CHILD("Child", "Specialized sub-concept"),
    COMMONLY_CONFUSED("Commonly Confused", "Often confused or compared in interviews");

    private final String displayName;
    private final String description;

    ConceptRelationshipType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
