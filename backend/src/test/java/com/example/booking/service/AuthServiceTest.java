package com.example.booking.service;

import com.example.booking.dto.AuthResult;
import com.example.booking.dto.LoginRequest;
import com.example.booking.dto.RegisterRequest;
import com.example.booking.exception.UnauthorizedException;
import com.example.booking.model.User;
import com.example.booking.security.JwtService;
import com.example.booking.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registerCreatesUser() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("new");
        request.setPassword("secret1");
        request.setEmail("new@example.com");
        request.setPhone("1234567890");

        when(userService.getUserByUsername("new")).thenReturn(Optional.empty());
        when(userService.getUserByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret1")).thenReturn("hashed");

        authService.register(request);

        verify(userService).createUser(any(User.class));
    }

    @Test
    void registerRejectsDuplicate() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("new");
        request.setEmail("new@example.com");

        when(userService.getUserByUsername("new")).thenReturn(Optional.of(new User()));

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verify(userService, never()).createUser(any());
    }

    @Test
    void loginSucceeds() {
        User user = new User();
        user.setUsername("u");
        user.setPassword("hashed");
        when(userService.getUserByUsername("u")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pw", "hashed")).thenReturn(true);
        when(jwtService.generateToken("u")).thenReturn("token");

        LoginRequest request = new LoginRequest();
        request.setUsername("u");
        request.setPassword("pw");

        AuthResult result = authService.login(request);
        assertEquals("token", result.getToken());
        assertEquals(user, result.getUser());
    }

    @Test
    void loginRejectsBadPassword() {
        User user = new User();
        user.setUsername("u");
        user.setPassword("hashed");
        when(userService.getUserByUsername("u")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pw", "hashed")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setUsername("u");
        request.setPassword("pw");

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }
}
