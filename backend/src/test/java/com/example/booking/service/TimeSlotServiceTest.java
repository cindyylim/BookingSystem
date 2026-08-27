package com.example.booking.service;

import com.example.booking.exception.ConflictException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.model.TimeSlot;
import com.example.booking.repository.AppointmentRepository;
import com.example.booking.repository.TimeSlotRepository;
import com.example.booking.service.impl.TimeSlotServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeSlotServiceTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    private TimeSlotServiceImpl timeSlotService;

    @BeforeEach
    void setUp() {
        PlatformTransactionManager transactionManager = new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        };
        timeSlotService = new TimeSlotServiceImpl(timeSlotRepository, appointmentRepository, transactionManager);
    }

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

        when(timeSlotRepository.findById(id)).thenReturn(Optional.of(slot));
        when(appointmentRepository.existsByTimeSlot_Id(id)).thenReturn(false);

        timeSlotService.deleteTimeSlot(id);

        verify(timeSlotRepository).deleteById(id);
    }

    @Test
    void deleteTimeSlot_NotFound() {
        Long id = 1L;
        when(timeSlotRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> timeSlotService.deleteTimeSlot(id));
        verify(timeSlotRepository, never()).deleteById(any());
    }

    @Test
    void deleteTimeSlot_Booked_ThrowsException() {
        Long id = 1L;
        TimeSlot slot = new TimeSlot();
        slot.setId(id);
        slot.setAvailable(false);

        when(timeSlotRepository.findById(id)).thenReturn(Optional.of(slot));

        assertThrows(ConflictException.class, () -> timeSlotService.deleteTimeSlot(id));
        verify(timeSlotRepository, never()).deleteById(any());
    }

    @Test
    void deleteTimeSlot_HasAppointments_ThrowsException() {
        Long id = 1L;
        TimeSlot slot = new TimeSlot();
        slot.setId(id);
        slot.setAvailable(true);

        when(timeSlotRepository.findById(id)).thenReturn(Optional.of(slot));
        when(appointmentRepository.existsByTimeSlot_Id(id)).thenReturn(true);

        assertThrows(ConflictException.class, () -> timeSlotService.deleteTimeSlot(id));
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

        assertThrows(ResourceNotFoundException.class, () -> timeSlotService.updateTimeSlot(id, update));
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
    void createTimeSlot_ConcurrentOverlapping_SerializedByLock() throws Exception {
        TimeSlot first = new TimeSlot();
        first.setStartTime(OffsetDateTime.parse("2023-10-01T10:00:00Z"));
        first.setEndTime(OffsetDateTime.parse("2023-10-01T11:00:00Z"));
        TimeSlot second = new TimeSlot();
        second.setStartTime(OffsetDateTime.parse("2023-10-01T10:30:00Z"));
        second.setEndTime(OffsetDateTime.parse("2023-10-01T11:30:00Z"));

        CountDownLatch overlapLookupStarted = new CountDownLatch(1);
        CountDownLatch allowOverlapLookupToFinish = new CountDownLatch(1);
        AtomicInteger overlapLookups = new AtomicInteger();

        when(timeSlotRepository.findOverlappingSlots(any(), any())).thenAnswer(invocation -> {
            int n = overlapLookups.incrementAndGet();
            if (n == 1) {
                overlapLookupStarted.countDown();
                if (!allowOverlapLookupToFinish.await(2, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("timed out waiting to finish overlap lookup");
                }
                return Collections.emptyList();
            }
            return List.of(first);
        });
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();

        executor.submit(() -> {
            try {
                timeSlotService.createTimeSlot(first);
                success.incrementAndGet();
            } catch (Exception e) {
                failure.incrementAndGet();
            } finally {
                done.countDown();
            }
        });
        executor.submit(() -> {
            try {
                if (!overlapLookupStarted.await(2, TimeUnit.SECONDS)) {
                    failure.incrementAndGet();
                    return;
                }
                timeSlotService.createTimeSlot(second);
                success.incrementAndGet();
            } catch (Exception e) {
                failure.incrementAndGet();
            } finally {
                done.countDown();
            }
        });

        // If the lock is held, the second create waits until the first lookup+save finishes.
        allowOverlapLookupToFinish.countDown();
        done.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(1, success.get());
        assertEquals(1, failure.get());
    }

    @Test
    void updateTimeSlot_ConcurrentOverlapping_SerializedByLock() throws Exception {
        TimeSlot existingA = new TimeSlot();
        existingA.setId(1L);
        existingA.setStartTime(OffsetDateTime.parse("2023-10-01T08:00:00Z"));
        existingA.setEndTime(OffsetDateTime.parse("2023-10-01T09:00:00Z"));
        TimeSlot existingB = new TimeSlot();
        existingB.setId(2L);
        existingB.setStartTime(OffsetDateTime.parse("2023-10-01T12:00:00Z"));
        existingB.setEndTime(OffsetDateTime.parse("2023-10-01T13:00:00Z"));

        TimeSlot updateA = new TimeSlot();
        updateA.setStartTime(OffsetDateTime.parse("2023-10-01T10:00:00Z"));
        updateA.setEndTime(OffsetDateTime.parse("2023-10-01T11:00:00Z"));
        TimeSlot updateB = new TimeSlot();
        updateB.setStartTime(OffsetDateTime.parse("2023-10-01T10:30:00Z"));
        updateB.setEndTime(OffsetDateTime.parse("2023-10-01T11:30:00Z"));

        CountDownLatch overlapLookupStarted = new CountDownLatch(1);
        CountDownLatch allowOverlapLookupToFinish = new CountDownLatch(1);
        AtomicInteger overlapLookups = new AtomicInteger();

        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(existingA));
        when(timeSlotRepository.findById(2L)).thenReturn(Optional.of(existingB));
        when(timeSlotRepository.findOverlappingSlotsExcluding(any(), any(), any())).thenAnswer(invocation -> {
            int n = overlapLookups.incrementAndGet();
            if (n == 1) {
                overlapLookupStarted.countDown();
                if (!allowOverlapLookupToFinish.await(2, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("timed out waiting to finish overlap lookup");
                }
                return Collections.emptyList();
            }
            return List.of(existingA);
        });
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();

        executor.submit(() -> {
            try {
                timeSlotService.updateTimeSlot(1L, updateA);
                success.incrementAndGet();
            } catch (Exception e) {
                failure.incrementAndGet();
            } finally {
                done.countDown();
            }
        });
        executor.submit(() -> {
            try {
                if (!overlapLookupStarted.await(2, TimeUnit.SECONDS)) {
                    failure.incrementAndGet();
                    return;
                }
                timeSlotService.updateTimeSlot(2L, updateB);
                success.incrementAndGet();
            } catch (Exception e) {
                failure.incrementAndGet();
            } finally {
                done.countDown();
            }
        });

        allowOverlapLookupToFinish.countDown();
        done.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(1, success.get());
        assertEquals(1, failure.get());
    }
}
