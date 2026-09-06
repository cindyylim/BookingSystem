package com.example.booking.service;

import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.model.User;
import com.example.booking.repository.UserRepository;
import com.example.booking.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void testGetUserByUsername() {
        String username = "testuser";
        User user = new User();
        user.setUsername(username);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        Optional<User> result = userService.getUserByUsername(username);

        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
        verify(userRepository).findByUsername(username);
    }

    @Test
    void testGetUserByEmail() {
        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        Optional<User> result = userService.getUserByEmail(email);

        assertTrue(result.isPresent());
        assertEquals(email, result.get().getEmail());
        verify(userRepository).findByEmail(email);
    }

    @Test
    void testCreateUser() {
        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);

        userService.createUser(user);
        verify(userRepository).save(user);
    }

    @Test
    void testUpdateUser() {
        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);

        userService.updateUser(user);
        verify(userRepository).save(user);
    }

    @Test
    void testUpdateProfile() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("old@example.com");
        user.setPhone("111");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User updated = userService.updateProfile("testuser", "new@example.com", "222");

        assertEquals("testuser", updated.getUsername());
        assertEquals("new@example.com", updated.getEmail());
        assertEquals("222", updated.getPhone());
        verify(userRepository).findByUsername("testuser");
        verify(userRepository).save(user);
    }

    @Test
    void testUpdateProfileSkipsNullEmailAndPhone() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("keep@example.com");
        user.setPhone("111");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User updated = userService.updateProfile("testuser", null, null);

        assertEquals("keep@example.com", updated.getEmail());
        assertEquals("111", updated.getPhone());
    }

    @Test
    void testUpdateProfileNotFound() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateProfile("missing", "a@b.com", "123"));
    }
}
