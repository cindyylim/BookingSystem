package com.example.booking.service;

import com.example.booking.model.User;

import java.util.Optional;

public interface UserService {
    Optional<User> getUserByUsername(String username);

    Optional<User> getUserByEmail(String email);

    User createUser(User user);

    User updateUser(User user);

    User updateProfile(String username, String email, String phone);
}
