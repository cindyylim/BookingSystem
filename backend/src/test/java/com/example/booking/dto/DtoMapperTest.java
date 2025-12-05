package com.example.booking.dto;

import com.example.booking.controller.AppointmentController;
import com.example.booking.controller.AuthController;
import com.example.booking.controller.TimeSlotController;
import com.example.booking.mapper.TimeSlotMapper;
import com.example.booking.model.Appointment;
import com.example.booking.model.TimeSlot;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DtoMapperTest {

    @Test
    public void testTimeSlotDTO() {
        TimeSlotDTO dto = new TimeSlotDTO();
        dto.setId(1L);
        dto.setStartTime(OffsetDateTime.parse("2024-01-01T10:00:00Z"));
        dto.setEndTime(OffsetDateTime.parse("2024-01-01T11:00:00Z"));
        dto.setAvailable(true);
        dto.setAppointments(List.of(new AppointmentDTO()));

        assertEquals(1L, dto.getId());
        assertNotNull(dto.getStartTime());
        assertNotNull(dto.getEndTime());
        assertTrue(dto.isAvailable());
        assertFalse(dto.getAppointments().isEmpty());
    }

    @Test
    public void testAppointmentDTO() {
        AppointmentDTO dto = new AppointmentDTO();
        dto.setId(1L);
        dto.setClientName("John");
        dto.setClientEmail("john@example.com");
        dto.setClientPhone("1234");
        dto.setLocation("Office");
        dto.setService("Consulting");

        assertEquals(1L, dto.getId());
        assertEquals("John", dto.getClientName());
        assertEquals("john@example.com", dto.getClientEmail());
        assertEquals("1234", dto.getClientPhone());
        assertEquals("Office", dto.getLocation());
        assertEquals("Consulting", dto.getService());
    }

    @Test
    public void testTimeSlotMapper() {
        TimeSlot ts = new TimeSlot();
        ts.setId(1L);
        ts.setStartTime(OffsetDateTime.now());
        ts.setEndTime(OffsetDateTime.now().plusHours(1));
        ts.setAvailable(true);

        Appointment appt = new Appointment();
        appt.setId(10L);
        appt.setCustomerName("Jane");
        ts.setAppointments(List.of(appt));

        TimeSlotDTO dto = TimeSlotMapper.toDTO(ts);

        assertEquals(ts.getId(), dto.getId());
        assertEquals(ts.getStartTime(), dto.getStartTime());
        assertEquals(1, dto.getAppointments().size());
        assertEquals("Jane", dto.getAppointments().get(0).getClientName());
    }

    @Test
    public void testMapperEmptyAppointments() {
        TimeSlot ts = new TimeSlot();
        ts.setId(1L);
        ts.setAppointments(List.of());

        TimeSlotDTO dto = TimeSlotMapper.toDTO(ts);
        assertTrue(dto.getAppointments().isEmpty());
    }

    @Test
    public void testAuthRequestDTOs() {
        AuthController.LoginRequest login = new AuthController.LoginRequest();
        login.setUsername("user");
        login.setPassword("pass");
        assertEquals("user", login.getUsername());
        assertEquals("pass", login.getPassword());

        AuthController.RegisterRequest reg = new AuthController.RegisterRequest();
        reg.setUsername("user");
        reg.setPassword("pass");
        reg.setEmail("email");
        reg.setPhone("123");
        assertEquals("user", reg.getUsername());
        assertEquals("pass", reg.getPassword());
        assertEquals("email", reg.getEmail());
        assertEquals("123", reg.getPhone());
    }

    @Test
    public void testAppointmentRequestDTO() {
        AppointmentController.AppointmentRequest req = new AppointmentController.AppointmentRequest();
        req.setCustomerName("Name");
        req.setCustomerEmail("Email");
        req.setCustomerPhone("Phone");
        req.setLocation("Loc");
        req.setService("Serv");
        req.setTimeSlotId(1L);

        assertEquals("Name", req.getCustomerName());
        assertEquals("Email", req.getCustomerEmail());
        assertEquals("Phone", req.getCustomerPhone());
        assertEquals("Loc", req.getLocation());
        assertEquals("Serv", req.getService());
        assertEquals(1L, req.getTimeSlotId());
    }

    @Test
    public void testTimeSlotRequestDTO() {
        TimeSlotController.TimeSlotRequest req = new TimeSlotController.TimeSlotRequest();
        req.setStartTime("start");
        req.setEndTime("end");
        req.setAvailable(true);

        assertEquals("start", req.getStartTime());
        assertEquals("end", req.getEndTime());
        assertTrue(req.isAvailable());
    }
}
