package com.byteforce.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Company domain entity representing an enterprise or target recruitment organization.
 * In ByteForce, a Company acts as a curated preparation lens over existing platform content.
 */
public class Company {

    private final Long id;
    private final String name;
    private final String slug;
    private final String logoPath;
    private final String websiteUrl;
    private final String shortDescription;
    private final String description;
    private final boolean active;
    private final Instant lastReviewedAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Company(Long id,
                   String name,
                   String slug,
                   String logoPath,
                   String websiteUrl,
                   String shortDescription,
                   String description,
                   boolean active,
                   Instant lastReviewedAt,
                   Instant createdAt,
                   Instant updatedAt) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.slug = Objects.requireNonNull(slug, "slug must not be null");
        this.logoPath = logoPath;
        this.websiteUrl = websiteUrl;
        this.shortDescription = shortDescription;
        this.description = description;
        this.active = active;
        this.lastReviewedAt = lastReviewedAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static Company create(String name,
                                 String slug,
                                 String logoPath,
                                 String websiteUrl,
                                 String shortDescription,
                                 String description,
                                 boolean active) {
        return new Company(null, name, slug, logoPath, websiteUrl, shortDescription, description, active, Instant.now(), Instant.now(), Instant.now());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getLogoPath() {
        return logoPath;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getLastReviewedAt() {
        return lastReviewedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Company company = (Company) o;
        return Objects.equals(id, company.id) || Objects.equals(slug, company.slug);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slug);
    }

    @Override
    public String toString() {
        return "Company{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", slug='" + slug + '\'' +
                ", active=" + active +
                '}';
    }
}
