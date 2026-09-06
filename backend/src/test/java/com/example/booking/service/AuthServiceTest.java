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
import org.springframework.dao.DataIntegrityViolationException;
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
        verify(passwordEncoder).encode("secret1");
    }

    @Test
    void registerRejectsDuplicate() {
        RegisterRequest request = validRegisterRequest();

        when(userService.getUserByUsername("new")).thenReturn(Optional.of(new User()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        assertEquals("Username or email already exists", ex.getMessage());
        verify(userService, never()).createUser(any());
    }

    @Test
    void registerMapsDataIntegrityViolationToAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("new");
        request.setPassword("secret1");
        request.setEmail("new@example.com");
        request.setPhone("1234567890");

        when(userService.getUserByUsername("new")).thenReturn(Optional.empty());
        when(userService.getUserByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret1")).thenReturn("hashed");
        doThrow(new DataIntegrityViolationException("duplicate")).when(userService).createUser(any(User.class));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        assertEquals("Username or email already exists", ex.getMessage());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = validRegisterRequest();
        when(userService.getUserByUsername("new")).thenReturn(Optional.empty());
        when(userService.getUserByEmail("new@example.com")).thenReturn(Optional.of(new User()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        assertEquals("Username or email already exists", ex.getMessage());
        verify(userService, never()).createUser(any());
    }

    @Test
    void registerRejectsEmptyUsername() {
        RegisterRequest request = validRegisterRequest();
        request.setUsername("");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        assertEquals("Username is required", ex.getMessage());
        verify(userService, never()).createUser(any());
    }

    @Test
    void registerRejectsEmptyEmail() {
        RegisterRequest request = validRegisterRequest();
        request.setEmail("");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        assertEquals("Email is required", ex.getMessage());
        verify(userService, never()).createUser(any());
    }

    @Test
    void registerRejectsEmptyPassword() {
        RegisterRequest request = validRegisterRequest();
        request.setPassword("");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        assertEquals("Password is required", ex.getMessage());
        verify(userService, never()).createUser(any());
    }

    @Test
    void registerRejectsEmptyPhone() {
        RegisterRequest request = validRegisterRequest();
        request.setPhone("");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        assertEquals("Phone is required", ex.getMessage());
        verify(userService, never()).createUser(any());
    }

    @Test
    void loginUnknownUsername() {
        when(userService.getUserByUsername("missing")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setUsername("missing");
        request.setPassword("pw");

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
        verify(jwtService, never()).generateToken(any());
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

    @Test
    void currentUserDelegatesToUserService() {
        User user = new User();
        user.setUsername("u");
        when(userService.getUserByUsername("u")).thenReturn(Optional.of(user));

        Optional<User> result = authService.currentUser("u");

        assertTrue(result.isPresent());
        assertEquals("u", result.get().getUsername());
    }

    private static RegisterRequest validRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("new");
        request.setPassword("secret1");
        request.setEmail("new@example.com");
        request.setPhone("1234567890");
        return request;
    }
}
