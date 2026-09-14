package com.byteforce.service;

import com.byteforce.domain.Activity;
import com.byteforce.domain.ActivityType;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for recording and querying user timeline activities.
 */
public interface ActivityService {

    Activity recordActivity(UUID userId, ActivityType activityType, String description);

    List<Activity> getRecentActivities(UUID userId, int limit);

    List<Activity> getUserActivities(UUID userId);

    long getActivityCount(UUID userId);
}
