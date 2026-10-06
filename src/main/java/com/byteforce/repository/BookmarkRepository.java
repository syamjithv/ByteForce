package com.byteforce.repository;

import com.byteforce.domain.Bookmark;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Bookmark domain entities.
 */
public interface BookmarkRepository {

    Optional<Bookmark> findById(long id);

    Optional<Bookmark> findByUserIdAndQuestionId(UUID userId, long questionId);

    List<Bookmark> findByUserId(UUID userId);

    List<Bookmark> findByQuestionId(long questionId);

    boolean isBookmarked(UUID userId, long questionId);

    Bookmark save(Bookmark bookmark);

    boolean deleteById(long id);

    boolean deleteByUserIdAndQuestionId(UUID userId, long questionId);

    long countByUserId(UUID userId);
}
