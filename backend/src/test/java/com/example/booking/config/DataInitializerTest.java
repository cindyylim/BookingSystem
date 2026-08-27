package com.example.booking.config;

import com.example.booking.model.User;
import com.example.booking.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserService userService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void runSkipsWhenAdminAlreadyExists() {
        when(userService.getUserByUsername("admin")).thenReturn(Optional.of(new User()));

        dataInitializer.run();

        verify(userService, never()).createUser(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void runCreatesAdminWhenMissing() {
        when(userService.getUserByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin123")).thenReturn("hashed");

        dataInitializer.run();

        verify(userService).createUser(any(User.class));
    }
}
