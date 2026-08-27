package com.example.booking.controller;

import com.example.booking.dto.UserAppointmentsResponse;
import com.example.booking.model.Appointment;
import com.example.booking.model.User;
import com.example.booking.security.JwtService;
import com.example.booking.service.AppointmentService;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.booking.config.SecurityConfig;
import com.example.booking.exception.RestExceptionHandler;
import com.example.booking.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Import;

@WebMvcTest(UserController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, RestExceptionHandler.class })
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AppointmentService appointmentService;

    @MockBean
    private JwtService jwtService;

    @Test
    public void testGetProfileUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetUserAppointmentsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/user/appointments"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testUpdateProfileUnauthenticated() throws Exception {
        mockMvc.perform(put("/api/user/profile")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"new@example.com\", \"phone\": \"0987654321\"}"))
                .andExpect(status().isForbidden());
    }

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

        when(userService.updateProfile("testuser", "new@example.com", "0987654321")).thenReturn(user);

        mockMvc.perform(put("/api/user/profile")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"new@example.com\", \"phone\": \"0987654321\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser")
    public void testUpdateProfileNotFound() throws Exception {
        when(userService.updateProfile("testuser", "new@example.com", "0987654321"))
                .thenThrow(new com.example.booking.exception.ResourceNotFoundException("User not found"));

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
        when(appointmentService.getUpcomingAndHistoryForUser(1L))
                .thenReturn(new UserAppointmentsResponse(List.of(appt), List.of()));

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

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(user));
        when(appointmentService.getUpcomingAndHistoryForUser(1L))
                .thenReturn(new UserAppointmentsResponse(List.of(), List.of()));

        mockMvc.perform(get("/api/user/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcoming").isArray())
                .andExpect(jsonPath("$.upcoming").isEmpty())
                .andExpect(jsonPath("$.history").isArray())
                .andExpect(jsonPath("$.history").isEmpty());
    }
}
