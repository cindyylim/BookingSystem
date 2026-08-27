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
     *
     * @return 1 if this caller reserved the slot, 0 if it was already taken or missing
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TimeSlot t SET t.available = false WHERE t.id = :id AND t.available = true")
    int markAsUnavailableIfAvailable(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TimeSlot t SET t.available = true WHERE t.id = :id")
    int markAsAvailable(@Param("id") Long id);

    List<TimeSlot> findByAvailableTrue();

    @Query("SELECT t FROM TimeSlot t WHERE t.startTime < :end AND t.endTime > :start")
    List<TimeSlot> findOverlappingSlots(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Query("SELECT t FROM TimeSlot t WHERE t.id != :excludeId AND t.startTime < :end AND t.endTime > :start")
    List<TimeSlot> findOverlappingSlotsExcluding(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end,
            @Param("excludeId") Long excludeId);
}
