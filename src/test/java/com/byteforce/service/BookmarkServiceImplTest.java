package com.byteforce.service;

import com.byteforce.domain.ActivityType;
import com.byteforce.domain.Bookmark;
import com.byteforce.exception.ValidationException;
import com.byteforce.repository.BookmarkRepository;
import com.byteforce.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceImplTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private ActivityService activityService;

    private BookmarkServiceImpl bookmarkService;

    private final UUID userId = UUID.randomUUID();
    private final long questionId = 100L;
    private Bookmark sampleBookmark;

    @BeforeEach
    void setUp() {
        bookmarkService = new BookmarkServiceImpl(bookmarkRepository, questionRepository, activityService);
        sampleBookmark = Bookmark.create(userId, questionId, "Review before interview");
    }

    @Test
    @DisplayName("addBookmark should succeed when question exists and record activity")
    void addBookmarkShouldSucceed() {
        when(questionRepository.existsById(questionId)).thenReturn(true);
        when(bookmarkRepository.save(any(Bookmark.class))).thenAnswer(inv -> inv.getArgument(0));

        Bookmark result = bookmarkService.addBookmark(userId, questionId, "Review before interview");

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(questionId, result.getQuestionId());
        assertEquals("Review before interview", result.getNotes());
        verify(activityService).recordActivity(eq(userId), eq(ActivityType.BOOKMARK_ADDED), anyString());
    }

    @Test
    @DisplayName("addBookmark should throw ValidationException for invalid inputs or non-existent question")
    void addBookmarkShouldValidate() {
        assertThrows(ValidationException.class, () -> bookmarkService.addBookmark(null, questionId, "note"));
        assertThrows(ValidationException.class, () -> bookmarkService.addBookmark(userId, 0L, "note"));
        assertThrows(ValidationException.class, () -> bookmarkService.addBookmark(userId, -5L, "note"));

        when(questionRepository.existsById(questionId)).thenReturn(false);
        assertThrows(ValidationException.class, () -> bookmarkService.addBookmark(userId, questionId, "note"));
        verify(bookmarkRepository, never()).save(any());
    }

    @Test
    @DisplayName("removeBookmark should delete bookmark and record activity if found")
    void removeBookmarkShouldSucceed() {
        when(bookmarkRepository.deleteByUserIdAndQuestionId(userId, questionId)).thenReturn(true);

        boolean removed = bookmarkService.removeBookmark(userId, questionId);

        assertTrue(removed);
        verify(activityService).recordActivity(eq(userId), eq(ActivityType.BOOKMARK_REMOVED), anyString());
    }

    @Test
    @DisplayName("removeBookmark should return false without activity if bookmark not found or input invalid")
    void removeBookmarkShouldHandleNotFoundAndInvalid() {
        when(bookmarkRepository.deleteByUserIdAndQuestionId(userId, questionId)).thenReturn(false);

        boolean removed = bookmarkService.removeBookmark(userId, questionId);
        assertFalse(removed);
        verify(activityService, never()).recordActivity(any(), any(), any());

        assertFalse(bookmarkService.removeBookmark(null, questionId));
        assertFalse(bookmarkService.removeBookmark(userId, -1L));
    }

    @Test
    @DisplayName("toggleBookmark should remove if already bookmarked")
    void toggleBookmarkShouldRemoveWhenBookmarked() {
        when(bookmarkRepository.isBookmarked(userId, questionId)).thenReturn(true);
        when(bookmarkRepository.deleteByUserIdAndQuestionId(userId, questionId)).thenReturn(true);

        boolean bookmarked = bookmarkService.toggleBookmark(userId, questionId, "note");

        assertFalse(bookmarked);
        verify(bookmarkRepository).deleteByUserIdAndQuestionId(userId, questionId);
        verify(bookmarkRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggleBookmark should add if not already bookmarked")
    void toggleBookmarkShouldAddWhenNotBookmarked() {
        when(bookmarkRepository.isBookmarked(userId, questionId)).thenReturn(false);
        when(questionRepository.existsById(questionId)).thenReturn(true);
        when(bookmarkRepository.save(any(Bookmark.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean bookmarked = bookmarkService.toggleBookmark(userId, questionId, "note");

        assertTrue(bookmarked);
        verify(bookmarkRepository).save(any(Bookmark.class));
    }

    @Test
    @DisplayName("isBookmarked, getBookmarksForUser, getBookmark, and getBookmarkCount should delegate to repository")
    void queryMethodsShouldDelegate() {
        when(bookmarkRepository.isBookmarked(userId, questionId)).thenReturn(true);
        when(bookmarkRepository.findByUserId(userId)).thenReturn(List.of(sampleBookmark));
        when(bookmarkRepository.findByUserIdAndQuestionId(userId, questionId)).thenReturn(Optional.of(sampleBookmark));
        when(bookmarkRepository.countByUserId(userId)).thenReturn(1L);

        assertTrue(bookmarkService.isBookmarked(userId, questionId));
        assertEquals(1, bookmarkService.getBookmarksForUser(userId).size());
        assertTrue(bookmarkService.getBookmark(userId, questionId).isPresent());
        assertEquals(1L, bookmarkService.getBookmarkCount(userId));

        // null handling
        assertFalse(bookmarkService.isBookmarked(null, questionId));
        assertTrue(bookmarkService.getBookmarksForUser(null).isEmpty());
        assertTrue(bookmarkService.getBookmark(null, questionId).isEmpty());
        assertEquals(0L, bookmarkService.getBookmarkCount(null));
    }
}
