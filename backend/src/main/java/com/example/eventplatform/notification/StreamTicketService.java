package com.example.eventplatform.notification;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * EventSourceはAuthorizationヘッダを付けられないため、JWTをURLに載せる代わりに
 * 30秒有効・1回限りのticketでSSE接続を認証する。
 */
@Service
public class StreamTicketService {
    private static final long TTL_SECONDS = 30;
    private record Ticket(Long userId, Instant expiresAt) {}
    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String issue(Long userId) {
        byte[] b = new byte[24];
        random.nextBytes(b);
        String t = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        tickets.put(t, new Ticket(userId, Instant.now().plusSeconds(TTL_SECONDS)));
        return t;
    }

    /** 使用と同時に失効する */
    public Optional<Long> consume(String ticket) {
        Ticket t = ticket == null ? null : tickets.remove(ticket);
        return t != null && t.expiresAt().isAfter(Instant.now()) ? Optional.of(t.userId()) : Optional.empty();
    }

    @Scheduled(fixedDelay = 60_000)
    void evictExpired() { tickets.values().removeIf(t -> t.expiresAt().isBefore(Instant.now())); }
}
