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
class ConcurrentTimeSlotUpdateTest {

    @Autowired
    private TimeSlotService timeSlotService;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Test
    void testConcurrentOverlappingUpdates_OnlyOneSucceeds() throws InterruptedException {
        OffsetDateTime base = OffsetDateTime.now(ZoneOffset.UTC).plusDays(50).withNano(0);

        TimeSlot slotA = new TimeSlot();
        slotA.setStartTime(base);
        slotA.setEndTime(base.plusHours(1));
        slotA.setAvailable(true);
        slotA = timeSlotRepository.save(slotA);

        TimeSlot slotB = new TimeSlot();
        slotB.setStartTime(base.plusHours(3));
        slotB.setEndTime(base.plusHours(4));
        slotB.setAvailable(true);
        slotB = timeSlotRepository.save(slotB);

        OffsetDateTime overlapStart = base.plusHours(6);
        OffsetDateTime overlapEnd = overlapStart.plusHours(1);

        Long idA = slotA.getId();
        Long idB = slotB.getId();

        int numThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Long[] ids = { idA, idB };
        for (Long id : ids) {
            executor.submit(() -> {
                try {
                    TimeSlot update = new TimeSlot();
                    update.setStartTime(overlapStart);
                    update.setEndTime(overlapEnd);
                    update.setAvailable(true);
                    timeSlotService.updateTimeSlot(id, update);
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

        assertEquals(1, successCount.get(), "Exactly one overlapping update should succeed");
        assertEquals(1, failureCount.get());
    }
}
