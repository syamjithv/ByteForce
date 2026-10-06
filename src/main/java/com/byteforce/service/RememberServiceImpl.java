package com.byteforce.service;

import com.byteforce.domain.RememberItem;
import com.byteforce.domain.RememberItemType;
import com.byteforce.exception.ResourceNotFoundException;
import com.byteforce.exception.ValidationException;
import com.byteforce.repository.ConceptRepository;
import com.byteforce.repository.RememberItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Production implementation of {@link RememberService}.
 */
public class RememberServiceImpl implements RememberService {

    private static final Logger log = LoggerFactory.getLogger(RememberServiceImpl.class);

    private final RememberItemRepository rememberItemRepository;
    private final ConceptRepository conceptRepository;

    public RememberServiceImpl(RememberItemRepository rememberItemRepository,
                               ConceptRepository conceptRepository) {
        this.rememberItemRepository = Objects.requireNonNull(rememberItemRepository, "rememberItemRepository must not be null");
        this.conceptRepository = Objects.requireNonNull(conceptRepository, "conceptRepository must not be null");
    }

    @Override
    public List<RememberItem> getItemsForConcept(long conceptId) {
        if (conceptId <= 0) {
            return List.of();
        }
        return rememberItemRepository.findByConceptId(conceptId);
    }

    @Override
    public List<RememberItem> getAllItems() {
        return rememberItemRepository.findAll();
    }

    @Override
    public List<RememberItem> getItemsByType(RememberItemType type) {
        if (type == null) {
            return getAllItems();
        }
        return rememberItemRepository.findByType(type);
    }

    @Override
    public Optional<RememberItem> getItemById(long id) {
        if (id <= 0) {
            return Optional.empty();
        }
        return rememberItemRepository.findById(id);
    }

    @Override
    public RememberItem createItem(long conceptId, RememberItemType type, String content, int displayOrder) {
        if (!conceptRepository.existsById(conceptId)) {
            throw new ResourceNotFoundException("Cannot create Remember item for non-existent concept ID: " + conceptId);
        }
        if (content == null || content.isBlank()) {
            throw new ValidationException("Remember item content must not be blank");
        }
        if (type == null) {
            throw new ValidationException("Remember item type must not be null");
        }

        RememberItem item = RememberItem.create(conceptId, type, content.trim(), displayOrder);
        RememberItem saved = rememberItemRepository.save(item);
        log.info("Created new Remember item ID {} for concept ID {}", saved.getId(), conceptId);
        return saved;
    }

    @Override
    public RememberItem updateItem(long id, RememberItemType type, String content, int displayOrder, boolean active) {
        RememberItem existing = rememberItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Remember item not found with ID: " + id));

        if (content == null || content.isBlank()) {
            throw new ValidationException("Remember item content must not be blank");
        }
        if (type == null) {
            throw new ValidationException("Remember item type must not be null");
        }

        RememberItem updated = existing
                .withType(type)
                .withContent(content.trim())
                .withDisplayOrder(displayOrder)
                .withActive(active);

        RememberItem saved = rememberItemRepository.save(updated);
        log.info("Updated Remember item ID {}", saved.getId());
        return saved;
    }

    @Override
    public void deleteItem(long id) {
        if (!rememberItemRepository.existsById(id)) {
            throw new ResourceNotFoundException("Remember item not found with ID: " + id);
        }
        rememberItemRepository.deleteById(id);
        log.info("Deleted Remember item ID {}", id);
    }

    @Override
    public long getTotalCount() {
        return rememberItemRepository.count();
    }
}
