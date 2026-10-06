package com.byteforce.service;

import com.byteforce.domain.Activity;
import com.byteforce.domain.ActivityType;
import com.byteforce.exception.ValidationException;
import com.byteforce.repository.ActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Production implementation of {@link ActivityService}.
 */
public class ActivityServiceImpl implements ActivityService {

    private static final Logger log = LoggerFactory.getLogger(ActivityServiceImpl.class);

    private final ActivityRepository activityRepository;

    public ActivityServiceImpl(ActivityRepository activityRepository) {
        this.activityRepository = Objects.requireNonNull(activityRepository, "activityRepository must not be null");
    }

    @Override
    public Activity recordActivity(UUID userId, ActivityType activityType, String description) {
        if (userId == null) {
            throw new ValidationException("User ID must not be null to record an activity.");
        }
        if (activityType == null) {
            throw new ValidationException("Activity type must not be null.");
        }

        Activity activity = Activity.create(userId, activityType, description != null ? description.trim() : null);
        Activity saved = activityRepository.save(activity);
        log.info("Recorded activity ID {} of type {} for user {}", saved.getId(), activityType, userId);
        return saved;
    }

    @Override
    public List<Activity> getRecentActivities(UUID userId, int limit) {
        if (userId == null || limit <= 0) {
            return List.of();
        }
        return activityRepository.findRecentByUserId(userId, limit);
    }

    @Override
    public List<Activity> getUserActivities(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        return activityRepository.findByUserId(userId);
    }

    @Override
    public long getActivityCount(UUID userId) {
        if (userId == null) {
            return 0;
        }
        return activityRepository.countByUserId(userId);
    }
}
