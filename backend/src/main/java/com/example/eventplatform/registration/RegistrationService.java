package com.example.eventplatform.registration;

import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** 楽観的ロック競合時に最大MAX_ATTEMPTS回リトライするファサード */
@Service
public class RegistrationService {
    static final int MAX_ATTEMPTS = 10;
    private final RegistrationTx tx;

    public RegistrationService(RegistrationTx tx) { this.tx = tx; }

    public Registration register(Long eventId, Long userId) { return retry(() -> tx.register(eventId, userId)); }
    public Registration cancel(Long eventId, Long userId) { return retry(() -> tx.cancel(eventId, userId)); }

    private Registration retry(Supplier<Registration> action) {
        for (int attempt = 1; ; attempt++) {
            try {
                return action.get();
            } catch (OptimisticLockingFailureException | DataIntegrityViolationException ex) {
                if (attempt >= MAX_ATTEMPTS)
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "conflict, please retry");
                try { Thread.sleep((long) (Math.random() * 10 * attempt)); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw ex; }
            }
        }
    }
}
