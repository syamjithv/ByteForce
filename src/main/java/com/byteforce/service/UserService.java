package com.byteforce.service;

import com.byteforce.domain.StudentProfile;
import com.byteforce.domain.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for User profile management and account operations.
 */
public interface UserService {

    Optional<User> getUserById(UUID id);

    Optional<User> getUserByEmail(String email);

    User updateProfile(UUID userId, String fullName);

    Optional<StudentProfile> getStudentProfile(UUID userId);

    StudentProfile updateStudentProfile(UUID userId, String fullName, String phone, String college, Integer graduationYear);

    void changePassword(UUID userId, String oldPassword, String newPassword);

    boolean existsByEmail(String email);
}
