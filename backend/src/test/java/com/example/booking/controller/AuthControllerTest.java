package com.example.booking.controller;

import com.example.booking.model.User;
import com.example.booking.security.JwtUtil;
import com.example.booking.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.booking.config.SecurityConfig;
import com.example.booking.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Import;

@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testRegister() throws Exception {
        AuthController.RegisterRequest req = new AuthController.RegisterRequest();
        req.setUsername("newuser");
        req.setPassword("password123");
        req.setEmail("new@example.com");
        req.setPhone("1234567890");

        when(userService.getUserByUsername("newuser")).thenReturn(Optional.empty());
        when(userService.getUserByEmail("new@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    public void testRegisterWithExistingUsername() throws Exception {
        AuthController.RegisterRequest req = new AuthController.RegisterRequest();
        req.setUsername("newuser");
        req.setPassword("password123");
        req.setEmail("new@example.com");
        req.setPhone("1234567890");
        User user = new User();
        user.setUsername("newuser");

        when(userService.getUserByUsername("newuser")).thenReturn(Optional.of(user));
        when(userService.getUserByEmail("new@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is(400));
    }

    @Test
    public void testRegisterWithExistingEmail() throws Exception {
        AuthController.RegisterRequest req = new AuthController.RegisterRequest();
        req.setUsername("newuser");
        req.setPassword("password123");
        req.setEmail("new@example.com");
        req.setPhone("1234567890");
        User user = new User();
        user.setEmail("new@example.com");

        when(userService.getUserByUsername("newuser")).thenReturn(Optional.empty());
        when(userService.getUserByEmail("new@example.com")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is(400));
    }

    @Test
    public void testLogin() throws Exception {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG");
        user.setEmail("test@example.com");
        user.setPhone("1234567890");
        user.setRole("USER");

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("testuser")).thenReturn("fake-jwt-token");

        AuthController.LoginRequest req = new AuthController.LoginRequest();
        req.setUsername("testuser");
        req.setPassword("password");

        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(cookie().value("jwt", "fake-jwt-token"));
    }

    @Test
    public void testLoginInvalidCredentials() throws Exception {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG");
        user.setEmail("test@example.com");
        user.setPhone("1234567890");
        user.setRole("USER");

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(user));

        AuthController.LoginRequest req = new AuthController.LoginRequest();
        req.setUsername("testuser");
        req.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is(401));
    }

    @Test
    public void testLogout() throws Exception {
        mockMvc.perform(post("/api/auth/logout").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("jwt", 0));
    }

    @Test
    @WithMockUser(username = "testuser")
    public void testMe() throws Exception {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPhone("1234567890");
        user.setRole("USER");
        user.setAppointments(new ArrayList<>());

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk());
    }

    @Test
    public void testMeNotFound() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().is(401));
    }
}
