package com.example.booking.repository;

import com.example.booking.model.TimeSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TimeSlotRepository.
 * Tests the custom atomic update query and standard JPA operations.
 */
@DataJpaTest
@ActiveProfiles("test")
public class TimeSlotRepositoryTest {

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    private TimeSlot testSlot;

    @BeforeEach
    void setUp() {
        timeSlotRepository.deleteAll();

        testSlot = new TimeSlot();
        testSlot.setStartTime(OffsetDateTime.now().plusDays(1));
        testSlot.setEndTime(OffsetDateTime.now().plusDays(1).plusHours(1));
        testSlot.setAvailable(true);
    }

    @Test
    void testMarkAsUnavailableIfAvailable_WhenAvailable_ShouldSucceed() {
        TimeSlot saved = timeSlotRepository.save(testSlot);
        assertTrue(saved.isAvailable());

        int updated = timeSlotRepository.markAsUnavailableIfAvailable(saved.getId());

        assertEquals(1, updated, "Should update 1 row");

        TimeSlot reloaded = timeSlotRepository.findById(saved.getId()).orElseThrow();
        assertFalse(reloaded.isAvailable(), "Slot should be marked as unavailable");
    }

    @Test
    void testMarkAsUnavailableIfAvailable_WhenAlreadyUnavailable_ShouldReturnZero() {
        testSlot.setAvailable(false);
        TimeSlot saved = timeSlotRepository.save(testSlot);
        assertFalse(saved.isAvailable());

        int updated = timeSlotRepository.markAsUnavailableIfAvailable(saved.getId());

        assertEquals(0, updated, "Should update 0 rows when already unavailable");

        TimeSlot reloaded = timeSlotRepository.findById(saved.getId()).orElseThrow();
        assertFalse(reloaded.isAvailable());
    }

    @Test
    void testMarkAsUnavailableIfAvailable_WhenSlotNotFound_ShouldReturnZero() {
        int updated = timeSlotRepository.markAsUnavailableIfAvailable(99999L);

        assertEquals(0, updated, "Should update 0 rows when slot doesn't exist");
    }

    @Test
    void testMarkAsUnavailableIfAvailable_IsAtomic() {
        
        TimeSlot saved = timeSlotRepository.save(testSlot);

        int firstUpdate = timeSlotRepository.markAsUnavailableIfAvailable(saved.getId());

        int secondUpdate = timeSlotRepository.markAsUnavailableIfAvailable(saved.getId());

        assertEquals(1, firstUpdate, "First update should succeed");
        assertEquals(0, secondUpdate, "Second update should fail (already unavailable)");
    }

    @Test
    void testMarkAsAvailable() {
        testSlot.setAvailable(false);
        TimeSlot saved = timeSlotRepository.save(testSlot);

        int updated = timeSlotRepository.markAsAvailable(saved.getId());

        assertEquals(1, updated);
        TimeSlot reloaded = timeSlotRepository.findById(saved.getId()).orElseThrow();
        assertTrue(reloaded.isAvailable());
    }

    @Test
    void testMarkAsUnavailableIfAvailable_OnlyAffectsSpecifiedSlot() {
        TimeSlot slot1 = timeSlotRepository.save(testSlot);

        TimeSlot slot2 = new TimeSlot();
        slot2.setStartTime(OffsetDateTime.now().plusDays(2));
        slot2.setEndTime(OffsetDateTime.now().plusDays(2).plusHours(1));
        slot2.setAvailable(true);
        TimeSlot savedSlot2 = timeSlotRepository.save(slot2);

        int updated = timeSlotRepository.markAsUnavailableIfAvailable(slot1.getId());

        assertEquals(1, updated);

        TimeSlot reloadedSlot1 = timeSlotRepository.findById(slot1.getId()).orElseThrow();
        TimeSlot reloadedSlot2 = timeSlotRepository.findById(savedSlot2.getId()).orElseThrow();

        assertFalse(reloadedSlot1.isAvailable(), "Slot 1 should be unavailable");
        assertTrue(reloadedSlot2.isAvailable(), "Slot 2 should still be available");
    }

    @Test
    void testFindByAvailableTrue() {
        timeSlotRepository.save(testSlot); 

        TimeSlot unavailable = new TimeSlot();
        unavailable.setStartTime(OffsetDateTime.now().plusDays(2));
        unavailable.setEndTime(OffsetDateTime.now().plusDays(2).plusHours(1));
        unavailable.setAvailable(false);
        timeSlotRepository.save(unavailable);

        List<TimeSlot> availableSlots = timeSlotRepository.findByAvailableTrue();

        assertEquals(1, availableSlots.size());
        assertTrue(availableSlots.get(0).isAvailable());
    }

    @Test
    void testFindOverlappingSlots() {
        
        // Slot 1: 10:00 - 11:00
        TimeSlot slot1 = new TimeSlot();
        slot1.setStartTime(OffsetDateTime.now().plusDays(1).withHour(10).withMinute(0));
        slot1.setEndTime(OffsetDateTime.now().plusDays(1).withHour(11).withMinute(0));
        timeSlotRepository.save(slot1);

        // Slot 2: 11:00 - 12:00 (Adjacent, not overlapping)
        TimeSlot slot2 = new TimeSlot();
        slot2.setStartTime(OffsetDateTime.now().plusDays(1).withHour(11).withMinute(0));
        slot2.setEndTime(OffsetDateTime.now().plusDays(1).withHour(12).withMinute(0));
        timeSlotRepository.save(slot2);

        // Overlap with Slot 1 only (10:15 - 10:45)
        List<TimeSlot> overlaps = timeSlotRepository.findOverlappingSlots(
                slot1.getStartTime().plusMinutes(15),
                slot1.getEndTime().minusMinutes(15));
        assertEquals(1, overlaps.size());
        assertEquals(slot1.getId(), overlaps.get(0).getId());

        // No overlap (09:00 - 10:00)
        List<TimeSlot> noOverlaps = timeSlotRepository.findOverlappingSlots(
                slot1.getStartTime().minusHours(1),
                slot1.getStartTime());
        assertEquals(0, noOverlaps.size());
    }

    @Test
    void testFindOverlappingSlotsExcluding() {
        TimeSlot slot1 = new TimeSlot();
        slot1.setStartTime(OffsetDateTime.now().plusDays(1).withHour(10).withMinute(0));
        slot1.setEndTime(OffsetDateTime.now().plusDays(1).withHour(11).withMinute(0));
        slot1 = timeSlotRepository.save(slot1);
        
        List<TimeSlot> selfOverlap = timeSlotRepository.findOverlappingSlotsExcluding(
                slot1.getStartTime(),
                slot1.getEndTime(),
                slot1.getId());

        assertEquals(0, selfOverlap.size());

        TimeSlot slot2 = new TimeSlot();
        slot2.setStartTime(OffsetDateTime.now().plusDays(1).withHour(10).withMinute(30));
        slot2.setEndTime(OffsetDateTime.now().plusDays(1).withHour(11).withMinute(30));
        timeSlotRepository.save(slot2);

        List<TimeSlot> otherOverlap = timeSlotRepository.findOverlappingSlotsExcluding(
                slot1.getStartTime(),
                slot1.getEndTime(),
                slot1.getId() 
        );
        assertEquals(1, otherOverlap.size());
    }
}
