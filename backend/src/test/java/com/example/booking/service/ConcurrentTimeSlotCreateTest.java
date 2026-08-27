package com.example.booking.service;

import com.example.booking.model.TimeSlot;
import com.example.booking.repository.TimeSlotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class ConcurrentTimeSlotCreateTest {

    @Autowired
    private TimeSlotService timeSlotService;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Test
    public void testConcurrentOverlappingCreates_OnlyOneSucceeds() throws InterruptedException {
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(30).withNano(0);
        OffsetDateTime end = start.plusHours(1);

        int numThreads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    TimeSlot slot = new TimeSlot();
                    slot.setStartTime(start);
                    slot.setEndTime(end);
                    slot.setAvailable(true);
                    timeSlotService.createTimeSlot(slot);
                    successCount.incrementAndGet();
                } catch (IllegalArgumentException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one overlapping create should succeed");
        assertEquals(numThreads - 1, failureCount.get());

        long matching = timeSlotRepository.findAll().stream()
                .filter(s -> start.equals(s.getStartTime()) && end.equals(s.getEndTime()))
                .count();
        assertEquals(1, matching);
    }
}
