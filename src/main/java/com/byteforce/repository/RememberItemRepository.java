package com.byteforce.repository;

import com.byteforce.domain.RememberItem;
import com.byteforce.domain.RememberItemType;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link RememberItem} persistence operations.
 */
public interface RememberItemRepository {

    List<RememberItem> findByConceptId(long conceptId);

    Optional<RememberItem> findById(long id);

    List<RememberItem> findAll();

    List<RememberItem> findByType(RememberItemType type);

    RememberItem save(RememberItem item);

    boolean existsById(long id);

    boolean deleteById(long id);

    long count();
}
