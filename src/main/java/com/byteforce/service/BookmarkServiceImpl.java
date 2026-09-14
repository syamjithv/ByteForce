package com.byteforce.service;

import com.byteforce.domain.ActivityType;
import com.byteforce.domain.Bookmark;
import com.byteforce.exception.ValidationException;
import com.byteforce.repository.BookmarkRepository;
import com.byteforce.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Production implementation of {@link BookmarkService}.
 */
public class BookmarkServiceImpl implements BookmarkService {

    private static final Logger log = LoggerFactory.getLogger(BookmarkServiceImpl.class);

    private final BookmarkRepository bookmarkRepository;
    private final QuestionRepository questionRepository;
    private final ActivityService activityService;

    public BookmarkServiceImpl(BookmarkRepository bookmarkRepository, QuestionRepository questionRepository,
                               ActivityService activityService) {
        this.bookmarkRepository = Objects.requireNonNull(bookmarkRepository, "bookmarkRepository must not be null");
        this.questionRepository = Objects.requireNonNull(questionRepository, "questionRepository must not be null");
        this.activityService = activityService;
    }

    public BookmarkServiceImpl(BookmarkRepository bookmarkRepository, QuestionRepository questionRepository) {
        this(bookmarkRepository, questionRepository, null);
    }

    @Override
    public Bookmark addBookmark(UUID userId, long questionId, String notes) {
        if (userId == null) {
            throw new ValidationException("Cannot bookmark question: User ID must not be null.");
        }
        if (questionId <= 0) {
            throw new ValidationException("Cannot bookmark question: Question ID must be positive.");
        }
        if (!questionRepository.existsById(questionId)) {
            throw new ValidationException("Cannot bookmark question: Question not found with ID: " + questionId);
        }

        Bookmark bookmark = Bookmark.create(userId, questionId, notes != null ? notes.trim() : null);
        Bookmark saved = bookmarkRepository.save(bookmark);
        log.info("Saved bookmark ID {} for user {} on question {}", saved.getId(), userId, questionId);

        if (activityService != null) {
            try {
                activityService.recordActivity(userId, ActivityType.BOOKMARK_ADDED, "Bookmarked question #" + questionId);
            } catch (Exception e) {
                log.warn("Failed to record activity for bookmark on question {}", questionId, e);
            }
        }

        return saved;
    }

    @Override
    public boolean removeBookmark(UUID userId, long questionId) {
        if (userId == null || questionId <= 0) {
            return false;
        }
        boolean deleted = bookmarkRepository.deleteByUserIdAndQuestionId(userId, questionId);
        if (deleted) {
            log.info("Removed bookmark for user {} on question {}", userId, questionId);
            if (activityService != null) {
                try {
                    activityService.recordActivity(userId, ActivityType.BOOKMARK_REMOVED, "Removed bookmark for question #" + questionId);
                } catch (Exception e) {
                    log.warn("Failed to record activity for bookmark removal on question {}", questionId, e);
                }
            }
        }
        return deleted;
    }

    @Override
    public boolean toggleBookmark(UUID userId, long questionId, String notes) {
        if (isBookmarked(userId, questionId)) {
            removeBookmark(userId, questionId);
            return false;
        } else {
            addBookmark(userId, questionId, notes);
            return true;
        }
    }

    @Override
    public boolean isBookmarked(UUID userId, long questionId) {
        if (userId == null || questionId <= 0) {
            return false;
        }
        return bookmarkRepository.isBookmarked(userId, questionId);
    }

    @Override
    public List<Bookmark> getBookmarksForUser(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        return bookmarkRepository.findByUserId(userId);
    }

    @Override
    public Optional<Bookmark> getBookmark(UUID userId, long questionId) {
        if (userId == null || questionId <= 0) {
            return Optional.empty();
        }
        return bookmarkRepository.findByUserIdAndQuestionId(userId, questionId);
    }

    @Override
    public long getBookmarkCount(UUID userId) {
        if (userId == null) {
            return 0;
        }
        return bookmarkRepository.countByUserId(userId);
    }
}
