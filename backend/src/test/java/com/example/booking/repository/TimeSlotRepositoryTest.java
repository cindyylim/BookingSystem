package com.example.booking.repository;

import com.example.booking.model.TimeSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;
<<<<<<< HEAD
=======
import com.example.booking.model.Appointment;
>>>>>>> d4766c3 (Add tests, indexes, and ensure strong consistency when booking)

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

<<<<<<< HEAD
=======
    @Autowired
    private AppointmentRepository appointmentRepository;

>>>>>>> d4766c3 (Add tests, indexes, and ensure strong consistency when booking)
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
        // Arrange
        TimeSlot saved = timeSlotRepository.save(testSlot);
        assertTrue(saved.isAvailable());

        // Act
        int updated = timeSlotRepository.markAsUnavailableIfAvailable(saved.getId());

        // Assert
        assertEquals(1, updated, "Should update 1 row");

        // Verify the slot is now unavailable
        TimeSlot reloaded = timeSlotRepository.findById(saved.getId()).orElseThrow();
        assertFalse(reloaded.isAvailable(), "Slot should be marked as unavailable");
    }

    @Test
    void testMarkAsUnavailableIfAvailable_WhenAlreadyUnavailable_ShouldReturnZero() {
        // Arrange
        testSlot.setAvailable(false);
        TimeSlot saved = timeSlotRepository.save(testSlot);
        assertFalse(saved.isAvailable());

        // Act
        int updated = timeSlotRepository.markAsUnavailableIfAvailable(saved.getId());

        // Assert
        assertEquals(0, updated, "Should update 0 rows when already unavailable");

        // Verify slot is still unavailable
        TimeSlot reloaded = timeSlotRepository.findById(saved.getId()).orElseThrow();
        assertFalse(reloaded.isAvailable());
    }

    @Test
    void testMarkAsUnavailableIfAvailable_WhenSlotNotFound_ShouldReturnZero() {
        // Act
        int updated = timeSlotRepository.markAsUnavailableIfAvailable(99999L);

        // Assert
        assertEquals(0, updated, "Should update 0 rows when slot doesn't exist");
    }

    @Test
    void testMarkAsUnavailableIfAvailable_IsAtomic() {
        // Arrange
        TimeSlot saved = timeSlotRepository.save(testSlot);

        // Act - First call should succeed
        int firstUpdate = timeSlotRepository.markAsUnavailableIfAvailable(saved.getId());

        // Second call should fail (already updated)
        int secondUpdate = timeSlotRepository.markAsUnavailableIfAvailable(saved.getId());

        // Assert
        assertEquals(1, firstUpdate, "First update should succeed");
        assertEquals(0, secondUpdate, "Second update should fail (already unavailable)");
    }

    @Test
    void testMarkAsUnavailableIfAvailable_OnlyAffectsSpecifiedSlot() {
        // Arrange - Create two available slots
        TimeSlot slot1 = timeSlotRepository.save(testSlot);

        TimeSlot slot2 = new TimeSlot();
        slot2.setStartTime(OffsetDateTime.now().plusDays(2));
        slot2.setEndTime(OffsetDateTime.now().plusDays(2).plusHours(1));
        slot2.setAvailable(true);
        TimeSlot savedSlot2 = timeSlotRepository.save(slot2);

        // Act - Mark only slot1 as unavailable
        int updated = timeSlotRepository.markAsUnavailableIfAvailable(slot1.getId());

        // Assert
        assertEquals(1, updated);

        TimeSlot reloadedSlot1 = timeSlotRepository.findById(slot1.getId()).orElseThrow();
        TimeSlot reloadedSlot2 = timeSlotRepository.findById(savedSlot2.getId()).orElseThrow();

        assertFalse(reloadedSlot1.isAvailable(), "Slot 1 should be unavailable");
        assertTrue(reloadedSlot2.isAvailable(), "Slot 2 should still be available");
    }

    @Test
    void testSaveAndRetrieve() {
        // Act
        TimeSlot saved = timeSlotRepository.save(testSlot);

        // Assert
        assertNotNull(saved.getId());

        TimeSlot found = timeSlotRepository.findById(saved.getId()).orElseThrow();
        assertEquals(saved.getId(), found.getId());
        assertTrue(found.isAvailable());
        assertEquals(saved.getStartTime(), found.getStartTime());
        assertEquals(saved.getEndTime(), found.getEndTime());
    }

    @Test
    void testDeleteTimeSlot() {
        // Arrange
        TimeSlot saved = timeSlotRepository.save(testSlot);
        Long id = saved.getId();

        // Act
        timeSlotRepository.deleteById(id);

        // Assert
        assertFalse(timeSlotRepository.existsById(id));
    }

    @Test
    void testFindAllTimeSlots() {
        // Arrange
        timeSlotRepository.save(testSlot);

        TimeSlot slot2 = new TimeSlot();
        slot2.setStartTime(OffsetDateTime.now().plusDays(2));
        slot2.setEndTime(OffsetDateTime.now().plusDays(2).plusHours(1));
        slot2.setAvailable(false);
        timeSlotRepository.save(slot2);

        // Act
        var allSlots = timeSlotRepository.findAll();

        // Assert
        assertEquals(2, allSlots.size());
    }

    @Test
    void testExistsById() {
        // Arrange
        TimeSlot saved = timeSlotRepository.save(testSlot);

        // Act & Assert
        assertTrue(timeSlotRepository.existsById(saved.getId()));
        assertFalse(timeSlotRepository.existsById(99999L));
    }

    @Test
    void testFindByAvailableTrue() {
        // Arrange
        timeSlotRepository.save(testSlot); // Available

        TimeSlot unavailable = new TimeSlot();
        unavailable.setStartTime(OffsetDateTime.now().plusDays(2));
        unavailable.setEndTime(OffsetDateTime.now().plusDays(2).plusHours(1));
        unavailable.setAvailable(false);
        timeSlotRepository.save(unavailable);

        // Act
        List<TimeSlot> availableSlots = timeSlotRepository.findByAvailableTrue();

        // Assert
        assertEquals(1, availableSlots.size());
        assertTrue(availableSlots.get(0).isAvailable());
    }

    @Test
    void testFindOverlappingSlots() {
        // Arrange
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

        // Act & Assert
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
        // Arrange
        TimeSlot slot1 = new TimeSlot();
        slot1.setStartTime(OffsetDateTime.now().plusDays(1).withHour(10).withMinute(0));
        slot1.setEndTime(OffsetDateTime.now().plusDays(1).withHour(11).withMinute(0));
        slot1 = timeSlotRepository.save(slot1);

        // Act
        // Check overlap with itself (should be excluded)
        List<TimeSlot> selfOverlap = timeSlotRepository.findOverlappingSlotsExcluding(
                slot1.getStartTime(),
                slot1.getEndTime(),
                slot1.getId());

        // Assert
        assertEquals(0, selfOverlap.size());

        // Check overlap with another range (should be included if it was another slot,
        // but here we only have one)
        // Let's add another slot to collide with
        TimeSlot slot2 = new TimeSlot();
        slot2.setStartTime(OffsetDateTime.now().plusDays(1).withHour(10).withMinute(30));
        slot2.setEndTime(OffsetDateTime.now().plusDays(1).withHour(11).withMinute(30));
        timeSlotRepository.save(slot2);

        List<TimeSlot> otherOverlap = timeSlotRepository.findOverlappingSlotsExcluding(
                slot1.getStartTime(),
                slot1.getEndTime(),
                slot1.getId() // Exclude slot1, but slot2 overlaps
        );
        assertEquals(1, otherOverlap.size());
    }
<<<<<<< HEAD
=======

    @Test
    void testFindByTime() {
        TimeSlot slot1 = new TimeSlot();
        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1).withHour(10).withMinute(0);
        OffsetDateTime endTime = OffsetDateTime.now().plusDays(1).withHour(11).withMinute(0);
        slot1.setStartTime(startTime);
        slot1.setEndTime(endTime);
        timeSlotRepository.save(slot1);

        TimeSlot ts = timeSlotRepository.findByTime(startTime, endTime);
        assertEquals(slot1.getId(), ts.getId());
    }

    @Test
    void testFindAppointmentsByTimeSlotId() {
        TimeSlot slot1 = new TimeSlot();
        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1).withHour(10).withMinute(0);
        OffsetDateTime endTime = OffsetDateTime.now().plusDays(1).withHour(11).withMinute(0);
        slot1.setStartTime(startTime);
        slot1.setEndTime(endTime);
        timeSlotRepository.save(slot1);

        Appointment appointment = new Appointment();
        appointment.setTimeSlot(slot1);
        appointmentRepository.save(appointment);

        List<Appointment> appointments = timeSlotRepository.findAppointmentsByTimeSlotId(slot1.getId());
        assertEquals(1, appointments.size());
        assertEquals(appointment.getId(), appointments.get(0).getId());
    }
>>>>>>> d4766c3 (Add tests, indexes, and ensure strong consistency when booking)
}
