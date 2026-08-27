package com.example.booking.service;

import com.example.booking.model.Appointment;
import com.example.booking.model.TimeSlot;
import com.example.booking.repository.AppointmentRepository;
import com.example.booking.repository.TimeSlotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AppointmentCancelReleaseSlotTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    void cancelAppointment_MarksSlotAvailableAgain() {
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(40).withNano(0);
        TimeSlot slot = new TimeSlot();
        slot.setStartTime(start);
        slot.setEndTime(start.plusHours(1));
        slot.setAvailable(true);
        slot = timeSlotRepository.save(slot);

        Appointment appointment = new Appointment();
        appointment.setCustomerName("Cancel User");
        appointment.setCustomerEmail("cancel@test.com");
        appointment.setCustomerPhone("555-0100");
        appointment.setLocation("Office");
        appointment.setService("Consult");

        Appointment saved = appointmentService.bookAppointment(appointment, slot.getId(), null);
        assertFalse(timeSlotRepository.findById(slot.getId()).orElseThrow().isAvailable());

        appointmentService.cancelAppointmentByToken(saved.getCancellationToken());

        assertTrue(appointmentRepository.findById(saved.getId()).isEmpty());
        assertTrue(timeSlotRepository.findById(slot.getId()).orElseThrow().isAvailable());
    }
}
