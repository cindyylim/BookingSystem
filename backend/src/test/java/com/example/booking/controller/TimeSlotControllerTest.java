package com.example.booking.controller;

import com.example.booking.dto.TimeSlotRequest;
import com.example.booking.exception.ConflictException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.model.TimeSlot;
import com.example.booking.security.JwtService;
import com.example.booking.service.TimeSlotService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.booking.config.SecurityConfig;
import com.example.booking.exception.RestExceptionHandler;
import com.example.booking.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Import;

@WebMvcTest(TimeSlotController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, RestExceptionHandler.class })
public class TimeSlotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TimeSlotService timeSlotService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetAllTimeSlots() throws Exception {
        TimeSlot slot = new TimeSlot();
        slot.setId(1L);
        slot.setStartTime(OffsetDateTime.parse("2024-01-01T10:00:00Z"));
        slot.setEndTime(OffsetDateTime.parse("2024-01-01T11:00:00Z"));
        slot.setAvailable(true);
        slot.setAppointments(java.util.Collections.emptyList());

        when(timeSlotService.getAvailableTimeSlots()).thenReturn(List.of(slot));

        mockMvc.perform(get("/api/timeslots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    public void testGetTimeSlot() throws Exception {
        TimeSlot slot = new TimeSlot();
        slot.setId(1L);
        slot.setStartTime(OffsetDateTime.parse("2024-01-01T10:00:00Z"));
        slot.setEndTime(OffsetDateTime.parse("2024-01-01T11:00:00Z"));
        slot.setAppointments(java.util.Collections.emptyList());

        when(timeSlotService.getTimeSlot(1L)).thenReturn(Optional.of(slot));

        mockMvc.perform(get("/api/timeslots/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    public void testGetTimeSlotNotFound() throws Exception {
        when(timeSlotService.getTimeSlot(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/timeslots/1"))
                .andExpect(status().is(404));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testCreateTimeSlot() throws Exception {
        TimeSlotRequest req = new TimeSlotRequest();
        req.setStartTime("2024-01-01T10:00:00Z");
        req.setEndTime("2024-01-01T11:00:00Z");
        req.setAvailable(true);

        TimeSlot created = new TimeSlot();
        created.setId(1L);

        when(timeSlotService.createTimeSlot(any(TimeSlot.class))).thenReturn(created);

        mockMvc.perform(post("/api/timeslots")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testCreateTimeSlotDateTimeParseException() throws Exception {
        String body = "{\"startTime\":\"not-a-date\",\"endTime\":\"2024-01-01T11:00:00Z\",\"available\":true}";

        mockMvc.perform(post("/api/timeslots")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid date/time format"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testCreateTimeSlotUnexpectedException() throws Exception {
        TimeSlotRequest req = new TimeSlotRequest();
        req.setStartTime("2024-01-01T10:00:00Z");
        req.setEndTime("2024-01-01T11:00:00Z");
        req.setAvailable(true);

        when(timeSlotService.createTimeSlot(any(TimeSlot.class)))
                .thenThrow(new RuntimeException("unexpected failure"));

        mockMvc.perform(post("/api/timeslots")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal server error"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testCreateTimeSlotIsOverlapping() throws Exception {
        TimeSlotRequest req = new TimeSlotRequest();
        req.setStartTime("2024-01-01T10:00:00Z");
        req.setEndTime("2024-01-01T11:00:00Z");
        req.setAvailable(true);

        when(timeSlotService.createTimeSlot(any(TimeSlot.class)))
                .thenThrow(new IllegalArgumentException("Time slot overlaps with an existing slot."));

        mockMvc.perform(post("/api/timeslots")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is(400));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testUpdateTimeSlot() throws Exception {
        TimeSlot updated = new TimeSlot();
        updated.setId(1L);
        updated.setAvailable(false);
        updated.setStartTime(OffsetDateTime.parse("2024-01-01T10:00:00Z"));
        updated.setEndTime(OffsetDateTime.parse("2024-01-01T11:00:00Z"));
        updated.setAppointments(java.util.Collections.emptyList());

        when(timeSlotService.updateTimeSlot(eq(1L), any(TimeSlot.class))).thenReturn(updated);

        mockMvc.perform(put("/api/timeslots/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testUpdateTimeSlotIsOverlapping() throws Exception {
        TimeSlot updated = new TimeSlot();
        updated.setId(1L);
        updated.setAvailable(false);
        updated.setStartTime(OffsetDateTime.parse("2024-01-01T10:00:00Z"));
        updated.setEndTime(OffsetDateTime.parse("2024-01-01T11:00:00Z"));
        updated.setAppointments(java.util.Collections.emptyList());

        when(timeSlotService.updateTimeSlot(eq(1L), any(TimeSlot.class)))
                .thenThrow(new IllegalArgumentException("Updated time slot overlaps with an existing slot."));

        mockMvc.perform(put("/api/timeslots/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().is(400));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testUpdateTimeSlotThrowsAnException() throws Exception {
        TimeSlot updated = new TimeSlot();
        updated.setId(1L);
        updated.setAvailable(false);
        updated.setStartTime(OffsetDateTime.parse("2024-01-01T10:00:00Z"));
        updated.setEndTime(OffsetDateTime.parse("2024-01-01T11:00:00Z"));
        updated.setAppointments(java.util.Collections.emptyList());

        when(timeSlotService.updateTimeSlot(eq(1L), any(TimeSlot.class)))
                .thenThrow(new ResourceNotFoundException("TimeSlot not found"));

        mockMvc.perform(put("/api/timeslots/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().is(404));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testDeleteTimeSlot() throws Exception {
        doNothing().when(timeSlotService).deleteTimeSlot(1L);

        mockMvc.perform(delete("/api/timeslots/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testDeleteTimeSlotIsBooked() throws Exception {
        doThrow(new ConflictException("Cannot delete a time slot that is already booked."))
                .when(timeSlotService).deleteTimeSlot(1L);

        mockMvc.perform(delete("/api/timeslots/1").with(csrf()))
                .andExpect(status().is(409));
    }

    @Test
    public void testTimeslotMutationsRequireAdmin() throws Exception {
        TimeSlotRequest req = new TimeSlotRequest();
        req.setStartTime("2024-01-01T10:00:00Z");
        req.setEndTime("2024-01-01T11:00:00Z");
        req.setAvailable(true);

        mockMvc.perform(post("/api/timeslots")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        TimeSlot updated = new TimeSlot();
        updated.setId(1L);
        updated.setStartTime(OffsetDateTime.parse("2024-01-01T10:00:00Z"));
        updated.setEndTime(OffsetDateTime.parse("2024-01-01T11:00:00Z"));
        mockMvc.perform(put("/api/timeslots/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/timeslots/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testAdminGetAllTimeSlotsIncludesService() throws Exception {
        TimeSlot slot = new TimeSlot();
        slot.setId(1L);
        slot.setStartTime(OffsetDateTime.parse("2024-01-01T10:00:00Z"));
        slot.setEndTime(OffsetDateTime.parse("2024-01-01T11:00:00Z"));
        slot.setAvailable(true);
        slot.setAppointments(java.util.Collections.emptyList());
        when(timeSlotService.getAllTimeSlots()).thenReturn(List.of(slot));

        mockMvc.perform(get("/api/timeslots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }
}
