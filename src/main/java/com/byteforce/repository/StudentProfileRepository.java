package com.byteforce.repository;

import com.byteforce.domain.StudentProfile;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link StudentProfile} persistence operations.
 */
public interface StudentProfileRepository {

    Optional<StudentProfile> findById(UUID id);

    Optional<StudentProfile> findByUserId(UUID userId);

    StudentProfile save(StudentProfile profile);

    boolean deleteByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
