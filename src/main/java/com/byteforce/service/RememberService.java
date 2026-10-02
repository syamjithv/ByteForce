package com.byteforce.service;

import com.byteforce.domain.RememberItem;
import com.byteforce.domain.RememberItemType;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for ByteForce Remember quick-revision layer.
 */
public interface RememberService {

    List<RememberItem> getItemsForConcept(long conceptId);

    List<RememberItem> getAllItems();

    List<RememberItem> getItemsByType(RememberItemType type);

    Optional<RememberItem> getItemById(long id);

    RememberItem createItem(long conceptId, RememberItemType type, String content, int displayOrder);

    RememberItem updateItem(long id, RememberItemType type, String content, int displayOrder, boolean active);

    void deleteItem(long id);

    long getTotalCount();
}
