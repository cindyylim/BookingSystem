package com.example.booking.controller;

import com.example.booking.model.Appointment;
import com.example.booking.model.User;
import com.example.booking.security.JwtUtil;
import com.example.booking.service.AppointmentService;
import com.example.booking.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.booking.config.SecurityConfig;
import com.example.booking.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Import;

@WebMvcTest(AppointmentController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
public class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppointmentService appointmentService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetAllAppointments() throws Exception {
        Appointment appt = new Appointment();
        appt.setId(1L);
        appt.setCustomerName("Test Customer");

        when(appointmentService.getAllAppointments()).thenReturn(List.of(appt));

        mockMvc.perform(get("/api/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerName").value("Test Customer"));
    }

    @Test
    public void testGetAppointment() throws Exception {
        Appointment appt = new Appointment();
        appt.setId(1L);

        when(appointmentService.getAppointment(1L)).thenReturn(Optional.of(appt));

        mockMvc.perform(get("/api/appointments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    public void testBookAppointment() throws Exception {
        AppointmentController.AppointmentRequest req = new AppointmentController.AppointmentRequest();
        req.setCustomerName("John Doe");
        req.setTimeSlotId(1L);
        req.setService("Consultation");

        Appointment booked = new Appointment();
        booked.setId(1L);
        booked.setCustomerName("John Doe");

        when(appointmentService.bookAppointment(any(Appointment.class), eq(1L), any())).thenReturn(booked);

        mockMvc.perform(post("/api/appointments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("John Doe"));
    }

    @Test
    public void testBookAppointmentIllegalArgumentException() throws Exception {
        AppointmentController.AppointmentRequest req = new AppointmentController.AppointmentRequest();
        req.setCustomerName("John Doe");
        req.setTimeSlotId(1L);
        req.setService("Consultation");

        Appointment booked = new Appointment();
        booked.setId(1L);
        booked.setCustomerName("John Doe");

        when(appointmentService.bookAppointment(any(Appointment.class), eq(1L), any()))
                .thenThrow(new IllegalArgumentException("Time slot not found"));

        mockMvc.perform(post("/api/appointments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is(400));
    }

    @Test
    public void testBookAppointmentIllegalStateException() throws Exception {
        AppointmentController.AppointmentRequest req = new AppointmentController.AppointmentRequest();
        req.setCustomerName("John Doe");
        req.setTimeSlotId(1L);
        req.setService("Consultation");

        Appointment booked = new Appointment();
        booked.setId(1L);
        booked.setCustomerName("John Doe");

        when(appointmentService.bookAppointment(any(Appointment.class), eq(1L), any()))
                .thenThrow(new IllegalStateException("Time slot is not available"));

        mockMvc.perform(post("/api/appointments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is(409));
    }

    @Test
    public void testBookAppointmentRuntimeException() throws Exception {
        AppointmentController.AppointmentRequest req = new AppointmentController.AppointmentRequest();
        req.setCustomerName("John Doe");
        req.setTimeSlotId(1L);
        req.setService("Consultation");

        Appointment booked = new Appointment();
        booked.setId(1L);
        booked.setCustomerName("John Doe");

        when(appointmentService.bookAppointment(any(Appointment.class), eq(1L), any()))
                .thenThrow(new RuntimeException("Error booking appointment"));

        mockMvc.perform(post("/api/appointments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is(500));
    }

    @Test
    @WithMockUser(username = "testuser")
    public void testBookAppointmentAuthenticated() throws Exception {
        AppointmentController.AppointmentRequest req = new AppointmentController.AppointmentRequest();
        req.setCustomerName("John Doe");
        req.setTimeSlotId(1L);

        User user = new User();
        user.setUsername("testuser");

        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(user));
        when(appointmentService.bookAppointment(any(Appointment.class), eq(1L), eq(user)))
                .thenReturn(new Appointment());

        mockMvc.perform(post("/api/appointments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    public void testDeleteAppointment() throws Exception {
        doNothing().when(appointmentService).cancelAppointment(1L);

        mockMvc.perform(delete("/api/appointments/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testCancelByToken() throws Exception {
        when(appointmentService.cancelAppointmentByToken("token123")).thenReturn(true);

        mockMvc.perform(get("/api/appointments/cancel/token123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Your appointment has been cancelled."));
    }

    @Test
    public void testCancelByTokenReturnFalse() throws Exception {
        when(appointmentService.cancelAppointmentByToken("token123")).thenReturn(false);

        mockMvc.perform(get("/api/appointments/cancel/token123"))
                .andExpect(status().is(404));
    }
}
