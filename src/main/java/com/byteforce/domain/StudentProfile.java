package com.byteforce.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable domain entity representing a student's profile details.
 */
public final class StudentProfile {

    private final UUID id;
    private final UUID userId;
    private final String fullName;
    private final String phone;
    private final String college;
    private final Integer graduationYear;
    private final Instant createdAt;
    private final Instant updatedAt;

    public StudentProfile(UUID id, UUID userId, String fullName, String phone,
                          String college, Integer graduationYear, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");

        Objects.requireNonNull(fullName, "fullName must not be null");
        if (fullName.isBlank()) {
            throw new IllegalArgumentException("fullName must not be blank");
        }
        this.fullName = fullName.trim();

        this.phone = phone != null ? phone.trim() : null;
        this.college = college != null ? college.trim() : null;
        this.graduationYear = graduationYear;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static StudentProfile create(UUID userId, String fullName, String phone, String college, Integer graduationYear) {
        Instant now = Instant.now();
        return new StudentProfile(UUID.randomUUID(), userId, fullName, phone, college, graduationYear, now, now);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getCollege() {
        return college;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public StudentProfile withFullName(String newFullName) {
        return new StudentProfile(this.id, this.userId, newFullName, this.phone, this.college, this.graduationYear, this.createdAt, Instant.now());
    }

    public StudentProfile withPhone(String newPhone) {
        return new StudentProfile(this.id, this.userId, this.fullName, newPhone, this.college, this.graduationYear, this.createdAt, Instant.now());
    }

    public StudentProfile withCollege(String newCollege) {
        return new StudentProfile(this.id, this.userId, this.fullName, this.phone, newCollege, this.graduationYear, this.createdAt, Instant.now());
    }

    public StudentProfile withGraduationYear(Integer newGraduationYear) {
        return new StudentProfile(this.id, this.userId, this.fullName, this.phone, this.college, newGraduationYear, this.createdAt, Instant.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StudentProfile that = (StudentProfile) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "StudentProfile{" +
                "id=" + id +
                ", userId=" + userId +
                ", fullName='" + fullName + '\'' +
                ", college='" + college + '\'' +
                ", graduationYear=" + graduationYear +
                '}';
    }
}
