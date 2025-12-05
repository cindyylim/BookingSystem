package com.example.booking.controller;

import com.example.booking.model.Appointment;
import com.example.booking.model.User;
import com.example.booking.repository.AppointmentRepository;
import com.example.booking.security.JwtUtil;
import com.example.booking.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.booking.config.SecurityConfig;
import com.example.booking.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Import;

@WebMvcTest(UserController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AppointmentRepository appointmentRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser(username = "testuser")
    public void testGetProfile() throws Exception {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPhone("1234567890");

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.phone").value("1234567890"));
    }

    @Test
    @WithMockUser(username = "testuser")
    public void testGetProfileNotFound() throws Exception {
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    public void testUpdateProfile() throws Exception {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("old@example.com");

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(user));

        mockMvc.perform(put("/api/user/profile")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"new@example.com\", \"phone\": \"0987654321\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser")
    public void testUpdateProfileNotFound() throws Exception {
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/user/profile")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"new@example.com\", \"phone\": \"0987654321\"}"))
                .andExpect(status().is(404));
    }

    @Test
    @WithMockUser(username = "testuser")
    public void testGetUserAppointments() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        Appointment appt = new Appointment();
        appt.setStartTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(user));
        when(appointmentRepository.findByUserId(1L)).thenReturn(List.of(appt));

        mockMvc.perform(get("/api/user/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcoming").isArray())
                .andExpect(jsonPath("$.upcoming[0]").exists())
                .andExpect(jsonPath("$.history").isArray())
                .andExpect(jsonPath("$.history").isEmpty());
    }

    @Test
    @WithMockUser(username = "testuser")
    public void testGetUserAppointmentsNotFound() throws Exception {
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/user/appointments"))
                .andExpect(status().is(404));
    }

    @Test
    @WithMockUser(username = "testuser")
    public void testGetUserAppointmentsEmpty() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        Appointment appt = new Appointment();
        appt.setStartTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(user));
        when(appointmentRepository.findByUserId(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/user/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcoming").isArray())
                .andExpect(jsonPath("$.upcoming").isEmpty())
                .andExpect(jsonPath("$.history").isArray())
                .andExpect(jsonPath("$.history").isEmpty());
    }
}
