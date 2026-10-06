package com.byteforce.repository;

import com.byteforce.domain.Subject;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link Subject} persistence operations.
 */
public interface SubjectRepository {

    List<Subject> findAll();

    Optional<Subject> findById(String id);

    Subject save(Subject subject);

    boolean existsById(String id);

    boolean deleteById(String id);

    long count();
}
