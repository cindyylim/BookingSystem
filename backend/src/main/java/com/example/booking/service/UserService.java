package com.example.booking.service;

import com.example.booking.model.User;
import com.example.booking.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User createUser(User user) {
        // Simple create for now, could add validation/hashing later
        return userRepository.save(user);
    }

    public User updateUser(User user) {
        return userRepository.save(user);
    }
}
