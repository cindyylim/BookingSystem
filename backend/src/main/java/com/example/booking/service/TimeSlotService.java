package com.example.booking.service;

import com.example.booking.model.TimeSlot;

import java.util.List;
import java.util.Optional;

public interface TimeSlotService {
    List<TimeSlot> getAllTimeSlots();

    List<TimeSlot> getAvailableTimeSlots();

    Optional<TimeSlot> getTimeSlot(Long id);

    TimeSlot createTimeSlot(TimeSlot timeSlot);

    TimeSlot updateTimeSlot(Long id, TimeSlot updated);

    void deleteTimeSlot(Long id);
}
