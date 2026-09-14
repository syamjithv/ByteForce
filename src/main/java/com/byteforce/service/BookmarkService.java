package com.byteforce.service;

import com.byteforce.domain.Bookmark;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for Bookmark business operations.
 */
public interface BookmarkService {

    Bookmark addBookmark(UUID userId, long questionId, String notes);

    boolean removeBookmark(UUID userId, long questionId);

    boolean toggleBookmark(UUID userId, long questionId, String notes);

    boolean isBookmarked(UUID userId, long questionId);

    List<Bookmark> getBookmarksForUser(UUID userId);

    Optional<Bookmark> getBookmark(UUID userId, long questionId);

    long getBookmarkCount(UUID userId);
}
