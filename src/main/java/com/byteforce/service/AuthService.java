package com.byteforce.service;

import com.byteforce.domain.User;

public interface AuthService {
    User login(String email, String password);

    void logout();
}
