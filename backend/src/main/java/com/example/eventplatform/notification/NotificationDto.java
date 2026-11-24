package com.example.eventplatform.notification;

import java.time.Instant;

public record NotificationDto(Long id, Notification.Type type, String message, Long eventId, Instant createdAt, boolean read) {
    static NotificationDto of(Notification n) {
        return new NotificationDto(n.getId(), n.getType(), n.getMessage(), n.getEventId(), n.getCreatedAt(), n.getReadAt() != null);
    }
}
