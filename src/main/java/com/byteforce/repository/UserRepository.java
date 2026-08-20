package com.byteforce.repository;

import com.byteforce.domain.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    User save(User user);
}
