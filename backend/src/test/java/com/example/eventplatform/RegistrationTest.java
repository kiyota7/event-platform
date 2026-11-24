package com.example.eventplatform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.eventplatform.event.Event;
import com.example.eventplatform.event.EventRepository;
import com.example.eventplatform.registration.Registration.Status;
import com.example.eventplatform.registration.RegistrationRepository;
import com.example.eventplatform.registration.RegistrationService;
import com.example.eventplatform.user.User;
import com.example.eventplatform.user.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RegistrationTest {
    @Autowired EventRepository events;
    @Autowired UserRepository users;
    @Autowired RegistrationRepository regs;
    @Autowired RegistrationService service;

    private Event newEvent(int capacity) {
        Event e = new Event(1L);
        e.setTitle("t"); e.setTimeZone("Asia/Tokyo"); e.setCapacity(capacity);
        e.setStartAt(Instant.now().plus(7, ChronoUnit.DAYS)); e.setEndAt(Instant.now().plus(8, ChronoUnit.DAYS));
        return events.save(e);
    }

    private List<Long> newUsers(String prefix, int n) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < n; i++) ids.add(users.save(new User(prefix + i + "@example.com", "x", "u", null)).getId());
        return ids;
    }

    @Test
    void concurrentRegistrationNeverExceedsCapacity() throws Exception {
        Event e = newEvent(5);
        List<Long> ids = newUsers("c", 30);
        ExecutorService pool = Executors.newFixedThreadPool(15);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> fs = new ArrayList<>();
        for (Long uid : ids) fs.add(pool.submit(() -> { start.await(); service.register(e.getId(), uid); return null; }));
        start.countDown();
        for (Future<?> f : fs) f.get(60, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(5, regs.countByEventIdAndStatus(e.getId(), Status.CONFIRMED));
        assertEquals(25, regs.countByEventIdAndStatus(e.getId(), Status.WAITLISTED));
        assertEquals(5, events.findById(e.getId()).orElseThrow().getConfirmedCount());
    }

    @Test
    void cancelPromotesWaitlistInOrder() {
        Event e = newEvent(1);
        List<Long> ids = newUsers("w", 3);
        service.register(e.getId(), ids.get(0));
        service.register(e.getId(), ids.get(1));
        service.register(e.getId(), ids.get(2));
        service.cancel(e.getId(), ids.get(0));

        assertEquals(Status.CONFIRMED, regs.findByEventIdAndUserId(e.getId(), ids.get(1)).orElseThrow().getStatus());
        assertEquals(Status.WAITLISTED, regs.findByEventIdAndUserId(e.getId(), ids.get(2)).orElseThrow().getStatus());
        assertEquals(1, events.findById(e.getId()).orElseThrow().getConfirmedCount());
        // 再申込はキャンセル待ちの末尾になる
        service.register(e.getId(), ids.get(0));
        assertEquals(Status.WAITLISTED, regs.findByEventIdAndUserId(e.getId(), ids.get(0)).orElseThrow().getStatus());
    }
}
