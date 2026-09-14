package com.byteforce.service;

import com.byteforce.domain.Activity;
import com.byteforce.domain.ActivityType;
import com.byteforce.exception.ValidationException;
import com.byteforce.repository.ActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityServiceImplTest {

    @Mock
    private ActivityRepository activityRepository;

    private ActivityServiceImpl activityService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        activityService = new ActivityServiceImpl(activityRepository);
    }

    @Test
    @DisplayName("Should record activity successfully with trimmed description")
    void shouldRecordActivitySuccessfully() {
        Activity savedMock = new Activity(1L, userId, ActivityType.SOLVED_QUESTION, "Solved Two Sum", Instant.now());
        when(activityRepository.save(any(Activity.class))).thenReturn(savedMock);

        Activity result = activityService.recordActivity(userId, ActivityType.SOLVED_QUESTION, "  Solved Two Sum  ");
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(ActivityType.SOLVED_QUESTION, result.getActivityType());

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(captor.capture());
        assertEquals("Solved Two Sum", captor.getValue().getDescription());
        assertEquals(userId, captor.getValue().getUserId());
        assertEquals(ActivityType.SOLVED_QUESTION, captor.getValue().getActivityType());
    }

    @Test
    @DisplayName("Should throw ValidationException when userId or activityType is null")
    void shouldValidateInputsOnRecordActivity() {
        assertThrows(ValidationException.class, () ->
                activityService.recordActivity(null, ActivityType.LOGIN, "desc"));

        assertThrows(ValidationException.class, () ->
                activityService.recordActivity(userId, null, "desc"));
    }

    @Test
    @DisplayName("Should retrieve recent activities with valid parameters")
    void shouldRetrieveRecentActivities() {
        List<Activity> mockList = List.of(
                new Activity(1L, userId, ActivityType.LOGIN, "Login", Instant.now())
        );
        when(activityRepository.findRecentByUserId(userId, 5)).thenReturn(mockList);

        List<Activity> result = activityService.getRecentActivities(userId, 5);
        assertEquals(1, result.size());

        assertTrue(activityService.getRecentActivities(null, 5).isEmpty());
        assertTrue(activityService.getRecentActivities(userId, 0).isEmpty());
        assertTrue(activityService.getRecentActivities(userId, -1).isEmpty());
    }

    @Test
    @DisplayName("Should return user activities and count")
    void shouldReturnUserActivitiesAndCount() {
        when(activityRepository.findByUserId(userId)).thenReturn(List.of());
        when(activityRepository.countByUserId(userId)).thenReturn(3L);

        assertEquals(0, activityService.getUserActivities(userId).size());
        assertEquals(3L, activityService.getActivityCount(userId));

        assertTrue(activityService.getUserActivities(null).isEmpty());
        assertEquals(0L, activityService.getActivityCount(null));
    }
}
