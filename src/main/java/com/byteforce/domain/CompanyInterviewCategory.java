package com.byteforce.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Curated interview preparation category / round linked to a target company.
 */
public class CompanyInterviewCategory {

    private final Long id;
    private final Long companyId;
    private final String title;
    private final String categoryType;
    private final String description;
    private final int displayOrder;
    private final Instant createdAt;

    public CompanyInterviewCategory(Long id,
                                    Long companyId,
                                    String title,
                                    String categoryType,
                                    String description,
                                    int displayOrder,
                                    Instant createdAt) {
        this.id = id;
        this.companyId = Objects.requireNonNull(companyId, "companyId must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.categoryType = categoryType != null ? categoryType : "TECHNICAL";
        this.description = description;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static CompanyInterviewCategory create(Long companyId, String title, String categoryType, String description, int displayOrder) {
        return new CompanyInterviewCategory(null, companyId, title, categoryType, description, displayOrder, Instant.now());
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getTitle() {
        return title;
    }

    public String getCategoryType() {
        return categoryType;
    }

    public String getDescription() {
        return description;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
