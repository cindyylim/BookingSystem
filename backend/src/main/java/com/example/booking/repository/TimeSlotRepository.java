package com.example.booking.repository;

import com.example.booking.model.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    /**
     * Atomically marks a time slot as unavailable if it is currently available.
     * This prevents race conditions where two users try to book the same slot.
     * 
     * @param id the time slot ID
     * @return number of rows updated (1 if successful, 0 if slot was already
     *         unavailable)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE TimeSlot t SET t.available = false WHERE t.id = :id AND t.available = true")
    int markAsUnavailableIfAvailable(@Param("id") Long id);

    // Uses idx_timeslot_available
    List<TimeSlot> findByAvailableTrue();

    // Uses idx_timeslot_time_range
    @Query("SELECT t FROM TimeSlot t WHERE t.startTime < :end AND t.endTime > :start")
    List<TimeSlot> findOverlappingSlots(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    // Uses idx_timeslot_time_range
    @Query("SELECT t FROM TimeSlot t WHERE t.id != :excludeId AND t.startTime < :end AND t.endTime > :start")
    List<TimeSlot> findOverlappingSlotsExcluding(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end,
            @Param("excludeId") Long excludeId);
}