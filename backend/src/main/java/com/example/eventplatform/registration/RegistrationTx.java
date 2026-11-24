package com.example.eventplatform.registration;

import com.example.eventplatform.event.Event;
import com.example.eventplatform.event.EventRepository;
import com.example.eventplatform.registration.Registration.Status;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 1回分のトランザクション。version競合時は例外を投げ、呼び出し側(RegistrationService)がリトライする */
@Service
public class RegistrationTx {
    private final EventRepository events;
    private final RegistrationRepository registrations;

    public RegistrationTx(EventRepository events, RegistrationRepository registrations) {
        this.events = events; this.registrations = registrations;
    }

    @Transactional
    public Registration register(Long eventId, Long userId) {
        Event e = events.findForRegistration(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "event not found"));
        if (!e.getStartAt().isAfter(Instant.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "event already started");
        Registration r = registrations.findByEventIdAndUserId(eventId, userId).orElseGet(() -> new Registration(eventId, userId));
        if (r.getStatus() == Status.CONFIRMED || r.getStatus() == Status.WAITLISTED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "already registered");
        if (e.getConfirmedCount() < e.getCapacity()) {
            r.setStatus(Status.CONFIRMED);
            e.setConfirmedCount(e.getConfirmedCount() + 1);
        } else {
            r.setStatus(Status.WAITLISTED);
        }
        r.setQueueOrder(registrations.maxQueueOrder(eventId) + 1);
        return registrations.saveAndFlush(r);
    }

    @Transactional
    public Registration cancel(Long eventId, Long userId) {
        Event e = events.findForRegistration(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "event not found"));
        Registration r = registrations.findByEventIdAndUserId(eventId, userId)
                .filter(x -> x.getStatus() != Status.CANCELED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "registration not found"));
        boolean freedSeat = r.getStatus() == Status.CONFIRMED;
        r.setStatus(Status.CANCELED);
        if (freedSeat) {
            e.setConfirmedCount(e.getConfirmedCount() - 1);
            // キャンセル待ちの先頭を繰り上げ
            registrations.findFirstByEventIdAndStatusOrderByQueueOrderAsc(eventId, Status.WAITLISTED).ifPresent(w -> {
                w.setStatus(Status.CONFIRMED);
                e.setConfirmedCount(e.getConfirmedCount() + 1);
            });
        }
        return registrations.saveAndFlush(r);
    }
}
