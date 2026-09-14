package com.byteforce.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable domain entity representing an activity log entry for a user.
 */
public final class Activity {

    private final long id;
    private final UUID userId;
    private final ActivityType activityType;
    private final String description;
    private final Instant createdAt;

    public Activity(long id, UUID userId, ActivityType activityType, String description, Instant createdAt) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.activityType = Objects.requireNonNull(activityType, "activityType must not be null");
        this.description = description;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static Activity create(UUID userId, ActivityType activityType, String description) {
        return new Activity(0, userId, activityType, description, Instant.now());
    }

    public long getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Activity withId(long newId) {
        return new Activity(newId, this.userId, this.activityType, this.description, this.createdAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Activity activity = (Activity) o;
        if (id != 0 && activity.id != 0) {
            return id == activity.id;
        }
        return Objects.equals(userId, activity.userId)
                && activityType == activity.activityType
                && Objects.equals(createdAt, activity.createdAt);
    }

    @Override
    public int hashCode() {
        return id != 0 ? Long.hashCode(id) : Objects.hash(userId, activityType, createdAt);
    }

    @Override
    public String toString() {
        return "Activity{" +
                "id=" + id +
                ", userId=" + userId +
                ", activityType=" + activityType +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
