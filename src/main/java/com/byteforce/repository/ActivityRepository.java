package com.byteforce.repository;

import com.byteforce.domain.Activity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link Activity} entity operations.
 */
public interface ActivityRepository {

    Activity save(Activity activity);

    Optional<Activity> findById(long id);

    List<Activity> findByUserId(UUID userId);

    List<Activity> findRecentByUserId(UUID userId, int limit);

    long countByUserId(UUID userId);

    boolean deleteById(long id);
}
