package com.example.eventplatform.notification;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    public record ListResponse(List<NotificationDto> items, long unreadCount) {}

    private final NotificationRepository repo;
    private final SseHub hub;
    private final StreamTicketService tickets;

    public NotificationController(NotificationRepository repo, SseHub hub, StreamTicketService tickets) {
        this.repo = repo; this.hub = hub; this.tickets = tickets;
    }

    @GetMapping
    public ListResponse list(Authentication a, @RequestParam(defaultValue = "50") int size) {
        Long uid = (Long) a.getPrincipal();
        var items = repo.findByUserIdOrderByIdDesc(uid, PageRequest.of(0, Math.min(Math.max(size, 1), 100)))
                .stream().map(NotificationDto::of).toList();
        return new ListResponse(items, repo.countByUserIdAndReadAtIsNull(uid));
    }

    @PostMapping("/{id}/read")
    @Transactional
    public void markRead(Authentication a, @PathVariable Long id) {
        Notification n = repo.findById(id).filter(x -> x.getUserId().equals(a.getPrincipal()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (n.getReadAt() == null) n.setReadAt(Instant.now());
    }

    @PostMapping("/read-all")
    @Transactional
    public Map<String, Integer> markAllRead(Authentication a) {
        return Map.of("updated", repo.markAllRead((Long) a.getPrincipal(), Instant.now()));
    }

    /** SSE接続用の使い捨てticketを発行(要JWT) */
    @PostMapping("/stream-ticket")
    public Map<String, String> ticket(Authentication a) {
        return Map.of("ticket", tickets.issue((Long) a.getPrincipal()));
    }

    /** GET /api/notifications/stream?ticket=... (EventSourceで接続) */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String ticket) {
        Long uid = tickets.consume(ticket).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return hub.subscribe(uid);
    }
}
