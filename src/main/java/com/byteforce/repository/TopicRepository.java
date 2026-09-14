package com.byteforce.repository;

import com.byteforce.domain.Topic;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Topic domain entities.
 */
public interface TopicRepository {

    Optional<Topic> findById(long id);

    Optional<Topic> findBySlug(String slug);

    Optional<Topic> findByName(String name);

    List<Topic> findAll();

    Topic save(Topic topic);

    boolean deleteById(long id);

    boolean existsById(long id);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    long count();
}
