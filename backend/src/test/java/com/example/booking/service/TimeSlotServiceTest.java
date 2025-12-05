package com.example.booking.service;

import com.example.booking.model.Appointment;
import com.example.booking.model.TimeSlot;
import com.example.booking.repository.TimeSlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeSlotServiceTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @InjectMocks
    private TimeSlotService timeSlotService;

    @Test
    void getAllTimeSlots() {
        TimeSlot slot = new TimeSlot();
        when(timeSlotRepository.findAll()).thenReturn(List.of(slot));

        List<TimeSlot> result = timeSlotService.getAllTimeSlots();

        assertEquals(1, result.size());
        verify(timeSlotRepository).findAll();
    }

    @Test
    void getAvailableTimeSlots() {
        TimeSlot slot = new TimeSlot();
        slot.setAvailable(true);
        when(timeSlotRepository.findByAvailableTrue()).thenReturn(List.of(slot));

        List<TimeSlot> result = timeSlotService.getAvailableTimeSlots();

        assertEquals(1, result.size());
        verify(timeSlotRepository).findByAvailableTrue();
    }

    @Test
    void getTimeSlot_WhenFound() {
        Long id = 1L;
        TimeSlot slot = new TimeSlot();
        slot.setId(id);
        when(timeSlotRepository.findById(id)).thenReturn(Optional.of(slot));

        Optional<TimeSlot> result = timeSlotService.getTimeSlot(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
    }

    @Test
    void getTimeSlot_WhenNotFound() {
        Long id = 1L;
        when(timeSlotRepository.findById(id)).thenReturn(Optional.empty());

        Optional<TimeSlot> result = timeSlotService.getTimeSlot(id);

        assertTrue(result.isEmpty());
    }

    @Test
    void deleteTimeSlot_Success() {
        Long id = 1L;
        TimeSlot slot = new TimeSlot();
        slot.setId(id);
        slot.setAvailable(true);
        slot.setAppointments(Collections.emptyList());

        when(timeSlotRepository.findById(id)).thenReturn(Optional.of(slot));

        timeSlotService.deleteTimeSlot(id);

        verify(timeSlotRepository).deleteById(id);
    }

    @Test
    void deleteTimeSlot_SuccessNoAppointments() {
        Long id = 1L;
        TimeSlot slot = new TimeSlot();
        slot.setId(id);
        slot.setAvailable(true);
        slot.setAppointments(null);

        when(timeSlotRepository.findById(id)).thenReturn(Optional.of(slot));

        timeSlotService.deleteTimeSlot(id);

        verify(timeSlotRepository).deleteById(id);
    }

    @Test
    void deleteTimeSlot_NotFound() {
        Long id = 1L;
        when(timeSlotRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> timeSlotService.deleteTimeSlot(id));
        verify(timeSlotRepository, never()).deleteById(any());
    }

    @Test
    void deleteTimeSlot_Booked_ThrowsException() {
        Long id = 1L;
        TimeSlot slot = new TimeSlot();
        slot.setId(id);
        slot.setAvailable(false); // Booked

        when(timeSlotRepository.findById(id)).thenReturn(Optional.of(slot));

        assertThrows(IllegalStateException.class, () -> timeSlotService.deleteTimeSlot(id));
        verify(timeSlotRepository, never()).deleteById(any());
    }

    @Test
    void deleteTimeSlot_HasAppointments_ThrowsException() {
        Long id = 1L;
        TimeSlot slot = new TimeSlot();
        slot.setId(id);
        slot.setAvailable(true);
        slot.setAppointments(List.of(new Appointment())); // Has appointments

        when(timeSlotRepository.findById(id)).thenReturn(Optional.of(slot));

        assertThrows(IllegalStateException.class, () -> timeSlotService.deleteTimeSlot(id));
        verify(timeSlotRepository, never()).deleteById(any());
    }

    @Test
    void createTimeSlot_Success() {
        TimeSlot slot = new TimeSlot();
        slot.setStartTime(OffsetDateTime.parse("2023-10-01T10:00:00Z"));
        slot.setEndTime(OffsetDateTime.parse("2023-10-01T11:00:00Z"));

        when(timeSlotRepository.findOverlappingSlots(any(), any())).thenReturn(Collections.emptyList());
        when(timeSlotRepository.save(slot)).thenReturn(slot);

        TimeSlot created = timeSlotService.createTimeSlot(slot);

        assertNotNull(created);
        verify(timeSlotRepository).save(slot);
    }

    @Test
    void createTimeSlot_ValidationFailed_NullTimes() {
        TimeSlot slot = new TimeSlot();
        // Missing start/end times

        assertThrows(IllegalArgumentException.class, () -> timeSlotService.createTimeSlot(slot));
        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    void createTimeSlot_ValidationFailed_StartIsNull() {
        TimeSlot slot = new TimeSlot();
        slot.setStartTime(null);
        slot.setEndTime(OffsetDateTime.parse("2023-10-01T10:00:00Z"));

        assertThrows(IllegalArgumentException.class, () -> timeSlotService.createTimeSlot(slot));
        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    void createTimeSlot_ValidationFailed_EndIsNull() {
        TimeSlot slot = new TimeSlot();
        slot.setStartTime(OffsetDateTime.parse("2023-10-01T11:00:00Z"));
        slot.setEndTime(null);

        assertThrows(IllegalArgumentException.class, () -> timeSlotService.createTimeSlot(slot));
        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    void createTimeSlot_ValidationFailed_EndBeforeStart() {
        TimeSlot slot = new TimeSlot();
        slot.setStartTime(OffsetDateTime.parse("2023-10-01T11:00:00Z"));
        slot.setEndTime(OffsetDateTime.parse("2023-10-01T10:00:00Z"));

        assertThrows(IllegalArgumentException.class, () -> timeSlotService.createTimeSlot(slot));
        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    void createTimeSlot_Conflict() {
        TimeSlot slot = new TimeSlot();
        slot.setStartTime(OffsetDateTime.parse("2023-10-01T10:00:00Z"));
        slot.setEndTime(OffsetDateTime.parse("2023-10-01T11:00:00Z"));

        when(timeSlotRepository.findOverlappingSlots(any(), any())).thenReturn(List.of(new TimeSlot()));

        assertThrows(IllegalArgumentException.class, () -> timeSlotService.createTimeSlot(slot));
        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    void updateTimeSlot_Success() {
        Long id = 1L;
        TimeSlot existing = new TimeSlot();
        existing.setId(id);

        TimeSlot update = new TimeSlot();
        update.setStartTime(OffsetDateTime.parse("2023-10-01T12:00:00Z"));
        update.setEndTime(OffsetDateTime.parse("2023-10-01T13:00:00Z"));
        update.setAvailable(false);

        when(timeSlotRepository.findById(id)).thenReturn(Optional.of(existing));
        when(timeSlotRepository.findOverlappingSlotsExcluding(any(), any(), eq(id)))
                .thenReturn(Collections.emptyList());
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TimeSlot result = timeSlotService.updateTimeSlot(id, update);

        assertEquals(update.getStartTime(), result.getStartTime());
        assertEquals(update.getEndTime(), result.getEndTime());
        assertFalse(result.isAvailable());
        verify(timeSlotRepository).save(existing);
    }

    @Test
    void updateTimeSlot_NotFound() {
        Long id = 1L;
        TimeSlot update = new TimeSlot();
        update.setStartTime(OffsetDateTime.now());
        update.setEndTime(OffsetDateTime.now().plusHours(1));

        when(timeSlotRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> timeSlotService.updateTimeSlot(id, update));
    }

    @Test
    void updateTimeSlot_Conflict() {
        Long id = 1L;
        TimeSlot existing = new TimeSlot();
        existing.setId(id);

        TimeSlot update = new TimeSlot();
        update.setStartTime(OffsetDateTime.parse("2023-10-01T12:00:00Z"));
        update.setEndTime(OffsetDateTime.parse("2023-10-01T13:00:00Z"));

        when(timeSlotRepository.findById(id)).thenReturn(Optional.of(existing));
        when(timeSlotRepository.findOverlappingSlotsExcluding(any(), any(), eq(id)))
                .thenReturn(List.of(new TimeSlot()));

        assertThrows(IllegalArgumentException.class, () -> timeSlotService.updateTimeSlot(id, update));
        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    void isOverlapping_True() {
        TimeSlot slot = new TimeSlot();
        slot.setStartTime(OffsetDateTime.now());
        slot.setEndTime(OffsetDateTime.now().plusHours(1));

        when(timeSlotRepository.findOverlappingSlots(any(), any())).thenReturn(List.of(new TimeSlot()));

        assertTrue(timeSlotService.isOverlapping(slot));
    }

    @Test
    void isOverlapping_False() {
        TimeSlot slot = new TimeSlot();
        slot.setStartTime(OffsetDateTime.now());
        slot.setEndTime(OffsetDateTime.now().plusHours(1));

        when(timeSlotRepository.findOverlappingSlots(any(), any())).thenReturn(Collections.emptyList());

        assertFalse(timeSlotService.isOverlapping(slot));
    }

    @Test
    void isOverlappingWithExclude_True() {
        TimeSlot slot = new TimeSlot();
        slot.setStartTime(OffsetDateTime.now());
        slot.setEndTime(OffsetDateTime.now().plusHours(1));
        Long excludeId = 123L;

        when(timeSlotRepository.findOverlappingSlotsExcluding(any(), any(), eq(excludeId)))
                .thenReturn(List.of(new TimeSlot()));

        assertTrue(timeSlotService.isOverlapping(slot, excludeId));
    }

    @Test
    void isOverlappingWithExclude_False() {
        TimeSlot slot = new TimeSlot();
        slot.setStartTime(OffsetDateTime.now());
        slot.setEndTime(OffsetDateTime.now().plusHours(1));
        Long excludeId = 123L;

        when(timeSlotRepository.findOverlappingSlotsExcluding(any(), any(), eq(excludeId)))
                .thenReturn(List.of());

        assertFalse(timeSlotService.isOverlapping(slot, excludeId));
    }

    @Test
    void isBooked_True() {
        Long id = 1L;
        TimeSlot slot = new TimeSlot();
        slot.setAvailable(false);
        when(timeSlotRepository.findById(id)).thenReturn(Optional.of(slot));

        assertTrue(timeSlotService.isBooked(id));
    }

    @Test
    void isBooked_False_WhenAvailable() {
        Long id = 1L;
        TimeSlot slot = new TimeSlot();
        slot.setAvailable(true);
        when(timeSlotRepository.findById(id)).thenReturn(Optional.of(slot));

        assertFalse(timeSlotService.isBooked(id));
    }

    @Test
    void isBooked_False_WhenNotFound() {
        Long id = 1L;
        when(timeSlotRepository.findById(id)).thenReturn(Optional.empty());

        assertFalse(timeSlotService.isBooked(id));
    }
}
