package com.example.eventplatform.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository repo;
    private final SseHub hub;

    public NotificationService(NotificationRepository repo, SseHub hub) { this.repo = repo; this.hub = hub; }

    /** 保存してから接続中のブラウザにpushする。push失敗は保存に影響しない */
    @Transactional
    public NotificationDto create(Long userId, Notification.Type type, String message, Long eventId) {
        NotificationDto dto = NotificationDto.of(repo.save(new Notification(userId, type, message, eventId)));
        try { hub.push(userId, dto); } catch (RuntimeException e) { log.warn("SSE push failed user={}", userId, e); }
        return dto;
    }
}
