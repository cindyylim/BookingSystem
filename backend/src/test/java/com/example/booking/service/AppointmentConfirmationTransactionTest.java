package com.example.booking.service;

import com.example.booking.model.Appointment;
import com.example.booking.model.TimeSlot;
import com.example.booking.repository.AppointmentRepository;
import com.example.booking.repository.TimeSlotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class AppointmentConfirmationTransactionTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockBean
    private AppointmentNotificationService notificationService;

    @Test
    void confirmationEmailSentAfterSuccessfulCommit() {
        TimeSlot slot = saveSlot(41);
        appointmentService.bookAppointment(sampleAppointment(), slot.getId(), null);
        verify(notificationService).sendAppointmentConfirmation(any(Appointment.class));
    }

    @Test
    void confirmationEmailNotSentWhenOuterTransactionRollsBack() {
        TimeSlot slot = saveSlot(42);
        TransactionTemplate template = new TransactionTemplate(transactionManager);

        assertThrows(IllegalStateException.class, () -> template.executeWithoutResult(status -> {
            appointmentService.bookAppointment(sampleAppointment(), slot.getId(), null);
            verify(notificationService, never()).sendAppointmentConfirmation(any());
            throw new IllegalStateException("force rollback");
        }));

        verify(notificationService, never()).sendAppointmentConfirmation(any());
        assertTrue(appointmentRepository.findAll().stream()
                .noneMatch(a -> a.getTimeSlot() != null && slot.getId().equals(a.getTimeSlot().getId())));
    }

    private TimeSlot saveSlot(int plusDays) {
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(plusDays).withNano(0);
        TimeSlot slot = new TimeSlot();
        slot.setStartTime(start);
        slot.setEndTime(start.plusHours(1));
        slot.setAvailable(true);
        return timeSlotRepository.save(slot);
    }

    private Appointment sampleAppointment() {
        Appointment appointment = new Appointment();
        appointment.setCustomerName("Txn User");
        appointment.setCustomerEmail("txn@test.com");
        appointment.setCustomerPhone("555-0200");
        appointment.setLocation("Office");
        appointment.setService("Consult");
        return appointment;
    }
}
