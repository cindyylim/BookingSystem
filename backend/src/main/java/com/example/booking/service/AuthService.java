package com.example.booking.service;

import com.example.booking.dto.AuthResult;
import com.example.booking.dto.LoginRequest;
import com.example.booking.dto.RegisterRequest;
import com.example.booking.model.User;

import java.util.Optional;

public interface AuthService {
    void register(RegisterRequest request);

    AuthResult login(LoginRequest request);

    Optional<User> currentUser(String username);
}
