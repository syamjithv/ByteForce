package com.byteforce.domain;

/**
 * Enumeration of Remember item categories for quick revision.
 */
public enum RememberItemType {
    KEY_FACT("Key Fact", ""),
    COMMON_CONFUSION("Common Confusion", ""),
    FORMULA("Formula", ""),
    QUICK_EXAMPLE("Quick Example", ""),
    MEMORY_TRICK("Memory Trick", ""),
    INTERVIEW_REMINDER("Interview Reminder", "");

    private final String displayName;
    private final String icon;

    RememberItemType(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    public String getBadgeClass() {
        return switch (this) {
            case COMMON_CONFUSION -> "badge-warning";
            case FORMULA -> "badge-info";
            case MEMORY_TRICK -> "badge-secondary";
            default -> "badge-primary";
        };
    }
}
