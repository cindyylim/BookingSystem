package com.example.booking.service.impl;

import com.example.booking.exception.ConflictException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.model.TimeSlot;
import com.example.booking.repository.AppointmentRepository;
import com.example.booking.repository.TimeSlotRepository;
import com.example.booking.service.TimeSlotService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

@Service
public class TimeSlotServiceImpl implements TimeSlotService {
    private final TimeSlotRepository timeSlotRepository;
    private final AppointmentRepository appointmentRepository;
    private final TransactionTemplate transactionTemplate;
    private final Object scheduleLock = new Object();

    public TimeSlotServiceImpl(TimeSlotRepository timeSlotRepository,
            AppointmentRepository appointmentRepository,
            PlatformTransactionManager transactionManager) {
        this.timeSlotRepository = timeSlotRepository;
        this.appointmentRepository = appointmentRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public List<TimeSlot> getAllTimeSlots() {
        return timeSlotRepository.findAll();
    }

    @Override
    public List<TimeSlot> getAvailableTimeSlots() {
        return timeSlotRepository.findByAvailableTrue();
    }

    @Override
    public Optional<TimeSlot> getTimeSlot(Long id) {
        return timeSlotRepository.findById(id);
    }

    @Override
    public TimeSlot createTimeSlot(TimeSlot timeSlot) {
        validateTimeSlot(timeSlot);
        synchronized (scheduleLock) {
            return transactionTemplate.execute(status -> {
                assertNoOverlap(timeSlot, null);
                return timeSlotRepository.save(timeSlot);
            });
        }
    }

    @Override
    public TimeSlot updateTimeSlot(Long id, TimeSlot updated) {
        validateTimeSlot(updated);
        synchronized (scheduleLock) {
            return transactionTemplate.execute(status -> {
                TimeSlot existing = timeSlotRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("TimeSlot not found"));
                assertNoOverlap(updated, id);
                existing.setStartTime(updated.getStartTime());
                existing.setEndTime(updated.getEndTime());
                existing.setAvailable(updated.isAvailable());
                return timeSlotRepository.save(existing);
            });
        }
    }

    @Override
    @Transactional
    public void deleteTimeSlot(Long id) {
        TimeSlot slot = timeSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TimeSlot not found"));

        if (!slot.isAvailable() || appointmentRepository.existsByTimeSlot_Id(id)) {
            throw new ConflictException("Cannot delete a time slot that is already booked.");
        }

        timeSlotRepository.deleteById(id);
    }

    private void validateTimeSlot(TimeSlot timeSlot) {
        if (timeSlot.getStartTime() == null || timeSlot.getEndTime() == null) {
            throw new IllegalArgumentException("Start time and end time must not be null.");
        }
        if (!timeSlot.getStartTime().isBefore(timeSlot.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time.");
        }
    }

    private void assertNoOverlap(TimeSlot slot, Long excludeId) {
        List<TimeSlot> conflicts;
        if (excludeId == null) {
            conflicts = timeSlotRepository.findOverlappingSlots(slot.getStartTime(), slot.getEndTime());
        } else {
            conflicts = timeSlotRepository.findOverlappingSlotsExcluding(slot.getStartTime(), slot.getEndTime(),
                    excludeId);
        }
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("Time slot overlaps with an existing slot.");
        }
    }
}
